/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.metrics.filters;

import com.kumuluz.ee.metrics.producers.MetricRegistryProducer;
import org.eclipse.microprofile.metrics.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Filter used for web instrumentation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class InstrumentedFilter implements Filter {

    public static final String PARAM_INSTRUMENTATION_NAME = InstrumentedFilter.class.getName() + ".instrumentationName";
    public static final String PARAM_METER_STATUS_CODES = InstrumentedFilter.class.getName() + ".meterStatusCodes";

    private ConcurrentMap<Integer, Meter> metersByStatusCode;
    private Meter otherMeter;
    private Meter timeoutsMeter;
    private Meter errorsMeter;
    private ConcurrentGauge activeRequests;
    private Timer requestTimer;

    @Override
    public void init(FilterConfig filterConfig) {

        String instrumentationName = filterConfig.getInitParameter(PARAM_INSTRUMENTATION_NAME);
        List<Integer> meterStatusCodes = Arrays.stream(
                filterConfig.getInitParameter(PARAM_METER_STATUS_CODES).split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        MetricRegistry metricsRegistry = MetricRegistryProducer.getVendorRegistry();

        String metricPrefix = "web-instrumentation." + instrumentationName;

        this.metersByStatusCode = new ConcurrentHashMap<>(meterStatusCodes.size());
        for (Integer sc : meterStatusCodes) {
            Metadata meterMetadata = Metadata.builder()
                    .withName(MetricRegistry.name(metricPrefix, "status"))
                    .withDisplayName("Response status codes on " + instrumentationName)
                    .withDescription("Number of responses with status codes on " + instrumentationName)
                    .withType(MetricType.METERED)
                    .build();
            metersByStatusCode.put(sc,
                    metricsRegistry.meter(meterMetadata, new Tag("statusCode", sc.toString())));
        }

        Metadata meterMetadata = Metadata.builder()
                .withName(MetricRegistry.name(metricPrefix, "status"))
                .withDisplayName("Response other status codes on " + instrumentationName)
                .withDescription("Number of responses with other status codes on " + instrumentationName)
                .withType(MetricType.METERED)
                .build();
        this.otherMeter = metricsRegistry.meter(meterMetadata, new Tag("statusCode", "other"));

        Metadata timeoutsMetadata = Metadata.builder()
                .withName(MetricRegistry.name(metricPrefix, "timeouts"))
                .withDisplayName("Timeouts on " + instrumentationName)
                .withDescription("Number of timeouts on " + instrumentationName)
                .withType(MetricType.METERED)
                .build();
        this.timeoutsMeter = metricsRegistry.meter(timeoutsMetadata);

        Metadata errorsMetadata = Metadata.builder()
                .withName(MetricRegistry.name(metricPrefix, "errors"))
                .withDisplayName("Errors on " + instrumentationName)
                .withDescription("Number of errors on " + instrumentationName)
                .withType(MetricType.METERED)
                .build();
        this.errorsMeter = metricsRegistry.meter(errorsMetadata);

        Metadata activeRequestsMetadata = Metadata.builder()
                .withName(MetricRegistry.name(metricPrefix, "activeRequests"))
                .withDisplayName("Active requests on " + instrumentationName)
                .withDescription("Number of active requests on " + instrumentationName)
                .withType(MetricType.CONCURRENT_GAUGE)
                .build();
        this.activeRequests = metricsRegistry.concurrentGauge(activeRequestsMetadata);

        Metadata timerMetadata = Metadata.builder()
                .withName(MetricRegistry.name(metricPrefix, "response"))
                .withDisplayName(instrumentationName + " response timer")
                .withDescription("Response timer for " + instrumentationName)
                .withType(MetricType.TIMER)
                .withUnit(MetricUnits.NANOSECONDS)
                .build();
        this.requestTimer = metricsRegistry.timer(timerMetadata);

    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        final StatusExposingServletResponse wrappedResponse =
                new StatusExposingServletResponse((HttpServletResponse) response);
        activeRequests.inc();
        final Timer.Context context = requestTimer.time();
        boolean error = false;
        try {
            chain.doFilter(request, wrappedResponse);
        } catch (IOException | RuntimeException | ServletException e) {
            error = true;
            throw e;
        } finally {
            if (!error && request.isAsyncStarted()) {
                request.getAsyncContext().addListener(new AsyncResultListener(context));
            } else {
                context.stop();
                activeRequests.dec();
                if (error) {
                    errorsMeter.mark();
                } else {
                    markMeterForStatusCode(wrappedResponse.getStatus());
                }
            }
        }
    }

    private void markMeterForStatusCode(int status) {
        final Meter metric = metersByStatusCode.get(status);
        if (metric != null) {
            metric.mark();
        } else {
            otherMeter.mark();
        }
    }

    private class AsyncResultListener implements AsyncListener {
        private Timer.Context context;
        private boolean done = false;

        AsyncResultListener(Timer.Context context) {
            this.context = context;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            if (!done) {
                HttpServletResponse suppliedResponse = (HttpServletResponse) event.getSuppliedResponse();
                context.stop();
                activeRequests.dec();
                markMeterForStatusCode(suppliedResponse.getStatus());
            }
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            context.stop();
            activeRequests.dec();
            timeoutsMeter.mark();
            done = true;
        }

        @Override
        public void onError(AsyncEvent event) {
            context.stop();
            activeRequests.dec();
            errorsMeter.mark();
            done = true;
        }

        @Override
        public void onStartAsync(AsyncEvent event) {

        }
    }
}

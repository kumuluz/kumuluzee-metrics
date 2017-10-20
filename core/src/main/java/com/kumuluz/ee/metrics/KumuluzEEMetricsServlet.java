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
package com.kumuluz.ee.metrics;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.metrics.json.MetricsModule;
import com.kumuluz.ee.metrics.prometheus.PrometheusMetricWriter;
import com.kumuluz.ee.metrics.utils.RequestInfo;
import com.kumuluz.ee.metrics.utils.ServiceConfigInfo;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Servlet, which exposes metrics in JSON and Prometheus format.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class KumuluzEEMetricsServlet extends HttpServlet {

    private static final String DEBUG_KEY = "kumuluzee.debug";

    private ObjectMapper metricMapper;
    private ObjectMapper metadataMapper;

    private boolean jsonServletEnabled;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.metricMapper = new ObjectMapper().registerModule(new MetricsModule(false));
        this.metadataMapper = new ObjectMapper().registerModule(new MetricsModule(true));

        this.jsonServletEnabled = true;
        if(!"dev".equalsIgnoreCase(ServiceConfigInfo.getInstance().getEnvironment())) {
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
            this.jsonServletEnabled = configurationUtil.getBoolean(DEBUG_KEY).orElse(false);
            configurationUtil.subscribe(DEBUG_KEY, (String key, String value) -> {
                if (DEBUG_KEY.equals(key)) {
                    jsonServletEnabled = "true".equalsIgnoreCase(value.toLowerCase());
                }
            });
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        RequestInfo requestInfo = new RequestInfo(request);

        if(requestInfo.getRequestType() == RequestInfo.RequestType.INVALID ||
                !jsonServletEnabled && (
                requestInfo.getRequestType() == RequestInfo.RequestType.JSON_METADATA ||
                requestInfo.getRequestType() == RequestInfo.RequestType.JSON_METRIC)) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }

        if(requestInfo.getRequestType() != RequestInfo.RequestType.INVALID) {

            switch (requestInfo.getRequestType()) {
                case JSON_METRIC:
                case JSON_METADATA:
                    response.setContentType(MediaType.APPLICATION_JSON);
                    break;
                case PROMETHEUS:
                    response.setContentType("text/plain; version=0.0.4; charset=utf-8");
                    break;
            }

            switch (requestInfo.getMetricsRequested()) {
                case NOT_FOUND:
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                case NO_CONTENT:
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
            }

            response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");

            if(requestInfo.getRequestType() == RequestInfo.RequestType.PROMETHEUS) {
                PrintWriter writer = response.getWriter();
                PrometheusMetricWriter prometheusMetricWriter = new PrometheusMetricWriter(writer);

                try {
                    switch (requestInfo.getMetricsRequested()) {
                        case ALL:
                            prometheusMetricWriter.write(requestInfo.getRequestedRegistries());
                            break;
                        case REGISTRY:
                            prometheusMetricWriter.write(requestInfo.getSingleRequestedRegistryName(),
                                    requestInfo.getSingleRequestedRegistry());
                            break;
                        case METRIC:
                            prometheusMetricWriter.write(requestInfo.getSingleRequestedRegistryName(),
                                    requestInfo.getSingleRequestedRegistry(), requestInfo.getMetricName());
                            break;
                    }
                } catch (IOException e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error exporting Prometheus metrics.");
                }

                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                Object value = null;
                switch (requestInfo.getMetricsRequested()) {
                    case ALL:
                        value = requestInfo.getRequestedRegistries();
                        break;
                    case REGISTRY:
                        value = requestInfo.getSingleRequestedRegistry();
                        break;
                    case METRIC:
                        if(requestInfo.getRequestType() == RequestInfo.RequestType.JSON_METADATA) {
                            value = Collections.singletonMap(requestInfo.getMetricName(), requestInfo.getMetadata());
                        } else {
                            value = Collections.singletonMap(requestInfo.getMetricName(), requestInfo.getMetric());
                        }
                        break;
                }
                try (ServletOutputStream output = response.getOutputStream()) {
                    this.getWriter(request, requestInfo.getRequestType()).writeValue(output, value);
                    response.setStatus(HttpServletResponse.SC_OK);
                } catch (Exception e) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error exporting JSON metrics.");
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private ObjectWriter getWriter(HttpServletRequest request, RequestInfo.RequestType requestType) {
        boolean prettyPrintOff = "false".equals(request.getParameter("pretty"));
        ObjectMapper mapper = (requestType == RequestInfo.RequestType.JSON_METADATA) ? this.metadataMapper : this.metricMapper;

        return prettyPrintOff ? mapper.writer() : mapper.writerWithDefaultPrettyPrinter();
    }
}

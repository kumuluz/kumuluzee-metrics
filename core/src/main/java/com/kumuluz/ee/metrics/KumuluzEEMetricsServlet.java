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

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.kumuluz.ee.metrics.json.ServletExportModule;
import com.kumuluz.ee.metrics.json.models.ServletExport;
import com.kumuluz.ee.metrics.utils.EnabledRegistries;
import com.kumuluz.ee.metrics.utils.KumuluzEEMetricRegistries;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Servlet, which exposes metrics.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class KumuluzEEMetricsServlet extends HttpServlet {

    private static final String RATE_UNIT = KumuluzEEMetricsServlet.class.getCanonicalName() + ".rateUnit";
    private static final String DURATION_UNIT = KumuluzEEMetricsServlet.class.getCanonicalName() + ".durationUnit";
    private static final String SHOW_SAMPLES = KumuluzEEMetricsServlet.class.getCanonicalName() + ".showSamples";
    private static final String METRIC_FILTER = KumuluzEEMetricsServlet.class.getCanonicalName() + ".metricFilter";
    private static final String CONTENT_TYPE = "application/json";
    private static final String JSON_ID_PARAM = "id";
    private static final String JSON_ENABLE_PARAM = "enable";
    private static final String JSON_DISABLE_PARAM = "disable";

    private transient ObjectMapper mapper;

    private EnabledRegistries enabledRegistries;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        TimeUnit rateUnit = this.parseTimeUnit(context.getInitParameter(RATE_UNIT), TimeUnit.SECONDS);
        TimeUnit durationUnit = this.parseTimeUnit(context.getInitParameter(DURATION_UNIT), TimeUnit.SECONDS);
        boolean showSamples = Boolean.parseBoolean(context.getInitParameter(SHOW_SAMPLES));
        MetricFilter filter = (MetricFilter) context.getAttribute(METRIC_FILTER);
        if (filter == null) {
            filter = MetricFilter.ALL;
        }

        enabledRegistries = EnabledRegistries.getInstance();

        this.mapper = (new ObjectMapper()).registerModule(new ServletExportModule(rateUnit, durationUnit, showSamples,
                filter));
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(CONTENT_TYPE);

        response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
        response.setStatus(200);

        try (ServletOutputStream output = response.getOutputStream()) {
            String[] registryIds = request.getParameterValues(JSON_ID_PARAM);
            String[] enableRegistries = request.getParameterValues(JSON_ENABLE_PARAM);
            String[] disableRegistries = request.getParameterValues(JSON_DISABLE_PARAM);

            if (disableRegistries != null) {
                for (String registry : disableRegistries) {
                    enabledRegistries.disableRegistry(registry);
                }
            }
            if (enableRegistries != null) {
                for (String registry : enableRegistries) {
                    enabledRegistries.enableRegistry(registry);
                }
            }

            Map<String, MetricRegistry> registries = new HashMap<>();

            Set<String> registriesToSend = new HashSet<>(KumuluzEEMetricRegistries.names());
            registriesToSend.retainAll(enabledRegistries.getEnabledRegistries());
            if (registryIds != null) {
                registriesToSend.retainAll(new HashSet<>(Arrays.asList(registryIds)));
            }

            for (String registryName : registriesToSend) {
                registries.put(registryName, KumuluzEEMetricRegistries.getOrCreate(registryName));
            }

            ServletExport servletExport = new ServletExport(KumuluzEEMetricRegistries.names(), registries);
            this.getWriter(request).writeValue(output, servletExport);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Something went wrong.");
        }
    }

    private ObjectWriter getWriter(HttpServletRequest request) {
        boolean prettyPrintOff = "false".equals(request.getParameter("pretty"));
        return prettyPrintOff ? this.mapper.writer() : this.mapper.writerWithDefaultPrettyPrinter();
    }

    private TimeUnit parseTimeUnit(String value, TimeUnit defaultValue) {
        try {
            return TimeUnit.valueOf(String.valueOf(value).toUpperCase(Locale.US));
        } catch (IllegalArgumentException var4) {
            return defaultValue;
        }
    }
}

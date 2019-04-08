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
package com.kumuluz.ee.metrics.utils;

import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.metrics.producers.MetricRegistryProducer;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for metric servlet requests.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class RequestInfo {

    private static final String APPLICATION_JSON = "application/json";

    private String servletEndpoint;

    private Map<String, MetricRegistry> requestedRegistries;
    private String metricName;
    private Metric metric;
    private Metadata metadata;
    private MetricsRequested metricsRequested;
    private RequestType requestType;

    public enum RequestType {
        JSON_METRIC,
        JSON_METADATA,
        PROMETHEUS,
        INVALID
    }

    public enum MetricsRequested {
        ALL,
        REGISTRY,
        METRIC,
        NOT_FOUND,
        NO_CONTENT
    }

    public RequestInfo(HttpServletRequest request) {
        this.servletEndpoint = request.getServletPath();

        this.requestedRegistries = new HashMap<>();
        this.metricName = null;
        this.metric = null;
        this.metadata = null;

        this.requestType = determineRequestType(request);
        if (this.requestType != RequestType.INVALID) {
            determineRequestedMetrics(request.getRequestURI());
        }
    }

    private RequestType determineRequestType(HttpServletRequest request) {
        if (request.getHeader("Accept") != null && request.getHeader("Accept").equals(APPLICATION_JSON)) {
            if (request.getMethod().equals("GET")) {
                return RequestType.JSON_METRIC;
            } else if (request.getMethod().equals("OPTIONS")) {
                return RequestType.JSON_METADATA;
            } else {
                return RequestType.INVALID;
            }
        } else {
            if (request.getMethod().equals("GET")) {
                return RequestType.PROMETHEUS;
            } else {
                return RequestType.INVALID;
            }
        }
    }

    private void determineRequestedMetrics(String uri) {
        // remove servlet endpoint from uri to get REST parts
        int contextPathLength = EeConfig.getInstance().getServer().getContextPath().length();
        if (contextPathLength == 1) contextPathLength = 0;

        String[] splittedUri = uri
                .substring(contextPathLength + servletEndpoint.length())
                .split("/");

        metricsRequested = MetricsRequested.ALL;
        if (splittedUri.length > 1) {
            metricsRequested = MetricsRequested.REGISTRY;
            MetricRegistry registry = parseRegistry(splittedUri[1]);
            if (registry == null) {
                metricsRequested = MetricsRequested.NOT_FOUND;
                return;
            }
            if (registry.getMetrics().size() == 0) {
                metricsRequested = MetricsRequested.NO_CONTENT;
                return;
            }
            requestedRegistries.put(splittedUri[1], registry);
        }
        if (splittedUri.length > 2 && getSingleRequestedRegistry() != null) {
            metricsRequested = MetricsRequested.METRIC;
            metricName = splittedUri[2];
            if (requestType.equals(RequestType.JSON_METADATA)) {
                metadata = getSingleRequestedRegistry().getMetadata().get(metricName);
                if (metadata == null) {
                    metricsRequested = MetricsRequested.NOT_FOUND;
                    return;
                }
            } else {
                metric = getSingleRequestedRegistry().getMetrics().get(metricName);
                if (metric == null) {
                    metricsRequested = MetricsRequested.NOT_FOUND;
                    return;
                }
            }
        }

        if (metricsRequested == MetricsRequested.ALL) {
            if (MetricRegistryProducer.getApplicationRegistry().getMetrics().size() > 0) {
                requestedRegistries.put("application", MetricRegistryProducer.getApplicationRegistry());
            }
            if (MetricRegistryProducer.getBaseRegistry().getMetrics().size() > 0) {
                requestedRegistries.put("base", MetricRegistryProducer.getBaseRegistry());
            }
            if (MetricRegistryProducer.getVendorRegistry().getMetrics().size() > 0) {
                requestedRegistries.put("vendor", MetricRegistryProducer.getVendorRegistry());
            }

            if (requestedRegistries.size() == 0) {
                metricsRequested = MetricsRequested.NO_CONTENT;
            }
        }
    }

    private MetricRegistry parseRegistry(String name) {
        if ("application".equals(name)) {
            return MetricRegistryProducer.getApplicationRegistry();
        } else if ("base".equals(name)) {
            return MetricRegistryProducer.getBaseRegistry();
        } else if ("vendor".equals(name)) {
            return MetricRegistryProducer.getVendorRegistry();
        }

        return null;
    }

    public Map<String, MetricRegistry> getRequestedRegistries() {
        return requestedRegistries;
    }

    public MetricRegistry getSingleRequestedRegistry() {
        if (requestedRegistries.size() == 1) {
            return requestedRegistries.values().iterator().next();
        } else {
            return null;
        }
    }

    public String getSingleRequestedRegistryName() {
        if (requestedRegistries.size() == 1) {
            return requestedRegistries.keySet().iterator().next();
        } else {
            return null;
        }
    }

    public String getMetricName() {
        return metricName;
    }

    public Metric getMetric() {
        return metric;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public MetricsRequested getMetricsRequested() {
        return metricsRequested;
    }

    public RequestType getRequestType() {
        return requestType;
    }
}

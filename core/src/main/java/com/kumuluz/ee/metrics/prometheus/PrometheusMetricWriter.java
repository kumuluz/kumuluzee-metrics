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
package com.kumuluz.ee.metrics.prometheus;

import org.eclipse.microprofile.metrics.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Prometheus metric writer.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class PrometheusMetricWriter {

    private final static String APPENDEDSECONDS = "_seconds";
    private final static String APPENDEDBYTES = "_bytes";
    private final static String APPENDEDPERCENT = "_percent";

    private static final Logger log = Logger.getLogger(PrometheusMetricWriter.class.getName());

    private final Writer writer;
    private final String additionalTags;

    public PrometheusMetricWriter(Writer writer, String additionalTags) {
        this.writer = writer;
        this.additionalTags = additionalTags;
    }

    public void write(Map<String, MetricRegistry> metricRegistries) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, MetricRegistry> entry : metricRegistries.entrySet()) {
            writeMetricsAsPrometheus(builder, entry.getKey(), entry.getValue());
        }
        serialize(builder);
    }

    public void write(String registryName, MetricRegistry registry) throws IOException {
        StringBuilder builder = new StringBuilder();
        writeMetricsAsPrometheus(builder, registryName, registry);
        serialize(builder);
    }

    public void write(StringBuilder builder, String registryName, MetricRegistry registry, MetricID metricName) throws IOException {
        writeMetricsAsPrometheus(builder, registryName, registry, metricName);
    }

    private void writeMetricsAsPrometheus(StringBuilder builder, String registryName, MetricRegistry registry) {
        writeMetricMapAsPrometheus(builder, registryName, registry.getMetrics(), registry.getMetadata());
    }

    private void writeMetricsAsPrometheus(StringBuilder builder, String registryName, MetricRegistry registry,
                                          MetricID metricName) {
        writeMetricMapAsPrometheus(builder, registryName,
                Collections.singletonMap(metricName, registry.getMetrics().get(metricName)), registry.getMetadata());
    }

    private void writeMetricMapAsPrometheus(StringBuilder builder, String registryName, Map<MetricID, Metric> metricMap,
                                            Map<String, Metadata> metricMetadataMap) {
        for (Map.Entry<MetricID, Metric> entry : metricMap.entrySet()) {
            String metricNamePrometheus = registryName + "_" + entry.getKey().getName();
            Metric metric = entry.getValue();
            MetricID entryName = entry.getKey();

            //description
            Metadata metricMetaData = metricMetadataMap.get(entryName.getName());

            String description;

            if (metricMetaData.description().isEmpty() || metricMetaData.description().get().trim().isEmpty()) {
                description = "";
            } else {
                description = metricMetaData.description().get().trim();
            }

            String tags = entryName.getTagsAsString();
            if (tags == null || tags.isEmpty()) {
                tags = additionalTags;
            } else if (!additionalTags.isEmpty()) {
                tags += "," + additionalTags;
            }

            //appending unit to the metric name
            String unit = metricMetaData.getUnit();

            //Unit determination / translation
            double conversionFactor;
            String appendUnit;

            if (unit == null || unit.trim().isEmpty() || unit.equals(MetricUnits.NONE)) {

                conversionFactor = Double.NaN;
                appendUnit = null;

            } else if (unit.equals(MetricUnits.NANOSECONDS)) {

                conversionFactor = 0.000000001;
                appendUnit = APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.MICROSECONDS)) {

                conversionFactor = 0.000001;
                appendUnit = APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.MILLISECONDS)) {

                conversionFactor = 0.001;
                appendUnit = APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.SECONDS)) {

                conversionFactor = 1;
                appendUnit = APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.MINUTES)) {

                conversionFactor = 60;
                appendUnit = APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.HOURS)) {

                conversionFactor = 3600;
                appendUnit = APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.DAYS)) {

                conversionFactor = 86400;
                appendUnit = APPENDEDSECONDS;

            } else if (unit.equals(MetricUnits.PERCENT)) {

                conversionFactor = Double.NaN;
                appendUnit = APPENDEDPERCENT;

            } else if (unit.equals(MetricUnits.BYTES)) {

                conversionFactor = 1;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.KILOBYTES)) {

                conversionFactor = 1024;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.MEGABYTES)) {

                conversionFactor = 1048576;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.GIGABYTES)) {

                conversionFactor = 1073741824;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.KILOBITS)) {

                conversionFactor = 125;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.MEGABITS)) {

                conversionFactor = 125000;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.GIGABITS)) {

                conversionFactor = 1.25e+8;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.KIBIBITS)) {

                conversionFactor = 128;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.MEBIBITS)) {

                conversionFactor = 131072;
                appendUnit = APPENDEDBYTES;

            } else if (unit.equals(MetricUnits.GIBIBITS)) {

                conversionFactor = 1.342e+8;
                appendUnit = APPENDEDBYTES;

            } else {

                conversionFactor = Double.NaN;
                appendUnit = "_" + unit;
            }

            if (Counter.class.isInstance(metric)) {
                PrometheusBuilder.buildCounter(builder, metricNamePrometheus, (Counter) metric, description, tags);
            } else if (Gauge.class.isInstance(metric)) {
                PrometheusBuilder.buildGauge(builder, metricNamePrometheus, (Gauge) metric, description,
                        conversionFactor, tags, appendUnit);
            } else if (Timer.class.isInstance(metric)) {
                PrometheusBuilder.buildTimer(builder, metricNamePrometheus, (Timer) metric, description, tags);
            } else if (SimpleTimer.class.isInstance(metric)){
                PrometheusBuilder.buildSimpleTimer(builder, metricNamePrometheus, (SimpleTimer) metric, description, tags);
            } else if (Histogram.class.isInstance(metric)) {
                PrometheusBuilder.buildHistogram(builder, metricNamePrometheus, (Histogram) metric, description,
                        conversionFactor, tags, appendUnit);
            } else if (Meter.class.isInstance(metric)) {
                PrometheusBuilder.buildMeter(builder, metricNamePrometheus, (Meter) metric, description, tags);
            } else if (ConcurrentGauge.class.isInstance(metric)) {
                PrometheusBuilder.buildConcurrentGauge(builder, metricNamePrometheus, (ConcurrentGauge) metric, description, tags);
            } else {
                log.warning("Metric type '" + metric.getClass() + " for " + entryName + " is invalid.");
            }
        }
    }

    public void serialize(StringBuilder builder) throws IOException {
        try {
            writer.write(builder.toString());
        } finally {
            writer.close();
        }
    }
}

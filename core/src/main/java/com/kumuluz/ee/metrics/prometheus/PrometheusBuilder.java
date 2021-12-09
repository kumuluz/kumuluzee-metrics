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

import java.time.Duration;
import java.util.logging.Logger;

/**
 * Prometheus metrics builders.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class PrometheusBuilder {

    private static final Logger log = Logger.getLogger(PrometheusBuilder.class.getName());

    private static final String QUANTILE = "quantile";

    public static void buildGauge(StringBuilder builder, String name, Gauge<?> gauge, String description,
                                  Double conversionFactor, String tags, String appendUnit) {
        // Skip non number values
        Number gaugeValNumber;
        Object gaugeValue;
        gaugeValue = gauge.getValue();

        if (!(gaugeValue instanceof Number)) {
            log.info("Skipping Prometheus output for Gauge: " + name + " of type " + gauge.getValue().getClass());
            return;
        }
        gaugeValNumber = (Number) gaugeValue;
        if (!(Double.isNaN(conversionFactor))) {
            gaugeValNumber = gaugeValNumber.doubleValue() * conversionFactor;
        }
        getPromTypeLine(builder, name, "gauge", appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);
        getPromValueLine(builder, name, gaugeValNumber, tags, appendUnit);
    }

    public static void buildCounter(StringBuilder builder, String name, Counter counter, String description,
                                    String tags) {
        String lineName = name;
        if (!lineName.endsWith("_total")) {
            lineName = lineName + "_total";
        }
        getPromTypeLine(builder, lineName, "counter");
        getPromHelpLine(builder, lineName, description);
        getPromValueLine(builder, lineName, counter.getCount(), tags);
    }

    public static void buildTimer(StringBuilder builder, String name, Timer timer, String description, String tags) {
        buildMetered(builder, name, timer, tags);
        double conversionFactor = 0.000000001;
        // Build Histogram
        buildSampling(builder, name, timer, description, conversionFactor, tags, "_seconds");
    }

    public static void buildSimpleTimer(StringBuilder builder, String name, SimpleTimer simpleTimer, String description, String tags) {
        buildCounting(builder, name, simpleTimer, description, tags);
        double conversionFactor = 0.000000001;

        String lineName = name + "_elapsedTime";
        double value = simpleTimer.getElapsedTime().toNanos() * conversionFactor;

        getPromTypeLine(builder, lineName, "gauge", "_seconds");
        getPromValueLine(builder, lineName, value, tags, "_seconds");

        Duration minMaxDuration = simpleTimer.getMaxTimeDuration();
        lineName = name + "_maxTimeDuration";
        if (minMaxDuration != null) {
            value = minMaxDuration.toNanos() * conversionFactor;
        } else {
            value = Double.NaN;
        }
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, value, tags, "_seconds");

        minMaxDuration = simpleTimer.getMinTimeDuration();

        lineName = name + "_minTimeDuration";
        if (minMaxDuration != null) {
            value = minMaxDuration.toNanos() * conversionFactor;
        } else {
            value = Double.NaN;
        }
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, value, tags, "_seconds");
    }

    public static void buildHistogram(StringBuilder builder, String name, Histogram histogram, String description,
                                      Double conversionFactor, String tags, String appendUnit) {
        // Build Histogram
        buildSampling(builder, name, histogram, description, conversionFactor, tags, appendUnit);
    }

    public static void buildMeter(StringBuilder builder, String name, Meter meter, String description, String tags) {
        buildCounting(builder, name, meter, description, tags);
        buildMetered(builder, name, meter, tags);
    }

    public static void buildConcurrentGauge(StringBuilder builder, String name, ConcurrentGauge concurrentGauge, String description, String tags) {
        String lineName = name + "_current";
        getPromTypeLine(builder, lineName, "gauge");
        getPromHelpLine(builder, lineName, description);
        getPromValueLine(builder, lineName, concurrentGauge.getCount(), tags);

        lineName = name + "_min";
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, concurrentGauge.getMin(), tags);

        lineName = name + "_max";
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, concurrentGauge.getMax(), tags);
    }

    private static void buildSampling(StringBuilder builder, String name, Sampling sampling, String description,
                                      Double conversionFactor, String tags, String appendUnit) {

        double meanVal = sampling.getSnapshot().getMean();
        double maxVal = sampling.getSnapshot().getMax();
        double minVal = sampling.getSnapshot().getMin();
        double stdDevVal = sampling.getSnapshot().getStdDev();
        double medianVal = sampling.getSnapshot().getMedian();
        double percentile75th = sampling.getSnapshot().get75thPercentile();
        double percentile95th = sampling.getSnapshot().get95thPercentile();
        double percentile98th = sampling.getSnapshot().get98thPercentile();
        double percentile99th = sampling.getSnapshot().get99thPercentile();
        double percentile999th = sampling.getSnapshot().get999thPercentile();

        if (!(Double.isNaN(conversionFactor))) {
            meanVal = sampling.getSnapshot().getMean() * conversionFactor;
            maxVal = sampling.getSnapshot().getMax() * conversionFactor;
            minVal = sampling.getSnapshot().getMin() * conversionFactor;
            stdDevVal = sampling.getSnapshot().getStdDev() * conversionFactor;
            medianVal = sampling.getSnapshot().getMedian() * conversionFactor;
            percentile75th = sampling.getSnapshot().get75thPercentile() * conversionFactor;
            percentile95th = sampling.getSnapshot().get95thPercentile() * conversionFactor;
            percentile98th = sampling.getSnapshot().get98thPercentile() * conversionFactor;
            percentile99th = sampling.getSnapshot().get99thPercentile() * conversionFactor;
            percentile999th = sampling.getSnapshot().get999thPercentile() * conversionFactor;
        }

        String lineName = name + "_mean";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, meanVal, tags, appendUnit);
        lineName = name + "_max";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, maxVal, tags, appendUnit);
        lineName = name + "_min";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, minVal, tags, appendUnit);
        lineName = name + "_stddev";
        getPromTypeLine(builder, lineName, "gauge", appendUnit);
        getPromValueLine(builder, lineName, stdDevVal, tags, appendUnit);

        getPromTypeLine(builder, name, "summary", appendUnit);
        getPromHelpLine(builder, name, description, appendUnit);
        if (sampling instanceof Counting) {
            getPromValueLine(builder, name, ((Counting) sampling).getCount(), tags,
                    appendUnit == null ? "_count" : appendUnit + "_count");
        }

        Double sumValue = null;
        if (sampling instanceof Histogram) {
            sumValue = (double) ((Histogram) sampling).getSum();
        } else if (sampling instanceof Timer) {
            sumValue = ((Timer) sampling).getElapsedTime().toNanos() * conversionFactor;
        }
        if (sumValue != null) {
            getPromValueLine(builder, name, sumValue, tags, appendUnit == null ? "_sum" : appendUnit + "_sum");
        }

        getPromValueLine(builder, name, medianVal, tags, new Tag(QUANTILE, "0.5"), appendUnit);
        getPromValueLine(builder, name, percentile75th, tags, new Tag(QUANTILE, "0.75"), appendUnit);
        getPromValueLine(builder, name, percentile95th, tags, new Tag(QUANTILE, "0.95"), appendUnit);
        getPromValueLine(builder, name, percentile98th, tags, new Tag(QUANTILE, "0.98"), appendUnit);
        getPromValueLine(builder, name, percentile99th, tags, new Tag(QUANTILE, "0.99"), appendUnit);
        getPromValueLine(builder, name, percentile999th, tags, new Tag(QUANTILE, "0.999"), appendUnit);
    }

    private static void buildCounting(StringBuilder builder, String name, Counting counting, String description,
                                      String tags) {
        String lineName = name + "_total";
        getPromTypeLine(builder, lineName, "counter");
        getPromHelpLine(builder, lineName, description);
        getPromValueLine(builder, lineName, counting.getCount(), tags);
    }

    private static void buildMetered(StringBuilder builder, String name, Metered metered,
                                     String tags) {
        String lineName = name + "_rate_" + MetricUnits.PER_SECOND;
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getMeanRate(), tags);

        lineName = name + "_one_min_rate_" + MetricUnits.PER_SECOND;
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getOneMinuteRate(), tags);

        lineName = name + "_five_min_rate_" + MetricUnits.PER_SECOND;
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getFiveMinuteRate(), tags);

        lineName = name + "_fifteen_min_rate_" + MetricUnits.PER_SECOND;
        getPromTypeLine(builder, lineName, "gauge");
        getPromValueLine(builder, lineName, metered.getFifteenMinuteRate(), tags);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags) {
        getPromValueLine(builder, name, value, tags, null);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags, Tag quantile,
                                         String appendUnit) {

        if (tags == null || tags.isEmpty()) {
            tags = quantile.getTagName() + "=\"" + quantile.getTagValue() + "\"";
        } else {
            tags = tags + "," + quantile.getTagName() + "=\"" + quantile.getTagValue() + "\"";
        }
        getPromValueLine(builder, name, value, tags, appendUnit);
    }

    private static void getPromValueLine(StringBuilder builder, String name, Number value, String tags,
                                         String appendUnit) {

        String metricName = getPrometheusMetricName(name);

        builder.append(metricName);

        if (appendUnit != null) {
            builder.append(appendUnit);
        }

        if (tags != null && tags.length() > 0) {
            builder.append("{").append(tags).append("}");
        }

        builder.append(" ").append(value).append('\n');
    }

    private static void getPromHelpLine(StringBuilder builder, String name, String description) {
        getPromHelpLine(builder, name, description, null);
    }

    private static void getPromHelpLine(StringBuilder builder, String name, String description, String appendUnit) {
        String metricName = getPrometheusMetricName(name);
        if (description != null && !description.isEmpty()) {
            builder.append("# HELP ").append(metricName);

            if (appendUnit != null) {
                builder.append(appendUnit);
            }
            builder.append(" ").append(description).append("\n");
        }
    }

    private static void getPromTypeLine(StringBuilder builder, String name, String type) {
        getPromTypeLine(builder, name, type, null);
    }

    private static void getPromTypeLine(StringBuilder builder, String name, String type, String appendUnit) {

        String metricName = getPrometheusMetricName(name);
        builder.append("# TYPE ").append(metricName);
        if (appendUnit != null) {
            builder.append(appendUnit);
        }
        builder.append(" ").append(type).append("\n");
    }

    /*
     * Create the Prometheus metric name by sanitizing some characters
     */
    private static String getPrometheusMetricName(String name) {

        String out = name.replaceAll("[^a-zA-Z0-9_]", "_");
        out = out.replaceAll("_+", "_");

        return out;
    }
}

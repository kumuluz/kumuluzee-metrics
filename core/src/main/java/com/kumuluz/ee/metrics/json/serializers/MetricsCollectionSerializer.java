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
package com.kumuluz.ee.metrics.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.kumuluz.ee.metrics.json.models.MetricsCollection;
import com.kumuluz.ee.metrics.utils.MetricIdUtil;
import org.eclipse.microprofile.metrics.*;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Serializer for MetricRegistry, which exposes metrics.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class MetricsCollectionSerializer extends StdSerializer<MetricsCollection> {

    public MetricsCollectionSerializer() {
        super(MetricsCollection.class);
    }

    @Override
    public void serialize(MetricsCollection metricRegistry, JsonGenerator json,
                          SerializerProvider provider) throws IOException {

        Map<String, Map<MetricID, Metric>> groupedMetrics = new HashMap<>();

        for (Map.Entry<MetricID, Metric> entry : metricRegistry.getMetrics().entrySet()) {
            Map<MetricID, Metric> group = groupedMetrics.computeIfAbsent(entry.getKey().getName(), k -> new HashMap<>());

            group.put(entry.getKey(), entry.getValue());
        }

        json.writeStartObject();
        for (Map.Entry<String, Map<MetricID, Metric>> groupEntry : groupedMetrics.entrySet()) {
            boolean groupHeadWritten = false;
            for (Map.Entry<MetricID, Metric> entry : groupEntry.getValue().entrySet()) {
                if (isMetricNonComposite(entry.getValue())) {
                    json.writeFieldName(MetricIdUtil.metricIdToString(entry.getKey()));
                    serializeMetric(entry.getKey(), entry.getValue(), json);
                } else {
                    if (!groupHeadWritten) {
                        json.writeFieldName(groupEntry.getKey());
                        json.writeStartObject();
                        groupHeadWritten = true;
                    }
                    serializeMetric(entry.getKey(), entry.getValue(), json);
                }
            }

            if (groupHeadWritten) {
                json.writeEndObject();
            }
        }
        json.writeEndObject();
    }

    private void serializeMetric(MetricID metricID, Metric metric, JsonGenerator json) throws IOException {

        String compositeSuffix = MetricIdUtil.tagsToSuffix(metricID);

        if (metric instanceof Gauge) {
            json.writeObject(((Gauge<?>) metric).getValue());
        } else if (metric instanceof Counter) {
            json.writeObject(((Counter) metric).getCount());
        } else if (metric instanceof Meter) {
            Meter meter = (Meter) metric;
            serializeMetered(meter, compositeSuffix, json);
        } else if (metric instanceof Histogram) {
            Histogram histogram = (Histogram) metric;
            Snapshot snapshot = histogram.getSnapshot();
            json.writeObjectField("count" + compositeSuffix, histogram.getCount());
            json.writeObjectField("sum" + compositeSuffix, histogram.getSum());
            serializeSnapshot(snapshot, compositeSuffix, json);
        } else if (metric instanceof Timer) {
            Timer timer = (Timer) metric;
            Snapshot snapshot = timer.getSnapshot();
            serializeMetered(timer, compositeSuffix, json);
            serializeSnapshot(snapshot, compositeSuffix, json);
            json.writeObjectField("elapsedTime" + compositeSuffix, timer.getElapsedTime().toNanos());
        } else if (metric instanceof SimpleTimer) {
            SimpleTimer simpleTimer = (SimpleTimer) metric;

            json.writeObjectField("count" + compositeSuffix, simpleTimer.getCount());
            json.writeObjectField("elapsedTime" + compositeSuffix, simpleTimer.getElapsedTime().toNanos());
            json.writeObjectField("maxTimeDuration" + compositeSuffix, Optional.ofNullable(simpleTimer.getMaxTimeDuration()).map(Duration::toNanos).orElse(null));
            json.writeObjectField("minTimeDuration" + compositeSuffix, Optional.ofNullable(simpleTimer.getMinTimeDuration()).map(Duration::toNanos).orElse(null));
        } else if (metric instanceof ConcurrentGauge) {
            ConcurrentGauge concurrentGauge = (ConcurrentGauge) metric;

            json.writeObjectField("current" + compositeSuffix, concurrentGauge.getCount());
            json.writeObjectField("min" + compositeSuffix, concurrentGauge.getMin());
            json.writeObjectField("max" + compositeSuffix, concurrentGauge.getMax());
        }
    }

    private void serializeMetered(Metered metered, String compositeSuffix, JsonGenerator json) throws IOException {
        json.writeObjectField("count" + compositeSuffix, metered.getCount());
        json.writeObjectField("meanRate" + compositeSuffix, metered.getMeanRate());
        json.writeObjectField("oneMinRate" + compositeSuffix, metered.getOneMinuteRate());
        json.writeObjectField("fiveMinRate" + compositeSuffix, metered.getFiveMinuteRate());
        json.writeObjectField("fifteenMinRate" + compositeSuffix, metered.getFifteenMinuteRate());
    }

    private void serializeSnapshot(Snapshot snapshot, String compositeSuffix, JsonGenerator json) throws IOException {
        json.writeObjectField("min" + compositeSuffix, snapshot.getMin());
        json.writeObjectField("max" + compositeSuffix, snapshot.getMax());
        json.writeObjectField("mean" + compositeSuffix, snapshot.getMean());
        json.writeObjectField("stddev" + compositeSuffix, snapshot.getStdDev());
        json.writeObjectField("p50" + compositeSuffix, snapshot.getMedian());
        json.writeObjectField("p75" + compositeSuffix, snapshot.get75thPercentile());
        json.writeObjectField("p95" + compositeSuffix, snapshot.get95thPercentile());
        json.writeObjectField("p98" + compositeSuffix, snapshot.get98thPercentile());
        json.writeObjectField("p99" + compositeSuffix, snapshot.get99thPercentile());
        json.writeObjectField("p999" + compositeSuffix, snapshot.get999thPercentile());
    }

    private boolean isMetricNonComposite(Metric metric) {
        return metric instanceof Gauge || metric instanceof Counter;
    }
}

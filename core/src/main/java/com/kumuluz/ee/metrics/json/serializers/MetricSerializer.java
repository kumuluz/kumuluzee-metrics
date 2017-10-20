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
import org.eclipse.microprofile.metrics.*;

import java.io.IOException;

/**
 * Serializer for Microprofile Metric.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
public class MetricSerializer extends StdSerializer<Metric> {

    public MetricSerializer() {
        super(Metric.class);
    }

    @Override
    public void serialize(Metric metric, JsonGenerator json, SerializerProvider provider) throws IOException {
        if(metric instanceof Gauge) {
            json.writeObject(((Gauge) metric).getValue());
        } else if(metric instanceof Counter) {
            json.writeObject(((Counter) metric).getCount());
        } else if(metric instanceof Meter) {
            Meter meter = (Meter)metric;
            json.writeStartObject();
            json.writeObjectField("count", meter.getCount());
            json.writeObjectField("meanRate", meter.getMeanRate());
            json.writeObjectField("oneMinRate", meter.getOneMinuteRate());
            json.writeObjectField("fiveMinRate", meter.getFiveMinuteRate());
            json.writeObjectField("fifteenMinRate", meter.getFifteenMinuteRate());
            json.writeEndObject();
        } else if(metric instanceof Histogram) {
            Histogram histogram = (Histogram)metric;
            Snapshot snapshot = histogram.getSnapshot();
            json.writeStartObject();
            json.writeObjectField("count", histogram.getCount());
            json.writeObjectField("min", snapshot.getMin());
            json.writeObjectField("max", snapshot.getMax());
            json.writeObjectField("mean", snapshot.getMean());
            json.writeObjectField("stddev", snapshot.getStdDev());
            json.writeObjectField("p50", snapshot.getMedian());
            json.writeObjectField("p75", snapshot.get75thPercentile());
            json.writeObjectField("p95", snapshot.get95thPercentile());
            json.writeObjectField("p98", snapshot.get98thPercentile());
            json.writeObjectField("p99", snapshot.get99thPercentile());
            json.writeObjectField("p999", snapshot.get999thPercentile());
            json.writeEndObject();
        } else if(metric instanceof Timer) {
            Timer timer = (Timer)metric;
            Snapshot snapshot = timer.getSnapshot();
            json.writeStartObject();
            json.writeObjectField("count", timer.getCount());
            json.writeObjectField("meanRate", timer.getMeanRate());
            json.writeObjectField("oneMinRate", timer.getOneMinuteRate());
            json.writeObjectField("fiveMinRate", timer.getFiveMinuteRate());
            json.writeObjectField("fifteenMinRate", timer.getFifteenMinuteRate());
            json.writeObjectField("min", snapshot.getMin());
            json.writeObjectField("max", snapshot.getMax());
            json.writeObjectField("mean", snapshot.getMean());
            json.writeObjectField("stddev", snapshot.getStdDev());
            json.writeObjectField("p50", snapshot.getMedian());
            json.writeObjectField("p75", snapshot.get75thPercentile());
            json.writeObjectField("p95", snapshot.get95thPercentile());
            json.writeObjectField("p98", snapshot.get98thPercentile());
            json.writeObjectField("p99", snapshot.get99thPercentile());
            json.writeObjectField("p999", snapshot.get999thPercentile());
            json.writeEndObject();
        }
    }
}

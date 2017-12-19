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
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;

import java.io.IOException;
import java.util.Map;

/**
 * Serializer for MetricRegistry, which exposes metrics.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class MetricRegistryMetricSerializer extends StdSerializer<MetricRegistry> {

    public MetricRegistryMetricSerializer() {
        super(MetricRegistry.class);
    }

    @Override
    public void serialize(MetricRegistry metricRegistry, JsonGenerator json,
                          SerializerProvider provider)throws IOException {
        json.writeStartObject();
        for(Map.Entry<String, Metric> entry : metricRegistry.getMetrics().entrySet()) {
            json.writeObjectField(entry.getKey(), entry.getValue());
        }
        json.writeEndObject();
    }
}

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
package com.kumuluz.ee.metrics.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.kumuluz.ee.metrics.json.serializers.MetadataSerializer;
import com.kumuluz.ee.metrics.json.serializers.MetricRegistryMetadataSerializer;
import com.kumuluz.ee.metrics.json.serializers.MetricsCollectionSerializer;
import com.kumuluz.ee.metrics.json.serializers.TagSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Jackson Module, which contains serializers for MetricRegistry
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class MetricsModule extends Module {

    private boolean forMetadata;

    /**
     * Creates Jackson Module.
     *
     * @param forMetadata If true serializes MetricRegistry metadata, otherwise serializes MetricRegistry metrics
     */
    public MetricsModule(boolean forMetadata) {
        this.forMetadata = forMetadata;
    }

    @Override
    public String getModuleName() {
        return MetricsModule.class.getSimpleName();
    }

    @Override
    public Version version() {
        return new Version(2, 0, 0, "",
                "com.kumuluz.ee.metrics", "kumuluzee-metrics-core");
    }

    @Override
    public void setupModule(SetupContext context) {
        List<JsonSerializer<?>> serializers = new ArrayList<>(2);

        if (forMetadata) {
            serializers.add(new MetricRegistryMetadataSerializer());
            serializers.add(new MetadataSerializer());
            serializers.add(new TagSerializer());
        } else {
            serializers.add(new MetricsCollectionSerializer());
        }

        context.addSerializers(new SimpleSerializers(serializers));
    }
}

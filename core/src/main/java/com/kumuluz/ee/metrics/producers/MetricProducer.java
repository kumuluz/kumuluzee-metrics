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
package com.kumuluz.ee.metrics.producers;

import com.kumuluz.ee.metrics.utils.AnnotationMetadata;
import com.kumuluz.ee.metrics.utils.MetadataWithTags;
import org.eclipse.microprofile.metrics.*;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

/**
 * Producers for Microprofile metrics.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
@Alternative
@ApplicationScoped
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class MetricProducer {

    @Inject
    private MetricRegistry applicationRegistry;

    @Produces
    public Meter produceMeter(InjectionPoint injectionPoint) {
        MetadataWithTags metadataWithTags = AnnotationMetadata.buildProducerMetadata(injectionPoint, MetricType.METERED);
        return applicationRegistry.meter(metadataWithTags.getMetadata(), metadataWithTags.getTags());
    }

    @Produces
    public Timer produceTimer(InjectionPoint injectionPoint) {
        MetadataWithTags metadataWithTags = AnnotationMetadata.buildProducerMetadata(injectionPoint, MetricType.TIMER);
        return applicationRegistry.timer(metadataWithTags.getMetadata(), metadataWithTags.getTags());
    }

    @Produces
    public SimpleTimer produceSimpleTimer(InjectionPoint injectionPoint){
        MetadataWithTags metadataWithTags = AnnotationMetadata.buildProducerMetadata(injectionPoint, MetricType.SIMPLE_TIMER);
        return applicationRegistry.simpleTimer(metadataWithTags.getMetadata(), metadataWithTags.getTags());
    }

    @Produces
    public Counter produceCounter(InjectionPoint injectionPoint) {
        MetadataWithTags metadataWithTags = AnnotationMetadata.buildProducerMetadata(injectionPoint, MetricType.COUNTER);
        return applicationRegistry.counter(metadataWithTags.getMetadata(), metadataWithTags.getTags());
    }

    @Produces
    public ConcurrentGauge produceConcurrentGauge(InjectionPoint injectionPoint) {
        MetadataWithTags metadataWithTags = AnnotationMetadata.buildProducerMetadata(injectionPoint, MetricType.CONCURRENT_GAUGE);
        return applicationRegistry.concurrentGauge(metadataWithTags.getMetadata(), metadataWithTags.getTags());
    }

    @Produces
    public Histogram produceHistogram(InjectionPoint injectionPoint) {
        MetadataWithTags metadataWithTags = AnnotationMetadata.buildProducerMetadata(injectionPoint, MetricType.HISTOGRAM);
        return applicationRegistry.histogram(metadataWithTags.getMetadata(), metadataWithTags.getTags());
    }

    @SuppressWarnings("unchecked")
    @Produces
    public <T extends Number> Gauge<T> produceGauge(InjectionPoint injectionPoint) {
        MetadataWithTags metadataWithTags = AnnotationMetadata.buildProducerMetadata(injectionPoint, MetricType.GAUGE);

        return () -> (T) applicationRegistry.getGauge(metadataWithTags.getMetricID()).getValue();
    }
}

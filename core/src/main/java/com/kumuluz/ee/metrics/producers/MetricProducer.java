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
import org.eclipse.microprofile.metrics.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

/**
 * Producers for Microprofile metrics.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
@ApplicationScoped
public class MetricProducer {

    @Inject
    MetricRegistry applicationRegistry;

    @Produces
    public Meter produceMeter(InjectionPoint injectionPoint) {
        return applicationRegistry.meter(AnnotationMetadata.buildMetadata(injectionPoint, MetricType.METERED));
    }

    @Produces
    public Timer produceTimer(InjectionPoint injectionPoint) {
        return applicationRegistry.timer(AnnotationMetadata.buildMetadata(injectionPoint, MetricType.TIMER));
    }

    @Produces
    public Counter produceCounter(InjectionPoint injectionPoint) {
        return applicationRegistry.counter(AnnotationMetadata.buildMetadata(injectionPoint, MetricType.COUNTER));
    }

    @Produces
    public Histogram produceHistogram(InjectionPoint injectionPoint) {
        return applicationRegistry.histogram(AnnotationMetadata.buildMetadata(injectionPoint, MetricType.HISTOGRAM));
    }
}

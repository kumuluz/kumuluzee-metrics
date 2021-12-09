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

import com.kumuluz.ee.metrics.interceptors.utils.AnnotatedTypeDecorator;
import com.kumuluz.ee.metrics.interceptors.utils.GaugeBeanBinding;
import com.kumuluz.ee.metrics.interceptors.utils.RegisterMetricsBinding;
import com.kumuluz.ee.metrics.producers.MetricRegistryProducer;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.*;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Registers metrics from producer fields and producer methods.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class MetricsExtensionCDI implements Extension {

    private final MetricRegistry applicationRegistry = MetricRegistryProducer.getApplicationRegistry();

    private static final AnnotationLiteral<Default> DEFAULT = new AnnotationLiteral<Default>() {
    };

    private static final AnnotationLiteral<GaugeBeanBinding> GAUGE_BEAN_BINDING =
            new AnnotationLiteral<>() {};

    private static final AnnotationLiteral<RegisterMetricsBinding> REGISTER_METRICS_BINDING =
            new AnnotationLiteral<>() {};

    private <X> void registerMetrics(@Observes @WithAnnotations({Counted.class, Metered.class, Timed.class, ConcurrentGauge.class, SimplyTimed.class})
                                             ProcessAnnotatedType<X> pat) {
        AnnotatedTypeDecorator<X> decoratedType = new AnnotatedTypeDecorator<>(pat.getAnnotatedType(),
                REGISTER_METRICS_BINDING);
        pat.setAnnotatedType(decoratedType);
    }

    private <X> void registerGauges(@Observes @WithAnnotations({Gauge.class}) ProcessAnnotatedType<X> pat) {
        AnnotatedTypeDecorator<X> decoratedType = new AnnotatedTypeDecorator<>(pat.getAnnotatedType(),
                GAUGE_BEAN_BINDING);
        pat.setAnnotatedType(decoratedType);
    }
}

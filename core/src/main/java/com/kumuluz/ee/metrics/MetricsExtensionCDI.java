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
import com.kumuluz.ee.metrics.producers.MetricRegistryProducer;
import com.kumuluz.ee.metrics.utils.AnnotationMetadata;
import com.kumuluz.ee.metrics.utils.ProducerMemberRegistration;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * Registers metrics from producer fields and producer methods.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
public class MetricsExtensionCDI implements Extension {

    private MetricRegistry applicationRegistry = MetricRegistryProducer.getApplicationRegistry();

    private List<ProducerMemberRegistration> producerMembers = new LinkedList<>();

    private static final AnnotationLiteral<GaugeBeanBinding> GAUGE_BEAN_BINDING =
            new AnnotationLiteral<GaugeBeanBinding>() {};

    private <X> void registerGauges(@Observes @WithAnnotations({Gauge.class}) ProcessAnnotatedType<X> pat) {
        AnnotatedTypeDecorator<X> decoratedType = new AnnotatedTypeDecorator<>(pat.getAnnotatedType(),
                GAUGE_BEAN_BINDING);
        pat.setAnnotatedType(decoratedType);
    }

    private void metricProducerField(@Observes ProcessProducerField<? extends Metric, ?> ppf) {
        if (ppf.getAnnotatedProducerField().getAnnotation(org.eclipse.microprofile.metrics.annotation.Metric.class)
                != null) {
            Metadata metadata = AnnotationMetadata.buildMetricMetadata(ppf.getAnnotatedProducerField()
                    .getJavaMember(), ppf.getAnnotatedProducerField().getBaseType());
            producerMembers.add(new ProducerMemberRegistration(ppf.getBean(), ppf.getAnnotatedProducerField(),
                    metadata));
        }
    }

    private void metricProducerMethod(@Observes ProcessProducerMethod<? extends Metric, ?> ppm) {
        if (ppm.getAnnotatedProducerMethod().getAnnotation(org.eclipse.microprofile.metrics.annotation.Metric.class)
                != null) {
            Metadata metadata = AnnotationMetadata.buildMetricMetadata(ppm.getAnnotatedProducerMethod()
                    .getJavaMember(), ppm.getAnnotatedProducerMethod().getBaseType());
            producerMembers.add(new ProducerMemberRegistration(ppm.getBean(), ppm.getAnnotatedProducerMethod(),
                    metadata));
        }
    }

    private void registerMetrics(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        for (ProducerMemberRegistration registration : producerMembers) {
            applicationRegistry.register(registration.getMetadata().getName(), getReference(manager,
                    registration.getMember().getBaseType(), registration.getBean()), registration.getMetadata());
        }
    }

    private static <T> T getReference(BeanManager manager, Type type, Bean<?> bean) {
        return (T) manager.getReference(bean, type, manager.createCreationalContext(bean));
    }
}

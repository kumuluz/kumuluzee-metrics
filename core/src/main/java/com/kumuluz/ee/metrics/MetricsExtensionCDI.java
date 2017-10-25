package com.kumuluz.ee.metrics;

import com.kumuluz.ee.metrics.producers.MetricRegistryProducer;
import com.kumuluz.ee.metrics.utils.AnnotationMetadata;
import com.kumuluz.ee.metrics.utils.ProducerMemberRegistration;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
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

    private void metricProducerField(@Observes ProcessProducerField<? extends Metric, ?> ppf) {
        if (ppf.getAnnotatedProducerField().getAnnotation(org.eclipse.microprofile.metrics.annotation.Metric.class)
                != null) {
            Metadata metadata = AnnotationMetadata.buildMetricMetadata(ppf.getAnnotatedProducerField()
                    .getJavaMember());
            producerMembers.add(new ProducerMemberRegistration(ppf.getBean(), ppf.getAnnotatedProducerField(),
                    metadata));
        }
    }

    private void metricProducerMethod(@Observes ProcessProducerMethod<? extends Metric, ?> ppm) {
        if (ppm.getAnnotatedProducerMethod().getAnnotation(org.eclipse.microprofile.metrics.annotation.Metric.class)
                != null) {
            Metadata metadata = AnnotationMetadata.buildMetricMetadata(ppm.getAnnotatedProducerMethod()
                    .getJavaMember());
            producerMembers.add(new ProducerMemberRegistration(ppm.getBean(), ppm.getAnnotatedProducerMethod(),
                    metadata));
        }
    }

    private void registerMetrics(@Observes AfterDeploymentValidation adv, BeanManager manager) {
        for (ProducerMemberRegistration registration : producerMembers) {
            applicationRegistry.register(registration.getMetadata().getName(), getReference(manager,
                    registration.getMember().getBaseType(), registration.getBean()));
        }
    }

    private static <T> T getReference(BeanManager manager, Type type, Bean<?> bean) {
        return (T) manager.getReference(bean, type, manager.createCreationalContext(bean));
    }
}

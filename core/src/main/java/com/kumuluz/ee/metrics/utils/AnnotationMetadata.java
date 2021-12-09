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
package com.kumuluz.ee.metrics.utils;

import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.*;

import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Metadata Builders.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class AnnotationMetadata {

    private static final Logger LOG = Logger.getLogger(AnnotationMetadata.class.getName());

    public static <E extends AnnotatedElement, T extends Annotation> T getAnnotation(Class<?> bean, E element,
                                                                                     Class<T> annotationClass) {

        if (element.getAnnotation(annotationClass) != null) {
            return element.getAnnotation(annotationClass);
        } else {

            Optional<T> stereotypeAnnotationElement = findStereotypeAnnotation(element, annotationClass);
            if (stereotypeAnnotationElement.isPresent()) {
                return stereotypeAnnotationElement.get();
            }

            do {
                if (bean.getAnnotation(annotationClass) != null) {
                    return bean.getAnnotation(annotationClass);
                }

                Optional<T> stereotypeAnnotationBean = findStereotypeAnnotation(bean, annotationClass);
                if (stereotypeAnnotationBean.isPresent()) {
                    return stereotypeAnnotationBean.get();
                }

                bean = bean.getSuperclass();
            } while (Object.class.equals(bean));

            return null;
        }
    }

    private static <T extends Annotation> Optional<T> findStereotypeAnnotation(AnnotatedElement element,
                                                                               Class<T> annotationClass) {

        for (Annotation annotation : element.getAnnotations()) {

            if (annotation.annotationType().isAnnotationPresent(Stereotype.class) &&
                    annotation.annotationType().isAnnotationPresent(annotationClass)) {

                return Optional.of(annotation.annotationType().getAnnotation(annotationClass));
            }
        }

        return Optional.empty();
    }

    public static <E extends Member & AnnotatedElement, T extends Annotation> MetadataWithTags buildMetadata
            (Class<?> bean, E element, Class<T> annotationClass, MetricType metricType) {
        T annotation = getAnnotation(bean, element, annotationClass);
        boolean fromElement = element.isAnnotationPresent(annotationClass);
        return buildMetadata(bean, element, annotation, fromElement, metricType);
    }

    private static <M extends Member, T extends Annotation> MetadataWithTags buildMetadata(Class<?> bean, M member,
                                                                                           T annotation,
                                                                                           boolean fromElement,
                                                                                           MetricType metricType) {

        MetricType type;
        boolean absolute;
        String name = "";
        String[] tags = {};
        String displayName = "";
        String description = "";
        String unit = MetricUnits.NONE;
        if (annotation instanceof Counted) {
            Counted a = (Counted) annotation;
            type = MetricType.COUNTER;
            absolute = a.absolute();
            name = a.name();
            tags = a.tags();
            displayName = a.displayName();
            description = a.description();
            unit = a.unit();
        } else if (annotation instanceof Timed) {
            Timed a = (Timed) annotation;
            type = MetricType.TIMER;
            absolute = a.absolute();
            name = a.name();
            tags = a.tags();
            displayName = a.displayName();
            description = a.description();
            unit = a.unit();
        } else if (annotation instanceof SimplyTimed) {
            SimplyTimed a = (SimplyTimed) annotation;
            type = MetricType.SIMPLE_TIMER;
            absolute = a.absolute();
            name = a.name();
            tags = a.tags();
            displayName = a.displayName();
            description = a.description();
            unit = a.unit();
        } else if (annotation instanceof Metered) {
            Metered a = (Metered) annotation;
            type = MetricType.METERED;
            absolute = a.absolute();
            name = a.name();
            tags = a.tags();
            displayName = a.displayName();
            description = a.description();
            unit = a.unit();
        } else if (annotation instanceof ConcurrentGauge) {
            ConcurrentGauge a = (ConcurrentGauge) annotation;
            type = MetricType.CONCURRENT_GAUGE;
            absolute = a.absolute();
            name = a.name();
            tags = a.tags();
            displayName = a.displayName();
            description = a.description();
            unit = a.unit();
        } else if (annotation instanceof Gauge) {
            Gauge a = (Gauge) annotation;
            type = MetricType.GAUGE;
            absolute = a.absolute();
            name = a.name();
            tags = a.tags();
            displayName = a.displayName();
            description = a.description();
            unit = a.unit();
        } else if (annotation instanceof Metric) {
            Metric a = (Metric) annotation;
            type = metricType;
            absolute = a.absolute();
            name = a.name();
            tags = a.tags();
            displayName = a.displayName();
            description = a.description();
            unit = a.unit();
        } else {
            absolute = false;
            type = metricType;
        }

        String finalName;

        if (annotation == null) {
            // no annotation
            finalName = MetricRegistry.name(member.getDeclaringClass(), memberName(member));
        } else if (fromElement) {
            // annotated member
            finalName = (name.isEmpty()) ? memberName(member) : name;
            if (!absolute) {
                finalName = MetricRegistry.name(member.getDeclaringClass(), finalName);
            }
        } else {
            // annotated class
            finalName = MetricRegistry.name((name.isEmpty()) ? bean.getSimpleName() : name,
                    memberName(member));
            if (!absolute) {
                finalName = MetricRegistry.name(bean.getPackage().getName(), finalName);
            }
        }

        MetadataBuilder metadataBuilder = Metadata.builder().withName(finalName).withType(type);
        List<String> missingEqualSign = Arrays.stream(tags)
                .filter(tag -> tag != null && !tag.isEmpty() && !tag.contains("="))
                .collect(Collectors.toList());
        if (!missingEqualSign.isEmpty()) {
            LOG.log(Level.WARNING, String.format("Annotation %s at %s#%s has tags that don't contain equal sign (=)." +
                            "They will be ignored. [%s]", type, bean.getName(), member.getName(),
                    String.join(",", missingEqualSign)));
        }
        Tag[] parsedTags = Arrays.stream(tags)
                .filter(tag -> tag != null && !tag.isEmpty() && tag.contains("="))
                .map(tag -> tag.split("=", 2))
                .map(tagSplit -> new Tag(tagSplit[0], tagSplit[1]))
                .toArray(Tag[]::new);

        metadataBuilder.withDisplayName(displayName);
        metadataBuilder.withDescription(description);
        metadataBuilder.withUnit(unit);

        return new MetadataWithTags(metadataBuilder.build(), parsedTags);
    }

    public static MetadataWithTags buildProducerMetadata(InjectionPoint injectionPoint, MetricType metricType) {
        return buildMetadata(injectionPoint.getMember().getDeclaringClass(), injectionPoint.getMember(),
                injectionPoint.getAnnotated().getAnnotation(Metric.class), true, metricType);
    }

    private static String memberName(Member member) {
        if (member instanceof Constructor)
            return member.getDeclaringClass().getSimpleName();
        else
            return member.getName();
    }

}

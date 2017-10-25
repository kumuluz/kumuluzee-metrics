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
import org.eclipse.microprofile.metrics.annotation.*;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;

import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

/**
 * Metadata Builders.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
public class AnnotationMetadata {

    public static Metadata buildProducerMetadata(InjectionPoint injectionPoint, MetricType metricType) {
        Metric annotation = injectionPoint.getAnnotated().getAnnotation(Metric.class);
        String name = (annotation == null || annotation.name().isEmpty()) ? memberName(injectionPoint.getMember()) :
                annotation.name();

        String namePrefix = (annotation == null || !annotation.absolute()) ? injectionPoint.getMember()
                .getDeclaringClass().getName() + "." : "";

        Metadata metadata = new Metadata(namePrefix + name, metricType);
        metadata.setDescription("");

        if (annotation != null) {
            for (String tag : annotation.tags()) {
                metadata.addTag(tag);
            }
            metadata.setDisplayName(annotation.displayName());
            metadata.setDescription(annotation.description());
            metadata.setUnit(annotation.unit());
        }

        return metadata;
    }

    public static <E extends Member & AnnotatedElement> Metadata buildMetricMetadata(E element) {
        Metric annotation = element.getAnnotation(Metric.class);
        String name = (annotation == null || annotation.name().isEmpty()) ? memberName(element) :
                annotation.name();

        String namePrefix = (annotation == null || !annotation.absolute()) ? element.getDeclaringClass().getName()
                + "." : "";

        Metadata metadata = new Metadata(namePrefix + name, getMetricType(element));
        metadata.setDescription("");

        if (annotation != null) {
            for (String tag : annotation.tags()) {
                metadata.addTag(tag);
            }
            metadata.setDisplayName(annotation.displayName());
            metadata.setDescription(annotation.description());
            metadata.setUnit(annotation.unit());
        }

        return metadata;
    }

    public static <E extends Member & AnnotatedElement> Metadata buildMetadataFromCounted(E element) {
        Counted annotation = element.getAnnotation(Counted.class);
        String name = (annotation == null || annotation.name().isEmpty()) ? memberName(element) : annotation.name();

        String namePrefix = (annotation == null || !annotation.absolute()) ? element.getDeclaringClass().getName()
                + "." : "";

        Metadata metadata = new Metadata(namePrefix + name, MetricType.COUNTER);
        metadata.setDescription("");

        if (annotation != null) {
            for (String tag : annotation.tags()) {
                metadata.addTag(tag);
            }
            metadata.setDisplayName(annotation.displayName());
            metadata.setDescription(annotation.description());
            metadata.setUnit(annotation.unit());
        }

        return metadata;
    }

    public static <E extends Member & AnnotatedElement> Metadata buildMetadataFromGauge(E element) {
        Gauge annotation = element.getAnnotation(Gauge.class);
        String name = (annotation == null || annotation.name().isEmpty()) ? memberName(element) : annotation.name();

        String namePrefix = (annotation == null || !annotation.absolute()) ? element.getDeclaringClass().getName()
                + "." : "";

        Metadata metadata = new Metadata(namePrefix + name, MetricType.COUNTER);
        metadata.setDescription("");

        if (annotation != null) {
            for (String tag : annotation.tags()) {
                metadata.addTag(tag);
            }
            metadata.setDisplayName(annotation.displayName());
            metadata.setDescription(annotation.description());
            metadata.setUnit(annotation.unit());
        }

        return metadata;
    }

    public static <E extends Member & AnnotatedElement> Metadata buildMetadataFromMetered(E element) {
        Metered annotation = element.getAnnotation(Metered.class);
        String name = (annotation == null || annotation.name().isEmpty()) ? memberName(element) : annotation.name();

        String namePrefix = (annotation == null || !annotation.absolute()) ? element.getDeclaringClass().getName()
                + "." : "";

        Metadata metadata = new Metadata(namePrefix + name, MetricType.COUNTER);
        metadata.setDescription("");

        if (annotation != null) {
            for (String tag : annotation.tags()) {
                metadata.addTag(tag);
            }
            metadata.setDisplayName(annotation.displayName());
            metadata.setDescription(annotation.description());
            metadata.setUnit(annotation.unit());
        }

        return metadata;
    }

    public static <E extends Member & AnnotatedElement> Metadata buildMetadataFromTimed(E element) {
        Timed annotation = element.getAnnotation(Timed.class);
        String name = (annotation == null || annotation.name().isEmpty()) ? memberName(element) : annotation.name();

        String namePrefix = (annotation == null || !annotation.absolute()) ? element.getDeclaringClass().getName()
                + "." : "";

        Metadata metadata = new Metadata(namePrefix + name, MetricType.COUNTER);
        metadata.setDescription("");

        if (annotation != null) {
            for (String tag : annotation.tags()) {
                metadata.addTag(tag);
            }
            metadata.setDisplayName(annotation.displayName());
            metadata.setDescription(annotation.description());
            metadata.setUnit(annotation.unit());
        }

        return metadata;
    }

    private static String memberName(Member member) {
        if (member instanceof Constructor)
            return member.getDeclaringClass().getSimpleName();
        else
            return member.getName();
    }

    private static <E extends Member & AnnotatedElement> MetricType getMetricType(E element) {
        if (element instanceof Counter) {
            return MetricType.COUNTER;
        } else if (element instanceof Histogram) {
            return MetricType.HISTOGRAM;
        } else if (element instanceof org.eclipse.microprofile.metrics.Gauge) {
            return MetricType.GAUGE;
        } else if (element instanceof Timer) {
            return MetricType.TIMER;
        } else if (element instanceof Meter) {
            return MetricType.METERED;
        }

        return MetricType.INVALID;
    }
}

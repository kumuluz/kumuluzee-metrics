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
package com.kumuluz.ee.metrics.interceptors;

import com.kumuluz.ee.metrics.api.*;
import com.kumuluz.ee.metrics.interceptors.utils.RegisterMetricsBinding;
import com.kumuluz.ee.metrics.utils.AnnotationMetadata;
import com.kumuluz.ee.metrics.utils.MetadataWithTags;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.annotation.*;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Interceptor for registering Timed, Metered and Counted annotations on bean construct.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
@Interceptor
@RegisterMetricsBinding
@Priority(Interceptor.Priority.LIBRARY_BEFORE - 10)
public class RegisterMetricsInterceptor {

    private static final ConcurrentMap<Member, Member> PROCESSED_ELEMENTS = new ConcurrentHashMap<>();

    @Inject
    private MetricRegistry registry;

    @AroundConstruct
    private Object registerMetrics(InvocationContext context) throws Exception {
        Class<?> bean = context.getConstructor().getDeclaringClass();

        registerMetrics(bean, context.getConstructor());

        Class<?> type = bean;
        do {
            for (Method method : type.getDeclaredMethods())
                if (!method.isSynthetic() && !Modifier.isPrivate(method.getModifiers())) {
                    registerMetrics(bean, method);
                }
            type = type.getSuperclass();
        } while (!Object.class.equals(type));

        return context.proceed();
    }

    private <E extends Member & AnnotatedElement> void registerMetrics(Class<?> bean, E element) {

        PROCESSED_ELEMENTS.computeIfAbsent(element, el -> {
            if (AnnotationMetadata.getAnnotation(bean, element, Counted.class) != null) {
                MetadataWithTags m = AnnotationMetadata.buildMetadata(bean, element, Counted.class, MetricType.COUNTER);
                registry.register(m.getMetadata(), new CounterImpl(), m.getTags());
            }
            if (AnnotationMetadata.getAnnotation(bean, element, Timed.class) != null) {
                MetadataWithTags m = AnnotationMetadata.buildMetadata(bean, element, Timed.class, MetricType.TIMER);
                registry.register(m.getMetadata(), new TimerImpl(), m.getTags());
            }
            if (AnnotationMetadata.getAnnotation(bean, element, SimplyTimed.class) != null) {
                MetadataWithTags m = AnnotationMetadata.buildMetadata(bean, element, SimplyTimed.class, MetricType.SIMPLE_TIMER);
                registry.register(m.getMetadata(), new SimpleTimerImpl(), m.getTags());
            }
            if (AnnotationMetadata.getAnnotation(bean, element, Metered.class) != null) {
                MetadataWithTags m = AnnotationMetadata.buildMetadata(bean, element, Metered.class, MetricType.METERED);
                registry.register(m.getMetadata(), new MeterImpl(), m.getTags());
            }
            if (AnnotationMetadata.getAnnotation(bean, element, ConcurrentGauge.class) != null) {
                MetadataWithTags m = AnnotationMetadata.buildMetadata(bean, element, ConcurrentGauge.class, MetricType.CONCURRENT_GAUGE);
                registry.register(m.getMetadata(), new ConcurrentGaugeImpl(), m.getTags());
            }

            return el;
        });
    }
}

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

import com.kumuluz.ee.metrics.api.ForwardingGauge;
import com.kumuluz.ee.metrics.interceptors.utils.GaugeBeanBinding;
import com.kumuluz.ee.metrics.utils.AnnotationMetadata;
import com.kumuluz.ee.metrics.utils.MetadataWithTags;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

/**
 * Interceptor for Gauge annotation.
 * <p>
 * Processes beans, which include methods, annotated with Gauge.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
@Interceptor
@GaugeBeanBinding
@Priority(Interceptor.Priority.LIBRARY_BEFORE - 11)
public class GaugeInterceptor {

    @Inject
    private MetricRegistry applicationRegistry;

    @AroundConstruct
    private Object gaugeBeanConstructor(InvocationContext context) throws Exception {
        Object target = context.proceed();

        Class<?> type = context.getConstructor().getDeclaringClass();

        do {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(org.eclipse.microprofile.metrics.annotation.Gauge.class)) {
                    MetadataWithTags metadata = AnnotationMetadata.buildMetadata(type, method,
                            org.eclipse.microprofile.metrics.annotation.Gauge.class, MetricType.GAUGE);
                    Gauge<?> gauge = applicationRegistry.getGauges().get(metadata.getMetricID());

                    if (gauge == null) {
                        applicationRegistry.register(metadata.getMetadata(),
                                new ForwardingGauge(method, context.getTarget()), metadata.getTags());
                    }
                }
            }
            type = type.getSuperclass();
        } while (!Object.class.equals(type));

        return target;
    }
}

/*
 *  Copyright (c) 2014-2020 Kumuluz and/or its affiliates
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

import com.kumuluz.ee.metrics.utils.AnnotationMetadata;
import com.kumuluz.ee.metrics.utils.MetadataWithTags;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

/**
 * Interceptor for SimplyTimed annotation.
 *
 * @author Aljaž Pavišič
 * @since 2.3.0
 */
@Interceptor
@SimplyTimed
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class SimplyTimedInterceptor {
    @Inject
    private MetricRegistry applicationRegistry;

    private Bean<?> bean;

    @Inject
    private SimplyTimedInterceptor(@Intercepted Bean<?> bean) {
        this.bean = bean;
    }

    @AroundConstruct
    private Object simplyTimedConstructor(InvocationContext context) throws Exception {
        return applyInterceptor(context, context.getConstructor());
    }

    @AroundInvoke
    private Object simplyTimedMethod(InvocationContext context) throws Exception {
        return applyInterceptor(context, context.getMethod());
    }

    private <E extends Member & AnnotatedElement> Object applyInterceptor(InvocationContext context, E member)
            throws Exception {
        MetadataWithTags metadata = AnnotationMetadata.buildMetadata(bean.getBeanClass(), member, SimplyTimed.class, MetricType.SIMPLE_TIMER);
        SimpleTimer simpleTimer = applicationRegistry.getSimpleTimers().get(metadata.getMetricID());
        if (simpleTimer == null) {
            throw new IllegalStateException("No simple timer with ID [" + metadata.getMetricID() + "] found in registry ["
                    + applicationRegistry + "]");
        }

        SimpleTimer.Context simpleTimerContext = simpleTimer.time();

        try {
            return context.proceed();
        } finally {
            simpleTimerContext.stop();
        }
    }
}

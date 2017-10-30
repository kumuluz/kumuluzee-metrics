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

import com.kumuluz.ee.metrics.utils.AnnotationMetadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Metered;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

/**
 * Interceptor for Metered annotation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
@Interceptor
@Metered
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class MeteredInterceptor {

    @Inject
    private MetricRegistry applicationRegistry;


    @AroundConstruct
    private Object meteredConstructor(InvocationContext context) throws Exception {
        return applyInterceptor(context, context.getConstructor());
    }

    @AroundInvoke
    private Object meteredMethod(InvocationContext context) throws Exception {
        return applyInterceptor(context, context.getMethod());
    }

    private <E extends Member & AnnotatedElement> Object applyInterceptor(InvocationContext context, E member)
            throws Exception {
        Meter meter = applicationRegistry.meter(AnnotationMetadata.buildMetadataFromMetered(member));
        meter.mark();

        return context.proceed();
    }
}

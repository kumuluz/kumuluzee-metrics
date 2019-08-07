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
package com.kumuluz.ee.metrics.interceptors.utils;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Adds annotations to AnnotatedType
 *
 * @author Urban Malc
 * @since 1.0.0
 */
public class AnnotatedTypeDecorator<X> extends AnnotatedDecorator implements AnnotatedType<X> {

    private final AnnotatedType<X> decoratedType;

    private final Set<AnnotatedMethod<? super X>> decoratedMethods;

    public AnnotatedTypeDecorator(AnnotatedType<X> decoratedType, Annotation decoratingAnnotation) {
        this(decoratedType, decoratingAnnotation, Collections.emptySet());
    }

    public AnnotatedTypeDecorator(AnnotatedType<X> decoratedType, Annotation decoratingAnnotation,
                                  Set<AnnotatedMethod<? super X>> decoratedMethods) {
        super(decoratedType, Collections.singleton(decoratingAnnotation));
        this.decoratedType = decoratedType;
        this.decoratedMethods = decoratedMethods;
    }

    @Override
    public Class<X> getJavaClass() {
        return decoratedType.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return decoratedType.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        Set<AnnotatedMethod<? super X>> methods = new HashSet<>(decoratedType.getMethods());
        for (AnnotatedMethod<? super X> method : decoratedMethods) {
            methods.remove(method);
            methods.add(method);
        }

        return Collections.unmodifiableSet(methods);
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return decoratedType.getFields();
    }
}
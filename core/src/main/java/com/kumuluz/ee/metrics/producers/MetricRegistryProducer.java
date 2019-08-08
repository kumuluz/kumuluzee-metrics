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
package com.kumuluz.ee.metrics.producers;

import com.kumuluz.ee.metrics.api.MetricRegistryImpl;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * Producers for Microprofile metric registries.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
@ApplicationScoped
public class MetricRegistryProducer {

    private static MetricRegistry applicationRegistry = null;
    private static MetricRegistry baseRegistry = null;
    private static MetricRegistry vendorRegistry = null;

    @Produces
    public static MetricRegistry getDefaultRegistry() {
        return getApplicationRegistry();
    }

    @Produces
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    public static MetricRegistry getApplicationRegistry() {
        if (applicationRegistry == null) {
            applicationRegistry = new MetricRegistryImpl();
        }

        return applicationRegistry;
    }

    @Produces
    @RegistryType(type = MetricRegistry.Type.BASE)
    public static MetricRegistry getBaseRegistry() {
        if (baseRegistry == null) {
            baseRegistry = new MetricRegistryImpl();
        }

        return baseRegistry;
    }

    @Produces
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    public static MetricRegistry getVendorRegistry() {
        if (vendorRegistry == null) {
            vendorRegistry = new MetricRegistryImpl();
        }

        return vendorRegistry;
    }
}

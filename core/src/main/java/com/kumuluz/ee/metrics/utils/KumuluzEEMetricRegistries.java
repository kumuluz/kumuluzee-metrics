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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Shared metric registry, replaces {@link SharedMetricRegistries} with added functionality of listening to added
 * registries.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class KumuluzEEMetricRegistries {

    private static List<MetricRegistriesListener> listeners = new LinkedList<>();

    public static void clear() {
        SharedMetricRegistries.clear();
    }

    public static Set<String> names() {
        return SharedMetricRegistries.names();
    }

    public static void remove(String key) {
        SharedMetricRegistries.remove(key);
    }

    public static MetricRegistry add(String name, MetricRegistry registry) {
        MetricRegistry addedRegistry = SharedMetricRegistries.add(name, registry);
        notifyListeners();
        return addedRegistry;
    }

    public static MetricRegistry getOrCreate(String name) {
        boolean wasPresent = names().contains(name);
        MetricRegistry ret = SharedMetricRegistries.getOrCreate(name);
        if (!wasPresent) {
            notifyListeners();
        }
        return ret;
    }

    public static MetricRegistry setDefault(String name) {
        return SharedMetricRegistries.setDefault(name);
    }

    public static MetricRegistry setDefault(String name, MetricRegistry metricRegistry) {
        MetricRegistry ret = SharedMetricRegistries.setDefault(name, metricRegistry);
        notifyListeners();
        return SharedMetricRegistries.setDefault(name, metricRegistry);
    }

    public static MetricRegistry getDefault() {
        return SharedMetricRegistries.getDefault();
    }

    public static void addListener(MetricRegistriesListener listener) {
        listeners.add(listener);
    }

    private static void notifyListeners() {
        Set<String> registries = names();
        for (MetricRegistriesListener listener : listeners) {
            listener.onChange(registries);
        }
    }
}

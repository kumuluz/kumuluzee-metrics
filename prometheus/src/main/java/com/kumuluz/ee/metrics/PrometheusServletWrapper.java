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
package com.kumuluz.ee.metrics;

import com.kumuluz.ee.metrics.utils.EnabledRegistries;
import com.kumuluz.ee.metrics.utils.KumuluzEEMetricRegistries;
import com.kumuluz.ee.metrics.utils.LabeledDropwizardExports;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Wrapper for {@link MetricsServlet}.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class PrometheusServletWrapper {

    private static final Logger log = Logger.getLogger(PrometheusServletWrapper.class.getName());

    private CollectorRegistry collectorRegistry;
    private MetricsServlet metricsServlet;

    private Map<String, Collector> registeredRegistries;

    public PrometheusServletWrapper() {

        EnabledRegistries enabledRegistries = EnabledRegistries.getInstance();

        this.registeredRegistries = new HashMap<>();

        collectorRegistry = new CollectorRegistry();
        for (String registry : enabledRegistries.getEnabledRegistries()) {
            registerCollector(registry);
        }

        enabledRegistries.addListener(newRegistries -> {
            Set<String> disabledRegistryNames = new HashSet<>(registeredRegistries.keySet());
            disabledRegistryNames.removeAll(newRegistries);

            Set<String> enabledRegistryNames = new HashSet<>(newRegistries);
            enabledRegistryNames.removeAll(registeredRegistries.keySet());

            for (String registryName : disabledRegistryNames) {
                unregisterCollector(registryName);
            }
            for (String registryName : enabledRegistryNames) {
                registerCollector(registryName);
            }
        });

        this.metricsServlet = new MetricsServlet(collectorRegistry);
    }

    private synchronized void registerCollector(String registry) {
        if (!registeredRegistries.containsKey(registry)) {
            log.info("Adding registry " + registry + " to Prometheus servlet");
            Collector collector = new LabeledDropwizardExports(KumuluzEEMetricRegistries.getOrCreate(registry));
            collectorRegistry.register(collector);
            registeredRegistries.put(registry, collector);
        }
    }

    private synchronized void unregisterCollector(String registry) {
        if (registeredRegistries.containsKey(registry)) {
            log.info("Removing registry " + registry + " from Prometheus servlet");
            collectorRegistry.unregister(registeredRegistries.get(registry));
            registeredRegistries.remove(registry);
        }
    }

    public MetricsServlet getMetricsServlet() {
        return metricsServlet;
    }
}

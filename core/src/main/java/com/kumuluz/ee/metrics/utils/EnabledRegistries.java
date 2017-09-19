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
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import java.util.*;

/**
 * Singleton, which contains info about current enabled registries.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class EnabledRegistries {

    private static final String GARBAGE_COLLECTOR_METRICS_PREFIX = "GarbageCollector";
    private static final String MEMORY_USAGE_METRICS_PREFIX = "MemoryUsage";
    private static final String THREAD_STATES_METRICS_PREFIX = "ThreadStates";
    private static final String JVM_NAME_CONFIG_KEY = "kumuluzee.metrics.jvm.registry-name";

    private Set<String> enabledRegistries;
    private Set<String> disabledRegistries;
    private List<EnabledRegistriesListener> enabledRegistriesListeners;

    private String jvmRegistryName;
    private MetricRegistry jvmRegistryHolder;

    private static EnabledRegistries instance = null;

    public static EnabledRegistries getInstance() {
        if (instance == null) {
            instance = new EnabledRegistries();
        }

        return instance;
    }

    private EnabledRegistries() {
        enabledRegistriesListeners = new LinkedList<>();
        enabledRegistries = readEnabledRegistriesFromConfig();
        disabledRegistries = new HashSet<>();

        // enable jvm
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();
        jvmRegistryName = configurationUtil.get(JVM_NAME_CONFIG_KEY).orElse("jvm");
        jvmRegistryHolder = new MetricRegistry();
        if (configurationUtil.getBoolean("kumuluzee.metrics.jvm.enabled").orElse(true)) {
            enableJVM();
        }

        if (enabledRegistries.size() == 0) {
            KumuluzEEMetricRegistries.addListener(newRegistries -> {
                this.enabledRegistries = new HashSet<>(newRegistries);
                this.enabledRegistries.removeAll(disabledRegistries);
                notifyListeners(this.enabledRegistries);
            });
            this.enabledRegistries = new HashSet<>(KumuluzEEMetricRegistries.names());
        }
    }

    public void addListener(EnabledRegistriesListener listener) {
        enabledRegistriesListeners.add(listener);
    }

    public void enableRegistry(String registry) {
        if (registry.equals(jvmRegistryName)) {
            enableJVM();
        }

        enabledRegistries.add(registry);
        disabledRegistries.remove(registry);
        notifyListeners(enabledRegistries);
    }

    public void disableRegistry(String registry) {
        if (registry.equals(jvmRegistryName)) {
            disableJVM();
        }

        enabledRegistries.remove(registry);
        disabledRegistries.add(registry);
        notifyListeners(enabledRegistries);
    }

    public Set<String> getEnabledRegistries() {
        return enabledRegistries;
    }

    private void notifyListeners(Set<String> newRegistries) {
        for (EnabledRegistriesListener listener : enabledRegistriesListeners) {
            listener.onChange(newRegistries);
        }
    }

    private Set<String> readEnabledRegistriesFromConfig() {
        Set<String> enabledRegistriesList = new HashSet<>();
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        Optional<String> enabledRegistries;
        int i = 0;
        while ((enabledRegistries = configurationUtil.get(
                String.format("kumuluzee.metrics.enabled-registries[%d]", i))).isPresent()) {
            enabledRegistriesList.add(enabledRegistries.get());
            i++;
        }

        return enabledRegistriesList;
    }

    private void enableJVM() {
        if (jvmRegistryHolder.getNames().size() == 0) {
            MetricSet garbageCollectorMS = new GarbageCollectorMetricSet();
            MetricSet memoryUsageMS = new MemoryUsageGaugeSet();
            MetricSet threadStatesMS = new ThreadStatesGaugeSet();

            // register to jvm registry
            MetricRegistry registry = KumuluzEEMetricRegistries.getOrCreate(jvmRegistryName);
            registry.register(GARBAGE_COLLECTOR_METRICS_PREFIX, garbageCollectorMS);
            registry.register(MEMORY_USAGE_METRICS_PREFIX, memoryUsageMS);
            registry.register(THREAD_STATES_METRICS_PREFIX, threadStatesMS);

            // register to private registry, used when removing jvm metrics
            jvmRegistryHolder.register(GARBAGE_COLLECTOR_METRICS_PREFIX, garbageCollectorMS);
            jvmRegistryHolder.register(MEMORY_USAGE_METRICS_PREFIX, memoryUsageMS);
            jvmRegistryHolder.register(THREAD_STATES_METRICS_PREFIX, threadStatesMS);
        }
    }

    private void disableJVM() {
        MetricRegistry registry = KumuluzEEMetricRegistries.getOrCreate(jvmRegistryName);
        registry.removeMatching((name, metric) -> jvmRegistryHolder.getMetrics().containsKey(name));
        jvmRegistryHolder.removeMatching((name, metric) -> true);
    }
}

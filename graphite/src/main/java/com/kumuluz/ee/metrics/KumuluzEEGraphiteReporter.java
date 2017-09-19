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

import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.PickledGraphite;
import com.kumuluz.ee.metrics.utils.EnabledRegistries;
import com.kumuluz.ee.metrics.utils.KumuluzEEMetricRegistries;
import com.kumuluz.ee.metrics.utils.ServiceConfigInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Reports metrics to Graphite.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class KumuluzEEGraphiteReporter {

    private static final Logger log = Logger.getLogger(KumuluzEEGraphiteReporter.class.getName());

    private GraphiteSender graphite;
    private Map<String, GraphiteReporter> registeredReporters;
    private long periodSeconds;

    private ServiceConfigInfo serviceConfigInfo;

    public KumuluzEEGraphiteReporter(String address, int port, long periodSeconds, boolean usePickle) {
        serviceConfigInfo = ServiceConfigInfo.getInstance();

        this.graphite = (usePickle) ? new PickledGraphite(address, port) :
                new Graphite(address, port);
        this.periodSeconds = periodSeconds;
        this.registeredReporters = new HashMap<>();

        EnabledRegistries enabledRegistries = EnabledRegistries.getInstance();

        for (String registry : enabledRegistries.getEnabledRegistries()) {
            registeredReporters.put(registry, createReporter(registry));

            log.info("Starting Graphite reporter for registry " + registry);
            registeredReporters.get(registry).start(periodSeconds, TimeUnit.SECONDS);
        }

        enabledRegistries.addListener(newRegistries -> {
            Set<String> disabledRegistryNames = new HashSet<>(registeredReporters.keySet());
            disabledRegistryNames.removeAll(newRegistries);

            Set<String> enabledRegistryNames = new HashSet<>(newRegistries);
            enabledRegistryNames.removeAll(registeredReporters.keySet());

            for (String registryName : disabledRegistryNames) {
                stopReporter(registryName);
            }
            for (String registryName : enabledRegistryNames) {
                startReporter(registryName);
            }
        });
    }

    private synchronized void startReporter(String registry) {
        if (!registeredReporters.containsKey(registry)) {
            log.info("Starting Graphite reporter for registry " + registry);
            GraphiteReporter reporter = createReporter(registry);

            registeredReporters.put(registry, reporter);
            reporter.start(periodSeconds, TimeUnit.SECONDS);
        }
    }

    private GraphiteReporter createReporter(String registry) {
        return GraphiteReporter.forRegistry(KumuluzEEMetricRegistries.getOrCreate(registry))
                .prefixedWith(
                        "KumuluzEE." +
                                parseForGraphite(serviceConfigInfo.getEnvironment()) + "." +
                                parseForGraphite(serviceConfigInfo.getServiceName()) + "." +
                                parseForGraphite(serviceConfigInfo.getServiceVersion()) + "." +
                                parseForGraphite(serviceConfigInfo.getInstanceId()))
                .build(graphite);
    }

    private synchronized void stopReporter(String registry) {
        if (registeredReporters.containsKey(registry)) {
            log.info("Stopping Graphite reporter for registry " + registry);
            registeredReporters.get(registry).stop();
            registeredReporters.remove(registry);
        }
    }

    private String parseForGraphite(String s) {
        return s.replaceAll("\\.", "_");
    }
}

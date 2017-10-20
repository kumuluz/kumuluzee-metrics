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

import com.codahale.metrics.Counter;
import com.codahale.metrics.JvmAttributeGaugeSet;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.metrics.api.CounterImpl;
import com.kumuluz.ee.metrics.api.HistogramImpl;
import com.kumuluz.ee.metrics.api.MeterImpl;
import com.kumuluz.ee.metrics.api.TimerImpl;
import com.kumuluz.ee.metrics.producers.MetricRegistryProducer;
import org.eclipse.microprofile.metrics.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Initializes Metrics module.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
@ApplicationScoped
public class MetricsInitiator {

    private static final Logger log = Logger.getLogger(MetricsInitiator.class.getName());

    private void initialiseBean(@Observes @Initialized(ApplicationScoped.class) Object init) {

        if (init instanceof ServletContext) {

            ServletContext servletContext = (ServletContext) init;
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            log.info("Initialising KumuluzEE Metrics");

            // register servlet
            boolean servletEnabled = configurationUtil.getBoolean("kumuluzee.metrics.servlet.enabled").orElse(true);
            if (servletEnabled) {
                String servletMapping = configurationUtil.get("kumuluzee.metrics.servlet.mapping").orElse("/metrics/*");

                log.info("Registering metrics servlet on " + servletMapping);
                ServletRegistration.Dynamic dynamicRegistration = servletContext.addServlet("metrics",
                        new KumuluzEEMetricsServlet());
                dynamicRegistration.addMapping(servletMapping);
            }

            registerBaseMetrics();
        }
    }

    private void registerBaseMetrics() {

        Map<String, Metadata> baseMetadata = new HashMap<>();
        baseMetadata.put("heap.used", new Metadata("memory.usedHeap",
                "Used Heap Memory",
                "Displays the amount of used heap memory in bytes.",
                MetricType.GAUGE,
                MetricUnits.BYTES));
        baseMetadata.put("heap.committed", new Metadata("memory.committedHeap",
                "Committed Heap Memory",
                "Displays the amount of memory in bytes that is committed for the Java virtual " +
                        "machine to use. This amount of memory is guaranteed for the Java virtual " +
                        "machine to use.",
                MetricType.GAUGE,
                MetricUnits.BYTES));
        baseMetadata.put("heap.max", new Metadata("memory.maxHeap",
                "Max Heap Memory",
                "Displays the maximum amount of heap memory in bytes that can be used for " +
                        "memory management. This attribute displays -1 if the maximum heap " +
                        "memory size is undefined. This amount of memory is not guaranteed to be " +
                        "available for memory management if it is greater than the amount of " +
                        "committed memory. The Java virtual machine may fail to allocate memory " +
                        "even if the amount of used memory does not exceed this maximum size.",
                MetricType.GAUGE,
                MetricUnits.BYTES));
        baseMetadata.put("count", new Metadata("thread.count",
                "Thread Count",
                "Displays the current number of live threads including both daemon and non-" +
                        "daemon threads",
                MetricType.COUNTER,
                MetricUnits.NONE));
        baseMetadata.put("daemon.count", new Metadata("thread.daemon.count",
                "Daemon Thread Count",
                "Displays the current number of live daemon threads.",
                MetricType.COUNTER,
                MetricUnits.NONE));
        baseMetadata.put("uptime", new Metadata("jvm.uptime",
                "JVM Uptime",
                "Displays the start time of the Java virtual machine in milliseconds. This " +
                        "attribute displays the approximate time when the Java virtual machine " +
                        "started.",
                MetricType.GAUGE,
                MetricUnits.MILLISECONDS));
        baseMetadata.put("loaded", new Metadata("classloader.totalLoadedClass.count",
                "Total Loaded Class Count",
                "Displays the total number of classes that have been loaded since the Java " +
                        "virtual machine has started execution.",
                MetricType.COUNTER,
                MetricUnits.NONE));
        baseMetadata.put("unloaded", new Metadata("classloader.totalUnloadedClass.count",
                "Total Unloaded Class Count",
                "Displays the total number of classes unloaded since the Java virtual machine " +
                        "has started execution.",
                MetricType.COUNTER,
                MetricUnits.NONE));

        MetricRegistry baseRegistry = MetricRegistryProducer.getBaseRegistry();

        registerDropwizardGcMetrics(baseRegistry, new GarbageCollectorMetricSet());
        registerDropwizardMetrics(baseRegistry, new MemoryUsageGaugeSet(), baseMetadata);
        registerDropwizardMetrics(baseRegistry, new ThreadStatesGaugeSet(), baseMetadata);
        registerDropwizardMetrics(baseRegistry, new JvmAttributeGaugeSet(), baseMetadata);
        registerDropwizardMetrics(baseRegistry, new ClassLoadingGaugeSet(), baseMetadata);
    }

    private void registerDropwizardMetrics(MetricRegistry registry, MetricSet metricSet,
                                           Map<String, Metadata> metadataMap) {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            Metadata metadata = metadataMap.get(entry.getKey());
            if(metadata != null) {
                registry.register(metadata.getName(), convertMetric(entry.getValue(), metadata.getTypeRaw()), metadata);
            }
        }
    }

    private void registerDropwizardGcMetrics(MetricRegistry registry, GarbageCollectorMetricSet metricSet) {
        for(Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if(entry.getKey().endsWith(".count")) {
                String garbageCollectorName = entry.getKey().substring(0, entry.getKey().lastIndexOf(".count"));
                Metadata metadata = new Metadata("gc." + garbageCollectorName + ".count",
                        "Garbage Collection Count",
                        "Displays the total number of collections that have occurred. This attribute lists " +
                        "-1 if the collection count is undefined for this collector.",
                        MetricType.COUNTER,
                        MetricUnits.NONE);
                registry.register(metadata.getName(), convertMetric(entry.getValue(), MetricType.COUNTER), metadata);
            } else if(entry.getKey().endsWith(".time")) {
                String garbageCollectorName = entry.getKey().substring(0, entry.getKey().lastIndexOf(".time"));
                Metadata metadata = new Metadata("gc." + garbageCollectorName + ".time",
                        "Garbage Collection Time",
                        "Displays the approximate accumulated collection elapsed time in milliseconds. " +
                                "This attribute displays -1 if the collection elapsed time is undefined for this " +
                                "collector. The Java virtual machine implementation may use a high resolution " +
                                "timer to measure the elapsed time. This attribute may display the same value " +
                                "even if the collection count has been incremented if the collection elapsed " +
                                "time is very short.",
                        MetricType.GAUGE,
                        MetricUnits.MILLISECONDS);
                registry.register(metadata.getName(), convertMetric(entry.getValue(), MetricType.GAUGE), metadata);
            }
        }
    }

    private org.eclipse.microprofile.metrics.Metric convertMetric(Metric metric, MetricType type) {
        if(metric instanceof Counter) {
            return new CounterImpl((Counter)metric);
        } else if(metric instanceof com.codahale.metrics.Histogram) {
            return new HistogramImpl((com.codahale.metrics.Histogram) metric);
        } else if(metric instanceof com.codahale.metrics.Meter) {
            return new MeterImpl((com.codahale.metrics.Meter) metric);
        } else if(metric instanceof com.codahale.metrics.Timer) {
            return new TimerImpl((com.codahale.metrics.Timer) metric);
        } else if(metric instanceof com.codahale.metrics.Gauge) {
            if(type == MetricType.COUNTER) {
                return new CounterImpl((com.codahale.metrics.Gauge) metric);
            } else {
                return (Gauge<Object>) ((com.codahale.metrics.Gauge) metric)::getValue;
            }
        } else {
            return null;
        }
    }
}

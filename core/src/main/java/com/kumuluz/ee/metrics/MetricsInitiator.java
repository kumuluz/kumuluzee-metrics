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
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.metrics.api.CounterImpl;
import com.kumuluz.ee.metrics.api.HistogramImpl;
import com.kumuluz.ee.metrics.api.MeterImpl;
import com.kumuluz.ee.metrics.api.TimerImpl;
import com.kumuluz.ee.metrics.filters.InstrumentedFilter;
import com.kumuluz.ee.metrics.producers.MetricRegistryProducer;
import com.kumuluz.ee.metrics.utils.ForwardingCounter;
import org.eclipse.microprofile.metrics.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Initializes Metrics module.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
@ApplicationScoped
public class MetricsInitiator {

    private static final Logger log = Logger.getLogger(MetricsInitiator.class.getName());

    private void initialiseBean(@Observes @Initialized(ApplicationScoped.class) Object init) {

        if (init instanceof ServletContext) {

            ServletContext servletContext = (ServletContext) init;
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            log.info("Initialising KumuluzEE Metrics");

            registerBaseMetrics();

            // register servlet
            boolean servletEnabled = configurationUtil.getBoolean("kumuluzee.metrics.servlet.enabled")
                    .orElse(true);
            if (servletEnabled) {
                String servletMapping = ConfigurationUtil.getInstance().get("kumuluzee.metrics.servlet.mapping")
                        .orElse("/metrics/*");

                if(!servletMapping.endsWith("/*")) {
                    if(servletMapping.endsWith("/")) {
                        servletMapping = servletMapping + "*";
                    } else {
                        servletMapping = servletMapping + "/*";
                    }
                }
                log.info("Registering metrics servlet on " + servletMapping);
                ServletRegistration.Dynamic dynamicRegistration = servletContext.addServlet("metrics",
                        new KumuluzEEMetricsServlet());
                dynamicRegistration.addMapping(servletMapping);
            }

            // register filters
            String webInstrumentationKey = "kumuluzee.metrics.web-instrumentation[%d]";
            Optional<String> urlPattern;
            int i = 0;
            while ((urlPattern = configurationUtil.get(String.format(webInstrumentationKey + ".url-pattern", i)))
                    .isPresent()) {
                Optional<String> filterName = configurationUtil.get(String
                        .format(webInstrumentationKey + ".name", i));
                if (filterName.isPresent()) {
                    List<Integer> statusCodes = parseStatusCodes(configurationUtil.get(String
                            .format(webInstrumentationKey + ".status-codes", i))
                            .orElse("200,201,204,400,404,500"));
                    log.info("Registering metrics filter " + filterName.get() + " on " + urlPattern.get() +
                            " with status codes [" + statusCodes.stream().map(Object::toString)
                            .collect(Collectors.joining(", ")) + "]");
                    FilterRegistration.Dynamic filterRegistration = servletContext
                            .addFilter(filterName.get(), new InstrumentedFilter(filterName.get(), statusCodes));
                    filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true,
                            urlPattern.get());
                }

                i++;
            }
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
        baseMetadata.put("thread.count", new Metadata("thread.count",
                "Thread Count",
                "Displays the current number of live threads including both daemon and non-" +
                        "daemon threads",
                MetricType.COUNTER,
                MetricUnits.NONE));
        baseMetadata.put("thread.daemon.count", new Metadata("thread.daemon.count",
                "Daemon Thread Count",
                "Displays the current number of live daemon threads.",
                MetricType.COUNTER,
                MetricUnits.NONE));
        baseMetadata.put("thread.max.count", new Metadata("thread.max.count",
                "Peak Thread Count",
                "Displays the peak live thread count since the Java virtual machine started or " +
                        "peak was reset. This includes daemon and non-daemon threads.",
                MetricType.COUNTER,
                MetricUnits.NONE));
        baseMetadata.put("uptime", new Metadata("jvm.uptime",
                "JVM Uptime",
                "Displays the start time of the Java virtual machine in milliseconds. This " +
                        "attribute displays the approximate time when the Java virtual machine " +
                        "started.",
                MetricType.GAUGE,
                MetricUnits.MILLISECONDS));
        baseMetadata.put("classloader.totalLoadedClass.count",
                new Metadata("classloader.totalLoadedClass.count",
                "Total Loaded Class Count",
                "Displays the total number of classes that have been loaded since the Java " +
                        "virtual machine has started execution.",
                MetricType.COUNTER,
                MetricUnits.NONE));
        baseMetadata.put("classloader.totalUnloadedClass.count",
                new Metadata("classloader.totalUnloadedClass.count",
                "Total Unloaded Class Count",
                "Displays the total number of classes unloaded since the Java virtual machine " +
                        "has started execution.",
                MetricType.COUNTER,
                MetricUnits.NONE));
        baseMetadata.put("classloader.currentLoadedClass.count",
                new Metadata("classloader.currentLoadedClass.count",
                        "Current Loaded Class Count",
                        "Displays the number of classes that are currently loaded in the Java virtual " +
                                "machine.",
                        MetricType.COUNTER,
                        MetricUnits.NONE));
        baseMetadata.put("cpu.availableProcessors",
                new Metadata("cpu.availableProcessors",
                        "Available Processors",
                        "Displays the number of processors available to the Java virtual machine. This " +
                                "value may change during a particular invocation of the virtual machine.",
                        MetricType.GAUGE,
                        MetricUnits.NONE));
        baseMetadata.put("cpu.systemLoadAverage",
                new Metadata("cpu.systemLoadAverage",
                        "System Load Average",
                        "Displays the system load average for the last minute. The system load average " +
                                "is the sum of the number of runnable entities queued to the available " +
                                "processors and the number of runnable entities running on the available " +
                                "processors averaged over a period of time.",
                        MetricType.GAUGE,
                        MetricUnits.NONE));

        MetricRegistry baseRegistry = MetricRegistryProducer.getBaseRegistry();

        registerDropwizardGcMetrics(baseRegistry, new GarbageCollectorMetricSet());
        registerDropwizardMetrics(baseRegistry, new MemoryUsageGaugeSet(), baseMetadata);
        registerDropwizardMetrics(baseRegistry, new JvmAttributeGaugeSet(), baseMetadata);

        registerNonDropwizardMetrics(baseRegistry, baseMetadata);
    }

    private void registerDropwizardMetrics(MetricRegistry registry, MetricSet metricSet,
                                           Map<String, Metadata> metadataMap) {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            Metadata metadata = metadataMap.get(entry.getKey());
            if(metadata != null) {
                registry.register(metadata, convertMetric(entry.getValue(), metadata.getTypeRaw()));
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
                registry.register(metadata, convertMetric(entry.getValue(), MetricType.COUNTER));
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
                registry.register(metadata, convertMetric(entry.getValue(), MetricType.GAUGE));
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
                return new ForwardingCounter() {
                    @Override
                    public long getCount() {
                        return ((Number)((com.codahale.metrics.Gauge) metric).getValue()).longValue();
                    }
                };
            } else {
                return (Gauge<Object>) ((com.codahale.metrics.Gauge) metric)::getValue;
            }
        } else {
            return null;
        }
    }

    private void registerNonDropwizardMetrics(MetricRegistry registry, Map<String, Metadata> metadataMap) {
        ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        registry.register(metadataMap.get("classloader.currentLoadedClass.count"), new ForwardingCounter() {
                    @Override
                    public long getCount() {
                        return classLoadingMXBean.getLoadedClassCount();
                    }
                });
        registry.register(metadataMap.get("classloader.totalLoadedClass.count"), new ForwardingCounter() {
            @Override
            public long getCount() {
                return classLoadingMXBean.getTotalLoadedClassCount();
            }
        });
        registry.register(metadataMap.get("classloader.totalUnloadedClass.count"), new ForwardingCounter() {
            @Override
            public long getCount() {
                return classLoadingMXBean.getUnloadedClassCount();
            }
        });

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        registry.register(metadataMap.get("thread.count"), new ForwardingCounter() {
            @Override
            public long getCount() {
                return threadMXBean.getThreadCount();
            }
        });
        registry.register(metadataMap.get("thread.daemon.count"), new ForwardingCounter() {
            @Override
            public long getCount() {
                return threadMXBean.getDaemonThreadCount();
            }
        });
        registry.register(metadataMap.get("thread.max.count"), new ForwardingCounter() {
            @Override
            public long getCount() {
                return threadMXBean.getPeakThreadCount();
            }
        });

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        registry.register(metadataMap.get("cpu.availableProcessors"),
                (Gauge<Integer>) operatingSystemMXBean::getAvailableProcessors);
        registry.register(metadataMap.get("cpu.systemLoadAverage"), (Gauge<Double>)
                operatingSystemMXBean::getSystemLoadAverage);
    }

    private List<Integer> parseStatusCodes(String codes) {
        List<Integer> scList = new LinkedList<>();
        for (String sc : codes.split(",")) {
            try {
                scList.add(Integer.parseInt(sc.trim()));
            } catch (NumberFormatException e) {
                log.warning("Unable to parse status code: " + sc.trim());
            }
        }

        return scList;
    }
}

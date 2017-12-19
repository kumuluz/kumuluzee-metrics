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
package com.kumuluz.ee.metrics.api;

import com.kumuluz.ee.metrics.utils.ServiceConfigInfo;
import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.Timer;

import java.util.*;

/**
 * Microprofile MetricRegistry implementation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class MetricRegistryImpl extends MetricRegistry {

    private com.codahale.metrics.MetricRegistry metricRegistry;

    public MetricRegistryImpl() {
        this.metricRegistry = new com.codahale.metrics.MetricRegistry();
    }

    @Override
    public <T extends Metric> T register(String name, T t) throws IllegalArgumentException {
        return register(name, t, new Metadata(name, MetricType.from(t.getClass())));
    }

    @Override
    public <T extends Metric> T register(String name, T t, Metadata metadata) throws IllegalArgumentException {
        // add default tags
        ServiceConfigInfo configInfo = ServiceConfigInfo.getInstance();
        if(configInfo.shouldAddToTags()) {
            metadata.addTag("environment=" + configInfo.getEnvironment());
            metadata.addTag("serviceName=" + configInfo.getServiceName());
            metadata.addTag("serviceVersion=" + configInfo.getServiceVersion());
            metadata.addTag("instanceId=" + configInfo.getInstanceId());
        }
        metricRegistry.register(name, new MetricAdapter(t, metadata));
        return t;
    }

    @Override
    public Counter counter(String name) {
        return counter(new Metadata(name, MetricType.COUNTER));
    }

    @Override
    public Counter counter(Metadata metadata) {
        Map<String, com.codahale.metrics.Metric> metrics = metricRegistry.getMetrics();
        if(metrics.containsKey(metadata.getName()) && metrics.get(metadata.getName()) instanceof MetricAdapter
                && ((MetricAdapter)metrics.get(metadata.getName())).getMetric() instanceof Counter) {
            return (Counter)((MetricAdapter)metrics.get(metadata.getName())).getMetric();
        }

        return register(metadata.getName(), new CounterImpl(), metadata);
    }

    @Override
    public Histogram histogram(String name) {
        return histogram(new Metadata(name, MetricType.HISTOGRAM));
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        Map<String, com.codahale.metrics.Metric> metrics = metricRegistry.getMetrics();
        if(metrics.containsKey(metadata.getName()) && metrics.get(metadata.getName()) instanceof MetricAdapter
                && ((MetricAdapter)metrics.get(metadata.getName())).getMetric() instanceof Histogram) {
            return (Histogram)((MetricAdapter)metrics.get(metadata.getName())).getMetric();
        }

        return register(metadata.getName(), new HistogramImpl(), metadata);
    }

    @Override
    public Meter meter(String name) {
        return meter(new Metadata(name, MetricType.METERED));
    }

    @Override
    public Meter meter(Metadata metadata) {
        Map<String, com.codahale.metrics.Metric> metrics = metricRegistry.getMetrics();
        if(metrics.containsKey(metadata.getName()) && metrics.get(metadata.getName()) instanceof MetricAdapter
                && ((MetricAdapter)metrics.get(metadata.getName())).getMetric() instanceof Meter) {
            return (Meter)((MetricAdapter)metrics.get(metadata.getName())).getMetric();
        }

        return register(metadata.getName(), new MeterImpl(), metadata);
    }

    @Override
    public Timer timer(String name) {
        return timer(new Metadata(name, MetricType.TIMER));
    }

    @Override
    public Timer timer(Metadata metadata) {
        Map<String, com.codahale.metrics.Metric> metrics = metricRegistry.getMetrics();
        if(metrics.containsKey(metadata.getName()) && metrics.get(metadata.getName()) instanceof MetricAdapter
                && ((MetricAdapter)metrics.get(metadata.getName())).getMetric() instanceof Timer) {
            return (Timer)((MetricAdapter)metrics.get(metadata.getName())).getMetric();
        }

        return register(metadata.getName(), new TimerImpl(), metadata);
    }

    @Override
    public boolean remove(String name) {
        return this.metricRegistry.remove(name);
    }

    @Override
    public void removeMatching(MetricFilter metricFilter) {
        this.metricRegistry.removeMatching(new MetricFilterAdapter(metricFilter));
    }

    @Override
    public SortedSet<String> getNames() {
        return this.metricRegistry.getNames();
    }

    @Override
    public SortedMap<String, Gauge> getGauges() {
        return getGauges(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Gauge> getGauges(MetricFilter metricFilter) {
        Map<String, com.codahale.metrics.Metric> metrics = this.metricRegistry.getMetrics();
        SortedMap<String, Gauge> gauges = new TreeMap<>();
        for(Map.Entry<String, com.codahale.metrics.Metric> entry : metrics.entrySet()) {
            com.codahale.metrics.Metric m = entry.getValue();
            if(m instanceof MetricAdapter && ((MetricAdapter)m).getMetric() instanceof Gauge &&
                    metricFilter.matches(entry.getKey(), ((MetricAdapter)m).getMetric())) {
                gauges.put(entry.getKey(), (Gauge)((MetricAdapter)m).getMetric());
            }
        }
        return gauges;
    }

    @Override
    public SortedMap<String, Counter> getCounters() {
        return getCounters(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Counter> getCounters(MetricFilter metricFilter) {
        Map<String, com.codahale.metrics.Metric> metrics = this.metricRegistry.getMetrics();
        SortedMap<String, Counter> counters = new TreeMap<>();
        for(Map.Entry<String, com.codahale.metrics.Metric> entry : metrics.entrySet()) {
            com.codahale.metrics.Metric m = entry.getValue();
            if(m instanceof MetricAdapter && ((MetricAdapter)m).getMetric() instanceof Counter &&
                    metricFilter.matches(entry.getKey(), ((MetricAdapter)m).getMetric())) {
                counters.put(entry.getKey(), (Counter)((MetricAdapter)m).getMetric());
            }
        }
        return counters;
    }

    @Override
    public SortedMap<String, Histogram> getHistograms() {
        return getHistograms(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Histogram> getHistograms(MetricFilter metricFilter) {
        Map<String, com.codahale.metrics.Metric> metrics = this.metricRegistry.getMetrics();
        SortedMap<String, Histogram> histograms = new TreeMap<>();
        for(Map.Entry<String, com.codahale.metrics.Metric> entry : metrics.entrySet()) {
            com.codahale.metrics.Metric m = entry.getValue();
            if(m instanceof MetricAdapter && ((MetricAdapter)m).getMetric() instanceof Histogram &&
                    metricFilter.matches(entry.getKey(), ((MetricAdapter)m).getMetric())) {
                histograms.put(entry.getKey(), (Histogram)((MetricAdapter)m).getMetric());
            }
        }
        return histograms;
    }

    @Override
    public SortedMap<String, Meter> getMeters() {
        return getMeters(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Meter> getMeters(MetricFilter metricFilter) {
        Map<String, com.codahale.metrics.Metric> metrics = this.metricRegistry.getMetrics();
        SortedMap<String, Meter> meters = new TreeMap<>();
        for(Map.Entry<String, com.codahale.metrics.Metric> entry : metrics.entrySet()) {
            com.codahale.metrics.Metric m = entry.getValue();
            if(m instanceof MetricAdapter && ((MetricAdapter)m).getMetric() instanceof Meter &&
                    metricFilter.matches(entry.getKey(), ((MetricAdapter)m).getMetric())) {
                meters.put(entry.getKey(), (Meter)((MetricAdapter)m).getMetric());
            }
        }
        return meters;
    }

    @Override
    public SortedMap<String, Timer> getTimers() {
        return getTimers(MetricFilter.ALL);
    }

    @Override
    public SortedMap<String, Timer> getTimers(MetricFilter metricFilter) {
        Map<String, com.codahale.metrics.Metric> metrics = this.metricRegistry.getMetrics();
        SortedMap<String, Timer> timers = new TreeMap<>();
        for(Map.Entry<String, com.codahale.metrics.Metric> entry : metrics.entrySet()) {
            com.codahale.metrics.Metric m = entry.getValue();
            if(m instanceof MetricAdapter && ((MetricAdapter)m).getMetric() instanceof Timer &&
                    metricFilter.matches(entry.getKey(), ((MetricAdapter)m).getMetric())) {
                timers.put(entry.getKey(), (Timer)((MetricAdapter)m).getMetric());
            }
        }
        return timers;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, com.codahale.metrics.Metric> metrics = this.metricRegistry.getMetrics();
        Map<String, Metric> metricsWrapped = new HashMap<>();
        for(Map.Entry<String, com.codahale.metrics.Metric> entry : metrics.entrySet()) {
            com.codahale.metrics.Metric m = entry.getValue();
            if(m instanceof MetricAdapter) {
                metricsWrapped.put(entry.getKey(), ((MetricAdapter)m).getMetric());
            }
        }
        return metricsWrapped;
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        Map<String, com.codahale.metrics.Metric> metrics = this.metricRegistry.getMetrics();
        Map<String, Metadata> metadata = new HashMap<>();
        for(Map.Entry<String, com.codahale.metrics.Metric> entry : metrics.entrySet()) {
            com.codahale.metrics.Metric m = entry.getValue();
            if(m instanceof MetricAdapter) {
                metadata.put(entry.getKey(), ((MetricAdapter)m).getMetadata());
            }
        }
        return metadata;
    }
}

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

import com.kumuluz.ee.metrics.json.models.MetadataWithMergedTags;
import com.kumuluz.ee.metrics.utils.MetricIdUtil;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.*;

import java.util.*;

/**
 * Microprofile MetricRegistry implementation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class MetricRegistryImpl extends MetricRegistry {

    private Map<MetricID, Metric> metricsStorage;
    private Map<String, Metadata> metadataStorage;

    public MetricRegistryImpl() {
        this.metricsStorage = new HashMap<>();
        this.metadataStorage = new HashMap<>();
    }

    @Override
    public <T extends Metric> T register(String name, T t) throws IllegalArgumentException {
        return register(Metadata.builder().withName(name).withType(MetricType.from(t.getClass())).build(), t);
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T t) throws IllegalArgumentException {
        return register(metadata, t, new Tag[0]);
    }

    @Override
    public synchronized <T extends Metric> T register(Metadata metadata, T metric, Tag... tags)
            throws IllegalArgumentException {

        MetricID metricID = MetricIdUtil.newMetricID(metadata.getName(), tags);

        Metric existing = this.metricsStorage.putIfAbsent(metricID, metric);

        if (existing != null) {
            Metadata existingMetadata = metadataStorage.get(metricID.getName());
            if (metadata.isReusable() && existingMetadata.isReusable()) {
                if (metadata.getTypeRaw().equals(existingMetadata.getTypeRaw()) &&
                        metadata.getUnit().equals(existingMetadata.getUnit())) {
                    //noinspection unchecked
                    return (T) existing;
                } else {
                    throw new IllegalArgumentException("Metric " + metricID +
                            " is not compatible with previously registered metric.");
                }
            } else {
                throw new IllegalArgumentException("Metric " + metricID + " already exists and is not reusable.");
            }
        }

        metadataStorage.putIfAbsent(metricID.getName(), metadata);

        return metric;
    }

    @Override
    public Counter counter(String name) {
        return counter(name, new Tag[0]);
    }

    @Override
    public Counter counter(String name, Tag... tags) {
        return counter(Metadata.builder().withName(name).withType(MetricType.COUNTER).build(), tags);
    }

    @Override
    public Counter counter(Metadata metadata) {
        return counter(metadata, new Tag[0]);
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        return getOrAdd(new CounterImpl(), Counter.class, metadata, tags);
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name) {
        return concurrentGauge(name, new Tag[0]);
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name, Tag... tags) {
        return concurrentGauge(Metadata.builder().withName(name).withType(MetricType.CONCURRENT_GAUGE).build(), tags);
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata) {
        return concurrentGauge(metadata, new Tag[0]);
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
        return getOrAdd(new ConcurrentGaugeImpl(), ConcurrentGauge.class, metadata, tags);
    }

    @Override
    public Histogram histogram(String name) {
        return histogram(name, new Tag[0]);
    }

    @Override
    public Histogram histogram(String name, Tag... tags) {
        return histogram(Metadata.builder().withName(name).withType(MetricType.HISTOGRAM).build(), tags);
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return histogram(metadata, new Tag[0]);
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        return getOrAdd(new HistogramImpl(), Histogram.class, metadata, tags);
    }

    @Override
    public Meter meter(String name) {
        return meter(name, new Tag[0]);
    }

    @Override
    public Meter meter(String name, Tag... tags) {
        return meter(Metadata.builder().withName(name).withType(MetricType.METERED).build(), tags);
    }

    @Override
    public Meter meter(Metadata metadata) {
        return meter(metadata, new Tag[0]);
    }

    @Override
    public Meter meter(Metadata metadata, Tag... tags) {
        return getOrAdd(new MeterImpl(), Meter.class, metadata, tags);
    }

    @Override
    public Timer timer(String name) {
        return timer(name, new Tag[0]);
    }

    @Override
    public Timer timer(String name, Tag... tags) {
        return timer(Metadata.builder().withName(name).withType(MetricType.TIMER).build(), tags);
    }

    @Override
    public Timer timer(Metadata metadata) {
        return timer(metadata, new Tag[0]);
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        return getOrAdd(new TimerImpl(), Timer.class, metadata, tags);
    }

    @Override
    public synchronized boolean remove(String name) {

        List<MetricID> toRemove = new LinkedList<>();

        for (MetricID mid : this.metricsStorage.keySet()) {
            if (mid.getName().equals(name)) {
                toRemove.add(mid);
            }
        }

        boolean removed = false;
        for (MetricID mid : toRemove) {
            removed |= this.remove(mid);
        }

        if (removed) {
            this.metadataStorage.remove(name);
        }

        return removed;
    }

    @Override
    public synchronized boolean remove(MetricID metricID) {
        boolean removed = this.metricsStorage.remove(metricID) != null;

        if (removed && this.metricsStorage.keySet().stream().noneMatch(mid -> mid.equals(metricID))) {
            this.metadataStorage.remove(metricID.getName());
        }

        return removed;
    }

    @Override
    public synchronized void removeMatching(MetricFilter metricFilter) {
        for (Map.Entry<MetricID, Metric> entry : this.metricsStorage.entrySet()) {
            if (metricFilter.matches(entry.getKey(), entry.getValue())) {
                this.remove(entry.getKey());
            }
        }
    }

    @Override
    public SortedSet<String> getNames() {
        SortedSet<String> names = new TreeSet<>();

        for (MetricID id : this.metricsStorage.keySet()) {
            names.add(id.getName());
        }

        return names;
    }

    @Override
    public SortedSet<MetricID> getMetricIDs() {
        return new TreeSet<>(this.metricsStorage.keySet());
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return getGauges(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter metricFilter) {
        return getMetricsOfType(metricFilter, Gauge.class);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return getCounters(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(MetricFilter metricFilter) {
        return getMetricsOfType(metricFilter, Counter.class);
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        return getConcurrentGauges(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter metricFilter) {
        return getMetricsOfType(metricFilter, ConcurrentGauge.class);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return getHistograms(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter metricFilter) {
        return getMetricsOfType(metricFilter, Histogram.class);
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters() {
        return getMeters(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters(MetricFilter metricFilter) {
        return getMetricsOfType(metricFilter, Meter.class);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return getTimers(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter metricFilter) {
        return getMetricsOfType(metricFilter, Timer.class);
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return Collections.unmodifiableMap(this.metricsStorage);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return Collections.unmodifiableMap(this.metadataStorage);
    }

    public Map<String, MetadataWithMergedTags> getMetadataWithTags() {

        Map<String, MetadataWithMergedTags> metadata = new HashMap<>();

        for (MetricID metricID : this.metricsStorage.keySet()) {
            String name = metricID.getName();

            if (!metadata.containsKey(metricID.getName())) {
                metadata.put(name, new MetadataWithMergedTags(this.metadataStorage.get(name)));
            }

            metadata.get(name).addTags(metricID.getTagsAsList());
        }

        return metadata;
    }

    @SuppressWarnings("unchecked")
    private synchronized <T extends Metric> T getOrAdd(T metric, Class<T> metricType, Metadata metadata, Tag... tags) {
        try {
            return register(metadata, metric, tags);
        } catch (IllegalArgumentException e) {
            MetricID metricID = MetricIdUtil.newMetricID(metadata.getName(), tags);
            Metric existing = this.metricsStorage.get(metricID);
            if (metricType.isInstance(existing)) {
                return (T) existing;
            } else {
                throw new IllegalArgumentException("Metric " + metricID + " is already registered with a different type.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> SortedMap<MetricID, T> getMetricsOfType(MetricFilter filter, Class<T> metricType) {
        SortedMap<MetricID, T> metrics = new TreeMap<>();

        for (Map.Entry<MetricID, Metric> entry : this.metricsStorage.entrySet()) {
            if (metricType.isInstance(entry.getValue()) && filter.matches(entry.getKey(), entry.getValue())) {
                metrics.put(entry.getKey(), (T) entry.getValue());
            }
        }

        return metrics;
    }
}

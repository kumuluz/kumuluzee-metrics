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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Microprofile MetricRegistry implementation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class MetricRegistryImpl implements MetricRegistry {

    private final ConcurrentMap<MetricID, Metric> metricsStorage;
    private final Map<String, Metadata> metadataStorage;
    private final Type type;

    public MetricRegistryImpl(Type type) {
        this.type = type;
        this.metricsStorage = new ConcurrentHashMap<>();
        this.metadataStorage = new HashMap<>();
    }

    @Override
    public <T extends Metric> T register(String name, T t) throws IllegalArgumentException {
        return register(Metadata.builder().withName(name).withType(MetricType.from(t.getClass())).withUnit(MetricUnits.NONE).build(), t);
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
            if (metadata.getTypeRaw().equals(existingMetadata.getTypeRaw()) &&
                    metadata.getUnit().equals(existingMetadata.getUnit())) {
                //noinspection unchecked
                return (T) existing;
            } else {
                throw new IllegalArgumentException("Metric " + metricID +
                        " is not compatible with previously registered metric.");
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
        return counter(Metadata.builder().withName(name).withType(MetricType.COUNTER).withUnit(MetricUnits.NONE).build(), tags);
    }

    @Override
    public Counter counter(MetricID metricID) {
        return counter(metricID.getName(), metricID.getTagsAsArray());
    }

    @Override
    public Counter counter(Metadata metadata) {
        return counter(metadata, new Tag[0]);
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {

        metadata = sanitizeMetadataIfRequired(metadata, MetricType.COUNTER);

        return getOrAdd(new CounterImpl(), Counter.class, metadata, tags);
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name) {
        return concurrentGauge(name, new Tag[0]);
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name, Tag... tags) {
        return concurrentGauge(Metadata.builder().withName(name).withType(MetricType.CONCURRENT_GAUGE).withUnit(MetricUnits.NONE).build(), tags);
    }

    @Override
    public ConcurrentGauge concurrentGauge(MetricID metricID) {
        return concurrentGauge(metricID.getName(), metricID.getTagsAsArray());
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata) {
        return concurrentGauge(metadata, new Tag[0]);
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {

        metadata = sanitizeMetadataIfRequired(metadata, MetricType.CONCURRENT_GAUGE);

        return getOrAdd(new ConcurrentGaugeImpl(), ConcurrentGauge.class, metadata, tags);
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(String name, T object, Function<T, R> function, Tag... tags) {
        return gauge(Metadata.builder().withName(name).withType(MetricType.GAUGE).build(), () -> function.apply(object), tags);
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(MetricID metricID, T object, Function<T, R> function) {
        return gauge(Metadata.builder().withName(metricID.getName()).withType(MetricType.GAUGE).build(), object, function);
    }

    @Override
    public <T, R extends Number> Gauge<R> gauge(Metadata metadata, T object, Function<T, R> function, Tag... tags) {
        return gauge(metadata, () -> function.apply(object), tags);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(String name, Supplier<T> supplier, Tag... tags) {
        return gauge(Metadata.builder().withName(name).withType(MetricType.GAUGE).build(), supplier, tags);
    }

    @Override
    public <T extends Number> Gauge<T> gauge(MetricID metricID, Supplier<T> supplier) {
        return gauge(metricID.getName(), supplier, metricID.getTagsAsArray());
    }

    @Override
    public <T extends Number> Gauge<T> gauge(Metadata metadata, Supplier<T> supplier, Tag... tags) {

        metadata = sanitizeMetadataIfRequired(metadata, MetricType.GAUGE);

        //noinspection unchecked
        return getOrAdd((Gauge<T>) supplier::get, Gauge.class, metadata, tags);
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
    public Histogram histogram(MetricID metricID) {
        return histogram(metricID.getName(), metricID.getTagsAsArray());
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return histogram(metadata, new Tag[0]);
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {

        metadata = sanitizeMetadataIfRequired(metadata, MetricType.HISTOGRAM);

        return getOrAdd(new HistogramImpl(), Histogram.class, metadata, tags);
    }

    @Override
    public Meter meter(String name) {
        return meter(name, new Tag[0]);
    }

    @Override
    public Meter meter(String name, Tag... tags) {
        return meter(Metadata.builder().withName(name).withType(MetricType.METERED).withUnit(MetricUnits.PER_SECOND).build(), tags);
    }

    @Override
    public Meter meter(MetricID metricID) {
        return meter(metricID.getName(), metricID.getTagsAsArray());
    }

    @Override
    public Meter meter(Metadata metadata) {
        return meter(metadata, new Tag[0]);
    }

    @Override
    public Meter meter(Metadata metadata, Tag... tags) {

        metadata = sanitizeMetadataIfRequired(metadata, MetricType.METERED);

        return getOrAdd(new MeterImpl(), Meter.class, metadata, tags);
    }

    @Override
    public Timer timer(String name) {
        return timer(name, new Tag[0]);
    }

    @Override
    public Timer timer(String name, Tag... tags) {
        return timer(Metadata.builder().withName(name).withType(MetricType.TIMER).withUnit(MetricUnits.NANOSECONDS).build(), tags);
    }

    @Override
    public Timer timer(MetricID metricID) {
        return timer(metricID.getName(), metricID.getTagsAsArray());
    }

    @Override
    public Timer timer(Metadata metadata) {
        return timer(metadata, new Tag[0]);
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {

        metadata = sanitizeMetadataIfRequired(metadata, MetricType.TIMER);

        return getOrAdd(new TimerImpl(), Timer.class, metadata, tags);
    }

    @Override
    public SimpleTimer simpleTimer(String name) {
        return simpleTimer(name, new Tag[0]);
    }

    @Override
    public SimpleTimer simpleTimer(String name, Tag... tags) {
        return simpleTimer(Metadata.builder().withName(name).withType(MetricType.SIMPLE_TIMER).withUnit(MetricUnits.NANOSECONDS).build(), tags);
    }

    @Override
    public SimpleTimer simpleTimer(MetricID metricID) {
        return simpleTimer(metricID.getName(), metricID.getTagsAsArray());
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata) {
        return simpleTimer(metadata, new Tag[0]);
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata, Tag... tags) {

        metadata = sanitizeMetadataIfRequired(metadata, MetricType.SIMPLE_TIMER);

        return getOrAdd(new SimpleTimerImpl(), SimpleTimer.class, metadata, tags);
    }

    @Override
    public Metric getMetric(MetricID metricID) {
        return this.metricsStorage.get(metricID);
    }

    @Override
    public <T extends Metric> T getMetric(MetricID metricID, Class<T> tClass) {

        Metric metric = getMetric(metricID);

        if (metric == null) {
            return null;
        }

        if (tClass.isAssignableFrom(metric.getClass())) {
            //noinspection unchecked
            return (T) metric;
        }

        throw new IllegalArgumentException("Registered metric of type " + metric.getClass() +
                " is not assignable to " + tClass);
    }

    @Override
    public Counter getCounter(MetricID metricID) {
        return getMetric(metricID, Counter.class);
    }

    @Override
    public ConcurrentGauge getConcurrentGauge(MetricID metricID) {
        return getMetric(metricID, ConcurrentGauge.class);
    }

    @Override
    public Gauge<?> getGauge(MetricID metricID) {
        return getMetric(metricID, Gauge.class);
    }

    @Override
    public Histogram getHistogram(MetricID metricID) {
        return getMetric(metricID, Histogram.class);
    }

    @Override
    public Meter getMeter(MetricID metricID) {
        return getMetric(metricID, Meter.class);
    }

    @Override
    public Timer getTimer(MetricID metricID) {
        return getMetric(metricID, Timer.class);
    }

    @Override
    public SimpleTimer getSimpleTimer(MetricID metricID) {
        return getMetric(metricID, SimpleTimer.class);
    }

    @Override
    public Metadata getMetadata(String name) {
        return this.metadataStorage.get(name);
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
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers() {
        return getSimpleTimers(MetricFilter.ALL);
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers(MetricFilter metricFilter) {
        return getMetricsOfType(metricFilter, SimpleTimer.class);
    }

    @Override
    public SortedMap<MetricID, Metric> getMetrics(MetricFilter filter) {

        return this.metricsStorage.entrySet().stream()
                .filter(entry -> filter.matches(entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
    }

    @Override
    public <T extends Metric> SortedMap<MetricID, T> getMetrics(Class<T> tClass, MetricFilter metricFilter) {
        return getMetricsOfType(metricFilter, tClass);
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return Collections.unmodifiableMap(this.metricsStorage);
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return Collections.unmodifiableMap(this.metadataStorage);
    }

    @Override
    public Type getType() {
        return this.type;
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

    private Metadata sanitizeMetadataIfRequired(Metadata metadata, MetricType requiredType) {

        if (metadata.getTypeRaw() != requiredType) {
            return Metadata.builder(metadata).withType(requiredType).build();
        }

        return metadata;
    }
}

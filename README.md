# KumuluzEE Metrics
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-metrics/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-metrics)

> Metrics extension for the KumuluzEE microservice framework

KumuluzEE Metrics is a metrics collecting extension for the KumuluzEE microservice framework. It provides support for
collecting different system, application and user-defined metrics and exposing them in different ways. Metrics can be
exposed on a URL as a JSON object or in Prometheus format. 

KumuluzEE Metrics currently provides Logs and Logstash reporters and a servlet, which exposes metrics in JSON or
Prometheus format.

KumuluzEE Metrics includes automatic collection of JVM metrics. It supports easy definition and collection
of application specific metrics, as described below. 

The implementation is based on Dropwizard metrics. More information about their implementation can be found on
[github](https://github.com/dropwizard/metrics) or their [official page](http://metrics.dropwizard.io).

KumuluzEE Metrics is fully compliant with [MicroProfile Metrics 2.2.1](https://microprofile.io/project/eclipse/microprofile-metrics).

## Usage

You can enable the metrics extension by adding the following dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.metrics</groupId>
    <artifactId>kumuluzee-metrics-core</artifactId>
    <version>${kumuluzee-metrics.version}</version>
</dependency>
```

This extension requires the [KumuluzEE Config MicroProfile extension](https://github.com/kumuluz/kumuluzee-config-mp)
in order to work. You can add it to your project like so:

```xml
<dependency>
    <groupId>com.kumuluz.ee.config</groupId>
    <artifactId>kumuluzee-config-mp</artifactId>
    <version>${kumuluzee-config-mp.version}</version>
</dependency>
```

## Metric Types

There are several different measuring tools available: 

- Gauge: measures a simple value
- Counter: measures an integer, which can only increase
- ConcurrentGauge: measures an integer, which can increase and decrease, as well as the minimum and maximum value in the
  previous minute
- Histogram: measures the distribution of values in a stream of data
- Meter: measures the rate at which a set of events occur
- Timer: measures a histogram of the duration of a type of event and a meter of the rate of its occurrence

### Gauge

A `Gauge` is a measurement of a value at a certain time. A good example would be monitoring the number of jobs in a
queue:
```java
@Gauge(name = "queue_length_gauge")
private int getQueueLength() {
    return queue.length();
}
```

### Counter

A `Counter` measures a value that only increments (except on application restart).

```java
@Counted(name = "simple_counter")
public void foo() {
    ...
}
```

Methods annotated with `@Counted` increment a counter on every invocation and thus count total invocations.

A `Counter` can be programmatically updated, as shown in the following example:

```java
@Inject
@Metric(name = "simple_counter")
private Counter counter;

public void foo() {
    counter.inc(2);
}
```

### ConcurrentGauge

A `ConcurrentGauge` measures incrementing and decrementing value. It also measures the minimum and maximum value in the
previous minute. It is especially useful when counting ongoing invocations.

```java
@ConcurrentGauge(name = "simple_concurrent_gauge")
public void foo() {
    ...
}
```

A `ConcurrentGauge` can be programmatically updated, as shown in the following example:

```java
@Inject
@Metric(name = "simple_concurrent_gauge")
private ConcurrentGauge concurrentGauge;

public void foo() {
    concurrentGauge.inc();
    concurrentGauge.dec();
}
```

### Histogram

A `Histogram` measures the statistical distribution of values in a stream of data.
```java
@Inject
@Metric(name = "simple_histogram")
Histogram histogram;

public void logName(String name) {
    histogram.update(name.length());
}
```

### Meter

A `Meter` is used for measuring the rate of events over time. It logs the number of events and the average rate,
so it's very useful for things like monitoring method calls.

A good example is measuring requests per second:
```java
@Metered(name = "requests")
public void handleRequest(Request request, Response response) {
}
```

If you want more control, you can inject the meter separately and then call the `mark()` method.

```java
@Inject
@Metric(name = "requests")
Meter meter;

public void handleRequest(Request request, Response response) {
    meter.mark();
}
```

### Timer

A `Timer` measures how long a method or block of code takes to execute. Here is an example of a method timer:

```java
@Timed(name = "long_lasting_method")
public void longLasting() {
    ...
}
```

In the following example, only a part of the method is timed:

```java
@Inject
@Metric(name = "long_lasting_method")
private Timer timer;

public void longLasting() {
    ...
    final Timer.Context context = timer.time();
    try {
        // complex computations
        return;
    } finally {
        context.stop();
    }
    ...
}
```

## Metric names & metadata

Each metric name is prepended with the fully qualified class name, so they can be easily differentiated between classes
and applications. To register a metric without the prepended class name, set the `absolute` parameter in `@Metric`
annotation to `true`.

Alongside metric's name, the following metadata is stored for each metric:
- `displayName` - The display friendly name of the metric. If not set, the metric's name is used.
- `description` - A human readable description. Default: `""`.
- `unit` - Unit of the metric. Default: `MetricUnits.NONE`.
- `tags` - Tags of the metric.
- `reusable` - Is the metric reusable. If `false`, the metric can not be registered more than once with the same name
  and scope. This is done to prevent hard to spot copy & paste errors. If this behaviour is required, all such metrics
  should have the `reusable` flag set to true. Default: `false`.

Metadata is specified using annotation parameters in `@Metric`, `@Gauge`, `@Counted`, `@Metered` and `@Timed`
annotations.

### Metric tags

Global metrics tags can be added by specifying them in the `MP_METRICS_TAGS` MicroProfile Config property. Since
MicroProfile Config translates configuration sources to the KumuluzEE Configuration, the `MP_METRICS_TAGS` property can
be defined in any KumuluzEE configuration source as well.

Example of specifying tags in an environment variable:

```bash
$ export MP_METRICS_TAGS=app=shop,tier=integration
```

By default the following tags are added to every metric:
- `environment` - Environment in which the service is running. Read from the configuration key `kumuluzee.env.name`.
  Default value `dev`.
- `serviceName` - Name of the service. Read from the configuration key `kumuluzee.name`. Default value `UNKNOWN`.
- `serviceVersion` - Version of the service. Read from the configuration key `kumuluzee.version`. Default value `1.0.0`.
- `instanceId` - UUID of the service. Changes every time the service is started.

To disable these default tags, set the configuration key `kumuluzee.metrics.add-default-tags` to `false`.

## Metric Registries

Metric registries are used for grouping metrics. All metrics from annotated methods and fields are stored in a generic
registry called `application`.

The `base` registry contains metrics, defined in the Microprofile Metrics specification.
(see [Base metrics](#base-metrics))

To register a metric in the `application` registry, use the following code:

```java
@Inject
private MetricRegistry injectedRegistry;

private MetricRegistry registry = MetricRegistryProducer.getApplicationRegistry();
```

And then create a new metric within the registry, for example:

```java
private Counter evictions = registry.counter(MetricRegistry.name(SessionStore.class, "cache-evictions"));
```

## Included monitoring tools

### Web Application Monitoring

The module also includes Web Application monitoring, which enables the instrumentation of all requests at a defined 
endpoint. This includes counting the number of responses by status code and the time it took to process the request.
You can enable Web Application monitoring on multiple endpoints by defining the following configuration keys:
- `kumuluzee.metrics.web-instrumentation[x].name`: Name of the Web Application monitoring. All metrics, collected for
  the defined web instrumentation, will have this value in their name.
- `kumuluzee.metrics.web-instrumentation[x].url-pattern`: All requests, matching this pattern will be instrumented.
- `kumuluzee.metrics.web-instrumentation[x].status-codes`: Comma separated list of status codes. For each status code
  in the list, a separate meter metering the number of responses will be created. Default value:
  `200,201,204,400,404,500`.

Here is an example of monitoring two different urls:

```yaml
kumuluzee:
    metrics:
        web-instrumentation:
          - name: metrics-endpoint
            url-pattern: /metrics/*
            status-codes: "200, 500"
          - name: prometheus-endpoint
            url-pattern: /prometheus/*
```

Web Application metrics will be reported in the `vendor` registry, prefixed with `web-instrumentation.<monitoring-name>`.

### Base metrics

Base metrics are included in the `base` registry. They contain various metrics about the Java Virtual Machine like
memory consumption and thread counts.

## Servlet

The common module includes a servlet, that exposes all the metrics in JSON format. The server is enabled by default
and can be configured using following configuration keys:
- `kumuluzee.metrics.servlet.enabled`: Is the servlet enabled. Default value is `true`.
- `kumuluzee.metrics.servlet.mapping`: URL on which the metrics are exposed. Default value is `/metrics/*`.

The servlet can only be accessed if the environment is set to `dev` (default) or if the `kumuluzee.debug` configuration
key is set to `true`.
You can also enable or disable the servlet during runtime by changing the debug configuration key.

Example of the configuration:

```yaml
kumuluzee:
    metrics:
        servlet:
          enabled: true
          mapping: /my-metrics-servlet
```

### JSON metrics

Servlet exposes the following endpoints, when the `Accept` header of the request is set to `application/json`:
- GET /metrics - Returns all registered metrics
- GET /metrics/{registry} - Returns metrics, registered in the specified scope
- GET /metrics/{registry}/{metric_name} - Returns metric, that matches the metric name for the specified registry
- OPTIONS /metrics - Returns all registered metrics' metadata
- OPTIONS /metrics/{registry} - Returns metrics' metadata for the metrics, registered in the specified scope
- OPTIONS /metrics/{registry}/{metric_name} - Returns metric's metadata, that matches the metric name for the specified
  registry

Note that when requesting a single metric all metrics registered under that name will be returned
(they must have different tags).

Example of the servlet output on GET request on `/metrics`:
```json
{
    "application": {
        "com.example.beans.TestBean.countedMethod": 10,
        "com.example.TestResource.countersTest": 1,
        "com.example.TestResource.timedMethod": {
            "count": 2,
            "meanRate": 0.4659556063121205,
            "oneMinRate": 0,
            "fiveMinRate": 0,
            "fifteenMinRate": 0,
            "min": 2073520,
            "max": 3837249,
            "mean": 2948770.6402594047,
            "stddev": 881839.6981422313,
            "p50": 2073520,
            "p75": 3837249,
            "p95": 3837249,
            "p98": 3837249,
            "p99": 3837249,
            "p999": 3837249
        }
    },
    "base": {
        "memory.committedHeap": 307232768,
        "thread.daemon.count": 12,
        "gc.PS-MarkSweep.count": 1,
        "classloader.totalLoadedClass.count": 4865,
        "thread.count": 21,
        "gc.PS-Scavenge.count": 3,
        "classloader.totalUnloadedClass.count": 0,
        "memory.maxHeap": 3713531904,
        "gc.PS-Scavenge.time": 33,
        "gc.PS-MarkSweep.time": 38,
        "memory.usedHeap": 35772696,
        "jvm.uptime": 7832
    }
}
```

Example of the servlet output on OPTIONS request on `/metrics`:
```json
{
    "application": {
        "com.example.TestResource.timedMethod": {
            "unit": "nanoseconds",
            "type": "counter",
            "description": "Times invocations of countersTest()",
            "displayName": "",
            "tags": ""
        },
        "com.example.beans.TestBean.countedMethod": {
            "unit": "none",
            "type": "counter",
            "description": "",
            "displayName": "com.example.beans.TestBean.countedMethod",
            "tags": ""
        },
        "com.example.TestResource.countersTest": {
            "unit": "none",
            "type": "counter",
            "description": "",
            "displayName": "",
            "tags": ""
        }
    },
    "base": {
        "thread.count": {
            "unit": "none",
            "type": "counter",
            "description": "Displays the current number of live threads including both daemon and non-daemon threads",
            "displayName": "Thread Count",
            "tags": ""
        },
        "memory.usedHeap": {
            "unit": "bytes",
            "type": "gauge",
            "description": "Displays the amount of used heap memory in bytes.",
            "displayName": "Used Heap Memory",
            "tags": ""
        },
        "jvm.uptime": {
            "unit": "milliseconds",
            "type": "gauge",
            "description": "Displays the start time of the Java virtual machine in milliseconds. This attribute displays the approximate time when the Java virtual machine started.",
            "displayName": "JVM Uptime",
            "tags": ""
        }
    }
}
```

### Prometheus metrics

Servlet exposes the following endpoints, when the `Accept` header of the request is set to anything else but
`application/json`:
- GET /metrics - Returns all registered metrics in Prometheus format
- GET /metrics/{registry} - Returns metrics, registered in the specified scope in Prometheus format
- GET /metrics/{registry}/{metric_name} - Returns metric, that matches the metric name for the specified registry
  in Prometheus format

Prometheus has to be configured to collect the exported metrics. Example static Prometheus job configuration for 3
services:

```yaml
- job_name: 'kumuluzee-metrics'
  metrics_path: /metrics
  static_configs:
    - targets: ['localhost:8080', 'localhost:8081', 'localhost:8082']
```

## Configuration

Most of the metrics components can be configured in the configuration file. In order to properly report metrics, the
following information about the service should be defined with the common configuration:
- `kumuluzee.name`: Name of the service.
- `kumuluzee.version`: Version of the service.
- `kumuluzee.env.name`: Name of the environment in which service is deployed.

Example of the metrics configuration is shown below:

```yaml
kumuluzee:
  name: metrics-sample
  version: 0.0.1
  env:
    name: test
  metrics:
    servlet:
      enabled: true
      mapping: /metrics

```

## Reporters

Reporters for Logs and Logstash can be enabled.

### Logs

The metrics can be reported to the available logging framework. To enable the Logs reporter, add the following
dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.metrics</groupId>
    <artifactId>kumuluzee-metrics-logs</artifactId>
    <version>${kumuluzee-metrics.version}</version>
</dependency>
```

Logs reporter can be configured using the following configuration keys:
- `kumuluzee.metrics.logs.enabled`: Is the Logs reporter enabled. Default value is `true`.
- `kumuluzee.metrics.logs.period-s`: Period in seconds, on which metrics are logged. The default value is `60`.
- `kumuluzee.metrics.logs.level`: Logging level. Default value is `FINE`.

Example of the Logs reporter configuration:

```yaml
kumuluzee:
    metrics:
        logs:
            period-s: 60
            level: INFO
```

The metrics are logged in the same JSON format as exposed by the servlet GET method alongside with service information.

### Logstash

To enable Logstash reporter, add the following dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.metrics</groupId>
    <artifactId>kumuluzee-metrics-logstash</artifactId>
    <version>${kumuluzee-metrics.version}</version>
</dependency>
```

Logstash reporter can be configured using the following configuration keys:
- `kumuluzee.metrics.logstash.enabled`: Is the Logstash reporter enabled. Default value is `true`.
- `kumuluzee.metrics.logstash.address`: Address of the Logstash server. Default value is `127.0.0.1`.
- `kumuluzee.metrics.logstash.port`: Port on which the Logstash server listens. Default value is `5000`.
- `kumuluzee.metrics.logstash.period-s`: Period in seconds, on which metrics are reported to Logstash. Default value is
  `60`.

Example of the Logstash reporter configuration:

```yaml
kumuluzee:
    metrics:
        logstash:
              address: 192.168.0.1
              port: 5043
              period-s: 15
```

Logstash `tcp` input needs to be defined with `json_lines` codec. Example Logstash configuration:

```
input {
	tcp {
		port => 5043
		codec => 'json_lines'
	}
}
```

## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/kumuluz/kumuluzee-metrics/releases)

## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-metrics/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the 
[guidelines](https://github.com/kumuluz/kumuluzee-metrics/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test 
alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT

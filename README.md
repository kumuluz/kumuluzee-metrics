# KumuluzEE Metrics
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-metrics/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-metrics)

> Metrics extension for the KumuluzEE microservice framework

KumuluzEE Metrics is a metrics collecting extension for the KumuluzEE microservice framework. It provides support for collecting different system, application and user-defined metrics and exposing them as in different ways. Metrics can be exposed on a URL, as a JSON object on in Prometheus format. 

KumuluzEE Metrics currently provides support for Prometheus, Graphite, Logs and Logstash reporters and a servlet, which exposes metrics in JSON or Prometheus format.

KumuluzEE Metrics includes modules for automatic collection of JVM and web application metrics. It supports easy definition and collection of application specific metrics, as described below. 

The implementation is based on Dropwizard metrics. More information about their implementation can be found on [github](https://github.com/dropwizard/metrics) or their [official page](http://metrics.dropwizard.io).

## Usage

You can enable the metrics extension by adding the following dependency:
```xml
<dependency>
    <groupId>com.kumuluz.ee.metrics</groupId>
    <artifactId>kumuluzee-metrics-core</artifactId>
    <version>${kumuluzee-metrics.version}</version>
</dependency>
```

## Metric Types

There are several different measuring tools available: 

- Gauge: measures a simple value
- Counter: measures an integer, which can increase and decrease
- Histogram: measures the distribution of values in a stream of data
- Meter: measures the rate at which a set of events occur
- Timer: measures a histogram of the duration of a type of event and a meter of the rate of its occurance

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

A `Counter` measures incrementing and decrementing value.

```java
@Counted(name = "simple_counter")
public void foo() {
    ...
}
```

A `Counter` can be programmatically updated, as shown in the following example:

```java
@Inject
@Metric(name = "simple_counter")
private Counter counter;

public void foo() {
    counter.inc(2);
    counter.dec();
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

If you want a little more control, you can inject the meter separately and then call the `mark()` method.

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

## Metric names

Each metric name is prepended with fully qualified class name, so they can be easily differentiated between classes and
applications.

## Metric Registries

Metric registries are used for grouping metrics. All metrics from annotated methods or fields are stored in a generic
registry called `defaultRegistry`. Generic registry name can be changed by specifying the configuration key
`kumuluzee.metrics.generic-registry-name`.

To register a metric in a different registry, use the following code to create a registry:

```java
private final MetricRegistry registry = KumuluzEEMetricRegistries.getOrCreate("my_custom_registry");
```

And then create a new metric within the registry, for example:

```java
private final Counter evictions = registry.counter(name(SessionStore.class, "cache-evictions"));
```

Usually, you can store all your metrics in a default registry. The only time you need to separate them is, when you want
to enable and disable registries, that are being reported.

## Included monitoring tools

### Web Application Monitoring

The module also includes Web Application monitoring, which enables the instrumentation of all requests at a defined 
endpoint. This includes counting the number of responses by status code and the time it took to process the request.
You can enable Web Application monitoring on multiple endpoints by defining the following configuration keys:
- `kumuluzee.metrics.web-instrumentation[x].name`: Name of the Web Application monitoring. All metrics, collected for
defined web instrumentation, will have this value set for their name.
- `kumuluzee.metrics.web-instrumentation[x].url-pattern`: All requests, matching this pattern will be instrumented.
- `kumuluzee.metrics.web-instrumentation[x].registry-name`: Name of the registry, in which collected metrics will be
stored. By default, metrics are stored in the generic registry.

Here is an example of monitoring two different urls:

```yaml
kumuluzee:
    metrics:
        web-instrumentation:
          - name: metrics-endpoint
            url-pattern: /metrics/*
            registry-name: customRegistry
          - name: prometheus-endpoint
            url-pattern: /prometheus/*
```

### JVM Monitoring

Java Virtual Machine monitoring is enabled by default. To configure `JVM` monitoring, you can do so by specifying
following configuration keys:
- `kumuluzee.metrics.jvm.enabled`: Is JVM monitoring enabled. Default value is `true`.
- `kumuluzee.metrics.jvm.registry-name`: Name of the registry, in which JVM metrics will be stored. By default, JVM
metrics are stored in the registry named `jvm`.

Example of the configuration:

```yaml
kumuluzee:
    metrics:
        jvm:
          enabled: true
          registry-name: myJvm
```

JVM monitoring can also be [enabled or disabled](#enable_disable_registries) through metrics endpoint by using query 
parameters `enable` or `disable`.

## Servlet

The common module includes a servlet, that exposes all the metrics in a json format. The server is enabled by default
and can be configured using following configuration keys:
- `kumuluzee.metrics.servlet.enabled`: Is the servlet enabled. Default value is `true`.
- `kumuluzee.metrics.servlet.mapping`: URL on which the metrics are exposed. Default value is `/metrics`.

The servlet can only be accessed if the environment is set to `dev` or if the debug value is set to `true`. You can also enable or disable the servlet during runtime by changing the debug value in the etcd.

Example of the configuration:

```yaml
kumuluzee:
    metrics:
        servlet:
          enabled: true
          mapping: /my-servlet-metrics
```

Example of the servlet output:
```json
{
  "service" : {
    "timestamp" : "2017-07-13T16:12:07.309Z",
    "environment" : "dev",
    "name" : "metrics-sample",
    "version" : "0.0.7",
    "instance" : "a634850e-1787-4e6f-aa74-1e29ca587a38",
    "availableRegistries" : [ "jvm", "default", "registry3" ]
  },
  "registries" : {
    "jvm" : {
      "version" : "3.1.3",
      "gauges" : {
        "GarbageCollector.PS-MarkSweep.count" : {
          "value" : 1
        }
      }
    },
    "default" : {
      "version" : "3.1.3",
      "gauges" : {
        "com.kumuluz.ee.samples.kumuluzee_metrics.CustomerResource.customer_count_gauge" : {
          "value" : 5
        }
      },
      "meters" : {
        "ServletMetricsFilter.metrics-endpoint.responseCodes.ok" : {
          "count" : 2,
          "m15_rate" : 0.002179735240776932,
          "m1_rate" : 0.025690219862652606,
          "m5_rate" : 0.006296838645263653,
          "mean_rate" : 0.0015924774955522764,
          "units" : "events/second"
        }
      }
    }
  }
}
```

The output includes service information under the `service` object. Service information includes a timestamp,
environment, service name, a version, an instance ID and available registries. Available registries array lists all 
defined registries, including the ones that are disabled and not shown under registries.

The second object is contains registries with their underlying metric types. Each type contains metrics, registered
within registry.

### Query Parameters

Servlet endpoint support the following functions using GET parameters:

#### Filter shown registries

To manipulate the servlet json output, use the `id` parameter. 
For example, if you only want to show the `my_custom_registry` registry, you would use the `id` parameter like so:
`/metrics?id=my_custom_registry`. 
You can also filter two or more registries by adding multiple ids: `/metrics?id=registry_1&id=registry_2`. 

#### <a name="enable_disable_registries"></a> Enable/Disable registries

You can also enable or disable reporting of any registry by adding the `enable` and `disable` parameter respectively:
 - disable example: `/metrics?disable=my_registry`
 - enable example: `/metrics?enable=my_registry`

The registries, enabled and disabled through servlet endpoint are also enabled or disabled on all configured reporters.

## Configuration

Most of the metrics components can be configured in the configuration file. In order to properly report metrics, the
following information about the service should be defined with the common configuration:
- `kumuluzee.name`: Name of the service.
- `kumuluzee.version`: Version of the service.
- `kumuluzee.env.name`: Name of the environment in which service is deployed.

The name of the default registry can be defined by specifying configuration key
`kumuluzee.metrics.generic-registry-name`. Default value is `default`.

Configuration options for JVM monitoring, metrics servlet and Web Instrumentation are described in their dedicated
chapters.

Example if the metrics configuration is shown below:

```yaml
kumuluzee:
  name: metrics-sample
  version: 0.0.1
  env:
    name: test
  metrics:
    genericregistryname: default
    jvm:
      enabled: true
      registry: jvm
    servlet:
      enabled: true
      mapping: /metrics
    webinstrumentation:
      - name: metrics-endpoint
        urlpattern: /metrics/*
        registryname: default
      - name: prometheus-endpoint
        urlpattern: /prometheus/*

```

## Reporters

Reporters for Graphite and Logstash can be enabled, as well as servlet, which exposes metrics in Prometheus format.

### Prometheus

To enable servlet, which exposes metrics in the Prometheus format, add the following dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.metrics</groupId>
    <artifactId>kumuluzee-metrics-prometheus</artifactId>
    <version>${kumuluzee-metrics.version}</version>
</dependency>
```

The servlet can be configured using the following configuration key:
- `kumuluzee.metrics.prometheus.mapping`: URL on which the metrics in Prometheus format are exposed. Default value is
`/prometheus`.

Example of the configuration:

```yaml
kumuluzee:
    metrics:
        prometheus:
          mapping: /prometheus
```

Prometheus has to be configured to collect the exported metrics. Example static Prometheus job configuration for 3
services:

```yaml
- job_name: 'kumuluzee-metrics'
  metrics_path: /prometheus
  static_configs:
    - targets: ['localhost:8080', 'localhost:8081', 'localhost:8082']
```

Metrics are exported with their name, prefixed by `KumuluzEE_`. All special characters except for `_` and `:` are
converted to `_`. Service information is reported through the metric labels.
Here is an example of the metric:
`KumuluzEE_com_kumuluz_ee_samples_kumuluzee_metrics_CustomerResource_customer_counter{environment="dev",serviceName="metrics-sample",serviceVersion="0.0.7",instanceId="instance1",} 5.0`

### Graphite

To enable Graphite reporter, add the following dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.metrics</groupId>
    <artifactId>kumuluzee-metrics-graphite</artifactId>
    <version>${kumuluzee-metrics.version}</version>
</dependency>
```

The Graphite reporter is configured with the following configuration keys:
- `kumuluzee.metrics.graphite.address`: Address of the Graphite server. Default value is `127.0.0.1`.
- `kumuluzee.metrics.graphite.port`: Port on which the Graphite server listens. Default value is `2003` if pickle
parameter is set to `false`, otherwise `2004`.
- `kumuluzee.metrics.graphite.period-s`: Period in seconds, on which metrics are reported to Graphite. Default value is
`60`.
- `kumuluzee.metrics.graphite.pickle`: Use
[pickle protocol](http://graphite.readthedocs.io/en/latest/feeding-carbon.html#the-pickle-protocol). Default value is
`true`.

Example of the configuration:

```yaml
kumuluzee:
    metrics:
        graphite:
            address: 192.168.0.1
            #port: 2003
            periods: 5
            pickle: true
```

The naming scheme for this measuring tools is `KumuluzEE`, followed by the environment, service name, version, instance
ID and the metric name, all dot separated. Here is an example of a metric's name:
`KumuluzEE.dev.metrics-sample.0_0_7.instance1.MemoryUsage`.

### Logs

The metrics can be reported to the available logging framework. To enable the Logs reporter, add the following dependency:

```xml
<dependency>
    <groupId>com.kumuluz.ee.metrics</groupId>
    <artifactId>kumuluzee-metrics-logs</artifactId>
    <version>${kumuluzee-metrics.version}</version>
</dependency>
```

Logs reporter can be configured using the following configuration keys:
- `kumuluzee.metrics.logs.period-s`: Period in seconds, on which metrics are logged. The default value is `60`.
- `kumuluzee.metrics.logs.level`: Logging level. Default value is `FINE`.

```yaml
kumuluzee:
    metrics:
        logstash:
            logs:
            period-s: 60
            level: INFO
```

The metrics are logged in the same json format as in the servlet.

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
- `kumuluzee.metrics.logstash.address`: Address of the Logstash server. Default value is `127.0.0.1`.
- `kumuluzee.metrics.logstash.port`: Port on which the Logstash server listens. Default value is `5000`.
- `kumuluzee.metrics.logstash.period-s`: Period in seconds, on which metrics are reported to Logstash. Default value is
`60`.

```yaml
kumuluzee:
    metrics:
        logstash:
              address: 192.168.0.1
              port: 5043
              period-s: 15
```

Logstash `tcp` input needs to be defined with `json` codec. Example Logstash configuration:

```
input {
	tcp {
		port => 5043
		codec => 'json'
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

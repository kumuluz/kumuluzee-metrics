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
package com.kumuluz.ee.metrics.apps.resources;

import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Resource that contains all possible metrics.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
@Path("metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class MetricsResource {

    @Inject
    @Metric(absolute = true, name = "test-meter")
    private Meter testMeter;

    @Inject
    @Metric(absolute = true, name = "test-concurrent-gauge")
    private ConcurrentGauge testConcurrentGauge;

    @Inject
    @Metric(absolute = true, name = "test-counter")
    private Counter testCounter;

    @Inject
    @Metric(absolute = true, name = "test-histogram")
    private Histogram testHistogram;

    @Inject
    @Metric(absolute = true, name = "test-timer")
    private Timer testTimer;

    @GET
    @org.eclipse.microprofile.metrics.annotation.ConcurrentGauge(absolute = true, name = "test-concurrent-gauge-annotation")
    @Counted(absolute = true, name = "test-counter-annotation")
    @org.eclipse.microprofile.metrics.annotation.Metered(absolute = true, name = "test-meter-annotation")
    @Timed(absolute = true, name = "test-timer-annotation")
    public Response updateMetrics() {
        testMeter.mark();
        testConcurrentGauge.inc();
        testCounter.inc();
        testHistogram.update(123);
        testTimer.update(Duration.of(100, ChronoUnit.SECONDS));

        return Response.ok(testGauge()).build();
    }

    @org.eclipse.microprofile.metrics.annotation.Gauge(unit = MetricUnits.NONE, absolute = true, name = "test-gauge")
    private int testGauge() {
        return 42;
    }
}

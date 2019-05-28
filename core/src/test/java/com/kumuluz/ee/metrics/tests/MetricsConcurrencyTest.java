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
package com.kumuluz.ee.metrics.tests;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Adds required dependencies to the deployments.
 *
 * @author gpor89
 * @since 1.1.0
 */
@RunWith(Arquillian.class)
public class MetricsConcurrencyTest {

    private static final Metadata METRIC = new Metadata("myMetric", MetricType.COUNTER);

    static {
        METRIC.setReusable(false);
    }

    @Inject
    private MetricRegistry kumuluzMetricRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class);
    }

    @Test
    public void kumuluzTest() throws Exception {
        final AtomicBoolean fail = new AtomicBoolean();
        fail.set(false);

        int n = 1000;
        ExecutorService es = Executors.newCachedThreadPool();
        final CyclicBarrier gate = new CyclicBarrier(n + 1);

        List<Thread> threadList = new LinkedList<>();
        for (int j = 0; j < n; j++) {
            threadList.add(new Thread(() -> {
                try {
                    gate.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                    fail.set(true);
                }
                try {
                    kumuluzMetricRegistry.counter(METRIC).inc();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail.set(true);
                }
            }));
        }

        for (Thread t : threadList) {
            es.execute(t);
        }

        //executes counts concurrently
        gate.await();

        es.shutdown();
        boolean finished = es.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue("Threads not finished in given time", finished);

        final Long count = kumuluzMetricRegistry.getCounters().get(METRIC.getName()).getCount();

        assertEquals("Metric count is not equal", Long.valueOf(n), count);
        assertFalse(fail.get());
    }
}

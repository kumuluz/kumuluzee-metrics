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

import com.codahale.metrics.Clock;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

/**
 * Microprofile Timer implementation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class TimerImpl implements Timer {

    private final com.codahale.metrics.Timer timer;
    private final SumKeepingReservoir reservoir;

    public TimerImpl() {
        Clock clock = Clock.defaultClock();
        this.reservoir = new SumKeepingReservoir();
        this.timer = new com.codahale.metrics.Timer(new Meter(clock), new Histogram(this.reservoir), clock);
    }

    public TimerImpl(com.codahale.metrics.Timer timer) {
        this.reservoir = null;
        this.timer = timer;
    }

    @Override
    public void update(Duration duration) {
        this.timer.update(duration);
    }

    @Override
    public <T> T time(Callable<T> callable) throws Exception {
        return this.timer.time(callable);
    }

    @Override
    public void time(Runnable runnable) {
        this.timer.time(runnable);
    }

    @Override
    public Context time() {
        return new ContextImpl(this.timer.time());
    }

    @Override
    public Duration getElapsedTime() {
        return this.reservoir != null ? Duration.ofNanos(this.reservoir.sum.longValue()) : null;
    }

    @Override
    public long getCount() {
        return this.timer.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return this.timer.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return this.timer.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return this.timer.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return this.timer.getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        return new SnapshotImpl(this.timer.getSnapshot());
    }

    private static class SumKeepingReservoir extends ExponentiallyDecayingReservoir {

        private final LongAdder sum = new LongAdder();

        @Override
        public void update(long value) {
            super.update(value);
            sum.add(value);
        }
    }
}

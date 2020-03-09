/*
 *  Copyright (c) 2014-2020 Kumuluz and/or its affiliates
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
import org.eclipse.microprofile.metrics.SimpleTimer;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Microprofile SimpleTimer implementation.
 *
 * @author Aljaž Pavišič
 * @since 2.3.0
 */
public class SimpleTimerImpl implements SimpleTimer {

    private Clock clock;

    private Duration elapsedTime;

    private LongAdder count;

    public SimpleTimerImpl() {
        this.clock = Clock.defaultClock();
        this.elapsedTime = Duration.ZERO;
        this.count = new LongAdder();
    }

    @Override
    public void update(Duration duration) {
        inc(1);
        this.elapsedTime = this.elapsedTime.plus(duration);
    }

    @Override
    public <T> T time(Callable<T> callable) throws Exception {
        final long startTime = clock.getTick();
        try {
            return callable.call();
        }
        finally {
            update(clock.getTick() - startTime);
        }
    }

    @Override
    public void time(Runnable runnable) {
        final long startTime = clock.getTick();
        try {
            runnable.run();
        } finally {
            update(clock.getTick() - startTime);
        }
    }

    @Override
    public Context time() {
        return new SimpleTimerContextImpl(this, clock);
    }

    @Override
    public Duration getElapsedTime() {
        return this.elapsedTime;
    }

    @Override
    public long getCount() {
        return this.count.sum();
    }

    public void update(long duration, TimeUnit timeUnit){
        update(timeUnit.toNanos(duration));
    }

    public void inc(long n){
        this.count.add(n);
    }

    public void dec(long n){
        this.count.add(-n);
    }

    private void update(long duration) {
        if (duration > 0) {
            inc(1);
            this.elapsedTime = this.elapsedTime.plusNanos(duration);
        }
    }
}

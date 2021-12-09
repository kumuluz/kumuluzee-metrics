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
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * Microprofile SimpleTimer implementation.
 *
 * @author Aljaž Pavišič
 * @author Urban Malc
 * @since 2.3.0
 */
public class SimpleTimerImpl implements SimpleTimer {

    private final Clock clock;

    private final LongAdder elapsedTimeNanos;
    private final LongAdder count;

    private long minMaxMinute;
    private Long minInPreviousMinute;
    private Long maxInPreviousMinute;
    private LongAccumulator minInMinute;
    private LongAccumulator maxInMinute;

    public SimpleTimerImpl() {
        this.clock = Clock.defaultClock();
        this.elapsedTimeNanos = new LongAdder();
        this.count = new LongAdder();

        minMaxMinute = 0;
        initMinMax();
        minInPreviousMinute = null;
        maxInPreviousMinute = null;
    }

    @Override
    public void update(Duration duration) {
        long nanos = duration.toNanos();

        this.count.increment();
        this.elapsedTimeNanos.add(nanos);

        checkTime();
        minInMinute.accumulate(nanos);
        maxInMinute.accumulate(nanos);
    }

    @Override
    public <T> T time(Callable<T> callable) throws Exception {
        final long startTime = clock.getTick();
        try {
            return callable.call();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    @Override
    public void time(Runnable runnable) {
        final long startTime = clock.getTick();
        try {
            runnable.run();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    @Override
    public Context time() {
        return new SimpleTimerContextImpl(this, clock);
    }

    @Override
    public Duration getElapsedTime() {
        return Duration.ofNanos(this.elapsedTimeNanos.longValue());
    }

    @Override
    public long getCount() {
        return this.count.longValue();
    }

    @Override
    public Duration getMaxTimeDuration() {
        checkTime();
        return this.maxInPreviousMinute == null ? null : Duration.ofNanos(this.maxInPreviousMinute);
    }

    @Override
    public Duration getMinTimeDuration() {
        checkTime();
        return this.minInPreviousMinute == null ? null : Duration.ofNanos(this.minInPreviousMinute);
    }

    private void initMinMax() {
        minInMinute = new LongAccumulator(Long::min, Long.MAX_VALUE);
        maxInMinute = new LongAccumulator(Long::max, Long.MIN_VALUE);
    }

    private synchronized void checkTime() {

        long currentMinute = this.clock.getTime() / (1000 * 60);

        if (minMaxMinute < currentMinute) {
            long result = this.minInMinute.get();
            this.minInPreviousMinute = (result == Long.MAX_VALUE) ? null : result;
            result = this.maxInMinute.get();
            this.maxInPreviousMinute = (result == Long.MIN_VALUE) ? null : result;

            initMinMax();
            minMaxMinute = currentMinute;
        }
    }
}

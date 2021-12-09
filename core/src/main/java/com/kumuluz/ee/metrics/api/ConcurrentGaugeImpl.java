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
import org.eclipse.microprofile.metrics.ConcurrentGauge;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * Implementation of {@link ConcurrentGauge}.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class ConcurrentGaugeImpl implements ConcurrentGauge {

    private final Clock clock;
    private final AtomicLong count;

    private long minMaxMinute;
    private long minInPreviousMinute;
    private long maxInPreviousMinute;

    private LongAccumulator curMin;
    private LongAccumulator curMax;

    public ConcurrentGaugeImpl() {
        this.clock = Clock.defaultClock();
        this.count = new AtomicLong(0);
        this.minInPreviousMinute = 0;
        this.maxInPreviousMinute = 0;
        initMinMax();
        this.minMaxMinute = 0;
    }

    @Override
    public long getCount() {
        return count.get();
    }

    @Override
    public long getMax() {
        checkTime();
        return maxInPreviousMinute;
    }

    @Override
    public long getMin() {
        checkTime();
        return minInPreviousMinute;
    }

    @Override
    public void inc() {
        checkTime();
        long cur = count.incrementAndGet();
        curMax.accumulate(cur);
    }

    @Override
    public void dec() {
        checkTime();
        long cur = count.decrementAndGet();
        curMin.accumulate(cur);
    }

    private void initMinMax() {
        long currentCount = this.count.get();
        curMin = new LongAccumulator(Long::min, Long.MAX_VALUE);
        curMin.accumulate(currentCount);
        curMax = new LongAccumulator(Long::max, Long.MIN_VALUE);
        curMax.accumulate(currentCount);
    }

    private synchronized void checkTime() {

        long currentMinute = this.clock.getTime() / (1000 * 60);

        if (currentMinute > minMaxMinute) {
            minMaxMinute = currentMinute;
            minInPreviousMinute = curMin.get();
            maxInPreviousMinute = curMax.get();
            initMinMax();
        }
    }
}

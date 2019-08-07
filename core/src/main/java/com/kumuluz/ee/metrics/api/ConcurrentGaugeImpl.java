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

import org.eclipse.microprofile.metrics.ConcurrentGauge;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link ConcurrentGauge}.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class ConcurrentGaugeImpl implements ConcurrentGauge {

    private AtomicLong count;

    private long prevMinuteMin;
    private long prevMinuteMax;

    private AtomicLong curMin;
    private AtomicLong curMax;

    private AtomicLong timestamp;

    public ConcurrentGaugeImpl() {
        this.count = new AtomicLong(0);
        this.prevMinuteMin = 0;
        this.prevMinuteMax = 0;
        this.curMin = new AtomicLong(0);
        this.curMax = new AtomicLong(0);
        this.timestamp = new AtomicLong(getTimestamp());
    }

    @Override
    public long getCount() {
        return count.get();
    }

    @Override
    public long getMax() {
        checkTime();
        return prevMinuteMax;
    }

    @Override
    public long getMin() {
        checkTime();
        return prevMinuteMin;
    }

    @Override
    public void inc() {
        checkTime();
        incAndCheckLimits();
    }

    private synchronized void incAndCheckLimits() {
        long cur = count.incrementAndGet();
        if (cur > curMax.get()) {
            curMax.set(cur);
        }
    }

    @Override
    public void dec() {
        checkTime();
        decAndCheckLimits();
    }

    private synchronized void decAndCheckLimits() {
        long cur = count.decrementAndGet();
        if (cur < curMin.get()) {
            curMin.set(cur);
        }
    }

    private void checkTime() {
        long curTime = getTimestamp();

        if (curTime > timestamp.get()) {
            updateTime(curTime);
        }
    }

    private synchronized void updateTime(long curTime) {
        if (curTime > timestamp.get()) {
            timestamp.set(curTime);
            prevMinuteMin = curMin.get();
            prevMinuteMax = curMax.get();
            curMin.set(count.get());
            curMax.set(count.get());
        }
    }

    private long getTimestamp() {
        return System.currentTimeMillis() / (60 * 1000);
    }
}

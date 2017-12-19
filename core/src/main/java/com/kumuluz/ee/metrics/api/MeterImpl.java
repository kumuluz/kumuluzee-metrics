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

import org.eclipse.microprofile.metrics.Meter;

/**
 * Microprofile Meter implementation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class MeterImpl implements Meter {

    private com.codahale.metrics.Meter meter;

    public MeterImpl() {
        this.meter = new com.codahale.metrics.Meter();
    }

    public MeterImpl(com.codahale.metrics.Meter meter) {
        this.meter = meter;
    }

    @Override
    public void mark() {
        this.meter.mark();
    }

    @Override
    public void mark(long l) {
        this.meter.mark(l);
    }

    @Override
    public long getCount() {
        return this.meter.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return this.meter.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return this.meter.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return this.meter.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return this.meter.getOneMinuteRate();
    }
}

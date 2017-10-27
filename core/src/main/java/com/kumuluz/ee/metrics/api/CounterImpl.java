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

import com.codahale.metrics.Gauge;
import org.eclipse.microprofile.metrics.Counter;

/**
 * Microprofile Counter implementation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
public class CounterImpl implements Counter {

    private com.codahale.metrics.Counter counter;

    public CounterImpl() {
        this.counter = new com.codahale.metrics.Counter();
    }

    public CounterImpl(com.codahale.metrics.Counter counter) {
        this.counter = counter;
    }

    public CounterImpl(Gauge gauge) {
        this.counter = new com.codahale.metrics.Counter();
        this.counter.inc(((Number)gauge.getValue()).longValue());
    }

    @Override
    public void inc() {
        this.counter.inc();
    }

    @Override
    public void inc(long l) {
        this.counter.inc(l);
    }

    @Override
    public void dec() {
        this.counter.dec();
    }

    @Override
    public void dec(long l) {
        this.counter.dec(l);
    }

    @Override
    public long getCount() {
        return this.counter.getCount();
    }
}
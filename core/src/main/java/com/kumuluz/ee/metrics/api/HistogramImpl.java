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

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Snapshot;

/**
 * Microprofile Histogram implementation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
public class HistogramImpl implements Histogram {

    private com.codahale.metrics.Histogram histogram;

    public HistogramImpl() {
        this.histogram = new com.codahale.metrics.Histogram(new ExponentiallyDecayingReservoir());
    }

    public HistogramImpl(com.codahale.metrics.Histogram histogram) {
        this.histogram = histogram;
    }

    @Override
    public void update(int i) {
        this.histogram.update(i);
    }

    @Override
    public void update(long l) {
        this.histogram.update(l);
    }

    @Override
    public long getCount() {
        return this.histogram.getCount();
    }

    @Override
    public Snapshot getSnapshot() {
        return new SnapshotImpl(this.histogram.getSnapshot());
    }
}

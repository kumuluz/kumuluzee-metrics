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

import org.eclipse.microprofile.metrics.Snapshot;

import java.io.OutputStream;

/**
 * Microprofile Snapshot implementation.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
public class SnapshotImpl extends Snapshot {

    private com.codahale.metrics.Snapshot snapshot;

    public SnapshotImpl(com.codahale.metrics.Snapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public double getValue(double v) {
        return this.snapshot.getValue(v);
    }

    @Override
    public long[] getValues() {
        return this.snapshot.getValues();
    }

    @Override
    public int size() {
        return this.snapshot.size();
    }

    @Override
    public long getMax() {
        return this.snapshot.getMax();
    }

    @Override
    public double getMean() {
        return this.snapshot.getMean();
    }

    @Override
    public long getMin() {
        return this.snapshot.getMin();
    }

    @Override
    public double getStdDev() {
        return this.snapshot.getStdDev();
    }

    @Override
    public void dump(OutputStream outputStream) {
        this.snapshot.dump(outputStream);
    }
}

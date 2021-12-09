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

/**
 * Microprofile SimpleTimer.Context implementation.
 *
 * @author Aljaž Pavišič
 * @since 2.3.0
 */
public class SimpleTimerContextImpl implements SimpleTimer.Context, AutoCloseable {

    private final SimpleTimerImpl simpleTimer;
    private final Clock clock;
    private final long startTime;

    public SimpleTimerContextImpl(SimpleTimerImpl simpleTimer, Clock clock){
        this.simpleTimer = simpleTimer;
        this.clock = clock;
        this.startTime = clock.getTick();
    }

    @Override
    public long stop() {
        final long elapsed = clock.getTick() - startTime;
        simpleTimer.update(Duration.ofNanos(elapsed));
        return elapsed;
    }

    @Override
    public void close() {
        stop();
    }
}

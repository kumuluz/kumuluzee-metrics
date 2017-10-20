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
package com.kumuluz.ee.metrics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Schedules {@link LogstashSender}.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
public class KumuluzEELogstashReporter {

    private static final Logger log = Logger.getLogger(KumuluzEELogstashReporter.class.getName());

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture handle;

    private String address;
    private int port;
    private long periodSeconds;

    private int startRetryDelay;
    private int maxRetryDelay;

    public KumuluzEELogstashReporter(String address, int port, long periodSeconds, int startRetryDelay,
                                     int maxRetryDelay) {
        this.address = address;
        this.port = port;
        this.periodSeconds = periodSeconds;
        this.startRetryDelay = startRetryDelay;
        this.maxRetryDelay = maxRetryDelay;
    }

    public void start() {
        log.info("Starting Logstash reporter.");
        LogstashSender sender = new LogstashSender(address, port, startRetryDelay, maxRetryDelay);
        handle = scheduler.scheduleWithFixedDelay(sender, 0, periodSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        handle.cancel(true);
    }
}

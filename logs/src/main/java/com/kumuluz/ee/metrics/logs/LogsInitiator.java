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
package com.kumuluz.ee.metrics.logs;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Initializes Logs reporter.
 *
 * @author Aljaž Blažej
 * @author Urban Malc
 */
@ApplicationScoped
public class LogsInitiator {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture handle;

    private void initialiseBean(@Observes @Initialized(ApplicationScoped.class) Object init) {
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        boolean enabled = configurationUtil.getBoolean("kumuluzee.metrics.logs.enabled").orElse(true);
        int periodSeconds = configurationUtil.getInteger("kumuluzee.metrics.logs.period-s").orElse(60);
        String level = configurationUtil.get("kumuluzee.metrics.logs.level").orElse("FINE");

        if (enabled) {
            LogsSender sender = new LogsSender(level);
            handle = scheduler.scheduleWithFixedDelay(sender, 0, periodSeconds, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    private void closeHandle() {
        handle.cancel(true);
    }
}

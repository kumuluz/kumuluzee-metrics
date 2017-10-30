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

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

/**
 * Initializes Logstash reporter.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 */
@ApplicationScoped
public class LogstashInitiator {

    private KumuluzEELogstashReporter logstashReporter;

    private void initialiseBean(@Observes @Initialized(ApplicationScoped.class) Object init) {

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        // start logstash reporter
        boolean enabled = configurationUtil.getBoolean("kumuluzee.metrics.logstash.enabled").orElse(true);
        if(enabled) {
            String address = configurationUtil.get("kumuluzee.metrics.logstash.address").orElse("127.0.0.1");
            int port = configurationUtil.getInteger("kumuluzee.metrics.logstash.port").orElse(5000);
            long periodSeconds = configurationUtil.getInteger("kumuluzee.metrics.logstash.period-s").orElse(60);

            int startRetryDelay = configurationUtil.getInteger("kumuluzee.metrics.logstash.start-retry-delay-ms")
                    .orElse(500);
            int maxRetryDelay = configurationUtil.getInteger("kumuluzee.metrics.logstash.max-retry-delay-ms")
                    .orElse(900000);

            logstashReporter = new KumuluzEELogstashReporter(address, port, periodSeconds,
                    startRetryDelay, maxRetryDelay);
            logstashReporter.start();
        }
    }

    @PreDestroy
    private void stopReporter() {
        logstashReporter.stop();
    }
}

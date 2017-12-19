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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.metrics.json.MetricsModule;
import com.kumuluz.ee.metrics.json.models.MetricsPayload;
import com.kumuluz.ee.metrics.producers.MetricRegistryProducer;
import org.eclipse.microprofile.metrics.MetricRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable, which logs metrics.
 *
 * @author Aljaž Blažej
 * @author Urban Malc
 * @since 1.0.0
 */
public class LogsSender implements Runnable {

    private static final Logger log = Logger.getLogger(LogsSender.class.getName());
    private Level level;

    private ObjectMapper mapper;

    public LogsSender(String level) {
        this.mapper = (new ObjectMapper()).registerModule(new MetricsModule(false));

        this.level = Level.parse(level.toUpperCase());
    }

    @Override
    public void run() {
        try {
            Map<String, MetricRegistry> registries = new HashMap<>();
            registries.put("application", MetricRegistryProducer.getApplicationRegistry());
            registries.put("base", MetricRegistryProducer.getBaseRegistry());
            registries.put("vendor", MetricRegistryProducer.getVendorRegistry());

            log.log(level, this.mapper.writer().writeValueAsString(new MetricsPayload(registries)));
        } catch (Exception exception) {
            log.log(Level.SEVERE, "An error occurred when trying to log metrics.", exception);
        }
    }
}

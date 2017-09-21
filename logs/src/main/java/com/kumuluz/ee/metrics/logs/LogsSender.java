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

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.metrics.json.ServletExportModule;
import com.kumuluz.ee.metrics.json.models.ServletExport;
import com.kumuluz.ee.metrics.utils.EnabledRegistries;
import com.kumuluz.ee.metrics.utils.KumuluzEEMetricRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable, which logs metrics.
 *
 * @author Aljaž Blažej, Urban Malc
 */
public class LogsSender implements Runnable {

    private static final Logger LOG = Logger.getLogger(LogsSender.class.getName());
    private static Level LEVEL;

    private ObjectMapper mapper;
    private EnabledRegistries enabledRegistries;

    public LogsSender(String level) {
        this.enabledRegistries = EnabledRegistries.getInstance();
        this.mapper = (new ObjectMapper()).registerModule(new ServletExportModule(TimeUnit.SECONDS, TimeUnit.SECONDS,
                true, MetricFilter.ALL));

        this.LEVEL = Level.parse(level.toUpperCase());
    }

    @Override
    public void run() {
        try {
            Map<String, MetricRegistry> registries = new HashMap<>();

            Set<String> registriesToSend = new HashSet<>(KumuluzEEMetricRegistries.names());
            registriesToSend.retainAll(enabledRegistries.getEnabledRegistries());

            for (String registryName : registriesToSend) {
                registries.put(registryName, KumuluzEEMetricRegistries.getOrCreate(registryName));
            }

            ServletExport servletExport = new ServletExport(KumuluzEEMetricRegistries.names(), registries);

            LOG.log(LEVEL, this.mapper.writer().writeValueAsString(servletExport));
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An error occurred when trying to log metrics.", exception);
        }
    }
}

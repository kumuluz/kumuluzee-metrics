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

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.common.runtime.EeRuntime;
import com.kumuluz.ee.logs.LogManager;
import com.kumuluz.ee.logs.Logger;
import com.kumuluz.ee.logs.enums.LogLevel;
import com.kumuluz.ee.metrics.json.ServletExportModule;
import com.kumuluz.ee.metrics.json.models.ServletExport;
import com.kumuluz.ee.metrics.utils.EnabledRegistries;
import com.kumuluz.ee.metrics.utils.KumuluzEEMetricRegistries;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Runnable, which sends metrics to KumuluzEE Logs.
 *
 * @author Aljaž Blažej, Urban Malc
 */
public class LogsSender implements Runnable {

    private static Logger log = null;
    private static java.util.logging.Logger JULLog = null;

    private EnabledRegistries enabledRegistries;
    private ObjectMapper mapper;
    private LogLevel level;
    private java.util.logging.Level JULLevel;
    private boolean kumuluzEELogs = false;

    public LogsSender(String loggingLevel) {
        this.enabledRegistries = EnabledRegistries.getInstance();
        this.mapper = (new ObjectMapper()).registerModule(new ServletExportModule(TimeUnit.SECONDS, TimeUnit.SECONDS,
                true, MetricFilter.ALL));

        List<String> availableExtensionGroups = EeRuntime.getInstance().getEeExtensions().stream().map(e -> e
                .getGroup()).collect(Collectors.toList());
        if (availableExtensionGroups.contains("logs")) {
            kumuluzEELogs = true;
        }

        if (kumuluzEELogs) {
            this.level = stringToLevel(loggingLevel);
            log = LogManager.getLogger(LogsSender.class.getName());
        } else {
            this.JULLevel = stringToJULLevel(loggingLevel);
            JULLog = java.util.logging.Logger.getLogger(LogsSender.class.getName());
        }
    }

    @Override
    public void run() {
        Map<String, MetricRegistry> registries = new HashMap<>();

        Set<String> registriesToSend = new HashSet<>(KumuluzEEMetricRegistries.names());
        registriesToSend.retainAll(enabledRegistries.getEnabledRegistries());

        for (String registryName : registriesToSend) {
            registries.put(registryName, KumuluzEEMetricRegistries.getOrCreate(registryName));
        }

        ServletExport servletExport = new ServletExport(KumuluzEEMetricRegistries.names(), registries);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            this.mapper.writer().writeValue(outputStream, servletExport);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (kumuluzEELogs) {
                log.log(level, outputStream.toString());
            } else {
                JULLog.log(JULLevel, outputStream.toString());
            }
        }
    }

    private LogLevel stringToLevel(String level) {
        String lowLevel = level.toLowerCase();
        switch (lowLevel) {
            case "finest":
                return LogLevel.FINEST;
            case "trace":
                return LogLevel.TRACE;
            case "debug":
                return LogLevel.DEBUG;
            case "info":
                return LogLevel.INFO;
            case "warn":
                return LogLevel.WARN;
            case "error":
                return LogLevel.ERROR;
            default:
                return LogLevel.DEBUG;
        }
    }

    private java.util.logging.Level stringToJULLevel(String level) {
        String lowLevel = level.toLowerCase();
        switch (lowLevel) {
            case "severe":
                return java.util.logging.Level.SEVERE;
            case "warning":
                return java.util.logging.Level.WARNING;
            case "info":
                return java.util.logging.Level.INFO;
            case "config":
                return java.util.logging.Level.CONFIG;
            case "fine":
                return java.util.logging.Level.FINE;
            case "finer":
                return java.util.logging.Level.FINER;
            case "finest":
                return java.util.logging.Level.FINEST;
            default:
                return java.util.logging.Level.FINE;
        }
    }
}

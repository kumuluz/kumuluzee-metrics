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

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.metrics.json.ServletExportModule;
import com.kumuluz.ee.metrics.json.models.ServletExport;
import com.kumuluz.ee.metrics.utils.EnabledRegistries;
import com.kumuluz.ee.metrics.utils.KumuluzEEMetricRegistries;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Runnable, which sends metrics to Logstash TCP port.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class LogstashSender implements Runnable {

    private static final Logger log = Logger.getLogger(LogstashSender.class.getName());

    private String address;
    private int port;
    private EnabledRegistries enabledRegistries;
    private ObjectMapper mapper;

    private Socket socket;
    private BufferedOutputStream outputStream = null;

    private int startRetryDelay;
    private int maxRetryDelay;
    private int currentRetryDelay;

    public LogstashSender(String address, int port, int startRetryDelay, int maxRetryDelay) {
        this.address = address;
        this.port = port;
        this.socket = null;
        this.enabledRegistries = EnabledRegistries.getInstance();
        this.startRetryDelay = this.currentRetryDelay = startRetryDelay;
        this.maxRetryDelay = maxRetryDelay;

        this.mapper = (new ObjectMapper(
                new JsonFactory().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)))
                .registerModule(new ServletExportModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false,
                        MetricFilter.ALL));
    }

    @Override
    public void run() {
        boolean success = false;
        while (!success) {
            try {
                if (socket == null || !socket.isConnected() || socket.isClosed()) {
                    socket = new Socket(address, port);
                    outputStream = new BufferedOutputStream(socket.getOutputStream());
                }

                Map<String, MetricRegistry> registries = new HashMap<>();
                for (String registryName : enabledRegistries.getEnabledRegistries()) {
                    registries.put(registryName, KumuluzEEMetricRegistries.getOrCreate(registryName));
                }
                ServletExport servletExport = new ServletExport(KumuluzEEMetricRegistries.names(), registries);

                this.mapper.writeValue(outputStream, servletExport);
                outputStream.write("\n".getBytes("UTF-8"));
                outputStream.flush();

                this.currentRetryDelay = startRetryDelay;
                success = true;
            } catch (IOException e) {
                log.severe("Cannot write metrics to Logstash: " + e.getLocalizedMessage());
                socket = null;
                try {
                    Thread.sleep(currentRetryDelay);
                } catch (InterruptedException ignored) {
                }

                // exponential increase, limited by maxRetryDelay
                currentRetryDelay *= 2;
                if (currentRetryDelay > maxRetryDelay) {
                    currentRetryDelay = maxRetryDelay;
                }
            }
        }
    }
}

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
package com.kumuluz.ee.metrics.json.models;

import com.kumuluz.ee.metrics.utils.ServiceConfigInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

/**
 * Model used for exporting service metadata.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class ServiceInfo {

    private final TimeZone tz = TimeZone.getTimeZone("UTC");
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    private String timestamp;
    private String environment;
    private String serviceName;
    private String serviceVersion;
    private String instanceId;
    private Set<String> availableRegistries;

    public ServiceInfo(Set<String> availableRegistries) {
        df.setTimeZone(tz);
        this.timestamp = df.format(new Date());
        this.environment = ServiceConfigInfo.getInstance().getEnvironment();
        this.serviceName = ServiceConfigInfo.getInstance().getServiceName();
        this.serviceVersion = ServiceConfigInfo.getInstance().getServiceVersion();
        this.instanceId = ServiceConfigInfo.getInstance().getInstanceId();

        this.availableRegistries = availableRegistries;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Set<String> getAvailableRegistries() {
        return availableRegistries;
    }
}

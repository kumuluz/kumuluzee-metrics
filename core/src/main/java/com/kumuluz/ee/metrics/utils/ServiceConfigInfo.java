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
package com.kumuluz.ee.metrics.utils;

import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.runtime.EeRuntime;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

/**
 * Configuration class, used for exporting metrics.
 *
 * @author Urban Malc
 * @author Aljaž Blažej
 * @since 1.0.0
 */
public class ServiceConfigInfo {

    private static ServiceConfigInfo instance = null;

    private String environment;
    private String serviceName;
    private String serviceVersion;
    private String instanceId;

    private boolean addToTags;

    private ServiceConfigInfo() {
        EeConfig eeConfig = EeConfig.getInstance();

        this.environment = eeConfig.getEnv().getName();
        if (this.environment == null || this.environment.isEmpty()) {
            this.environment = "dev";
        }

        this.serviceName = eeConfig.getName();
        if (this.serviceName == null || this.serviceName.isEmpty()) {
            this.serviceName = "UNKNOWN";
        }

        this.serviceVersion = eeConfig.getVersion();
        if (this.serviceVersion == null || this.serviceVersion.isEmpty()) {
            this.serviceVersion = "1.0.0";
        }

        this.instanceId = EeRuntime.getInstance().getInstanceId();

        this.addToTags = ConfigurationUtil.getInstance().getBoolean("kumuluzee.metrics.add-default-tags")
                .orElse(true);
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

    public static ServiceConfigInfo getInstance() {
        if (instance == null) {
            instance = new ServiceConfigInfo();
        }

        return instance;
    }

    public boolean shouldAddToTags() {
        return addToTags;
    }
}

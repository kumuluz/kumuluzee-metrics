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

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.Tag;

import java.util.Arrays;
import java.util.Map;

/**
 * Utility class for working with {@link MetricID}.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class MetricIdUtil {

    private static final ServiceConfigInfo configInfo = ServiceConfigInfo.getInstance();

    public static String metricIdToString(MetricID metricID) {
        return metricID.getName() + tagsToSuffix(metricID);
    }

    public static String tagsToSuffix(MetricID metricID) {

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> tag : metricID.getTags().entrySet()) {
            sb.append(";").append(tag.getKey()).append("=").append(tag.getValue().replaceAll(";", "_"));
        }
        sb.append(GlobalTagsUtil.getJsonGlobalTags());

        return sb.toString();
    }

    public static MetricID newMetricID(String name, Tag... tags) {

        if (configInfo.shouldAddToTags()) {
            int oldLen = tags.length;
            tags = Arrays.copyOf(tags, tags.length + 4);

            tags[oldLen] = new Tag("environment", configInfo.getEnvironment());
            tags[oldLen + 1] = new Tag("serviceName", configInfo.getServiceName());
            tags[oldLen + 2] = new Tag("serviceVersion", configInfo.getServiceVersion());
            tags[oldLen + 3] = new Tag("instanceId", configInfo.getInstanceId());
        }

        return new MetricID(name, tags);
    }
}

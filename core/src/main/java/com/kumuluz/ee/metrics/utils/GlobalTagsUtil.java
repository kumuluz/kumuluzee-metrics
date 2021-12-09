/*
 *  Copyright (c) 2014-2021 Kumuluz and/or its affiliates
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

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.eclipse.microprofile.metrics.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility for parsing and storing metrics global tags.
 *
 * @author Urban Malc
 * @since 3.0.0
 */
public class GlobalTagsUtil {

    private static final String GLOBAL_TAG_MALFORMED_EXCEPTION = "Malformed list of Global Tags. Tag names "
            + "must match the following regex [a-zA-Z_][a-zA-Z0-9_]*."
            + " Global Tag values must not be empty."
            + " Global Tag values MUST escape equal signs `=` and commas `,`"
            + " with a backslash `\\` ";

    private static final String GLOBAL_TAGS_VARIABLE = "mp.metrics.tags";
    private static final String APPLICATION_NAME_VARIABLE = "mp.metrics.appName";
    private static final String APPLICATION_NAME_TAG = "_app";

    private static final String PROMETHEUS_GLOBAL_TAGS;
    private static final String JSON_GLOBAL_TAGS;
    private static final List<Tag> GLOBAL_TAGS;

    static {
        ConfigurationUtil config = ConfigurationUtil.getInstance();

        Map<String, String> tags = new HashMap<>();

        config.get(GLOBAL_TAGS_VARIABLE).ifPresent(tagsVar -> parseGlobalTags(tagsVar, tags));
        config.get(APPLICATION_NAME_VARIABLE)
                .filter(s -> !s.isEmpty())
                .ifPresent(tagValue -> tags.put(APPLICATION_NAME_TAG, tagValue));

        PROMETHEUS_GLOBAL_TAGS = tags.entrySet().stream()
                .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(","));

        GLOBAL_TAGS = tags.entrySet().stream()
                .map(e -> new Tag(e.getKey(), e.getValue()))
                .collect(Collectors.toUnmodifiableList());

        JSON_GLOBAL_TAGS = tags.isEmpty() ? "" : ";" + tags.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().replace(";", "_"))
                .collect(Collectors.joining(";"));
    }

    public static String getPrometheusGlobalTags() {
        return PROMETHEUS_GLOBAL_TAGS;
    }
    public static String getJsonGlobalTags() {
        return JSON_GLOBAL_TAGS;
    }

    public static List<Tag> getGlobalTags() {
        return GLOBAL_TAGS;
    }

    private static void parseGlobalTags(String globalTags, Map<String, String> tags) throws IllegalArgumentException {

        if (globalTags == null || globalTags.length() == 0) {
            return;
        }

        String[] kvPairs = globalTags.split("(?<!\\\\),");
        for (String kvString : kvPairs) {

            if (kvString.length() == 0) {
                throw new IllegalArgumentException(GLOBAL_TAG_MALFORMED_EXCEPTION);
            }

            String[] keyValueSplit = kvString.split("(?<!\\\\)=");

            if (keyValueSplit.length != 2 || keyValueSplit[0].length() == 0 || keyValueSplit[1].length() == 0) {
                throw new IllegalArgumentException(GLOBAL_TAG_MALFORMED_EXCEPTION);
            }

            String key = keyValueSplit[0];
            String value = keyValueSplit[1];

            if (!key.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                throw new IllegalArgumentException("Invalid Tag name. Tag names must match the following regex "
                        + "[a-zA-Z_][a-zA-Z0-9_]*");
            }
            value = value.replace("\\,", ",");
            value = value.replace("\\=", "=");
            tags.put(key, value);
        }
    }
}

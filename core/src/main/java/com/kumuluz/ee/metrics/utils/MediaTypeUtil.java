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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for media type (Accept header) parsing.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class MediaTypeUtil {

    private static final String APP_JSON = "application/json";
    private static final String TEXT_PLAIN = "text/plain";
    private static final List<String> KNOWN_HEADERS = new LinkedList<>();

    static {
        KNOWN_HEADERS.add(APP_JSON);
        KNOWN_HEADERS.add(TEXT_PLAIN);
        KNOWN_HEADERS.add("*/*");
    }

    public enum ReturnType {
        JSON,
        PROMETHEUS,
        UNKNOWN
    }

    public static ReturnType parseMediaType(String headers) {
        List<MediaTypeTuple> parsedHeaders = new ArrayList<>();

        for (String header : headers.split(",")) {
            String[] headerElements = header.split(";");

            if (!KNOWN_HEADERS.contains(headerElements[0])) {
                continue;
            }

            double priority = 1.0;
            for (String el : headerElements) {
                if (el.startsWith("q=")) {
                    priority = Double.parseDouble(el.substring(2));
                }
            }

            parsedHeaders.add(new MediaTypeTuple(headerElements[0], priority));
        }

        if (parsedHeaders.size() == 0) {
            if (headers.length() > 0) {
                // no known headers
                return ReturnType.UNKNOWN;
            } else {
                // actually no headers
                return ReturnType.PROMETHEUS;
            }
        }

        List<MediaTypeTuple> primaryMediaTypes = new LinkedList<>();
        primaryMediaTypes.add(parsedHeaders.get(0));

        for (int i = 1; i < parsedHeaders.size(); i++) {
            MediaTypeTuple currentMediaType = parsedHeaders.get(i);

            if (Math.abs(currentMediaType.priority - primaryMediaTypes.get(0).priority) < 0.001) {
                primaryMediaTypes.add(currentMediaType);
            } else if (currentMediaType.priority > primaryMediaTypes.get(0).priority) {
                primaryMediaTypes.clear();
                primaryMediaTypes.add(currentMediaType);
            }
        }

        if (primaryMediaTypes.stream().map(mt -> mt.mediaType).anyMatch(TEXT_PLAIN::equals)) {
            return ReturnType.PROMETHEUS;
        } else if (primaryMediaTypes.stream().map(mt -> mt.mediaType).anyMatch(APP_JSON::equals)) {
            return ReturnType.JSON;
        } else {
            return ReturnType.PROMETHEUS;
        }
    }

    private static final class MediaTypeTuple {
        private final String mediaType;
        private final double priority;

        MediaTypeTuple(String mediaType, double priority) {
            this.mediaType = mediaType;
            this.priority = priority;
        }
    }
}

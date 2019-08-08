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

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Tag;

import java.util.LinkedList;
import java.util.List;

/**
 * Holder object for metadata and merged tags (for metrics with the same name and different tags).
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class MetadataWithMergedTags {

    private Metadata metadata;
    private List<List<Tag>> tags;

    public MetadataWithMergedTags(Metadata metadata) {
        this.metadata = metadata;
        this.tags = new LinkedList<>();
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void addTags(List<Tag> tags) {
        this.tags.add(tags);
    }

    public List<List<Tag>> getTags() {
        return tags;
    }
}

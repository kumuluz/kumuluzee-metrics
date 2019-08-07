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
package com.kumuluz.ee.metrics.apps.beans;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * Bean with two counters sharing the same name but different tags.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
@RequestScoped
public class SameNameBean {

    @Inject
    @Metric(absolute = true, name = "sameName", tags = {"test=1"})
    private Counter c1;

    @Inject
    @Metric(absolute = true, name = "sameName", tags = {"test=2"})
    private Counter c2;

    public void increaseFirst() {
        c1.inc();
    }

    public void increaseSecond() {
        c2.inc();
    }
}

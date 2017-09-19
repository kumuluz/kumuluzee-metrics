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

import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link DropwizardExports} with added labels containing service metadata.
 *
 * @author Urban Malc, Aljaž Blažej
 */
public class LabeledDropwizardExports extends DropwizardExports {

    private ServiceConfigInfo serviceConfigInfo;

    /**
     * @param registry a labeled metric registry to export in prometheus.
     */
    public LabeledDropwizardExports(MetricRegistry registry) {
        super(registry);

        serviceConfigInfo = ServiceConfigInfo.getInstance();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> receivedSamples = super.collect();
        List<MetricFamilySamples> labeledSamplesList = new ArrayList<>(receivedSamples.size());

        for (MetricFamilySamples samples : receivedSamples) {
            List<MetricFamilySamples.Sample> labeledSamples = new ArrayList<>(samples.samples.size());
            for (MetricFamilySamples.Sample sample : samples.samples) {
                List<String> newLabelNames = new ArrayList<>(sample.labelNames);
                newLabelNames.add("environment");
                newLabelNames.add("serviceName");
                newLabelNames.add("serviceVersion");
                newLabelNames.add("instanceId");

                List<String> newLabelValues = new ArrayList<>(sample.labelValues);
                newLabelValues.add(serviceConfigInfo.getEnvironment());
                newLabelValues.add(serviceConfigInfo.getServiceName());
                newLabelValues.add(serviceConfigInfo.getServiceVersion());
                newLabelValues.add(serviceConfigInfo.getInstanceId());

                MetricFamilySamples.Sample labeledSample = new MetricFamilySamples.Sample("KumuluzEE_" +
                        sample.name, newLabelNames, newLabelValues, sample.value);
                labeledSamples.add(labeledSample);
            }
            labeledSamplesList.add(new MetricFamilySamples(samples.name, samples.type, samples.help, labeledSamples));
        }

        return labeledSamplesList;
    }
}

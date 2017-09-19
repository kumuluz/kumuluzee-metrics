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

import com.codahale.metrics.MetricRegistry;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.metrics.utils.KumuluzEEMetricRegistries;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.EnumSet;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Initializes Metrics module.
 *
 * @author Urban Malc, Aljaž Blažej
 */
@ApplicationScoped
public class MetricsInitiator {

    private static final Logger log = Logger.getLogger(MetricsInitiator.class.getName());

    private static final String GENERIC_REGISTRY_NAME_CONFIG_KEY = "kumuluzee.metrics.generic-registry-name";
    private static final String GENERIC_REGISTRY_NAME = "defaultRegistry";

    private String genericRegistryName;

    // inject generic registry to CDI, so annotations can use it
    @Produces
    @ApplicationScoped
    public MetricRegistry getGenericRegistry() {
        return KumuluzEEMetricRegistries.getOrCreate(genericRegistryName);
    }

    private void initialiseBean(@Observes @Initialized(ApplicationScoped.class) Object init) {

        if (init instanceof ServletContext) {

            ServletContext servletContext = (ServletContext) init;
            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            genericRegistryName = configurationUtil.get(GENERIC_REGISTRY_NAME_CONFIG_KEY).orElse(GENERIC_REGISTRY_NAME);

            log.info("Initialising KumuluzEE Metrics");

            // register servlet
            boolean servletEnabled = configurationUtil.getBoolean("kumuluzee.metrics.servlet.enabled").orElse(true);
            if (servletEnabled) {
                String servletMapping = configurationUtil.get("kumuluzee.metrics.servlet.mapping").orElse("/metrics");

                log.info("Registering metrics servlet on " + servletMapping);
                ServletRegistration.Dynamic dynamicRegistration = servletContext.addServlet("metrics",
                        new KumuluzEEMetricsServlet());
                dynamicRegistration.addMapping(servletMapping);

                String environment = EeConfig.getInstance().getEnv().getName();

                boolean devEnv = "dev".equals(environment.toLowerCase());
                if (!devEnv) {
                    FilterRegistration.Dynamic debugFilter = servletContext.addFilter("debugFilter", new
                            KumuluzEEServletFilter());

                    debugFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, servletMapping);
                }
            }

            // register filters
            String webInstrumentationKey = "kumuluzee.metrics.web-instrumentation[%d]";
            Optional<String> urlPattern;
            int i = 0;
            while ((urlPattern = configurationUtil.get(String.format(webInstrumentationKey + ".url-pattern", i)))
                    .isPresent()) {
                Optional<String> filterName = configurationUtil.get(String.format(webInstrumentationKey + ".name", i));
                if (filterName.isPresent()) {
                    String registryName = configurationUtil.get(String.format(webInstrumentationKey + ".registry-name",
                            i)).orElse(genericRegistryName);

                    log.info("Registering metrics filter on " + urlPattern.get() + " with registry " + registryName +
                            ".");

                    String servletRegistryAttribute = "filter." + filterName + ".registry";
                    servletContext.setAttribute(servletRegistryAttribute, KumuluzEEMetricRegistries
                            .getOrCreate(registryName));
                    FilterRegistration.Dynamic filterRegistration = servletContext
                            .addFilter(filterName.get(), new KumuluzEEMetricsFilter(servletRegistryAttribute));
                    filterRegistration.setInitParameter("name-prefix", "ServletMetricsFilter." + filterName.get());
                    filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true,
                            urlPattern.get());
                }

                i++;
            }
        }
    }
}

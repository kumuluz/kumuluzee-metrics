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
package com.kumuluz.ee.metrics.tests;

import com.kumuluz.ee.metrics.apps.App;
import com.kumuluz.ee.metrics.apps.resources.MetricsResource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests default tags behaviour (kumuluzee.metrics.add-default-tags).
 *
 * @author Urban Malc
 * @since 2.0.0
 */
@RunWith(Arquillian.class)
public class DefaultTagsTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(App.class)
                .addClass(MetricsResource.class)
                .addAsResource("configs/default-tags-config.yml", "config.yml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @RunAsClient
    public void defaultTagsTest() {
        given()
                .header("Accept", "application/json")
        .when()
                .get("/metrics")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(containsString("jvm.uptime;environment=dev;"));
    }

    @Test
    @RunAsClient
    public void applicationMetricsDefaultTagsTest() {

        int noRequests = 10;

        for (int i = 0; i < noRequests; i++) {
            updateAppMetrics();
            validateAppMetrics();
        }

    }

    private void updateAppMetrics() {
        when()
                .get("/v1/metrics")
        .then()
                .statusCode(200);
    }

    private void validateAppMetrics() {
        given()
                .header("Accept", "application/json")
        .when()
                .get("/metrics")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(containsString("test-counter;environment=dev;"))
                .body(containsString("test-meter"))
                .body(containsString("test-counter-annotation;environment=dev"))
                .body(containsString("test-histogram"))
                .body(containsString("test-timer"))
                .body(containsString("test-gauge;environment=dev;"))
                .body(containsString("test-concurrent-gauge-annotation"))
                .body(containsString("test-meter-annotation"))
                .body(containsString("test-timer-annotation"))
                .body(containsString("test-concurrent-gauge"));
    }
}

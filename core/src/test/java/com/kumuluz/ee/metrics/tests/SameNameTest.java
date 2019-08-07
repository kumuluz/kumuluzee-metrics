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
import com.kumuluz.ee.metrics.apps.beans.SameNameBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

/**
 * Tests metrics registered under the same name but different tags. See also {@link SameNameBean}.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
@RunWith(Arquillian.class)
public class SameNameTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(App.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private SameNameBean bean;

    @Test
    @InSequence(1)
    public void sameMetricInitTest() {
        bean.increaseFirst();
        bean.increaseSecond();
        bean.increaseSecond();
    }

    @Test
    @InSequence(2)
    @RunAsClient
    public void sameMetricTest() {

        when()
                .get("/metrics")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(containsString("application_sameName_total{test=\"1\",tier=\"integration\"} 1"))
                .body(containsString("application_sameName_total{test=\"2\",tier=\"integration\"} 2"));

        when()
                .get("/metrics/application/sameName")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body(containsString("application_sameName_total{test=\"1\",tier=\"integration\"} 1"))
                .body(containsString("application_sameName_total{test=\"2\",tier=\"integration\"} 2"));
    }

    @Test
    @InSequence(3)
    @RunAsClient
    public void sameMetricOptionsTest() {
        given()
                .header("Accept", "application/json")
        .when()
                .options("/metrics/application/sameName")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("sameName.tags.size()", is(2))
                .body("sameName.tags.flatten()", hasItems("test=1", "test=2", "tier=integration"));
    }
}

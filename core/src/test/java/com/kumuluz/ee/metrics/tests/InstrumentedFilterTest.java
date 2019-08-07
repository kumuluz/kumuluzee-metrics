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
import com.kumuluz.ee.metrics.apps.resources.StatusCodeResource;
import io.restassured.response.ValidatableResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.LinkedList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

/**
 * Tests the {@link com.kumuluz.ee.metrics.filters.InstrumentedFilter} class.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
@RunWith(Arquillian.class)
public class InstrumentedFilterTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(App.class)
                .addClass(StatusCodeResource.class)
                .addAsResource("configs/instrumented-filter-config.yml", "config.yml")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    @RunAsClient
    public void instrumentedFilterTest() {

        int[] codes = {200, 201, 204, 400, 404, 500};
        int requestsPerCode = 20;

        // initially no requests
        for (int code : codes) {
            validateStatusCode(code, 0);
        }

        // perform requests and validate results
        for (int i = 0; i < requestsPerCode; i++) {
            for (int code : codes) {
                requestStatusCode(code);

                validateStatusCode(code, i + 1);
            }
        }

        // validate other codes
        validateOtherCode(0);
        requestStatusCode(403);
        requestStatusCode(203);
        requestStatusCode(503);
        validateOtherCode(3);

    }

    private void validateStatusCode(int statusCode, int expected) {
        performMetricsRequest()
                .log().ifValidationFails()
                .statusCode(200)
                .body("vendor.'web-instrumentation.test-endpoint.status'.'count;statusCode=" + statusCode + ";tier=integration'",
                        is(expected));
    }

    private void validateOtherCode(int expected) {
        performMetricsRequest()
                .log().ifValidationFails()
                .statusCode(200)
                .body("vendor.'web-instrumentation.test-endpoint.status'.'count;statusCode=other;tier=integration'",
                        is(expected));
    }

    private void requestStatusCode(int statusCode) {
        when()
                .get("/v1/statusCodes/" + statusCode)
                .then()
                .log().ifValidationFails()
                .statusCode(statusCode);
    }

    private ValidatableResponse performMetricsRequest() {
        return given().header("Accept", "application/json").when().get("/metrics").then();
    }

    @Test
    @RunAsClient
    public void activeRequestsTest() throws InterruptedException {
        // init latch
        when()
                .get("/v1/statusCodes/initManaged")
        .then()
                .statusCode(200);

        int noRequests = 20;

        // initially no active requests
        validateActiveRequests(0);
        List<Thread> threads = new LinkedList<>();
        for (int i = 0; i < noRequests; i++) {
            threads.add(addActiveRequest());
        }

        // wait for all threads to start
        Thread.sleep(500);

        // validate that the number of current requests matches
        validateActiveRequests(noRequests);

        // release latch
        when()
                .get("/v1/statusCodes/releaseManaged")
        .then()
                .statusCode(200);

        // wait for threads to finish
        for (Thread t : threads) {
            t.join();
        }

        // validate that there are no active requests left
        validateActiveRequests(0);
    }

    private Thread addActiveRequest() {
        Thread t = new Thread(() -> when().get("/v1/statusCodes/managed").then().statusCode(200));
        t.start();
        return t;
    }

    private void validateActiveRequests(int expected) {
        performMetricsRequest()
                .log().ifValidationFails()
                .statusCode(200)
                .body("vendor.'web-instrumentation.test-endpoint.activeRequests'.'current;tier=integration'",
                        is(expected));
    }

    @Test
    @RunAsClient
    public void nonExistentRequestsTest() {
        performMetricsRequest()
                .log().ifValidationFails()
                .statusCode(200)
                .body("vendor.'web-instrumentation.empty-endpoint.status'.'count;statusCode=404;tier=integration'",
                        is(0));

        when()
                .get("/v1/non-existent")
        .then()
                .statusCode(404);

        performMetricsRequest()
                .log().ifValidationFails()
                .statusCode(200)
                .body("vendor.'web-instrumentation.empty-endpoint.status'.'count;statusCode=404;tier=integration'",
                        is(1));
    }

    @Test
    @RunAsClient
    public void instrumentedOptionsTest() {
        given()
                .header("Accept", "application/json")
        .when()
                .options("/metrics/vendor")
        .then()
                .log().ifValidationFails()
                .body("'web-instrumentation.test-endpoint.status'.tags.size()", is(7)) // all codes (6) + other
                .statusCode(200);
    }
}

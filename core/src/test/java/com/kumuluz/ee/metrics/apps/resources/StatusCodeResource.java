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
package com.kumuluz.ee.metrics.apps.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Resource that returns a status code given as path parameter.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
@Path("statusCodes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class StatusCodeResource {

    private CountDownLatch managedLatch;

    @GET
    @Path("{statusCode}")
    public Response returnStatusCode(@PathParam("statusCode") int statusCode) {
        return Response.status(statusCode).build();
    }

    @GET
    @Path("initManaged")
    public Response initLatch() {
        this.managedLatch = new CountDownLatch(1);
        return Response.ok().build();
    }

    @GET
    @Path("releaseManaged")
    public Response releaseLatch() {

        if (this.managedLatch == null) {
            return Response.serverError().build();
        }

        this.managedLatch.countDown();

        return Response.ok().build();
    }

    @GET
    @Path("managed")
    public Response managedRequest() throws InterruptedException {

        if (this.managedLatch == null) {
            return Response.serverError().build();
        }

        this.managedLatch.await(10, TimeUnit.SECONDS);

        return Response.ok().build();
    }
}

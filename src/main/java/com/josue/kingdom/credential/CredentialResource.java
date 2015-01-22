/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.josue.kingdom.credential;

import com.josue.kingdom.credential.entity.Manager;
import com.josue.kingdom.rest.ResponseUtils;
import static com.josue.kingdom.rest.ResponseUtils.CONTENT_TYPE;
import com.josue.kingdom.rest.ex.RestException;
import com.josue.kingdom.util.cdi.Current;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Josue
 */
@ApplicationScoped
@Path("credentials")
public class CredentialResource {

    @Context
    UriInfo info;

    @Inject
    CredentialControl control;

    @Current
    @Inject
    Manager currentManager;

    /*
     returns the ManagerCredential for the login
     */
    @GET
    @Path("current")
    @Produces(value = CONTENT_TYPE)
    public Response getCurrentCredential() {
        return ResponseUtils.buildSimpleResponse(currentManager, Response.Status.OK, info);
    }

    @GET
    @Path("{login}")
    @Produces(value = CONTENT_TYPE)
    public Response getAccount(@PathParam("login") String login) throws RestException {
        Manager managerBylogin = control.getManagerBylogin(login);
        return ResponseUtils.buildSimpleResponse(managerBylogin, Response.Status.OK, info);
    }

    @GET
    @Path("{login}/password-reset")
    public Response passwordReset(@PathParam("login") String login) throws RestException {
        control.passwordReset(login);
        return ResponseUtils.buildSimpleResponse(null, Response.Status.OK, info);
    }

    @GET
    @Path("{email}/login-recover")
    //TODO return any body ? SHOULD THOSE ACCOUNT METHOD HAVE AN ENTITY CLASS ?
    public Response loginRecover(@PathParam("email") String email) throws RestException {
        control.loginRecovery(email);
        return ResponseUtils.buildSimpleResponse(null, Response.Status.OK, info);
    }

    //TODO move to CredentialResource or keep it here ?
    @POST
    @Path("{token}")
    @Produces(value = CONTENT_TYPE)
    @Consumes(value = CONTENT_TYPE)
    public Response createManager(@PathParam("token") String token, Manager managerCredential) throws RestException {
        Manager createdManager = control.createManager(token, managerCredential);
        return ResponseUtils.buildSimpleResponse(createdManager, Response.Status.CREATED, info);
    }
}

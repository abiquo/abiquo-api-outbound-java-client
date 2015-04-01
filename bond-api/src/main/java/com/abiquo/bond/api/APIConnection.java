/**
 * The Abiquo Platform
 * Cloud management application for hybrid clouds
 * Copyright (C) 2008 - Abiquo Holdings S.L.
 *
 * This application is free software; you can redistribute it and/or
 * modify it under the terms of the GNU LESSER GENERAL PUBLIC
 * LICENSE as published by the Free Software Foundation under
 * version 3 of the License
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * LESSER GENERAL PUBLIC LICENSE v.3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package com.abiquo.bond.api;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.model.rest.RESTLink;
import com.abiquo.server.core.enterprise.UserDto;

/**
 * This is the base class for any class that needs to request data via the Abiquo API. It uses the
 * JAX-RS Client interface which is part of Java Enterprise Edition 7, although we have actually
 * used the Jersey 2.0 implementation of the specification for our testing as it was available
 * before Java EE 7 was released.
 * <p>
 * Instances of this class or its subclasses should be shared as much as possible as Client objects
 * are expensive to create. New connections to the Abiquo API should be made by creating a new
 * WebTarget instance. This can be done using the Client.target method if you have the full URI (for
 * instance when using a link returned from a previous request) or by using the WebTarget.path
 * method is you are creating a URI relative to an existing WebTarget. This class contains a
 * WebTarget instance that represents the base URI of the Abiquo API.
 */
public class APIConnection
{
    private final static Logger logger = LoggerFactory.getLogger(APIConnection.class);

    protected WebTarget targetAPIBase;

    protected Client client;

    protected WrapperNotification wrapperNotifications;

    /**
     * This constructor create a Client instance and a WebTarget instance representing the base URI
     * of the Abiquo API. Either of these can be used to create new WebTarget instances. The Client
     * instance is initialised with a HttpBasicAuthFilter so there is no need to add an
     * Authentication header to any requests. It is also initialised with support for requesting
     * that message bodies in responses from the Abiquo server are in Jackson format.
     * 
     * @param server the name or IP address of the Abiquo server
     * @param user an Abiquo user with sufficient rights to request data via the API
     * @param password the user's password
     */
    public APIConnection(final String server, final String user, final String password)
    {
        ClientBuilder builder = ClientBuilder.newBuilder();
        client = builder.build();
        client.register(new HttpBasicAuthFilter(user, password));
        client.register(JacksonFeature.class);
        logger.debug("Connecting to: {}/api", server);
        targetAPIBase = client.target(server + "/api");
    }

    /**
     * Allows the client to pass messages back to the client wrapper without interrupting the flow
     * of the program.
     * 
     * @param handler notification handler supplied by the client wrapper
     */
    public void setNotificationHandler(final WrapperNotification handler)
    {
        wrapperNotifications = handler;
    }

    /**
     * Pass a notification back to the client wrapper if a notification handler has been set
     * 
     * @param msg the notification to be return to the client wrapper
     */
    protected void notifyWrapper(final String msg)
    {
        if (wrapperNotifications != null)
        {
            wrapperNotifications.notification(msg);
        }
    }

    /**
     * Pass a notification and exception back to the client wrapper if a notification handler has
     * been set
     * 
     * @param msg the notification to be return to the client wrapper
     * @param t the exception to be return to the client wrapper
     */
    protected void notifyWrapper(final String msg, final Throwable t)
    {
        if (wrapperNotifications != null)
        {
            wrapperNotifications.notification(msg, t);
        }
    }

    /**
     * Returns the URI of the currently logged in user. This is required in certain instances where
     * events sent by the outbound API that were generated by this user need to be ignored.
     * 
     * @return The URI that can be used to access the details of the currently logged in user. As
     *         user information in all outbound API messages is represented by a URI, this can be
     *         used to check if the message was generated by the current user.
     * @throws OutboundAPIClientException if the request fails for any reason
     */
    RESTLink getCurrentUserLink() throws OutboundAPIClientException
    {
        WebTarget targetUser = targetAPIBase.path("login");
        Invocation.Builder invocationBuilder = targetUser.request(UserDto.MEDIA_TYPE);
        Response response = invocationBuilder.get();
        int status = response.getStatus();
        if (status == 200)
        {
            UserDto resourceUser = response.readEntity(UserDto.class);
            RESTLink userlink = resourceUser.getEditLink();
            if (userlink != null)
            {
                logger.debug("Current user url: {}", userlink.getHref());
                return userlink;
            }
            throw new OutboundAPIClientException("Error getting current user URI: 'edit' link not found");
        }
        else
        {
            throw new OutboundAPIClientHTTPException("Error getting current user URI",
                targetAPIBase.getUri(),
                response.getStatus());
        }
    }
}

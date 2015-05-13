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

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.model.rest.RESTLink;
import com.abiquo.model.transport.SingleResourceTransportDto;
import com.abiquo.server.core.cloud.VirtualMachineDto;
import com.abiquo.server.core.enterprise.EnterpriseDto;
import com.abiquo.server.core.enterprise.UserDto;

/**
 * Fetches and translates data from the REST API
 */
public class ResourceExpander extends APIConnection
{
    private final static Logger logger = LoggerFactory.getLogger(ResourceExpander.class);

    public ResourceExpander(final String server, final String user, final String password)
    {
        super(server, user, password);
    }

    private SingleResourceTransportDto expandResource(final String resource, final String type,
        final Class< ? extends SingleResourceTransportDto> resourceClass)
        throws OutboundAPIClientHTTPException
    {
        WebTarget targetResource = targetAPIBase.path(resource);
        Invocation.Builder invocationBuilder = targetResource.request(type);
        Response response = invocationBuilder.get();
        int status = response.getStatus();
        if (status == 200)
        {
            SingleResourceTransportDto resourceObject = response.readEntity(resourceClass);
            return resourceObject;
        }
        else if (status == 404)
        {
            logger.warn("Request for object that does not exist: {}", targetResource.getUri());
            return null;
        }
        else
        {
            String error = response.readEntity(String.class);
            logger.debug(error);
            throw new OutboundAPIClientHTTPException(error, targetResource.getUri(), status);
        }
    }

    /**
     * Translates the supplied link into an instance of the supplied class
     *
     * @param link Link from which data is to be fetched
     * @param resourceClass Type of class to which the data is to be translated
     * @return an instance of the resourceClass
     */
    public SingleResourceTransportDto expandResource(final RESTLink link,
        final Class< ? extends SingleResourceTransportDto> resourceClass)
    {
        WebTarget targetResource = client.target(link.getHref());
        Invocation.Builder invocationBuilder = targetResource.request(link.getType());
        Response response = invocationBuilder.get();
        SingleResourceTransportDto resourceObject = response.readEntity(resourceClass);
        return resourceObject;
    }

    /**
     * Translates the supplied resource String into an instance of the UserDto class
     *
     * @param resource String containing the data to be converted
     * @return an instance of the UserDto class created from the resource String
     * @throws OutboundAPIClientHTTPException
     */
    public UserDto expandUserResource(final String resource) throws OutboundAPIClientHTTPException
    {
        UserDto user = (UserDto) expandResource(resource, UserDto.MEDIA_TYPE_JSON, UserDto.class);
        if (user != null)
        {
            logger.trace("UserDto: id:{} name:{}", user.getId(), user.getName());
        }
        return user;
    }

    /**
     * Translates the supplied resource String into an instance of the EnterpriseDto class
     *
     * @param resource String containing the data to be converted
     * @return an instance of the EnterpriseDto class created from the resource String
     * @throws OutboundAPIClientHTTPException
     */
    public EnterpriseDto expandEnterpriseResource(final String resource)
        throws OutboundAPIClientHTTPException
    {
        EnterpriseDto enterprise =
            (EnterpriseDto) expandResource(resource, EnterpriseDto.MEDIA_TYPE_JSON,
                EnterpriseDto.class);
        if (enterprise != null)
        {
            logger.trace("EnterpriseDto: id:{} name:{}", enterprise.getId(), enterprise.getName());
        }
        return enterprise;
    }

    /**
     * Translates the supplied resource String into an instance of the VirtualMachineDto class
     *
     * @param resource String containing the data to be converted
     * @return an instance of the VirtualMachineDto class created from the resource String
     * @throws OutboundAPIClientHTTPException
     */
    public VirtualMachineDto expandVirtualMachine(final String resource)
        throws OutboundAPIClientHTTPException
    {
        VirtualMachineDto vm =
            (VirtualMachineDto) expandResource(resource, VirtualMachineDto.SHORT_MEDIA_TYPE_XML,
                VirtualMachineDto.class);
        if (vm != null)
        {
            logger.trace("VirtualMachineDto: id:{} name:{}", vm.getId(), vm.getName());
        }
        return vm;
    }
}

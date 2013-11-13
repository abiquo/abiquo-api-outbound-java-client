/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.abiquo.bond.api.plugin.PluginException;
import com.abiquo.model.rest.RESTLink;
import com.abiquo.model.transport.SingleResourceTransportDto;
import com.abiquo.server.core.cloud.VirtualMachineDto;
import com.abiquo.server.core.enterprise.EnterpriseDto;
import com.abiquo.server.core.enterprise.UserDto;

/**
 * Fetches and translates data from the REST API
 * 
 */
public class ResourceExpander extends APIConnection
{
    public ResourceExpander(final String server, final String user, final String password)
    {
        super(server, user, password);
    }

    private SingleResourceTransportDto expandResource(final String resource, final String type,
        final Class< ? extends SingleResourceTransportDto> resourceClass) throws PluginException
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
        else
        {
            String error = response.readEntity(String.class);
            logger.debug(error);
            throw new PluginException(error);
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
     * @throws PluginException
     */
    public UserDto expandUserResource(final String resource) throws PluginException
    {
        UserDto user = (UserDto) expandResource(resource, UserDto.MEDIA_TYPE_JSON, UserDto.class);
        logger.debug("UserDto: id:{} name:{}", user.getId(), user.getName());
        return user;
    }

    /**
     * Translates the supplied resource String into an instance of the EnterpriseDto class
     * 
     * @param resource String containing the data to be converted
     * @return an instance of the EnterpriseDto class created from the resource String
     * @throws PluginException
     */
    public EnterpriseDto expandEnterpriseResource(final String resource) throws PluginException
    {
        EnterpriseDto enterprise =
            (EnterpriseDto) expandResource(resource, EnterpriseDto.MEDIA_TYPE_JSON,
                EnterpriseDto.class);
        logger.debug("EnterpriseDto: id:{} name:{}", enterprise.getId(), enterprise.getName());
        return enterprise;
    }

    /**
     * Translates the supplied resource String into an instance of the VirtualMachineDto class
     * 
     * @param resource String containing the data to be converted
     * @return an instance of the VirtualMachineDto class created from the resource String
     * @throws PluginException
     */
    public VirtualMachineDto expandVirtualMachine(final String resource) throws PluginException
    {
        VirtualMachineDto vm =
            (VirtualMachineDto) expandResource(resource, VirtualMachineDto.BASE_MEDIA_TYPE,
                VirtualMachineDto.class);
        logger.debug("VirtualMachineDto: id:{} name:{}", vm.getId(), vm.getName());
        return vm;
    }
}

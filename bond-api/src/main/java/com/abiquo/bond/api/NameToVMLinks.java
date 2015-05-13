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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.model.rest.RESTLink;
import com.abiquo.model.transport.SingleResourceTransportDto;
import com.abiquo.server.core.cloud.VirtualMachineDto;
import com.abiquo.server.core.cloud.VirtualMachinesDto;
import com.abiquo.server.core.infrastructure.DatacenterDto;
import com.abiquo.server.core.infrastructure.DatacentersDto;
import com.abiquo.server.core.infrastructure.MachineDto;
import com.abiquo.server.core.infrastructure.MachinesDto;
import com.abiquo.server.core.infrastructure.RackDto;
import com.abiquo.server.core.infrastructure.RacksDto;
import com.google.common.base.Optional;

/**
 * Class that maintains a mapping of the names of all the VMs deployed in Abiquo to their associated
 * REST API links. Currently, it only maintains the 'metadata' link to avoid wasting memory, but
 * other links can be added if they are required.
 */
public class NameToVMLinks extends APIConnection
{
    private final static Logger logger = LoggerFactory.getLogger(NameToVMLinks.class);

    public static final String VM_LINK_METADATA = "metadata";

    private List<String> supportedLinks = new ArrayList<>();

    private Map<String, Map<String, RESTLink>> mapVMtoLinks = new HashMap<>();

    public NameToVMLinks(final String server, final String user, final String password)
    {
        super(server, user, password);

        supportedLinks.add(VM_LINK_METADATA);

        fetchAllVMs();
    }

    /**
     * Retrieve a link for a specific VM
     * 
     * @param vmname name of VM for which link is required
     * @param linktype The value of the 'rel' attribute for the required link
     * @return A RESTLink representing the required link wrapped in an Optional. If the link
     *         couldn't be found an absent Optional is returned.
     */
    public Optional<RESTLink> getLink(final String vmname, final String linktype)
    {
        Map<String, RESTLink> links = mapVMtoLinks.get(vmname);
        if (links != null)
        {
            RESTLink link = links.get(linktype);
            return Optional.fromNullable(link);
        }
        return Optional.absent();
    }

    public void addVM(final VirtualMachineDto vmdetails)
    {
        if (vmdetails != null)
        {
            logger.trace("Adding links for {}", vmdetails.getName());
            Map<String, RESTLink> supported = new HashMap<>();
            for (String supportedRel : supportedLinks)
            {
                RESTLink link = vmdetails.searchLink(supportedRel);
                if (link != null)
                {
                    logger.trace("Added {} link: {}", supportedRel, link.getHref());
                    supported.put(supportedRel, link);
                }
            }
            mapVMtoLinks.put(vmdetails.getName(), supported);
        }
    }

    /**
     * Update all the links for the specified vm
     * 
     * @param vmdetails details of the vm to be updated
     */
    public void updateVM(final VirtualMachineDto vmdetails)
    {
        updateVM(vmdetails, supportedLinks);
    }

    /**
     * Update the specified links for the specified vm
     * 
     * @param vmdetails details of the vm to be updated
     * @param links a list of the links that are to be uodated
     */
    public void updateVM(final VirtualMachineDto vmdetails, final List<String> links)
    {
        if (vmdetails != null)
        {
            logger.trace("Updating links for {}", vmdetails.getName());
            Map<String, RESTLink> supported = new HashMap<>();
            for (String supportedRel : links)
            {
                RESTLink link = vmdetails.searchLink(supportedRel);
                if (link != null)
                {
                    logger.trace("Added {} link: {}", supportedRel, link.getHref());
                    supported.put(supportedRel, link);
                }
            }
            mapVMtoLinks.put(vmdetails.getName(), supported);
        }
    }

    public void removeVM(final String name)
    {
        mapVMtoLinks.remove(name);
    }

    public Set<String> getVMNames()
    {
        return mapVMtoLinks.keySet();
    }

    private void fetchAllVMs()
    {
        WebTarget targetAllDCs =
            targetAPIBase.path("admin").path("datacenters").queryParam("limit", "100");
        while (targetAllDCs != null)
        {
            Invocation.Builder invocationBuilder = targetAllDCs.request(DatacentersDto.MEDIA_TYPE);
            Response response = invocationBuilder.get();
            int status = response.getStatus();
            if (status == 200)
            {
                DatacentersDto resourceDCs = response.readEntity(DatacentersDto.class);
                List<DatacenterDto> dclist = resourceDCs.getCollection();

                for (DatacenterDto dc : dclist)
                {
                    RESTLink rackslink = dc.searchLink("racks");
                    if (rackslink != null)
                    {
                        response = getResource(rackslink);
                        status = response.getStatus();
                        if (status == 200)
                        {
                            RacksDto resourceRacks = response.readEntity(RacksDto.class);
                            List<RackDto> racklist = resourceRacks.getCollection();

                            for (RackDto rack : racklist)
                            {
                                RESTLink machineslink = rack.searchLink("machines");
                                if (machineslink != null)
                                {
                                    response = getResource(machineslink);
                                    status = response.getStatus();
                                    if (status == 200)
                                    {
                                        MachinesDto resourceMachines =
                                            response.readEntity(MachinesDto.class);
                                        List<MachineDto> machinelist =
                                            resourceMachines.getCollection();

                                        for (MachineDto machine : machinelist)
                                        {
                                            RESTLink vmslink =
                                                machine.searchLink("virtualmachines");
                                            if (vmslink != null)
                                            {
                                                response = getResource(vmslink);
                                                status = response.getStatus();
                                                if (status == 200)
                                                {
                                                    VirtualMachinesDto resourceVMs =
                                                        response
                                                            .readEntity(VirtualMachinesDto.class);
                                                    List<VirtualMachineDto> vmlist =
                                                        resourceVMs.getCollection();

                                                    for (VirtualMachineDto vm : vmlist)
                                                    {
                                                        addVM(vm);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                targetAllDCs = getNextTarget(resourceDCs);
            }
        }
    }

    private WebTarget getNextTarget(final SingleResourceTransportDto resource)
    {
        RESTLink nextlink = resource.searchLink("next");
        if (nextlink != null)
        {
            return client.target(nextlink.getHref()).queryParam("limit", 100);
        }
        return null;
    }

    private Response getResource(final RESTLink link)
    {
        WebTarget target = client.target(link.getHref());
        Invocation.Builder invocationBuilder = target.request(link.getType());
        Response response = invocationBuilder.get();
        return response;
    }
}

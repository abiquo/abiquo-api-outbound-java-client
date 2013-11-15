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

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.model.rest.RESTLink;
import com.abiquo.server.core.cloud.VirtualMachineDto;
import com.abiquo.server.core.cloud.VirtualMachinesDto;
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

    private Map<String, Map<String, RESTLink>> map = new HashMap<>();

    public NameToVMLinks(final String server, final String user, final String password)
    {
        super(server, user, password);

        supportedLinks.add(VM_LINK_METADATA);

        populateMap();
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
        Map<String, RESTLink> links = map.get(vmname);
        if (links != null)
        {
            RESTLink link = links.get(linktype);
            return Optional.fromNullable(link);
        }
        return Optional.absent();
    }

    /**
     * Fetch the links for all the current VMs. Uses the paging support in Abiquo to fetch them 1000
     * at a time.
     */
    private void populateMap()
    {
        WebTarget targetAllVms =
            targetAPIBase.path("cloud").path("virtualmachines").queryParam("limit", "1000");
        boolean morevms = true;
        while (morevms)
        {
            Invocation.Builder invocationBuilder =
                targetAllVms.request(VirtualMachinesDto.MEDIA_TYPE);
            Response response = invocationBuilder.get();
            int status = response.getStatus();
            if (status == 200)
            {
                VirtualMachinesDto resourceObject = response.readEntity(VirtualMachinesDto.class);

                List<VirtualMachineDto> vms = resourceObject.getCollection();
                for (VirtualMachineDto vm : vms)
                {
                    logger.debug("Finding links for {}", vm.getName());
                    Map<String, RESTLink> supported = new HashMap<>();
                    List<RESTLink> vmlinks = vm.getLinks();
                    for (RESTLink vmlink : vmlinks)
                    {
                        String rel = vmlink.getRel().toLowerCase();
                        logger.debug("    {}: {}", rel, vmlink.getHref());
                        if (supportedLinks.contains(rel))
                        {
                            supported.put(rel, vmlink);
                        }
                    }
                    map.put(vm.getName(), supported);
                }

                morevms = false;
                List<RESTLink> links = resourceObject.getLinks();
                for (RESTLink link : links)
                {
                    if (link.getRel().equalsIgnoreCase("next"))
                    {
                        targetAllVms = client.target(link.getHref());
                        morevms = true;
                    }
                }
            }
        }
    }
}

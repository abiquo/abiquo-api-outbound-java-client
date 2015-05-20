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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.plugin.PluginException;
import com.abiquo.model.rest.RESTLink;
import com.abiquo.model.transport.SingleResourceTransportDto;
import com.abiquo.model.transport.WrapperDto;
import com.abiquo.server.core.cloud.VirtualMachineDto;
import com.abiquo.server.core.cloud.VirtualMachinesDto;
import com.abiquo.server.core.infrastructure.DatacenterDto;
import com.abiquo.server.core.infrastructure.DatacentersDto;
import com.abiquo.server.core.infrastructure.MachineDto;
import com.abiquo.server.core.infrastructure.MachinesDto;
import com.abiquo.server.core.infrastructure.RackDto;
import com.abiquo.server.core.infrastructure.RacksDto;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;

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

    /**
     * Fetchs all vms to "cache"
     */
    private void fetchAllVMs()
    {
        WebTarget targetAllDCs = targetAPIBase.path("admin").path("datacenters");
        AutoPagginatedList<DatacentersDto, DatacenterDto> dcs =
            new AutoPagginatedList<>(client, targetAllDCs.getUri().toString(), DatacentersDto.class);

        List<VirtualMachineDto> retrievedVms = new ArrayList<>();
        for (DatacenterDto dc : dcs)
        {
            RESTLink racksLink =
                checkNotNull(dc.searchLink("racks"),
                    "Missing 'racks' link from datacenter %s. Should be here.", dc.getName());
            AutoPagginatedList<RacksDto, RackDto> racks =
                new AutoPagginatedList<>(client, racksLink.getHref(), RacksDto.class);
            for (RackDto rack : racks)
            {
                RESTLink machinesLink =
                    checkNotNull(rack.searchLink("machines"),
                        "Missing 'machines' link from rack %s. Should be here.", rack.getName());
                AutoPagginatedList<MachinesDto, MachineDto> machines =
                    new AutoPagginatedList<>(client, machinesLink.getHref(), MachinesDto.class);
                for (MachineDto machine : machines)
                {
                    RESTLink vmsLink =
                        checkNotNull(machine.searchLink("virtualmachines"),
                            "Missing 'virtualmachines' link from machine %s (%s). Should be here.",
                            machine.getName(), machine.getIp());
                    AutoPagginatedList<VirtualMachinesDto, VirtualMachineDto> vms =
                        new AutoPagginatedList<>(client,
                            vmsLink.getHref(),
                            VirtualMachinesDto.class);
                    vms.forEach(retrievedVms::add);
                }
            }
        }
        logger.debug("{} vms found in abiquo api while caching them", retrievedVms.size());
        retrievedVms.forEach(this::addVM);

    }

    /**
     * Iterable class that allows to iterate all elements from an Abiquo API resource with auto
     * management of pagination.
     * 
     * @author scastro
     * @param <E> Wrapper dto class that contains the collection of T, p.e: {@link DatacentersDto}
     * @param <T> Dto class of each element of the iterable, p.e: {@link DatacenterDto}
     */
    static private class AutoPagginatedList<E extends WrapperDto<T>, T extends SingleResourceTransportDto>
        implements Iterable<T>
    {

        private AutoPagginatedIterator<T> iterator;

        public AutoPagginatedList(final Client client, final String uri,
            final Class<E> wrapperDtoClass)
        {
            iterator = new AutoPagginatedIterator<>(client, uri, wrapperDtoClass);
        }

        @Override
        public Iterator<T> iterator()
        {
            return iterator;
        }

        static private class AutoPagginatedIterator<A extends SingleResourceTransportDto>
            implements Iterator<A>
        {
            private List<A> collection = null;

            private int actualElement = 0;

            private RESTLink nextLink = null;

            private final String mediatype;

            private final Client client;

            private final Class< ? extends WrapperDto<A>> wrapperDtoClass;

            public AutoPagginatedIterator(final Client client, final String uri,
                final Class< ? extends WrapperDto<A>> wrapperDtoClass)
            {
                this.client = client;
                this.wrapperDtoClass = wrapperDtoClass;
                try
                {
                    mediatype =
                        (String) wrapperDtoClass.getField("MEDIA_TYPE").get(wrapperDtoClass);
                    doGet(uri);
                }
                catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
                    | SecurityException e)
                {
                    throw Throwables.propagate(e);
                }
            }

            private void doGet(final String uri)
            {
                Response response = client.target(uri).request(mediatype).get();
                if (response.getStatus() == 200)
                {
                    WrapperDto<A> wrapperDto = response.readEntity(wrapperDtoClass);
                    collection = wrapperDto.getCollection();
                    nextLink = wrapperDto.searchLink("next");
                }
                else
                {
                    Throwables.propagate(new PluginException(format("Unable to retrieve %s: %s",
                        uri, response.getStatusInfo().getReasonPhrase())));
                }
            }

            @Override
            public boolean hasNext()
            {
                return actualElement < collection.size() || nextLink != null;
            }

            @Override
            public A next()
            {
                if (actualElement < collection.size())
                {
                    return collection.get(actualElement++);
                }
                else
                {
                    doGet(nextLink.getHref());
                    actualElement = 0;
                    return collection.get(actualElement++);
                }
            }
        }
    }

}

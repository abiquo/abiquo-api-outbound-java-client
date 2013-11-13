/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.abiquo.event.model.Event;
import com.abiquo.event.model.details.EventDetails;
import com.abiquo.server.core.event.EventDto;
import com.google.common.base.Optional;

/**
 * Generic class for handling any virtual machine related events received from the 'M' server.
 * 
 */
public class VirtualMachineEvent extends APIEvent
{
    protected String vmname;

    protected String hypervisorname;

    protected String hypervisorip;

    protected String hypervisortype;

    public VirtualMachineEvent(final Event event)
    {
        super(event);
        Map<String, String> details = getEventDetails(event);
        vmname = details.get("VIRTUAL_MACHINE_NAME");
        hypervisorname = details.get("MACHINE_NAME");
        hypervisorip = details.get("HYPERVISOR_IP");
        hypervisortype = details.get("HYPERVISOR_TYPE");
    }

    public VirtualMachineEvent(final EventDto event)
    {
        super(event);
        vmname = event.getVirtualMachine();
        hypervisorname = event.getPhysicalMachine();
    }

    public String getVMName()
    {
        return vmname;
    }

    public List<String> getHypervisorNames()
    {
        List<String> hypervisors = new ArrayList<>();
        if (hypervisorname != null)
        {
            hypervisors.add(hypervisorname);
        }
        if (hypervisorip != null)
        {
            hypervisors.add(hypervisorip);
        }
        return hypervisors;
    }

    private Map<String, String> getEventDetails(final Event event)
    {
        Optional< ? extends EventDetails> optVMDetails = event.getDetails();
        if (optVMDetails.isPresent())
        {
            EventDetails details = optVMDetails.get();
            return details.getTransportMap();
        }
        return new HashMap<String, String>();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" vm:").append(vmname);
        return sb.toString();
    }
}

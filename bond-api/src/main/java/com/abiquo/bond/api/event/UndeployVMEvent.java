/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.event;

import com.abiquo.event.model.Event;
import com.abiquo.server.core.event.EventDto;

/**
 * Class for representing virtual machine undeployment events received from the M server
 */
public class UndeployVMEvent extends VirtualMachineEvent
{

    public UndeployVMEvent(final Event event)
    {
        super(event);
    }

    public UndeployVMEvent(final EventDto event)
    {
        super(event);
    }

}

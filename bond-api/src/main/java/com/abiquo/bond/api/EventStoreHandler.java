/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api;

import com.abiquo.bond.api.event.APIEvent;

/**
 * Any class that is designed to handle messages from the permanent event store should implement
 * this interface
 */
public interface EventStoreHandler
{
    /**
     * For each message received from the event store, this method will be passed an APIEvent
     * containing the message
     * 
     * @param event a message received from the M server and converted to a format suitable for
     *            passing to a plugin
     */
    public void handleMessage(APIEvent event);
}

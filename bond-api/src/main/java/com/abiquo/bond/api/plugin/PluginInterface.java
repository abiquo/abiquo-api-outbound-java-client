/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.plugin;

import com.abiquo.bond.api.event.APIEvent;

/**
 * This is the interface that any plugin for the sample Outbound API Client should implement. Users
 * can choose to implement this directly or extend the
 * {@link com.abiquo.bond.api.plugin.AbstractPlugin} class.
 */
public interface PluginInterface extends Runnable
{
    /**
     * Process an event received from the M server. Ideally this should run as quickly as possible
     * as the client will be calling it sequentially for every event on every plugin. The
     * recommended approach is for this method to simply add the event to a queue for later
     * processing. The {@link com.abiquo.bond.api.plugin.PluginInterface#handlesEventType} method
     * can also be used to potentially speed processing.
     * 
     * @param event
     */
    void processEvent(APIEvent event);

    /**
     * Checks whether a plugin wants to handle a specific type of event. This method should be used
     * if the processEvent might take some time to run and hence it is better to call it only when
     * the event will definitely be processed.
     * 
     * @param event type of event
     * @return true if the class can handle the specified event type, false if it cannot
     */
    boolean handlesEventType(Class< ? extends APIEvent> event);

    /**
     * Get the name of the plugin.
     * 
     * @return the name of the plugin
     */
    String getName();

    /**
     * Get the thread name of the plugin. This is a short name that will be used to name the thread
     * that the plugin is run in and will be seen in log messages.
     * 
     * @return the name of the plugin
     */
    String getThreadName();

    /**
     * Perform any initial processing that needs doing when the thread first starts up.
     * 
     * @throws PluginException
     */
    void startup() throws PluginException;

    /**
     * Cancels the plugin
     */
    public void cancel();
}

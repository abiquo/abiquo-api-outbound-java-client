/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api;

/**
 * Interface that needs to be implemented by any class that is used to provide communications
 * between the client and the M server
 * 
 */
public interface MConnector
{
    /**
     * Opens a connection to the M server
     */
    public void connect(String server, String user, String password);

    /**
     * Closes the connection to the M server
     */
    public void disconnect();
}

/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api;

/**
 * Any class that is designed to handle messages from the outbound api should implement this
 * interface
 */
public interface CommsHandler
{
    /**
     * For each message received from the M server, this method will be passed a string containing
     * the message
     * 
     * @param msg a message received from the M server
     */
    public void handleMessage(String msg);

    /**
     * This method will be passed any headers received in the HTTP response when connecting to the M
     * server.
     * 
     * @param headers a semi-colon separated String containing all the received headers
     */
    public void handleHeaders(String headers);

    /**
     * This method will be passed the transport type negotiated when connecting to the M server.
     * 
     * @param transport
     */
    public void handleTransportType(Transport transport);
}

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

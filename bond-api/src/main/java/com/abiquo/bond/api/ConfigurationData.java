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

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.plugin.PluginException;

/**
 * This class represents the configuration data required by the OutboundAPIClient class. An instance
 * of this class is passed to the OutboundAPIClient constructor. This is used instead of a
 * properties file as it forces the client wrapper to provide better validated data.
 */
public class ConfigurationData
{
    private final static Logger logger = LoggerFactory.getLogger(ConfigurationData.class);

    /**
     * Name or ip address of M server. A value for this field must be provided.
     */
    private String mserver;

    /**
     * User the client will authenticate to the M server as. A value for this field must be provided
     * and the user must have at least Cloud Administrator privileges
     */
    private String musername;

    /**
     * User's password. A value for this field must be provided.
     */
    private String muserpassword;

    /**
     * Requested transport protocol to be used to attach to the M server. The default (and currently
     * only supported value) is Transport.SSE
     * 
     * @see com.abiquo.bond.api.Transport
     */
    private Transport transport = Transport.SSE;

    // Actual transport protocol negotiated with the M server.
    private Transport negotiatedTransport;

    /**
     * Logging level. The default value is MessageLevel.INFORMATION
     * 
     * @see com.abiquo.bond.api.MessageLevel
     */
    private MessageLevel messagelevel = MessageLevel.INFORMATION;

    /**
     * Class that will provide the communication link between the client and the M server. None of
     * the ones I have tested work perfectly with the Server Sent Events protocol, so I have
     * implemented this as an separate package so that it can be easily replaced. Any such package
     * needs to implement the MConnector interface. The current default is
     * {@link com.abiquo.bond.api.connector.WAsyncConnector}
     */
    private String connectorname = "com.abiquo.bond.api.connector.WAsyncConnector";

    /**
     * Date of the last message handled by the outbound api client. This will be used by the client
     * when it starts up to fetch any missed messages since the last time it ran. If this value is
     * not set no events will be retrieved from the event store.
     */
    private Date lastProcessedEvent;

    /**
     * Constructor that sets values for the server, user name and password and uses default values
     * for all other fields.
     * 
     * @param mserver name or ip address of the 'M' server
     * @param musername abiquo user with cloud administrator privileges
     * @param muserpassword user's password
     * @throws PluginException if server name, user name and/or password are null.
     */
    public ConfigurationData(final String mserver, final String musername,
        final String muserpassword) throws PluginException
    {
        logger.debug("Constructor: server: {}, username: {}", mserver, musername);
        this.mserver = mserver;
        this.musername = musername;
        this.muserpassword = muserpassword;

        if (mserver == null || musername == null || muserpassword == null)
        {
            throw new PluginException("The 'M' server name, user and password cannot be null.");
        }
    }

    /**
     * Copy constructor.
     * 
     * @param original ConfigurationData instance to be copied
     * @throws PluginException if server name, user name and/or password are not specified.
     */
    public ConfigurationData(final ConfigurationData original) throws PluginException
    {
        logger
            .debug(
                "Constructor: server: {}, username: {}, transport: {}, messagelevel: {}, connectorname: {}",
                new Object[] {original.mserver, original.musername, original.transport,
                original.messagelevel, original.connectorname});
        this.mserver = original.mserver;
        this.musername = original.musername;
        this.muserpassword = original.muserpassword;
        this.transport = original.transport;
        this.messagelevel = original.messagelevel;
        this.connectorname = original.connectorname;
        this.lastProcessedEvent = original.lastProcessedEvent;

        if (mserver == null || musername == null || muserpassword == null)
        {
            throw new PluginException("The 'M' server name, user and password must be specified in the configuration data.");
        }
    }

    public void setTransport(final Transport t)
    {
        logger.debug("Setting transport to {}", transport);
        this.transport = t;
    }

    void setNegotiatedTransport(final Transport t)
    {
        logger.debug("Setting negotiated transport to {}", negotiatedTransport);
        this.negotiatedTransport = t;
    }

    public void setMessageLevel(final MessageLevel ml)
    {
        logger.debug("Setting message level to {}", messagelevel);
        this.messagelevel = ml;
    }

    public void setConnector(final String c)
    {
        logger.debug("Setting connector name to {}", connectorname);
        this.connectorname = c;
    }

    public String getMServer()
    {
        return mserver;
    }

    public String getMUser()
    {
        return musername;
    }

    public String getMUserPassword()
    {
        return muserpassword;
    }

    public Transport getTransport()
    {
        return negotiatedTransport != null ? negotiatedTransport : transport;
    }

    public String getConnector()
    {
        return connectorname;
    }

    public Date getLastProcessedEvent()
    {
        return lastProcessedEvent;
    }

    public void setLastProcessedEvent(final Date date)
    {
        logger.debug("Setting last processed event date to {}", date);
        lastProcessedEvent = date;
    }
}

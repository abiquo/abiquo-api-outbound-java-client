/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.plugin;

import java.net.URI;


/**
 * Wrapper class for exceptions that occur within the plugins when communicating to HTTP servers.
 */

@SuppressWarnings("serial")
public class PluginHTTPException extends PluginException
{
    private URI uri;

    private int status;

    public PluginHTTPException(final String msg, final URI URL, final int status)
    {
        super(msg);
        this.uri = URL;
        this.status = status;
    }

    public PluginHTTPException(final String msg, final URI URL)
    {
        super(msg);
        this.uri = URL;
    }

    public PluginHTTPException(final String msg, final int status)
    {
        super(msg);
        this.status = status;
    }

    @Override
    public String getMessage()
    {
        return super.getMessage() + " URL: " + uri + " Status: " + status;
    }
}

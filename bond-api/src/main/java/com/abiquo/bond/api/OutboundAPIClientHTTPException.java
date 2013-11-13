/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api;

import java.net.URI;

public class OutboundAPIClientHTTPException extends OutboundAPIClientException
{
    private URI uri;

    private int status;

    public OutboundAPIClientHTTPException(final String msg, final URI URL, final int status)
    {
        super(msg);
        this.uri = URL;
        this.status = status;
    }

    public OutboundAPIClientHTTPException(final String msg, final URI URL)
    {
        super(msg);
        this.uri = URL;
    }

    public OutboundAPIClientHTTPException(final String msg, final int status)
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

/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api;

/**
 * Wrapper class for exceptions that occur within the client. This class will be used for returning
 * exceptions to the wrapper client.
 */
public class OutboundAPIClientException extends Exception
{
    private static final long serialVersionUID = -2158784095462160264L;

    public OutboundAPIClientException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public OutboundAPIClientException(final String message)
    {
        super(message);
    }
}

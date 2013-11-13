/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.plugin;

/**
 * Wrapper class for exceptions that occur within the plugins.
 */
public class PluginException extends Exception
{
    private static final long serialVersionUID = 7716575020552404640L;

    public PluginException()
    {
    }

    public PluginException(final String message)
    {
        super(message);
    }

    public PluginException(final Throwable cause)
    {
        super(cause);
    }

    public PluginException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public PluginException(final String message, final Throwable cause,
        final boolean enableSuppression, final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

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

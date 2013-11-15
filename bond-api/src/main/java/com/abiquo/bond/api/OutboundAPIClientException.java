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

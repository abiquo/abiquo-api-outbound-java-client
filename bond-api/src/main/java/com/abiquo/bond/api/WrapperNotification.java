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
 * The client wrapper class should create an instance of a class that implements this interface and
 * pass it to the client in order to receive notifications of anything that occurs in the client. It
 * will then be up to the client wrapper class to decide what action to take in response (close
 * down, notify user in some way, etc).
 */
public interface WrapperNotification
{
    public void notification(String msg);

    public void notification(String msg, Throwable t);

    public void notification(String msg, String url, int statuscode);
}

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

import com.abiquo.bond.api.event.APIEvent;

/**
 * Any class that is designed to handle messages from the permanent event store should implement
 * this interface
 */
public interface EventStoreHandler
{
    /**
     * For each message received from the event store, this method will be passed an APIEvent
     * containing the message
     * 
     * @param event a message received from the M server and converted to a format suitable for
     *            passing to a plugin
     */
    public void handleMessage(APIEvent event);
}

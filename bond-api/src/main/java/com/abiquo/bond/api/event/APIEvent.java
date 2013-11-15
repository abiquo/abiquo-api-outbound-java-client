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
package com.abiquo.bond.api.event;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.event.model.Event;
import com.abiquo.server.core.event.EventDto;

/**
 * Generic class for representing events received from the M server or event store that don't
 * currently have a specific class to represent them.
 */
public class APIEvent
{
    private final static Logger logger = LoggerFactory.getLogger(APIEvent.class);

    private Event originalEvent;

    private EventDto originalEventDto;

    private Date timestamp;

    /**
     * As this is a handler for generic events, it simple stores a reference to the original
     * com.abiquo.event.model.Event object received from the M server. It will be the responsibility
     * of the plugin to extract any required data.
     * 
     * @param event
     */
    public APIEvent(final Event event)
    {
        originalEvent = event;
        timestamp = new Date(event.getTimestamp());
    }

    /**
     * As this is a handler for generic events, it simple stores a reference to the original
     * com.abiquo.server.core.event.EventDto object received from the event store. It will be the
     * responsibility of the plugin to extract any required data.
     * 
     * @param event
     */
    public APIEvent(final EventDto event)
    {
        originalEventDto = event;
        timestamp = event.getTimestamp();
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public Event getOriginalEvent()
    {
        return originalEvent;
    }

    public EventDto getOriginalEventDto()
    {
        return originalEventDto;
    }

    @Override
    public String toString()
    {
        StringBuilder sb =
            new StringBuilder(this.getClass().getName()).append(" ts:").append(timestamp);
        return sb.toString();
    }
}

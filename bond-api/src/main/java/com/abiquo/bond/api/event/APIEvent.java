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
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

/**
 * Generic class for representing events received from the M server or event store that don't
 * currently have a specific class to represent them.
 */
public class APIEvent implements Comparable<APIEvent>
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
        if (originalEvent != null)
        {
            return eventToString(Objects.toStringHelper(this)).toString();
        }
        if (originalEvent != null)
        {
            return eventDtoToString(Objects.toStringHelper(this)).toString();
        }
        return Objects.toStringHelper(this).toString();
    }

    @Override
    public int compareTo(final APIEvent other)
    {
        return timestamp.compareTo(other.timestamp);
    }

    private ToStringHelper eventToString(final ToStringHelper helper)
    {
        return helper.add("from", originalEvent.getClass().getCanonicalName())
            .add("enitity id", originalEvent.getEntityIdentifier())
            .add("type", originalEvent.getType()).add("action", originalEvent.getAction())
            .add("severity", originalEvent.getSeverity()).add("user", originalEvent.getUser())
            .omitNullValues();
    }

    private ToStringHelper eventDtoToString(final ToStringHelper helper)
    {
        return helper.add("from", originalEventDto.getClass().getCanonicalName())
            .add("action performed", originalEventDto.getActionPerformed())
            .add("entity id", originalEventDto.getEntityId())
            .add("virtual machine", originalEventDto.getIdVirtualMachine())
            .add("performed by", originalEventDto.getUser()) //
            .omitNullValues();
    }
}

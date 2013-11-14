/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
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
    final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

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

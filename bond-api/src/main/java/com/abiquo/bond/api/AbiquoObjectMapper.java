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

import com.abiquo.event.json.module.AbiquoModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * Holder of the instance of the {@link ObjectMapper}. Only to be used by consumers and producers.
 * Singleton for performance (http://wiki.fasterxml.com/JacksonFAQThreadSafety). package visibility
 * intended so no one can access the {@link #instance()} method and change its configuration.
 * 
 * @author <a href="mailto:serafin.sedano@abiquo.com">Serafin Sedano</a>
 */
enum AbiquoObjectMapper
{

    OBJECT_MAPPER;

    private final ObjectMapper objectMapper;

    private AbiquoObjectMapper()
    {
        objectMapper =
            new ObjectMapper().setAnnotationIntrospector( //
                new AnnotationIntrospectorPair(new JacksonAnnotationIntrospector(),
                    new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()))) //
                .registerModule(new AbiquoModule());
    }

    /**
     * Returns the instance of the {@link ObjectMapper}. Default access because anyone could change
     * its configuration.
     */
    ObjectMapper instance()
    {
        return this.objectMapper;
    }

}

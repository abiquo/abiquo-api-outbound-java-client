/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
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

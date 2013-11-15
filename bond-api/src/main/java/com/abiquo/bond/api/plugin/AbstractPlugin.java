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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.WrapperNotification;
import com.abiquo.bond.api.annotations.HandleAnyEvent;
import com.abiquo.bond.api.annotations.HandleBackupVMEvent;
import com.abiquo.bond.api.annotations.HandleDeployVMEvent;
import com.abiquo.bond.api.annotations.HandleUndeployVMEvent;
import com.abiquo.bond.api.event.APIEvent;
import com.abiquo.bond.api.event.BackupVMEvent;
import com.abiquo.bond.api.event.DeployVMEvent;
import com.abiquo.bond.api.event.UndeployVMEvent;

/**
 * This is an abstract implementation of the {@link PluginInterface} interface. It adds events to a
 * queue when they are passed via the processEvent method and then reads events from the queue and
 * processes them in the run method. It decides how to process events by checking for annotated
 * methods and mapping these to the appropriate event type.
 * <p>
 * The supported annotations can be found in the com.abiquo.bond.api.annotations package.
 */
public abstract class AbstractPlugin implements PluginInterface
{
    private final static Logger logger = LoggerFactory.getLogger(AbstractPlugin.class);

    private WrapperNotification handlerNotifications;

    private boolean cancelled = false;

    /**
     * A mapping of method annotation and the expected parameter type. This is used to search for
     * methods in the class that can be used to handle events.
     */
    static private Map<Class< ? >, Class< ? >> mapAnnotationToEvent = new HashMap<>();
    static
    {
        mapAnnotationToEvent.put(HandleBackupVMEvent.class, BackupVMEvent.class);
        mapAnnotationToEvent.put(HandleDeployVMEvent.class, DeployVMEvent.class);
        mapAnnotationToEvent.put(HandleUndeployVMEvent.class, UndeployVMEvent.class);
        mapAnnotationToEvent.put(HandleAnyEvent.class, APIEvent.class);
    }

    private Map<Class< ? extends APIEvent>, Method> mapEventToMethod = new HashMap<>();

    private LinkedBlockingQueue<APIEvent> eventqueue = new LinkedBlockingQueue<>();

    @SuppressWarnings("unchecked")
    public AbstractPlugin() throws PluginException
    {
        // Work out which method to call for each event type.
        Method[] handlermethods = this.getClass().getMethods();
        for (Method method : handlermethods)
        {
            for (Class< ? > annotation : mapAnnotationToEvent.keySet())
            {
                if (method.isAnnotationPresent((Class< ? extends Annotation>) annotation))
                {
                    logger.debug("Annotation {} found on method {}", annotation, method);
                    Class< ? extends APIEvent>[] parameterTypes =
                        (Class< ? extends APIEvent>[]) method.getParameterTypes();
                    if (parameterTypes.length == 1)
                    {
                        if (parameterTypes[0] == mapAnnotationToEvent.get(annotation))
                        {
                            if (mapEventToMethod.get(parameterTypes[0]) == null)
                            {
                                mapEventToMethod.put(parameterTypes[0], method);
                            }
                            else
                            {
                                // More than one handler method for event type
                                throw new PluginException("Multiple methods found for handling event type "
                                    + parameterTypes[0]);
                            }
                        }
                        else
                        {
                            // Wrong parameter type
                            throw new PluginException("Annotated method (" + method
                                + ") found with wrong parameter type (" + parameterTypes[0] + ")");
                        }
                    }
                    else
                    {
                        // Invalid method signature
                        throw new PluginException("Annotated method (" + method
                            + ") found with too many parameters (" + parameterTypes.length + ")");
                    }
                }
            }
        }
        logger.debug("Started plugin {}", this.getName());
    }

    /**
     * Allows the plugin to pass messages back to the client wrapper without interrupting the flow
     * of the program.
     * 
     * @param handler notification handler supplied by the client wrapper
     */
    public void setNotificationHandler(final WrapperNotification handler)
    {
        handlerNotifications = handler;
    }

    /**
     * Pass a notification back to the client wrapper if a notification handler has been set
     * 
     * @param msg the notification to be return to the client wrapper
     */
    protected void notifyWrapper(final String msg)
    {
        if (handlerNotifications != null)
        {
            handlerNotifications.notification(msg);
        }
    }

    /**
     * Pass a notification and exception back to the client wrapper if a notification handler has
     * been set
     * 
     * @param msg the notification to be return to the client wrapper
     * @param t the exception to be return to the client wrapper
     */
    protected void notifyWrapper(final String msg, final Throwable t)
    {
        if (handlerNotifications != null)
        {
            handlerNotifications.notification(msg, t);
        }
    }

    /**
     * Takes events from the queue and uses reflection to call the correct method to process them.
     */
    @Override
    public void run()
    {
        try
        {
            logger.debug("Starting up {}", this.getName());
            startup();
            logger.debug("Start up of {} complete", this.getName());
        }
        catch (PluginException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        while (!cancelled)
        {
            try
            {
                APIEvent event = eventqueue.poll(10, TimeUnit.SECONDS);
                if (!cancelled && event != null)
                {
                    Method eventhandler = mapEventToMethod.get(event.getClass());
                    logger.debug("Processing {} with method {}", event.toString(),
                        eventhandler.getName());
                    try
                    {
                        eventhandler.invoke(this, new Object[] {event});
                    }
                    catch (IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e)
                    {
                        notifyWrapper("Plugin failed to process event " + event.toString(), e);
                    }
                }
            }
            catch (InterruptedException e)
            {
                logger.info("{} event queue interrupted", this.getName());
            }
        }
        logger.info("{} was cancelled", this.getName());
    }

    /**
     * Adds an event to the queue for processing in the
     * {@link com.abiquo.bond.api.plugin.AbstractPlugin#run} method.
     */
    @Override
    public void processEvent(final APIEvent event)
    {
        if (mapEventToMethod.containsKey(event.getClass()))
        {
            logger.debug("Adding {} to queue", event.toString());
            eventqueue.offer(event);
        }
        else
        {
            // Received event for which we had not registered
        }
    }

    @Override
    public boolean handlesEventType(final Class< ? extends APIEvent> event)
    {
        return mapEventToMethod.get(event) != null;
    }

    @Override
    public void cancel()
    {
        this.cancelled = true;
    }
}

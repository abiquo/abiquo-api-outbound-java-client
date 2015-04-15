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

import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.WrapperNotification;
import com.abiquo.bond.api.annotations.HandleAnyEvent;
import com.abiquo.bond.api.annotations.HandleBackupVMEvent;
import com.abiquo.bond.api.annotations.HandleDeployVMEvent;
import com.abiquo.bond.api.annotations.HandleRestoreVMEvent;
import com.abiquo.bond.api.annotations.HandleUndeployVMEvent;
import com.abiquo.bond.api.event.APIEvent;
import com.abiquo.bond.api.event.APIEventResult;
import com.abiquo.bond.api.event.APIEventResultState;
import com.abiquo.bond.api.event.BackupVMEvent;
import com.abiquo.bond.api.event.DeployVMEvent;
import com.abiquo.bond.api.event.RestoreVMEvent;
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
        mapAnnotationToEvent.put(HandleRestoreVMEvent.class, RestoreVMEvent.class);
        mapAnnotationToEvent.put(HandleAnyEvent.class, APIEvent.class);
        // TODO investigate what happens adding this line (SCG SAN)
        // mapAnnotationToEvent.put(HandleAnyEvent.class, APIEventResult.class);
    }

    private Map<Class< ? extends APIEvent>, Method> mapEventToMethod = new HashMap<>();

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
     * Works out which method to use to process the plugin and then executes that method..
     *
     * @return the result of the event processing
     */
    @Override
    public APIEventResult processEvent(final APIEvent event)
    {
        APIEventResult result;
        if (mapEventToMethod.containsKey(event.getClass()))
        {
            logger.debug("Adding {} to queue", event.toString());
            Method eventhandler = mapEventToMethod.get(event.getClass());
            logger.debug("Processing {} with method {}", event.toString(), eventhandler.getName());
            try
            {
                eventhandler.invoke(this, new Object[] {event});
                result = new APIEventResult(APIEventResultState.COMPLETE, event);
            }
            catch (Throwable t)
            {
                String msg = format("Error processing event: %s", event.toString());
                notifyWrapper(msg, t);
                result = new APIEventResult(APIEventResultState.FAILED, event, msg, t);
            }
        }
        else
        {
            result = new APIEventResult(APIEventResultState.EVENTTYPENOTSUPPORTED, event);
        }
        return result;
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

    @Override
    public boolean isRunning()
    {
        return !cancelled;
    }
}

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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.event.APIEvent;
import com.abiquo.bond.api.event.APIEventResult;
import com.abiquo.bond.api.plugin.PluginEventException;
import com.abiquo.bond.api.plugin.PluginInterface;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * This class handles the dispatching of events to the registered plugins. It does this by adding a
 * call to the plugin.processEvent message for each event and for each plugin to an ExecutorService
 */
public class EventDispatcher
{
    private final static Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private final ListeningExecutorService eventDispatcher;

    private final Set<PluginInterface> plugins;

    private Date lastEventTimestamp;

    public EventDispatcher(final Set<PluginInterface> plugins, final int numThreads)
    {
        this.plugins = plugins;
        eventDispatcher =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads));
    }

    /**
     * Submits a Future to the ExecutorService that will execute the plugin's processEvent method
     * for the event. Also creates a callback that will be executed when all the plugins have
     * completed processing the event. The callback method updates the lastEventTimestamp variable.
     * 
     * @param event the event to be processed by each plugin
     */
    void dispatchEvent(final APIEvent event)
    {
        List<ListenableFuture<APIEventResult>> futures = new ArrayList<>();
        for (final PluginInterface plugin : plugins)
        {
            if (plugin.handlesEventType(event.getClass()))
            {
                ListenableFuture<APIEventResult> task =
                    eventDispatcher.submit(new Callable<APIEventResult>()
                    {
                        @Override
                        public APIEventResult call()
                        {
                            return plugin.processEvent(event);
                        }
                    });
                futures.add(task);
            }
        }

        if (!futures.isEmpty())
        {
            Futures.addCallback(Futures.allAsList(futures),
                new FutureCallback<List<APIEventResult>>()
                {

                    @Override
                    public void onSuccess(final List<APIEventResult> results)
                    {
                        for (APIEventResult result : results)
                        {
                            Date eventts = result.getEvent().getTimestamp();
                            if (lastEventTimestamp == null || lastEventTimestamp.before(eventts))
                            {
                                lastEventTimestamp = eventts;
                                logger.info("Last event timestamp updated to {}",
                                    lastEventTimestamp);
                            }
                        }

                    }

                    @Override
                    public void onFailure(final Throwable t)
                    {
                        if (t instanceof PluginEventException)
                        {
                            PluginEventException pee = (PluginEventException) t;
                            Optional<APIEvent> optEvent = pee.getAPIEvent();
                            if (optEvent.isPresent())
                            {
                                Date eventts = optEvent.get().getTimestamp();
                                if (lastEventTimestamp == null
                                    || lastEventTimestamp.before(eventts))
                                {
                                    lastEventTimestamp = eventts;
                                }
                            }
                        }
                        else
                        {
                            // There's nothing we can do if any other exception type is thrown as we
                            // won't know which event was being processed. Simply log a message.
                            // Hopefully one of the other plugins will process the event
                            // successfully and that will update the timestamp. If not then the
                            // event will be resubmitted for processing by all plugins if no later
                            // events are successfully processed.
                            logger.error("Processing of event failed", t);
                        }
                    }
                });
        }
    }

    /**
     * Get the timestamp of the last event to complete processing by all plugins.
     * 
     * @return
     */
    public Date getLastEventTimestamp()
    {
        return lastEventTimestamp;
    }

    /**
     * Request the shutdown of the executor service. If the level is AWAIT_ALL_TASKS or
     * AWAIT_RUNNING_TASKS, use the default timeout of 5 minutes.
     * 
     * @param level the type of shutdown to perform
     */
    public void shutdown(final ShutdownLevel level)
    {
        shutdown(level, 0, null);
    }

    /**
     * Request the shutdown of the executor service and wait up to a specified time for this to
     * complete
     * 
     * @param level the type of shutdown to perform
     * @param timeout how long to wait for the executor service to shut down. If the level is
     *            IMMEDIATE this parameter will be ignored. If the value is zero the default timeout
     *            of 5 minutes will be used.
     * @param unit the unit the timeout is specified in. If the level is IMMEDIATE this parameter
     *            will be ignored. If the value is null the default timeout of 5 minutes will be
     *            used.
     */
    public void shutdown(final ShutdownLevel level, final int timeout, final TimeUnit unit)
    {
        if (level == ShutdownLevel.AWAIT_ALL_TASKS)
        {
            eventDispatcher.shutdown();
        }
        else
        {
            eventDispatcher.shutdownNow();
        }
        if (level != ShutdownLevel.IMMEDIATE)
        {
            try
            {
                int to = timeout == 0 || unit == null ? 5 : timeout;
                TimeUnit tu = timeout == 0 || unit == null ? TimeUnit.MINUTES : unit;
                eventDispatcher.awaitTermination(to, tu);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Indicates the way that the executor should be shutdown
     * <p>
     * AWAIT_ALL_TASKS: shut down after all started and queued tasks have completed (or the
     * specified timeout is reached)
     * <p>
     * AWAIT_RUNNING_TASKS: shut down after all started tasks have completed (or the specified
     * timeout is reached). Any waiting tasks will not be started
     * <p>
     * IMMEDIATE: shut down after requesting started tasks to complete, but don't wait for the tasks
     * to complete
     */
    public enum ShutdownLevel
    {
        AWAIT_ALL_TASKS, AWAIT_RUNNING_TASKS, IMMEDIATE;
    }
}

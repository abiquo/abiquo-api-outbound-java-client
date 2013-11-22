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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.event.APIEvent;
import com.abiquo.bond.api.event.APIEventResult;
import com.abiquo.bond.api.plugin.PluginInterface;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * This class handles the dispatching of events to the registered plugins. It does this by adding a
 * call to the plugin.processEvent message for each event and for each plugin to an ExecutorService
 */
public class EventDispatcher
{
    private final static Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private ExecutorService eventDispatcher;

    private Set<PluginInterface> plugins;

    private SortedMap<APIEvent, List<Future<APIEventResult>>> results = new TreeMap<>();

    private ResultsChecker checker;

    private ScheduledExecutorService checkerExecutor;

    public EventDispatcher(final Set<PluginInterface> plugins, final int numThreads)
    {
        this.plugins = plugins;
        eventDispatcher =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads));
        checker = new ResultsChecker();
        checkerExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory());
        checkerExecutor.scheduleAtFixedRate(checker, 10, 10, TimeUnit.SECONDS);
    }

    void dispatchEvent(final APIEvent event)
    {
        List<Future<APIEventResult>> futures = new ArrayList<>();
        for (final PluginInterface plugin : plugins)
        {
            if (plugin.handlesEventType(event.getClass()))
            {
                Future<APIEventResult> task = eventDispatcher.submit(new Callable<APIEventResult>()
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
        results.put(event, futures);
    }

    public Date getLastEventTimestamp()
    {
        return checker.getLastEventTimestamp();
    }

    class ResultsChecker implements Runnable
    {
        private Date lastEventTimestamp;

        @Override
        public void run()
        {
            Iterator<APIEvent> iterEvents = results.keySet().iterator();
            while (iterEvents.hasNext())
            {
                APIEvent event = iterEvents.next();
                boolean eventFullyProcessed = true;
                List<Future<APIEventResult>> futures = results.get(event);
                Iterator<Future<APIEventResult>> iterFutures = futures.listIterator();
                while (eventFullyProcessed && iterFutures.hasNext())
                {
                    Future<APIEventResult> future = iterFutures.next();
                    try
                    {
                        future.get(100, TimeUnit.MILLISECONDS);
                        iterFutures.remove();
                    }
                    catch (InterruptedException e)
                    {
                        eventFullyProcessed = false;
                    }
                    catch (ExecutionException e)
                    {
                        iterFutures.remove();
                    }
                    catch (TimeoutException e)
                    {
                        eventFullyProcessed = false;
                    }
                }
                if (eventFullyProcessed)
                {
                    if (lastEventTimestamp == null
                        || event.getTimestamp().after(lastEventTimestamp))
                    {
                        lastEventTimestamp = event.getTimestamp();
                        logger.debug("All processing of event {} complete.", event);
                    }
                    iterEvents.remove();
                }
                else
                {
                    break;
                }
            }
        }

        public Date getLastEventTimestamp()
        {
            return lastEventTimestamp;
        }
    }

    class NamedThreadFactory implements ThreadFactory
    {
        private ThreadFactory defFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(final Runnable r)
        {
            Thread t = defFactory.newThread(r);
            t.setName("ABQ_Event_Dispatcher_Results_Checker");
            return t;
        }
    }
}

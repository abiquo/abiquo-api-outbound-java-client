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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.EventDispatcher.ShutdownLevel;
import com.abiquo.bond.api.event.APIEvent;
import com.abiquo.bond.api.plugin.BackupPluginInterface;
import com.abiquo.bond.api.plugin.PluginException;
import com.abiquo.bond.api.plugin.PluginInterface;
import com.abiquo.bond.api.response.ResponsesHandler;
import com.abiquo.event.model.Event;
import com.abiquo.model.rest.RESTLink;
import com.google.common.base.Optional;

/**
 * This class is the central controller for the Outbound API client. It's main functions are:
 * <ul>
 * <li>Connect to the Outbound API ('M') server
 * <li>Load plugins that will handle the messages received from the M server
 * <li>Pass messages received from the M server to the plugins
 * </ul>
 * A client wrapper class should
 * <ul>
 * <li>Create an instance of this class.
 * <li>Call <code>getLoadFailures</code> to check for any failures during plugin loading
 * <li>If the result of the plugin loading is satisfactory, the plugins should be configured. How
 * this is done is specific to each plugin.
 * <li>Call <code>startPluginThreads</code> to get the plugins ready to process messages
 * <li>Call <code>connectToM</code>. This will start pulling messages down from the M server.
 * <li>To stop messages being passed to the plugins, call <code>disconnect</code>
 * </ul>
 */
public class OutboundAPIClient implements CommsHandler, EventStoreHandler
{
    private final static Logger logger = LoggerFactory.getLogger(OutboundAPIClient.class);

    private MConnector mConnector;

    private ConfigurationData config;

    /*
     * References to all the plugins loaded by this class
     */
    private Set<PluginInterface> handlers;

    private Set<BackupPluginInterface> handlersWithResponses = new HashSet<>();

    private List<Throwable> failures = new ArrayList<>();

    private EventTranslator translator;

    private EventStore eventstore;

    private List<APIEvent> msgq = new ArrayList<>();

    private boolean processingEventStore = true;

    private NameToVMLinks mapNameToVMLinks;

    private APIConnection apiconn;

    private RESTLink currUserEditLink;

    private WrapperNotification handlerNotifications;

    private ResponsesHandler responses;

    private EventDispatcher eventDispatcher;

    private boolean shutdown = false;

    /**
     * The purpose of the constructor is to identify and load plugins.
     * 
     * @param data Configuration data for the client. See {@link ConfigurationData} for details of
     *            what data is required.
     * @param version The version indicated by the client
     * @throws OutboundAPIClientException
     */
    public OutboundAPIClient(final ConfigurationData data, final String version)
        throws OutboundAPIClientException
    {
        this.config = new ConfigurationData(data);

        apiconn =
            new APIConnection(config.getMServer(), config.getMUser(), config.getMUserPassword());
        currUserEditLink = apiconn.getCurrentUserLink();
        if (!version.trim().equalsIgnoreCase(apiconn.getAPIVersion().trim()))
        {
            throw new OutboundAPIClientException("Api version indicated to start plugin mismatch with api version in use");
        }

        // Create a cache of the REST links associated with each VM
        mapNameToVMLinks =
            new NameToVMLinks(config.getMServer(), config.getMUser(), config.getMUserPassword());

        // Set up response handlers to fetch data from the third party applications and update
        // Abiquo server with it
        // We need to get the repeat time from the configuration at some point - for now default it
        // to 10 minutes.
        responses =
            new ResponsesHandler(config.getMServer(),
                config.getMUser(),
                config.getMUserPassword(),
                mapNameToVMLinks,
                10,
                TimeUnit.MINUTES);

        // Find and load any plugins on the classpath that support the returning of data from the
        // third party app to Abiquo. At the moment this just means Backup plugins
        Set<PluginInterface> plugins = new HashSet<>();
        ServiceLoader<BackupPluginInterface> bsl = ServiceLoader.load(BackupPluginInterface.class);
        Iterator<BackupPluginInterface> biterator = bsl.iterator();
        while (biterator.hasNext())
        {
            try
            {
                BackupPluginInterface handler = biterator.next();
                logger.info("Loaded backup plugin: " + handler.getName());
                plugins.add(handler);
                handlersWithResponses.add(handler);
                // responses.addBackupPlugin(handler);
            }
            catch (Throwable t)
            {
                logger.error("Loading of plugin failed", t);
                failures.add(t);
            }
        }

        // Find and load all other plugins
        ServiceLoader<PluginInterface> sl = ServiceLoader.load(PluginInterface.class);
        Iterator<PluginInterface> iterator = sl.iterator();
        while (iterator.hasNext())
        {
            try
            {
                PluginInterface handler = iterator.next();
                logger.info("Loaded plugin: " + handler.getName());
                plugins.add(handler);
            }
            catch (Throwable t)
            {
                logger.error("Loading of plugin failed", t);
                failures.add(t);
            }
        }

        handlers = Collections.unmodifiableSet(plugins);

        eventDispatcher = new EventDispatcher(handlers, 1);

        // Initialise the class that will fecth events from the permanent store that may have been
        // missed since the last time the client was run
        eventstore =
            new EventStore(config.getMServer(),
                config.getMUser(),
                config.getMUserPassword(),
                currUserEditLink,
                mapNameToVMLinks);
    }

    /**
     * Allows the client to pass messages back to the client wrapper without interrupting the flow
     * of the program.
     * 
     * @param handler notification handler supplied by the client wrapper
     */
    public void setNotificationHandler(final WrapperNotification handler)
    {
        handlerNotifications = handler;
        if (eventstore != null)
        {
            eventstore.setNotificationHandler(handler);
        }
        if (responses != null)
        {
            responses.setNotificationHandler(handler);
        }
        if (mapNameToVMLinks != null)
        {
            mapNameToVMLinks.setNotificationHandler(handler);
        }
    }

    /**
     * Pass a notification and exception back to the client wrapper if a notification handler has
     * been set
     * 
     * @param msg the notification to be return to the client wrapper
     * @param t the exception to be return to the client wrapper
     */
    private void notifyWrapper(final String msg, final Throwable t)
    {
        if (handlerNotifications != null)
        {
            handlerNotifications.notification(msg, t);
        }
    }

    /**
     * When the wrapper class is closing down it should call this method to get the timestamp of the
     * last outbound api event that has been handled. This timestamp can then be used the next time
     * the client starts up to fecth messages from the permanent event store.
     * 
     * @return Timestamp of last processed outbound api event
     */
    public LocalDateTime getLastEventTimestamp()
    {
        return eventDispatcher.getLastEventTimestamp();
    }

    /**
     * Start each successfully loaded plugin
     */
    public void startPlugins()
    {
        translator =
            new EventTranslator(config.getMServer(),
                config.getMUser(),
                config.getMUserPassword(),
                currUserEditLink,
                mapNameToVMLinks);

        for (BackupPluginInterface plugin : handlersWithResponses)
        {
            responses.addBackupPlugin(plugin);
        }

        failures.clear();
        for (PluginInterface handler : handlers)
        {
            try
            {
                handler.startup();
            }
            catch (PluginException e)
            {
                logger.error("Startup of plugin failed", e);
                failures.add(e);
            }
        }
    }

    /**
     * Wait for all the plugin threads to complete
     */
    public boolean isRunning()
    {
        return !shutdown;
    }

    /**
     * Returns a Set of instances of successfully loaded plugins
     * 
     * @return Set of PluginInterface instances
     */
    public Set<PluginInterface> getPluginList()
    {
        return handlers;
    }

    /**
     * Returns a List of exceptions thrown by plugins during loading
     * 
     * @return List of Throwables
     */
    public List<Throwable> getLoadFailures()
    {
        return Collections.unmodifiableList(failures);
    }

    /**
     * Opens an HTTP connection to the M server
     * 
     * @throws OutboundAPIClientException if any errors occur during the connection attempt
     */
    public void run() throws OutboundAPIClientException
    {
        try
        {
            // Open the connection to the M server
            @SuppressWarnings("unchecked")
            Class< ? extends MConnector> connectorclass =
                (Class< ? extends MConnector>) Class.forName(config.getConnector());
            Constructor< ? extends MConnector> constructor =
                connectorclass.getConstructor(CommsHandler.class);
            mConnector = constructor.newInstance(this);
            mConnector.connect(config.getMServer(), config.getMUser(), config.getMUserPassword());

            // As the connector is now running we can ignore anything in the event store after this
            // time.
            eventstore.setMsgTimeLimit();

            // Check the Event log for any messages missed since the program last ran
            LocalDateTime lastmsg = config.getLastProcessedEvent();
            if (lastmsg != null)
            {
                eventstore.getMissedEvents(lastmsg, this);

                List<APIEvent> msgqcopy = new ArrayList<>();
                while (processingEventStore)
                {
                    synchronized (msgq)
                    {
                        if (msgq.isEmpty())
                        {
                            processingEventStore = false;
                        }
                        else
                        {
                            msgqcopy.addAll(msgq);
                            msgq.clear();
                        }
                    }
                    for (APIEvent event : msgqcopy)
                    {
                        handleMessage(event);
                    }
                }
            }
            else
            {
                processingEventStore = false;
            }
        }
        catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e)
        {
            throw new OutboundAPIClientException("Configuration error", e);
        }
    }

    /**
     * Closes the connection to the M server
     */
    public void close()
    {
        // Stop each of the plugins
        for (PluginInterface plugin : handlers)
        {
            plugin.cancel();
        }

        // Wait for the plugins to stop
        eventDispatcher.shutdown(ShutdownLevel.AWAIT_RUNNING_TASKS);

        // Disconnect from the Outbound API
        if (mConnector != null)
        {
            mConnector.disconnect();
        }
        shutdown = true;
    }

    @Override
    public void handleMessage(final APIEvent apievent)
    {
        eventDispatcher.dispatchEvent(apievent);
    }

    @Override
    public void handleMessage(final String msg)
    {
        APIEvent apievent = null;
        try
        {
            Event event = AbiquoObjectMapper.OBJECT_MAPPER.instance().readValue(msg, Event.class);
            logger.debug(
                "Received event: Type:{} Action:{} Time:{}",
                new Object[] {
                event.getType(),
                event.getAction(),
                LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp()),
                    ZoneId.systemDefault())});
            Optional<APIEvent> optapievent = translator.translate(event);
            if (optapievent.isPresent())
            {
                apievent = optapievent.get();
                if (processingEventStore)
                {
                    synchronized (msgq)
                    {
                        msgq.add(apievent);
                    }
                }
                else
                {
                    handleMessage(apievent);
                }
            }
        }
        catch (IOException | OutboundAPIClientException e)
        {
            logger.warn("Exception whilst translating message from outbound api", e);
            notifyWrapper("Exception whilst translating message from outbound api", e);
        }
    }

    @Override
    public void handleHeaders(final String headers)
    {
        logger.info("HTTP headers from 'M' server connection: {}", headers);
    }

    @Override
    public void handleTransportType(final Transport transport)
    {
        if (transport.equals(config.getTransport()))
        {
            logger.info("Negotiated requested transport ({}) with 'M' server", transport);
        }
        else
        {
            logger.info("Negotiated alternative transport ({}) with 'M' server", transport);
            config.setNegotiatedTransport(transport);
        }
    }
}

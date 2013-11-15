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
package com.abiquo.bond.api.connector;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.atmosphere.wasync.impl.AtmosphereRequest;
import org.atmosphere.wasync.impl.DefaultOptions;
import org.atmosphere.wasync.impl.DefaultOptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.CommsHandler;
import com.abiquo.bond.api.MConnector;
import com.abiquo.bond.api.OutboundAPIClientException;
import com.abiquo.bond.api.Transport;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Realm;

/**
 * Class that uses the wAsync library to create communication links between the client and the M
 * server. It uses the AsyncHttpClient library to provide authentication credentials. The wAsync
 * library has built-in support for connecting to Atmosphere servers and adds the required
 * Atmosphere HTTP params automatically
 * 
 * @see <a href="http://atmosphere.github.io/wasync/apidocs/">wAsync javadoc</a>
 * @see <a
 *      href="http://sonatype.github.io/async-http-client/apidocs/reference/packages.html">AsyncHttpClient
 *      javadoc</a>
 */
public class WAsyncConnector implements MConnector
{
    private final static Logger logger = LoggerFactory.getLogger(WAsyncConnector.class);

    private final CommsHandler msghandler;

    private Socket socket;

    public WAsyncConnector(final CommsHandler mh)
    {
        this.msghandler = mh;
    }

    /**
     * Connects to the M server and sets up callbacks for handling responses.
     * <p>
     * Currently the wasync library doesn't seem to communicate to the AsyncHttpClientConfig that a
     * connection has been successfully established and so the AsyncHttpClientConfig throws a
     * timeout exception after 60 seconds. To get around this until the problem is fixed, we set the
     * connection and idle timeouts to -1, meaning no timeout.
     * 
     * @throws OutboundAPIClientException
     */
    @Override
    public void connect(final String server, final String user, final String password)
        throws OutboundAPIClientException
    {
        AsyncHttpClientConfig.Builder ccBuilder = new AsyncHttpClientConfig.Builder();
        ccBuilder.setRequestTimeoutInMs(-1);
        ccBuilder.setIdleConnectionTimeoutInMs(-1);
        Realm realm =
            new Realm.RealmBuilder().setPrincipal(user).setPassword(password)
                .setUsePreemptiveAuth(true).setScheme(Realm.AuthScheme.BASIC).build();
        ccBuilder.setRealm(realm);
        AsyncHttpClient ahcClient = new AsyncHttpClient(ccBuilder.build());

        AtmosphereClient client = ClientFactory.getDefault().newClient(AtmosphereClient.class);

        String url = "http://" + server + "/m/stream?Content-Type=application/json";
        AtmosphereRequest.AtmosphereRequestBuilder requestbuilder =
            client.newRequestBuilder().method(Request.METHOD.GET).uri(url)
                .transport(Request.TRANSPORT.SSE);
        Request request = requestbuilder.build();

        DefaultOptionsBuilder optionsBuilder = client.newOptionsBuilder();
        DefaultOptions options = optionsBuilder.build();
        options.runtime(ahcClient);

        socket = client.create(options);
        socket.on(new Function<TimeoutException>()
        {
            @Override
            public void on(final TimeoutException t)
            {
                logger.error("'M' Server comms: Error: {}", t.getMessage(), t);
            }

        }).on(Event.CLOSE, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                logger.info("'M' Server comms: Connection closed");
            }
        }).on(Event.ERROR, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                logger.error("'M' Server comms: Error: " + t);
            }
        }).on(Event.HEADERS, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                logger.debug("'M' Server comms: Header: " + t);
                msghandler.handleHeaders(t);
            }
        }).on(Event.MESSAGE, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                logger.debug("'M' Server comms: Message: " + t);
                msghandler.handleMessage(t);
            }
        }).on(Event.MESSAGE_BYTES, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                logger.debug("'M' Server comms: Message bytes: " + t);
                logger.info("Message Bytes");
            }
        }).on(Event.OPEN, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                logger.info("Server comms: Connection opened");
            }
        }).on(Event.REOPENED, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                logger.info("Server comms: Connection reopened");
            }
        }).on(Event.STATUS, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                logger.info("Server comms: Connection status: {}", t);
            }
        }).on(Event.TRANSPORT, new Function<String>()
        {
            @Override
            public void on(final String t)
            {
                try
                {
                    logger.debug("'M' Server comms: Transport: {}", t);
                    Transport transport = Transport.valueOf(t.toUpperCase());
                    msghandler.handleTransportType(transport);
                }
                catch (IllegalArgumentException iae)
                {
                    logger.error("Server comms: Unsupported transport type {}.", t);
                }
            }
        });
        try
        {
            socket.open(request);
        }
        catch (IOException e)
        {
            logger.error("Opening connection to Outbound API Server failed", e);
            throw new OutboundAPIClientException("Opening connection to Outbound API Server failed",
                e);
        }
    }

    @Override
    public void disconnect()
    {
        if (socket != null)
        {
            socket.close();
        }
    }

}

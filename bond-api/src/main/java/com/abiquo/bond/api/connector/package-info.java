/**
 * This package contains classes that can be used to establish a connection between the Outbound API
 * Client and the Abiquo 'M' server.
 * <p>
 * Communication with the Abiquo 'M' server requires an implementation of the JAX-RS 2.0
 * specification that supports Server Sent Events (SSE). A number of these exist but each, so far,
 * has had bugs in it. Therefore the client code has been designed to make it as easy as possible to
 * switch to another implementation. This will be done by means of a connector class. Such a class
 * will need to implement the {@link com.abiquo.bond.api.MConnector} interface and be able to
 * establish a connection to the 'M' server and pass on returned data to the Client.
 * <p>
 * This package contains the connectors that have been implemented so far.
 */
package com.abiquo.bond.api.connector;
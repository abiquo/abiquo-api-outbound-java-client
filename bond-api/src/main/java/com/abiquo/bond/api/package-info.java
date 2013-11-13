/**
 * This package contains the main classes for the Abiquo Outbound API Client.
 * <p>
 * As well as the main class that controls the Client ({@link com.abiquo.bond.api.OutboundAPIClient}
 * ), it also contains classes that support the following requirements:
 * <ul>
 * <li>Fetching data using the Abiquo standard API.
 * <li>Fetching events from the permanent event store.
 * <li>Converting the Outbound API events to objects that can be passed on to the plugins.
 * <li>Caching information about the virtual machines controlled by Abiquo
 * <li>Passing messages back to the client wrapper class.
 * </ul>
 */
package com.abiquo.bond.api;
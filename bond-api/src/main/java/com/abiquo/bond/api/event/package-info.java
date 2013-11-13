/**
 * This package contains classes representing the events received from the Outbound API that can be
 * passed on to the plugins.
 * <p>
 * When an event is received from the Outbound API, it doesn't contain much information about the
 * event. Instead it contains a number of REST URLs that can be used to fetch more information using
 * the Abiquo standard API. Rather than simply passing on the bare bones event to the plugins, the
 * client fetches the extra data and combines this with the original event into a new object. This
 * package contains those objects.
 */
package com.abiquo.bond.api.event;
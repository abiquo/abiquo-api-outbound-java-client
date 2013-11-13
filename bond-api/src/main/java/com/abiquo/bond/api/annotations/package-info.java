/**
 * This package contains Annotations to indicate which plugin methods are to be used to handle each
 * type of Outbound API event.
 * <p>
 * Plugins indicate to the Outbound API Client which events they are interested in by putting
 * Annotations on their methods. Individual methods can have multiple annotations if they are
 * designed to handle more than one type of event, but there cannot be multiple methods with the
 * same annotation.
 */
package com.abiquo.bond.api.annotations;
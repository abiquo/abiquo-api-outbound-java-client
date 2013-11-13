/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api;

/**
 * The client wrapper class should create an instance of a class that implements this interface and
 * pass it to the client in order to receive notifications of anything that occurs in the client. It
 * will then be up to the client wrapper class to decide what action to take in response (close
 * down, notify user in some way, etc).
 */
public interface WrapperNotification
{
    public void notification(String msg);

    public void notification(String msg, Throwable t);

    public void notification(String msg, String url, int statuscode);
}

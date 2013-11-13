/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.client;

import com.abiquo.bond.api.WrapperNotification;

public class NotificationHandler implements WrapperNotification
{

    public NotificationHandler()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void notification(final String msg)
    {
        System.out.println("Received message from client: " + msg);
    }

    @Override
    public void notification(final String msg, final Throwable t)
    {
        System.out.println("Received message from client: " + msg);
        System.out.println("Received exception from client: " + t.getMessage());
    }

    @Override
    public void notification(final String msg, final String url, final int statuscode)
    {
        System.out.println("Received message from client: HTTP request to " + url
            + " returned status " + statuscode + ". " + msg);
    }

}

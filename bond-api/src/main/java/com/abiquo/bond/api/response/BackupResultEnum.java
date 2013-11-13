/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.response;

/**
 * An interface representing the three backup result types handled by the Abiquo server.
 */
public enum BackupResultEnum
{
    DONE("done"), INPROGRESS("progress"), FAILED("failed");

    private String abq_value;

    BackupResultEnum(final String abq_value)
    {
        this.abq_value = abq_value;
    }

    @Override
    public String toString()
    {
        return abq_value;
    }
}

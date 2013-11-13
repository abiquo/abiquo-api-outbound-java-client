/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.response;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of backup results related to a single virtual machine
 */
public class VMBackupStatusList
{
    private String vmname;

    private List<VMBackupStatus> statuses = new ArrayList<>();

    public VMBackupStatusList(final String vmname, final List<VMBackupStatus> statuses)
    {
        this.vmname = vmname;
        this.statuses.addAll(statuses);
    }

    public String getVMName()
    {
        return vmname;
    }

    public List<VMBackupStatus> getStatuses()
    {
        return statuses;
    }

    public int size()
    {
        return statuses.size();
    }
}

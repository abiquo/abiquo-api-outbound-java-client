/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.abiquo.bond.api.abqapi.VMMetadata;

/**
 * A class representing the result of a backup operation. A backup plugin will need to convert the
 * results it receives from the backup software into instances of this class which will then be used
 * to update the Abiquo server.
 */
public class VMBackupStatus implements Comparable<VMBackupStatus>
{
    private static SimpleDateFormat dateformat = new SimpleDateFormat(VMMetadata.DATE_FORMAT);

    private String reason;

    private BackupResultEnum state;

    private String name = "defined_hour";

    private String type = "3";

    private Date date;

    private long size;

    public VMBackupStatus()
    {
        // TODO Auto-generated constructor stub
    }

    public Map<String, Object> getMetaData()
    {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put(VMMetadata.DATE, dateformat.format(date));
        metadata.put("status", state.toString());
        metadata.put("name", name);
        metadata.put("size", size);
        metadata.put("type", type);

        return metadata;
    }

    public void setState(final BackupResultEnum state, final String reason)
    {
        this.state = state;
        this.reason = reason;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public void setType(final String type)
    {
        this.type = type;
    }

    public void setDate(final Date date)
    {
        this.date = date;
    }

    public void setSize(final long size)
    {
        this.size = size;
    }

    @Override
    public int compareTo(final VMBackupStatus o)
    {
        return (int) (date.getTime() - o.date.getTime());
    }
}

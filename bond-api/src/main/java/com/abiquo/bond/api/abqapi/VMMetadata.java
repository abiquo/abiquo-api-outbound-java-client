/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.abqapi;

/**
 * This class is a partial copy of the
 * {@link com.abiquo.api.services.cloud.VirtualMachineMetadataService} class. The only parts of it
 * we need are the definitions of the keys and date and time formats used in the virtual machine
 * metadata Map.
 */
public class VMMetadata
{
    public static final String METADATA = "metadata";

    public static final String BACKUP = "backupSchedule";

    public static final String COMPLETE = "complete";

    public static final String SNAPSHOT = "snapshot";

    public static final String FILESYSTEM = "filesystem";

    public static final String DEFINED_HOUR = "defined_hour";

    public static final String HOURLY = "hourly";

    public static final String WEEKLY = "weekly_planned";

    public static final String MONDAY = "monday";

    public static final String TUESDAY = "tuesday";

    public static final String WEDNESDAY = "wednesday";

    public static final String THURSDAY = "thursday";

    public static final String FRIDAY = "friday";

    public static final String SATURDAY = "saturday";

    public static final String SUNDAY = "sunday";

    public static final String TIME = "time";

    public static final String DISKS = "disks";

    public static final String PATHS = "paths";

    public static final String LAST_BACKUPS = "backupResults";

    public static final String RESULTS = "results";

    public static final String DATE = "date";

    public static final String DONE = "done";

    public static final String PROGRESS = "progress";

    public static final String FAILED = "failed";

    public static final Object STATUS = "status";

    public static final String TIME_FORMAT = "HH:mm:ss Z";

    public static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss Z";

    public static final String CHEF = "chef";

    /**
     * This field is missing from the
     * {@link com.abiquo.api.services.cloud.VirtualMachineMetadataService} class.
     */
    public static final String MONTHLY = "monthly";

    /**
     * This field is missing from the
     * {@link com.abiquo.api.services.cloud.VirtualMachineMetadataService} class.
     */
    public static final String DAILY = "daily";

    private VMMetadata()
    {
    }

}

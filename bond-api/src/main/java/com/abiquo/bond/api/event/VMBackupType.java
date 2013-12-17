/**
 * The Abiquo Platform
 * Cloud management application for hybrid clouds
 * Copyright (C) 2008 - Abiquo Holdings S.L.
 *
 * This application is free software; you can redistribute it and/or
 * modify it under the terms of the GNU LESSER GENERAL PUBLIC
 * LICENSE as published by the Free Software Foundation under
 * version 3 of the License
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * LESSER GENERAL PUBLIC LICENSE v.3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package com.abiquo.bond.api.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public enum VMBackupType
{
    COMPLETE("Complete"), FILESYSTEM("File System"), SNAPSHOT("Snapshot");

    private final static Logger logger = LoggerFactory.getLogger(VMBackupType.class);

    private static Map<String, Method> getConfigMethods = new HashMap<>();
    static
    {
        try
        {
            getConfigMethods.put("Complete", VirtualMachineEvent.class.getMethod(
                "getCompleteConfiguration", (Class< ? >[]) null));
            getConfigMethods.put("File System", VirtualMachineEvent.class.getMethod(
                "getCompleteConfiguration", (Class< ? >[]) null));
            getConfigMethods.put("Snapshot", VirtualMachineEvent.class.getMethod(
                "getCompleteConfiguration", (Class< ? >[]) null));
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            logger.error("Unexpected error resolving event methods", e);
        }
    }

    private String displaytext;

    VMBackupType(final String displaytext)
    {
        this.displaytext = displaytext;
    }

    String getDisplayText()
    {
        return displaytext;
    }

    @SuppressWarnings("unchecked")
    public Optional<BackupEventConfiguration> getConfigData(final VirtualMachineEvent event)
    {
        Method method = getConfigMethods.get(displaytext);
        try
        {
            return (Optional<BackupEventConfiguration>) method.invoke(event, (Object[]) null);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            logger.error("Unexpected error executing event method", e);
        }
        return Optional.absent();
    }
}

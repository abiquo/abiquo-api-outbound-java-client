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

import java.text.ParseException;
import java.util.Map;

import com.abiquo.bond.api.abqapi.VMMetadata;
import com.abiquo.event.model.Event;
import com.abiquo.server.core.cloud.MetadataDto;
import com.abiquo.server.core.cloud.VirtualMachineDto;
import com.abiquo.server.core.event.EventDto;
import com.google.common.base.Optional;

/**
 * Class for representing backup events received from the M server
 */
public class BackupVMEvent extends VirtualMachineEvent
{
    private BackupEventConfiguration bcComplete;

    private BackupEventConfiguration bcSnapshot;

    private BackupEventConfiguration bcFileSystem;

    /**
     * Extracts the name of the machine and the backup configuration data from the supplied
     * VirtualMachineDto instance
     * 
     * @param event the original event received from the M server. A reference to this is kept in
     *            case the plugin requires and extra data
     * @param vmdetails Details of the virtual machine requesting a backup
     * @throws ParseException
     */
    public BackupVMEvent(final Event event, final VirtualMachineDto vmdetails)
    {
        super(event);

        if (vmname == null)
        {
            vmname = vmdetails.getName();
        }
        Map<String, Object> metadata = vmdetails.getMetadata();
        extractBackupData(metadata);
    }

    public BackupVMEvent(final EventDto event, final MetadataDto vmdetails)
    {
        super(event);
        if (vmname == null)
        {
            vmname = event.getVirtualMachine();
        }
        Map<String, Object> metadata = vmdetails.getMetadata();
        extractBackupData(metadata);
    }

    @SuppressWarnings("unchecked")
    private void extractBackupData(final Map<String, Object> metadata)
    {
        Map<String, Object> submetadata = (Map<String, Object>) metadata.get(VMMetadata.METADATA);
        if (submetadata != null)
        {
            Map<String, Object> backupschedule =
                (Map<String, Object>) submetadata.get(VMMetadata.BACKUP);
            if (backupschedule != null)
            {
                Map<String, Object> configdata =
                    (Map<String, Object>) backupschedule.get(VMMetadata.COMPLETE);
                if (configdata != null)
                {
                    bcComplete = new BackupEventConfiguration(configdata);
                }
                configdata = (Map<String, Object>) backupschedule.get(VMMetadata.FILESYSTEM);
                if (configdata != null)
                {
                    bcSnapshot = new BackupEventConfiguration(configdata);
                }
                configdata = (Map<String, Object>) backupschedule.get(VMMetadata.SNAPSHOT);
                if (configdata != null)
                {
                    bcFileSystem = new BackupEventConfiguration(configdata);
                }
            }
        }
    }

    public Optional<BackupEventConfiguration> getCompleteConfiguration()
    {
        return Optional.fromNullable(bcComplete);
    }

    public Optional<BackupEventConfiguration> getSnapshotConfiguration()
    {
        return Optional.fromNullable(bcSnapshot);
    }

    public Optional<BackupEventConfiguration> getFileSystemConfiguration()
    {
        return Optional.fromNullable(bcFileSystem);
    }
}

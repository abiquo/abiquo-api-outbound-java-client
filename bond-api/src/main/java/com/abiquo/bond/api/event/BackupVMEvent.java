/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.event;

import java.text.ParseException;
import java.util.Map;

import com.abiquo.api.services.cloud.VirtualMachineMetadataService;
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
        Map<String, Object> submetadata =
            (Map<String, Object>) metadata.get(VirtualMachineMetadataService.METADATA);
        if (submetadata != null)
        {
            Map<String, Object> backupschedule =
                (Map<String, Object>) submetadata.get(VirtualMachineMetadataService.BACKUP);
            if (backupschedule != null)
            {
                Map<String, Object> configdata =
                    (Map<String, Object>) backupschedule
                        .get(VirtualMachineMetadataService.COMPLETE);
                if (configdata != null)
                {
                    bcComplete = new BackupEventConfiguration(configdata);
                }
                configdata =
                    (Map<String, Object>) backupschedule
                        .get(VirtualMachineMetadataService.FILESYSTEM);
                if (configdata != null)
                {
                    bcSnapshot = new BackupEventConfiguration(configdata);
                }
                configdata =
                    (Map<String, Object>) backupschedule
                        .get(VirtualMachineMetadataService.SNAPSHOT);
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

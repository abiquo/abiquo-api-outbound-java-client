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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.abiquo.bond.api.abqapi.VMMetadata;
import com.abiquo.event.model.Event;
import com.abiquo.event.model.details.EventDetails;
import com.abiquo.server.core.cloud.MetadataDto;
import com.abiquo.server.core.cloud.VirtualMachineDto;
import com.abiquo.server.core.event.EventDto;
import com.google.common.base.Optional;

/**
 * Generic class for handling any virtual machine related events received from the 'M' server.
 */
public class VirtualMachineEvent extends APIEvent
{
    protected String vmname;

    protected String hypervisorname;

    protected String hypervisorip;

    protected String hypervisortype;

    private BackupEventConfiguration bcComplete;

    private BackupEventConfiguration bcSnapshot;

    private BackupEventConfiguration bcFileSystem;

    private boolean backupIsConfigured = false;

    private Map<VMBackupType, EnumSet<VMBackupConfiguration>> reqCfgs;

    /**
     * Extracts the name of the machine and the backup configuration data from the supplied
     * VirtualMachineDto instance
     * 
     * @param event the original event received from the M server. A reference to this is kept in
     *            case the plugin requires and extra data
     * @param vmdetails Details of the virtual machine
     * @param vmdetails Details of the virtual machine requesting a backup
     */
    public VirtualMachineEvent(final Event event, final VirtualMachineDto vmdetails)
    {
        this(event);
        if (vmdetails != null)
        {
            if (vmname == null)
            {
                vmname = vmdetails.getName();
            }
            Map<String, Object> metadata = vmdetails.getMetadata();
            extractBackupData(metadata);
        }
    }

    public VirtualMachineEvent(final Event event)
    {
        super(event);
        Map<String, String> details = getEventDetails(event);
        vmname = details.get("VIRTUAL_MACHINE_NAME");
        hypervisorname = details.get("MACHINE_NAME");
        hypervisorip = details.get("HYPERVISOR_IP");
        hypervisortype = details.get("HYPERVISOR_TYPE");
    }

    public VirtualMachineEvent(final EventDto event, final Optional<MetadataDto> optMetaData)
    {
        this(event);
        MetadataDto vmdetails = optMetaData.orNull();
        if (vmdetails != null)
        {
            Map<String, Object> metadata = vmdetails.getMetadata();
            extractBackupData(metadata);
        }
    }

    public VirtualMachineEvent(final EventDto event)
    {
        super(event);
        vmname = event.getVirtualMachine();
        hypervisorname = event.getPhysicalMachine();
    }

    public String getVMName()
    {
        return vmname;
    }

    public List<String> getHypervisorNames()
    {
        List<String> hypervisors = new ArrayList<>();
        if (hypervisorname != null)
        {
            hypervisors.add(hypervisorname);
        }
        if (hypervisorip != null)
        {
            hypervisors.add(hypervisorip);
        }
        return hypervisors;
    }

    private Map<String, String> getEventDetails(final Event event)
    {
        Optional< ? extends EventDetails> optVMDetails = event.getDetails();
        if (optVMDetails.isPresent())
        {
            EventDetails details = optVMDetails.get();
            return details.getTransportMap();
        }
        return new HashMap<String, String>();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" vm:").append(vmname);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void extractBackupData(final Map<String, Object> metadata)
    {
        if (metadata != null)
        {
            Map<String, Object> submetadata =
                (Map<String, Object>) metadata.get(VMMetadata.METADATA);
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

                    backupIsConfigured = true;

                    reqCfgs = new HashMap<>();
                    if (bcComplete != null)
                    {
                        reqCfgs.put(VMBackupType.COMPLETE, getEnabledConfigurations(bcComplete));
                    }
                    if (bcSnapshot != null)
                    {
                        reqCfgs.put(VMBackupType.SNAPSHOT, getEnabledConfigurations(bcSnapshot));
                    }
                    if (bcFileSystem != null)
                    {
                        reqCfgs
                            .put(VMBackupType.FILESYSTEM, getEnabledConfigurations(bcFileSystem));
                    }
                }
            }
        }
    }

    public boolean backupIsConfigured()
    {
        return backupIsConfigured(EnumSet.allOf(VMBackupType.class),
            EnumSet.allOf(VMBackupConfiguration.class));
    }

    public boolean backupIsConfigured(final EnumSet<VMBackupType> acceptableTypes,
        final EnumSet<VMBackupConfiguration> acceptableConfigurations)
    {
        if (backupIsConfigured)
        {
            for (VMBackupType bt : acceptableTypes)
            {
                switch (bt)
                {
                    case COMPLETE:
                        if (bcComplete.isConfigured(acceptableConfigurations))
                        {
                            return true;
                        }
                        break;
                    case FILESYSTEM:
                        if (bcFileSystem.isConfigured(acceptableConfigurations))
                        {
                            return true;
                        }
                        break;
                    case SNAPSHOT:
                        if (bcSnapshot.isConfigured(acceptableConfigurations))
                        {
                            return true;
                        }
                        break;
                }
            }
        }
        return false;
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

    private EnumSet<VMBackupConfiguration> getEnabledConfigurations(
        final BackupEventConfiguration cfg)
    {
        EnumSet<VMBackupConfiguration> set = EnumSet.noneOf(VMBackupConfiguration.class);
        if (cfg.getDefinedHourDateAndTime().isPresent())
        {
            set.add(VMBackupConfiguration.DEFINED_HOUR);
        }
        if (cfg.getHourlyHour().isPresent())
        {
            set.add(VMBackupConfiguration.HOURLY);
        }
        if (cfg.getDailyTime().isPresent())
        {
            set.add(VMBackupConfiguration.DAILY);
        }
        if (cfg.getWeeklyTime().isPresent())
        {
            set.add(VMBackupConfiguration.WEEKLY);
        }
        if (cfg.getMonthlyTime().isPresent())
        {
            set.add(VMBackupConfiguration.MONTHLY);
        }
        return set;
    }

    public Map<VMBackupType, EnumSet<VMBackupConfiguration>> getRequiredConfigurations()
    {
        return reqCfgs;
    }
}

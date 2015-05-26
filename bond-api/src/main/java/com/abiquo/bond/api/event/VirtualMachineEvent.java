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

import static java.lang.String.format;
import static java.lang.String.valueOf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(VirtualMachineEvent.class);

    protected String vmname;

    protected String hypervisorname;

    protected String hypervisorip;

    protected String hypervisortype;

    protected String backupStatus;

    protected String restoreStatus;

    protected String backupDate;

    protected String resultId;

    protected String state;

    private BackupEventConfiguration bcComplete;

    private boolean backupIsConfigured = false;

    private EnumSet<VMBackupConfiguration> reqCfgs;

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
        Map<String, Object> details = getEventDetails(event);
        if (!details.isEmpty())
        {
            if ("RESTORE_BACKUP".equals(event.getAction()))
            {
                backupDate = valueOf(details.get("BACKUP_DATE"));
                resultId = valueOf(details.get("BACKUP_ID"));
            }
            else if ("VSM_CHANGE_STATE".equals(event.getAction()))
            {
                vmname = valueOf(details.get("VIRTUAL_MACHINE_NAME"));
                state = valueOf(details.get("VIRTUAL_MACHINE_STATE"));
            }
            else
            {
                vmname = valueOf(details.get("VIRTUAL_MACHINE_NAME"));
                hypervisorname = valueOf(details.get("MACHINE_NAME"));
                hypervisorip = valueOf(details.get("HYPERVISOR_IP"));
                hypervisortype = valueOf(details.get("HYPERVISOR_TYPE"));
            }
        }
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

    public String getBackupDate()
    {
        return backupDate;
    }

    public void setBackupDate(final String backupDate)
    {
        this.backupDate = backupDate;
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

    private Map<String, Object> getEventDetails(final Event event)
    {
        Optional< ? extends EventDetails> optVMDetails = event.getDetails();
        if (optVMDetails.isPresent())
        {
            EventDetails details = optVMDetails.get();
            return details.getTransportMap();
        }
        return new HashMap<String, Object>();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" for vm:").append(vmname);
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
                        logger.debug("Complete backup configuration found for vm {}: {}",
                            this.vmname, configdata);
                        bcComplete = new BackupEventConfiguration(configdata);
                        backupIsConfigured = true;
                        reqCfgs = getEnabledConfigurations(bcComplete);
                    }

                    List<String> otherConfigs =
                        backupschedule.keySet().stream()
                            .filter(k -> !"complete".equalsIgnoreCase(k))
                            .filter(k -> backupschedule.get(k) != null)
                            .map(k -> format("%s : %s", k, backupschedule.get(k).toString()))
                            .collect(Collectors.toList());
                    if (!otherConfigs.isEmpty())
                    {
                        logger.warn("The are no compatible backup configurations for vm {}: {}",
                            this.vmname, otherConfigs);
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
                        if (bcComplete != null && bcComplete.isConfigured(acceptableConfigurations))
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

    public EnumSet<VMBackupConfiguration> getRequiredConfigurations()
    {
        return reqCfgs;
    }
}

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

import com.google.common.base.Optional;

public enum VMBackupConfiguration
{
    DEFINED_HOUR()
    {
        @Override
        String formatDisplayText(final BackupEventConfiguration cfg)
        {
            return "Defined Hour (" + cfg.getDefinedHourDateAndTimeAsText() + ")";
        }
    },
    HOURLY()
    {
        @Override
        String formatDisplayText(final BackupEventConfiguration cfg)
        {
            return "Hourly (" + cfg.getHourlyHourAsText() + " hours)";
        }
    },
    DAILY()
    {
        @Override
        String formatDisplayText(final BackupEventConfiguration cfg)
        {
            return "Daily (" + cfg.getDailyTimeAsText() + ")";
        }
    },
    WEEKLY()
    {
        @Override
        String formatDisplayText(final BackupEventConfiguration cfg)
        {
            return "Weekly (" + cfg.getWeeklyTimeAsText() + " " + cfg.getWeeklyDaysAsText() + ")";
        }
    },
    MONTHLY()
    {
        @Override
        String formatDisplayText(final BackupEventConfiguration cfg)
        {
            return "Monthly (" + cfg.getMonthlyTimeAsText() + ")";
        }
    };

    public String getDisplayText(final VMBackupType type, final VirtualMachineEvent event)
    {
        BackupEventConfiguration cfg;
        Optional<BackupEventConfiguration> optcfg = Optional.absent();
        switch (type)
        {
            case COMPLETE:
                optcfg = event.getCompleteConfiguration();
                break;
            case FILESYSTEM:
                optcfg = event.getCompleteConfiguration();
                break;
            case SNAPSHOT:
                optcfg = event.getCompleteConfiguration();
                break;
        }
        if (optcfg.isPresent())
        {
            cfg = optcfg.get();
            return formatDisplayText(cfg);
        }
        return "";
    }

    abstract String formatDisplayText(BackupEventConfiguration cfg);
}

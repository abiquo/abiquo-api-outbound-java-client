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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.abqapi.VMMetadata;
import com.google.common.base.Optional;

/**
 * Class that converts the Abiquo backup configuration for a virtual machined into a generic form
 * that can be used to update whatever Backup System the customer is using.
 */
public class BackupEventConfiguration
{
    private final static Logger logger = LoggerFactory.getLogger(BackupEventConfiguration.class);

    private static DateFormat df = new SimpleDateFormat(VMMetadata.DATE_FORMAT);

    private static DateFormat tf = new SimpleDateFormat(VMMetadata.TIME_FORMAT);

    private BackupDetailsDate definedhour;

    private BackupDetailsWeek weekly_planned;

    private BackupDetailsHour hourly;

    private BackupDetailsTime monthly;

    private BackupDetailsTime daily;

    @SuppressWarnings("unchecked")
    public BackupEventConfiguration(final Map<String, Object> configdata)
    {
        Map<String, Object> settings =
            (Map<String, Object>) configdata.get(VMMetadata.DEFINED_HOUR);
        if (settings != null)
        {
            try
            {
                definedhour = new BackupDetailsDate(settings);
            }
            catch (ParseException e)
            {
                logger.error("Invalid DEFINED_HOUR value", e);
            }
        }

        settings = (Map<String, Object>) configdata.get(VMMetadata.WEEKLY);
        if (settings != null)
        {
            try
            {
                weekly_planned = new BackupDetailsWeek(settings);
            }
            catch (ParseException e)
            {
                logger.error("Invalid WEEKLY value", e);
            }
        }

        settings = (Map<String, Object>) configdata.get(VMMetadata.HOURLY);
        if (settings != null)
        {
            try
            {
                hourly = new BackupDetailsHour(settings);
            }
            catch (NumberFormatException e)
            {
                logger.error("Invalid HOURLY value", e);
            }
        }

        settings = (Map<String, Object>) configdata.get(VMMetadata.MONTHLY);
        if (settings != null)
        {
            try
            {
                monthly = new BackupDetailsTime(settings);
            }
            catch (ParseException e)
            {
                logger.error("Invalid monthly value", e);
            }
        }

        settings = (Map<String, Object>) configdata.get(VMMetadata.DAILY);
        if (settings != null)
        {
            try
            {
                daily = new BackupDetailsTime(settings);
            }
            catch (ParseException e)
            {
                logger.error("Invalid daily value", e);
            }
        }
    }

    public boolean isConfigured()
    {
        return hourly != null || daily != null || weekly_planned != null || monthly != null
            || definedhour != null;
    }

    public Optional<Date> getDefinedHourDateAndTime()
    {
        if (definedhour != null)
        {
            return Optional.of(definedhour.getDateAndTime());
        }
        return Optional.absent();
    }

    public Optional<Integer> getHourlyHour()
    {
        if (hourly != null)
        {
            return Optional.of(hourly.getHour());
        }
        return Optional.absent();
    }

    public Optional<Date> getDailyTime()
    {
        if (daily != null)
        {
            return Optional.of(daily.getTime());
        }
        return Optional.absent();
    }

    public Optional<Date> getWeeklyTime()
    {
        if (weekly_planned != null)
        {
            return Optional.of(weekly_planned.getTime());
        }
        return Optional.absent();
    }

    public Optional<Date> getMonthlyTime()
    {
        if (monthly != null)
        {
            return Optional.of(monthly.getTime());
        }
        return Optional.absent();
    }

    public Optional<EnumSet<WEEKDAYS>> getWeeklyDays()
    {
        if (weekly_planned != null)
        {
            return Optional.of(weekly_planned.getDays());
        }
        return Optional.absent();
    }

    public abstract class BackupDetails
    {
        private List<BackupDisk> disks = new ArrayList<>();

        BackupDetails(final Map<String, Object> settings)
        {
            @SuppressWarnings("unchecked")
            List<String> disklist = (List<String>) settings.get(VMMetadata.DISKS);
            if (disklist != null)
            {
                for (String disk : disklist)
                {
                    disks.add(new BackupDisk(Integer.parseInt(disk)));
                }
            }
        }
    }

    public class BackupDetailsHour extends BackupDetails
    {
        int hour;

        BackupDetailsHour(final Map<String, Object> settings) throws NumberFormatException
        {
            super(settings);
            String hoursetting = (String) settings.get(VMMetadata.TIME);
            hour = Integer.parseInt(hoursetting);
        }

        public int getHour()
        {
            return hour;
        }
    }

    public class BackupDetailsTime extends BackupDetails
    {
        Date time;

        BackupDetailsTime(final Map<String, Object> settings) throws ParseException
        {
            super(settings);
            String timesetting = (String) settings.get(VMMetadata.TIME);
            time = tf.parse(timesetting);
        }

        public Date getTime()
        {
            return time;
        }
    }

    public class BackupDetailsDate extends BackupDetails
    {
        Date dateandtime;

        BackupDetailsDate(final Map<String, Object> settings) throws ParseException
        {
            super(settings);
            String datesetting = (String) settings.get(VMMetadata.DATE);
            dateandtime = df.parse(datesetting);
        }

        public Date getDateAndTime()
        {
            return dateandtime;
        }
    }

    public class BackupDetailsWeek extends BackupDetailsTime
    {
        private EnumSet<WEEKDAYS> days = EnumSet.noneOf(WEEKDAYS.class);

        BackupDetailsWeek(final Map<String, Object> settings) throws ParseException
        {
            super(settings);
            Set<String> keyset = settings.keySet();
            Set<String> lckeyset = new HashSet<>();
            for (String key : keyset)
            {
                lckeyset.add(key.toLowerCase());
            }
            EnumSet<WEEKDAYS> allweek = EnumSet.allOf(WEEKDAYS.class);
            for (WEEKDAYS day : allweek)
            {
                if (lckeyset.contains(day.getKey()))
                {
                    days.add(day);
                }
            }
        }

        public EnumSet<WEEKDAYS> getDays()
        {
            return days;
        }
    }

    public class BackupDisk
    {
        private int id;

        public BackupDisk(final int id)
        {
            this.id = id;
        }
    }

    public enum WEEKDAYS
    {
        MONDAY(VMMetadata.MONDAY), TUESDAY(VMMetadata.TUESDAY), WEDNESDAY(VMMetadata.WEDNESDAY), THURSDAY(
            VMMetadata.THURSDAY), FRIDAY(VMMetadata.FRIDAY), SATURDAY(VMMetadata.SATURDAY), SUNDAY(
            VMMetadata.SUNDAY);

        private String key;

        WEEKDAYS(final String key)
        {
            this.key = key.toLowerCase();
        }

        public String getKey()
        {
            return key;
        }
    }
}

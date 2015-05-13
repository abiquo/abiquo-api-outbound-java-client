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
package com.abiquo.bond.api.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.APIConnection;
import com.abiquo.bond.api.NameToVMLinks;
import com.abiquo.bond.api.abqapi.VMMetadata;
import com.abiquo.bond.api.plugin.BackupPluginInterface;
import com.abiquo.bond.api.plugin.PluginException;
import com.abiquo.model.rest.RESTLink;
import com.abiquo.server.core.cloud.MetadataDto;
import com.google.common.base.Optional;

/**
 * Although the client is intended to handle outbound api events and pass them on to third party
 * applications via the plugins, there may be times when it is useful to retrieve data from the
 * third party application and return it to Abiquo (for example, the results of backup operations).
 * This class handles the fetching of data from the plugin and the updating of the Abiquo system
 * with this data. At the moment it only supports the updating of backup results. Other handlers can
 * be added by modifing the run method to handle the returned data.
 */
public class ResponsesHandler extends APIConnection implements Runnable
{
    private final static Logger logger = LoggerFactory.getLogger(ResponsesHandler.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private List<ScheduledFuture< ? >> resultsfetchers = new ArrayList<>();

    private NameToVMLinks mapNameToVMLinks;

    private LinkedBlockingQueue<VMBackupStatusList> resultqueue = new LinkedBlockingQueue<>();

    private LinkedBlockingQueue<VMRestoreStatusList> restorequeue = new LinkedBlockingQueue<>();

    private long timeperiod;

    private TimeUnit timeunit;

    private Thread updateserver;

    public ResponsesHandler(final String server, final String user, final String password,
        final NameToVMLinks mapNameToVMLinks, final long timeperiod, final TimeUnit timeunit)
    {
        super(server, user, password);
        this.mapNameToVMLinks = mapNameToVMLinks;
        this.timeperiod = timeperiod;
        this.timeunit = timeunit;
        updateserver = new Thread(this, "ABQ_RESPONSE_HANDLER");
        updateserver.start();
        logger.debug("Handler started");
    }

    public void addBackupPlugin(final BackupPluginInterface plugin)
    {
        try
        {
            BackupResultsHandler handler = plugin.getResultsHandler();
            handler.setQueue(resultqueue);
            handler.setRestoreQueue(restorequeue);
            handler.linkToVMCache(mapNameToVMLinks.getVMNames());
            resultsfetchers.add(scheduler.scheduleAtFixedRate(handler, 0, timeperiod, timeunit));
            logger.debug("Backup results handler {} running at {} {} intervals", new Object[] {
            handler.getClass(), timeperiod, timeunit.toString().toLowerCase()});
        }
        catch (PluginException e)
        {
            wrapperNotifications.notification("Failed to start backup results handler "
                + plugin.getClass().getName(), e);
        }
    }

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                List<VMRestoreStatus> restoreStatuses = new ArrayList<>();
                VMBackupStatusList event = resultqueue.take();
                List<VMBackupStatus> backupStatuses = event.getStatuses();
                String vmName = event.getVMName();
                logger.debug("Backup result took for vm {}", vmName);
                if (!restorequeue.isEmpty())
                {
                    VMRestoreStatusList restoreEvent = restorequeue.take();
                    restoreStatuses = restoreEvent.getStatuses();
                    logger.debug("Restore result took for vm {}", restoreEvent.getVMName());
                }

                Optional<RESTLink> optlink =
                    mapNameToVMLinks.getLink(vmName, NameToVMLinks.VM_LINK_METADATA);

                if (optlink.isPresent())
                {
                    RESTLink link = optlink.get();
                    logger.debug("Results from backup of virtual machine {}", vmName);
                    WebTarget targetMetaData = client.target(link.getHref());
                    Invocation.Builder invocationBuilderMeta =
                        targetMetaData.request(MetadataDto.MEDIA_TYPE);
                    Response responseMeta = invocationBuilderMeta.get();
                    int statusMeta = responseMeta.getStatus();
                    if (statusMeta == 200)
                    {
                        MetadataDto resourceObjectMeta = responseMeta.readEntity(MetadataDto.class);
                        Map<String, Object> metadata = resourceObjectMeta.getMetadata();

                        if (metadata != null)
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> mapMetadata =
                                (Map<String, Object>) metadata.get(VMMetadata.METADATA);
                            if (mapMetadata == null)
                            {
                                mapMetadata = new HashMap<>();
                                resourceObjectMeta.setMetadata(mapMetadata);
                            }

                            List<Map<String, Object>> resultslist = new ArrayList<>();
                            for (VMBackupStatus status : backupStatuses)
                            {

                                if (restoreStatuses.isEmpty())
                                {
                                    resultslist.add(status.getMetaData());
                                }
                                else
                                {
                                    Map<String, Object> resultsMap = status.getMetaData();

                                    for (VMRestoreStatus restoreStatus : restoreStatuses)
                                    {
                                        if (status.getVmRestorePoint().equals(
                                            restoreStatus.getVmRestorePoint()))
                                        {
                                            resultsMap.put(VMMetadata.RESTORE, "requested");
                                            restoreStatus.setName(status.getMetaData()
                                                .get(VMMetadata.NAME).toString());
                                            restoreStatus.setSize((long) status.getMetaData().get(
                                                VMMetadata.SIZE));
                                            resultsMap.put("restoreInfo",
                                                restoreStatus.getMetaData());
                                        }
                                    }
                                    resultslist.add(resultsMap);
                                }
                            }

                            Map<String, Object> backupResults = new HashMap<>();
                            backupResults.put(VMMetadata.RESULTS, resultslist);

                            mapMetadata.put(VMMetadata.LAST_BACKUPS, backupResults);

                            WebTarget targetUpdate = client.target(link.getHref());
                            Invocation.Builder invocationBuilder =
                                targetUpdate.request(MetadataDto.SHORT_MEDIA_TYPE_JSON);
                            Response response =
                                invocationBuilder.put(Entity.entity(resourceObjectMeta,
                                    MetadataDto.SHORT_MEDIA_TYPE_JSON));
                            int status = response.getStatus();
                            if (status == 200)
                            {
                                logger
                                    .debug("Backup status for vm {} updated successfully", vmName);
                            }
                            else
                            {
                                logger.error(
                                    "Error occurred while updating the metadata of vm {}: {}",
                                    vmName, response.getStatusInfo().getReasonPhrase());
                            }
                        }
                        else
                        {
                            logger.error("Failed to update backup status for vm {}", vmName);
                            wrapperNotifications.notification("Failed to update backup status of "
                                + vmName, link.getHref(), statusMeta);
                        }
                    }
                    else
                    {
                        logger.error("Failed to retrieve current metadata for vm {}", vmName);
                        wrapperNotifications.notification("Failed to retrieve current metadata",
                            link.getHref(), statusMeta);
                    }
                }
                else
                {
                    logger.error("No metadata link found for vm {} in handler response", vmName);
                }
            }
            catch (Throwable t)
            {
                logger.error("Response handler error.", t);
            }
        }
    }
}

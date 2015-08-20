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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
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
import com.abiquo.model.transport.error.ErrorsDto;
import com.abiquo.server.core.cloud.MetadataDto;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

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

    private LinkedBlockingQueue<VMBackupRestorePairStatusList> resultQueue =
        new LinkedBlockingQueue<>();

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
            handler.setQueue(resultQueue);
            handler.linkToVMCache(mapNameToVMLinks.getVMNames());
            resultsfetchers.add(scheduler.scheduleAtFixedRate(handler, 0, timeperiod, timeunit));
            logger.debug("Backup results handler {} running at {} {} intervals",
                new Object[] {handler.getClass(), timeperiod, timeunit.toString().toLowerCase()});
        }
        catch (PluginException e)
        {
            wrapperNotifications.notification(
                "Failed to start backup results handler " + plugin.getClass().getName(), e);
        }
    }

    private Response getMetadataResponse(final RESTLink link)
    {
        WebTarget targetMetaData = client.target(link.getHref());
        Invocation.Builder invocationBuilderMeta = targetMetaData.request(MetadataDto.MEDIA_TYPE);
        return invocationBuilderMeta.get();
    }

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                VMBackupRestorePairStatusList event = resultQueue.take();
                String vmName = event.getVmName();
                List<VMBackupRestorePairStatus> pairStatusList = event.getPairStatusesList();
                Optional<RESTLink> optlink =
                    mapNameToVMLinks.getLink(vmName, NameToVMLinks.VM_LINK_METADATA);

                if (optlink.isPresent())
                {
                    RESTLink link = optlink.get();
                    Response responseMeta = getMetadataResponse(link);
                    int statusMeta = responseMeta.getStatus();
                    if (statusMeta == 200)
                    {
                        MetadataDto resourceObjectMeta = responseMeta.readEntity(MetadataDto.class);
                        Map<String, Object> metadata = resourceObjectMeta.getMetadata();

                        if (metadata != null)
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> originalMetadata =
                                (Map<String, Object>) metadata.get(VMMetadata.METADATA);
                            if (originalMetadata == null)
                            {
                                originalMetadata = new HashMap<>();
                            }

                            List<Map<String, Object>> resultslist = new ArrayList<>();

                            for (VMBackupRestorePairStatus pairStatus : pairStatusList)
                            {

                                Map<String, Object> backupStatusMetadata =
                                    pairStatus.backupStatus.getMetaData();

                                if (pairStatus.restoreStatuses.isEmpty())
                                {
                                    resultslist.add(backupStatusMetadata);
                                }
                                else
                                {
                                    for (VMRestoreStatus restoreStatus : pairStatus.restoreStatuses)
                                    // FIXME should be in a list, but what happens with UI?
                                    {
                                        backupStatusMetadata.put(VMMetadata.RESTORE, "requested");
                                        restoreStatus.setName(
                                            backupStatusMetadata.get(VMMetadata.NAME).toString());
                                        restoreStatus.setSize(
                                            (long) backupStatusMetadata.get(VMMetadata.SIZE));
                                        backupStatusMetadata.put("restoreInfo",
                                            restoreStatus.getMetaData());
                                    }
                                    resultslist.add(backupStatusMetadata);
                                }
                            }

                            Map<String, Object> resultsMetadata = Maps.newHashMap(originalMetadata);
                            Map<String, Object> backupResults = new HashMap<>();
                            backupResults.put(VMMetadata.RESULTS, resultslist);
                            resultsMetadata.put(VMMetadata.LAST_BACKUPS, backupResults);

                            if (!equalsMetadataString(originalMetadata, resultsMetadata))
                            {
                                Map<String, Object> withMetadataTag = new HashMap<>();
                                withMetadataTag.put(VMMetadata.METADATA, resultsMetadata);
                                resourceObjectMeta.setMetadata(withMetadataTag);

                                WebTarget targetUpdate = client.target(link.getHref());
                                Invocation.Builder invocationBuilder =
                                    targetUpdate.request(MetadataDto.SHORT_MEDIA_TYPE_JSON);
                                Response response = invocationBuilder.put(Entity
                                    .entity(resourceObjectMeta, MetadataDto.SHORT_MEDIA_TYPE_JSON));
                                int status = response.getStatus();
                                if (status == 200)
                                {
                                    logger.debug(
                                        "Backup/restore results status for vm {} updated successfully",
                                        vmName);
                                }
                                else
                                {
                                    String messageCause;
                                    try
                                    {
                                        messageCause = response.readEntity(ErrorsDto.class)
                                            .getCollection().stream()
                                            .map(error -> format("%s-%s", error.getCode(),
                                                error.getMessage()))
                                            .collect(Collectors.joining(","));
                                    }
                                    catch (ProcessingException | IllegalStateException ex)
                                    {
                                        messageCause = "Cannot deserialize error";
                                    }
                                    logger
                                        .error(
                                            "Error occurred while updating the metadata of vm {}: {} {}",
                                            new Object[] {vmName,
                                            response.getStatusInfo().getReasonPhrase(),
                                            messageCause});
                                }
                            }
                            else
                            {
                                logger.debug(
                                    "No changes from backup/restore of virtual machine {} detected. So no update is performed",
                                    vmName);
                            }
                        }
                        // TODO add it even if metadata is null
                        else if (!pairStatusList.isEmpty())
                        {
                            logger.error(
                                "Failed to update backup status for vm {} because original metadata is null",
                                vmName);
                            wrapperNotifications.notification(format(
                                "Failed to update backup status of %s because original metadata is null",
                                vmName), link.getHref(), statusMeta);
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

    private static boolean equalsMetadataString(final Map<String, Object> a,
        final Map<String, Object> b)
    {
        return Objects.equals(new TreeMap<>(a).toString(), new TreeMap<>(b).toString());
    }
}

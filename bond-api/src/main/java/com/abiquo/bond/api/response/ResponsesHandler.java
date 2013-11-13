/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
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

import com.abiquo.api.services.cloud.VirtualMachineMetadataService;
import com.abiquo.bond.api.APIConnection;
import com.abiquo.bond.api.NameToVMLinks;
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
 * 
 */
public class ResponsesHandler extends APIConnection implements Runnable
{
    final Logger logger = LoggerFactory.getLogger(ResponsesHandler.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private List<ScheduledFuture< ? >> resultsfetchers = new ArrayList<>();

    private NameToVMLinks mapNameToVMLinks;

    private LinkedBlockingQueue<VMBackupStatusList> resultqueue = new LinkedBlockingQueue<>();

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
                VMBackupStatusList event = resultqueue.take();

                Optional<RESTLink> optlink =
                    mapNameToVMLinks.getLink(event.getVMName(), NameToVMLinks.VM_LINK_METADATA);

                if (optlink.isPresent())
                {
                    RESTLink link = optlink.get();
                    logger.debug("Results from backup of virtual machine {}", event.getVMName());
                    WebTarget targetMetaData = client.target(link.getHref());
                    Invocation.Builder invocationBuilderMeta =
                        targetMetaData.request(MetadataDto.MEDIA_TYPE);
                    Response responseMeta = invocationBuilderMeta.get();
                    int statusMeta = responseMeta.getStatus();
                    if (statusMeta == 200)
                    {
                        MetadataDto resourceObjectMeta = responseMeta.readEntity(MetadataDto.class);
                        Map<String, Object> metadata = resourceObjectMeta.getMetadata();

                        @SuppressWarnings("unchecked")
                        Map<String, Object> mapMetadata =
                            (Map<String, Object>) metadata
                                .get(VirtualMachineMetadataService.METADATA);
                        if (mapMetadata == null)
                        {
                            mapMetadata = new HashMap<>();
                            resourceObjectMeta.setMetadata(mapMetadata);
                        }

                        List<Map<String, Object>> resultslist = new ArrayList<>();
                        for (VMBackupStatus status : event.getStatuses())
                        {
                            resultslist.add(status.getMetaData());
                        }

                        Map<String, Object> backupResults = new HashMap<>();
                        backupResults.put(VirtualMachineMetadataService.RESULTS, resultslist);

                        mapMetadata.put(VirtualMachineMetadataService.LAST_BACKUPS, backupResults);

                        WebTarget targetUpdate = client.target(link.getHref());
                        Invocation.Builder invocationBuilder =
                            targetUpdate.request(MetadataDto.BASE_MEDIA_TYPE);
                        Response response =
                            invocationBuilder.put(Entity.entity(resourceObjectMeta,
                                MetadataDto.BASE_MEDIA_TYPE));
                        int status = response.getStatus();
                        if (status == 200)
                        {
                            logger.debug("Backup status for vm {} updated successfully",
                                event.getVMName());
                        }
                        else
                        {
                            logger.error("Failed to update backup status for vm {}",
                                event.getVMName());
                            wrapperNotifications.notification("Failed to update backup status of "
                                + event.getVMName(), link.getHref(), statusMeta);
                        }
                    }
                    else
                    {
                        logger.error("Failed to retrieve current metadata for vm {}",
                            event.getVMName());
                        wrapperNotifications.notification("Failed to retrieve current metadata",
                            link.getHref(), statusMeta);
                    }
                }
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

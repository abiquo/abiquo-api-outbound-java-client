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
package com.abiquo.bond.api;

import static com.abiquo.event.model.enumerations.EntityAction.VIRTUAL_MACHINE.CREATE;
import static com.abiquo.event.model.enumerations.EntityAction.VIRTUAL_MACHINE.DEPLOY_FINISH;
import static com.abiquo.event.model.enumerations.EntityAction.VIRTUAL_MACHINE.METADATA_MODIFIED;
import static com.abiquo.event.model.enumerations.EntityAction.VIRTUAL_MACHINE.UNDEPLOY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.bond.api.event.APIEvent;
import com.abiquo.bond.api.event.BackupVMEvent;
import com.abiquo.bond.api.event.DeployVMEvent;
import com.abiquo.bond.api.event.RestoreVMEvent;
import com.abiquo.bond.api.event.UndeployVMEvent;
import com.abiquo.bond.api.event.VirtualMachineEvent;
import com.abiquo.event.model.Event;
import com.abiquo.event.model.details.EventDetails;
import com.abiquo.model.rest.RESTLink;
import com.abiquo.server.core.cloud.VirtualMachineDto;
import com.abiquo.server.core.cloud.VirtualMachineState;
import com.google.common.base.Optional;

/**
 * This class is responsible for converting the messages received from the M server into instances
 * of classes that can be passed to the plugins for processing. As most of the messages consist of
 * REST part URLs, much of the processing consists of making REST calls to the Abiquo API to fetch
 * the required data.
 * <p>
 * At the moment all events are handled in the translate method. This will need to be altered once
 * we start handling events other than virtual machine events.
 */
public class EventTranslator
{
    private final static Logger logger = LoggerFactory.getLogger(EventTranslator.class);

    private static final List<String> linkBackup = new ArrayList<>();
    static
    {
        linkBackup.add(NameToVMLinks.VM_LINK_METADATA);
    }

    private ResourceExpander expander;

    private RESTLink currUserEditLink;

    private Map<String, String> startActions = new HashMap<>();

    private NameToVMLinks mapNameToVMLinks;

    /**
     * Creates a connection to the REST API that is used to fetch any required extra data
     *
     * @param server Name or ip address of server on which the REST API is running
     * @param user User to authenticate to server as
     * @param password User's password
     */
    public EventTranslator(final String server, final String user, final String password,
        final RESTLink currUserEditLink, final NameToVMLinks mapNameToVMLinks)
    {
        expander = new ResourceExpander(server, user, password);
        this.currUserEditLink = currUserEditLink;
        this.mapNameToVMLinks = mapNameToVMLinks;
    }

    /**
     * This method parses the Event received from the M server and creates a instance of a suitable
     * class to hold the data. It then uses the part URLs in the Event to fetch the data required to
     * populate the class. If the received Event does not represent one of the specific types below
     * then it will be used to create a generic APIEvent instance. The following specific types are
     * currently supported.
     * <ul>
     * <li>BackupVMEvent: Type is VIRTUAL_MACHINE, Action is METADATA_MODIFIED
     * <li>DeployVMEvent: Type is VIRTUAL_MACHINE, Action is DEPLOY_FINISH
     * <li>UndeployVMEvent: Type is VIRTUAL_MACHINE, Action is UNDEPLOY_FINISH
     * </ul>
     *
     * @param event received from the M server
     * @return An APIEvent or subclass instance populated with extra data fetched using the REST API
     * @throws OutboundAPIClientException
     */
    public Optional<APIEvent> translate(final Event event) throws OutboundAPIClientException
    {
        APIEvent apievent = null;
        if (event.getType().equalsIgnoreCase("VIRTUAL_MACHINE"))
        {
            VirtualMachineDto vmdetails = getVM(event);
            String action = event.getAction();
            if (action.equalsIgnoreCase(METADATA_MODIFIED.action()))
            {
                // Ignore any backup events before the machine is deployed as there will be no
                // physical machine yet and so the backup system will not know anything about it.
                if (vmdetails != null && vmdetails.getState() != VirtualMachineState.NOT_ALLOCATED)
                {
                    // Updating the backup results causes a new backup event to be generated, so we
                    // need to ignore any events caused by the current user.
                    if (!currUserEditLink.getHref().endsWith(event.getUser().toLowerCase()))
                    {
                        mapNameToVMLinks.updateVM(vmdetails, linkBackup);
                        apievent = new BackupVMEvent(event, vmdetails);
                    }
                    else
                    {
                        logger.debug("Event was generated by dedicated user. Ignoring.");
                    }
                }
            }
            else if (action.equalsIgnoreCase(CREATE.action())
                || action.equalsIgnoreCase(UNDEPLOY.action()))
            {
                startActions.put(getVMName(vmdetails, event), action);
            }
            else if (action.equalsIgnoreCase(DEPLOY_FINISH.action()))
            {
                // Both deploys and undeploys currently finish with a DEPLOY_FINISH action, so
                // until that bug (ABICLOUDPREMIUM-6135) is fixed we need to check what the original
                // action was
                String vmname = getVMName(vmdetails, event);
                String originalaction = startActions.get(vmname);
                if (UNDEPLOY.action().equalsIgnoreCase(originalaction))
                {
                    mapNameToVMLinks.removeVM(vmname);
                    apievent = new UndeployVMEvent(event);
                }
                else
                {
                    mapNameToVMLinks.addVM(vmdetails);
                    apievent = new DeployVMEvent(event, vmdetails);
                }
            }
            // else if (action.equalsIgnoreCase(UNDEPLOY_FINISH.action()))
            /*
             * UNDEPLOY_FINISH.action() should be used here, but due to a bug that has not yet been
             * fixed in a publically available version of the event-model api, it currently returns
             * "DEPLOY_FINISH" instead of "UNDEPLOY_FINISH". I have therefore hardcoded the proper
             * value here and we can reinstate the proper test once the fix is publically available.
             */
            else if (action.equalsIgnoreCase("UNDEPLOY_FINISH"))
            {
                String vmname = getVMName(vmdetails, event);
                if (vmname != null)
                {
                    mapNameToVMLinks.removeVM(vmname);
                }
                apievent = new UndeployVMEvent(event);
            }
            /*
             * RESTORE_BACKUP
             */
            else if (action.equalsIgnoreCase("RESTORE_BACKUP"))
            {
                mapNameToVMLinks.updateVM(vmdetails, linkBackup);
                apievent = new RestoreVMEvent(event, vmdetails);
            }
            else
            {
                apievent = new VirtualMachineEvent(event, vmdetails);
            }
        }
        else
        {
            apievent = new APIEvent(event);
        }
        return Optional.fromNullable(apievent);
    }

    private VirtualMachineDto getVM(final Event event) throws OutboundAPIClientException
    {
        Optional<String> optVMId = event.getEntityIdentifier();
        VirtualMachineDto vmdetails = null;
        if (optVMId.isPresent())
        {
            String vmid = optVMId.get();
            vmdetails = expander.expandVirtualMachine(vmid);
        }
        return vmdetails;
    }

    private String getVMName(final VirtualMachineDto vmdto, final Event event)
    {
        String name = null;
        if (vmdto != null)
        {
            name = vmdto.getName();
        }
        if (name == null)
        {
            Optional< ? extends EventDetails> optVMDetails = event.getDetails();
            if (optVMDetails.isPresent())
            {
                EventDetails details = optVMDetails.get();
                Map<String, Object> values = details.getTransportMap();
                name = values.get("VIRTUAL_MACHINE_NAME").toString();
            }
        }
        return name;
    }
}

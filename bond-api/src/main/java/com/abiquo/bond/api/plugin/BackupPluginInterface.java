/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.plugin;

import com.abiquo.bond.api.response.BackupResultsHandler;

/**
 * Interface that any plugin that handles backup events should extend. Backup event plugins differ
 * from standard plugins in that they need to return the results of backups from the backup software
 * to the Abiquo server.
 */
public interface BackupPluginInterface extends PluginInterface
{
    /*
     * Returns an instance of the class which provided the client with the backup results.
     */
    BackupResultsHandler getResultsHandler() throws PluginException;
}

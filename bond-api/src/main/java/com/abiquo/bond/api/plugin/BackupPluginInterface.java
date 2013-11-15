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

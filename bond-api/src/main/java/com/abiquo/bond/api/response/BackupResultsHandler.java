/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.bond.api.response;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * A BackupResultsHandler is a class that retrieves results from the backup system and adds them to
 * a queue provided by the client. The client will then update the Abiquo server with any results it
 * finds on the queue.
 */
public interface BackupResultsHandler extends Runnable
{
    void setQueue(LinkedBlockingQueue<VMBackupStatusList> queue);
}

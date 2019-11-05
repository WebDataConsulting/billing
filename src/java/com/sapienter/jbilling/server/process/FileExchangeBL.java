package com.sapienter.jbilling.server.process;
/*
jBilling - The Enterprise Open Source Billing System
Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

This file is part of jbilling.

jbilling is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

jbilling is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

*/

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.task.IFileExchangeTask;
import com.sapienter.jbilling.server.util.ServerConstants;

/**
 * Created by marcomanzi on 3/3/14.
 */
public class FileExchangeBL {

    public void filesDownload(int entityId) {
        launchTasksInPlugin(entityId, true);
    }

    public void filesUpload(int entityId) {
        launchTasksInPlugin(entityId, false);
    }

    public void filesExchange(int entityId) {
        launchTasksInPlugin(entityId, true);
        launchTasksInPlugin(entityId, false);
    }

    private void launchTasksInPlugin(int entityId, boolean downloadOnly) {
        try {
            PluggableTaskManager<IFileExchangeTask> taskManager
                    = new PluggableTaskManager<IFileExchangeTask>(entityId, ServerConstants.PLUGGABLE_TASK_FILE_EXCHANGE);
            IFileExchangeTask task = taskManager.getNextClass();
            while (task != null) {
                if (downloadOnly && task.isDownloadTask() ||
                        !downloadOnly && !task.isDownloadTask()) {
                    task.execute();
                }
                task = taskManager.getNextClass();
            }
        } catch (Exception e) {
            throw new SessionInternalError("File Exchange: exception while running filesDownload.", e);
        }
    }

}

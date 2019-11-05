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

package com.sapienter.jbilling.server.util;

import java.util.List;
import java.util.Set;

import com.sapienter.jbilling.client.process.JobScheduler;
import com.sapienter.jbilling.client.process.Trigger;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.task.IScheduledTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import static org.quartz.impl.matchers.GroupMatcher.*;

/**
 * Spring bean that bootstraps jBilling services on application start.
 *
 * @author Brian Cowdery
 * @since 22-09-2010
 */
public class SchedulerBootstrapHelper {
    private static final FormatLogger LOG = new FormatLogger(SchedulerBootstrapHelper.class);

    /**
     * Schedule all core jBilling batch processes.
     */
    private void scheduleBatchJobs() {
        // todo: refactor "Trigger" into separate scheduled Job classes.
        Trigger.Initialize();
    }

    /**
     * Schedule all configured {@link IScheduledTask} plug-ins for each entity.
     */
    private void schedulePluggableTasks() {
        JobScheduler scheduler = JobScheduler.getInstance();
        try {
            for (CompanyDTO entity : new CompanyDAS().findEntities()) {
                PluggableTaskManager<IScheduledTask> manager =
                        new PluggableTaskManager<IScheduledTask>
                                (entity.getId(), ServerConstants.PLUGGABLE_TASK_SCHEDULED);

                LOG.debug("Processing %s scheduled tasks for entity %s", manager.getAllTasks().size(), entity.getId());
                for (IScheduledTask task = manager.getNextClass(); task != null; task = manager.getNextClass()) {
                    try {
                        if(task.getJobDetail() != null && task.getTrigger() != null){
                            scheduler.getScheduler().scheduleJob(task.getJobDetail(), task.getTrigger());
                            LOG.debug("Scheduled: [%s]", task.getTaskName());
                        }
                    } catch (PluggableTaskException e) {
                        LOG.warn("Failed to schedule pluggable task [%s]", task.getTaskName());
                    } catch (SchedulerException e) {
                        LOG.warn("Failed to schedule pluggable task [%s]", task.getTaskName());
                    }                    
                }
            }
        } catch (PluggableTaskException e) {
            LOG.error("Exception occurred scheduling pluggable tasks.", e);
        }
    }
    
	/**
	 * Reschedule a jBilling IScheduledTask after it has been saved.
	 * @param task
	 * @author Vikas Bodani
	 */
	public void rescheduleJob(IScheduledTask task) throws Exception {
		LOG.debug("Rescheduling instance of: %s", task.getClass().getName());
		if (null != task) {
			LOG.debug("Task Name: %s", task.getTaskName());
			try {
				Scheduler sd = JobScheduler.getInstance().getScheduler();
				boolean found = unScheduleExisting(task);
				// schedule new plugin if not found, no need to restart jbilling
				// then
				if (!found) {
					LOG.debug("This is a new scheduled task.");
				}
				LOG.debug("scheduling %s", task.getTaskName());
				sd.scheduleJob(task.getJobDetail(), task.getTrigger());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
    
	/**
	 * Reschedule a jBilling IScheduledTask after it has been saved.
	 * @param task
	 * @author Vikas Bodani
	 */
	public boolean unScheduleExisting(IScheduledTask task) throws Exception {
		LOG.debug("Unscheduling instance of: %s", task.getClass().getName());
		boolean found = false;
		if (null != task) {
			Scheduler sd = JobScheduler.getInstance().getScheduler();
            List<String> triggerGrps;
            Set<TriggerKey> triggers;
            try {
                triggerGrps = sd.getTriggerGroupNames();
                for (String stTriggerGrp : triggerGrps) {
                    LOG.debug("Trigger Group Name: %s", stTriggerGrp);
                    triggers = sd.getTriggerKeys(GroupMatcher.<TriggerKey>groupEquals(stTriggerGrp));

                    for (TriggerKey keyTrigger : triggers) {
                        LOG.debug("Trigger Name : %s", keyTrigger.getName());

                        if (keyTrigger.getName().equals(task.getTaskName())) {
                            found = true;
                            LOG.debug("unscheduling %s", keyTrigger.getName());
                            sd.unscheduleJob(keyTrigger);
                        }
                    }
                }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return found;
	}
    
}

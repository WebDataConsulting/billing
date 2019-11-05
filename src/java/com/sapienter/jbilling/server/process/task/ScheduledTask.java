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
package com.sapienter.jbilling.server.process.task;

import java.util.HashMap;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.IPluggableTaskSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

import org.quartz.impl.JobDetailImpl;

/**
 * @author Brian Cowdery
 * @since 04-02-2010
 */
public abstract class ScheduledTask extends PluggableTask implements IScheduledTask {
    private static final FormatLogger LOG = new FormatLogger(ScheduledTask.class);

    public static final String JOB_LIST_KEY = "job_chain_ids";
    public static final String JOB_LIST_SEPARATOR = ",";

    protected static final ParameterDescription JOB_CHAIN_IDS =
            new ParameterDescription("job_chain_ids", false, ParameterDescription.Type.STR);

    //initializer for pluggable params
    {
        descriptions.add(JOB_CHAIN_IDS);
    }

    /**
     * Constructs the JobDetail for this scheduled task, and copies the plug-in parameter
     * map into the detail JobDataMap for use when the task is executed by quartz.
     *
     * @return job detail
     * @throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException
     *
     */
    public JobDetailImpl getJobDetail () throws PluggableTaskException {
        JobDetailImpl detail = new JobDetailImpl(getTaskName() + " job", Scheduler.DEFAULT_GROUP, this.getClass());
        detail.getJobDataMap().put("entityId", getEntityId());
        detail.getJobDataMap().put("taskId", getTaskId());
        detail.getJobDataMap().putAll(parameters);
        return detail;
    }

    /**
     * Copies plug-in parameters from the JobDetail map into the plug-in's working
     * parameter map. This is a compatibility step so that we don't have to write
     * separate parameter handling code specifically for scheduled tasks.
     *
     * @param context executing job context
     * @throws JobExecutionException thrown if an exception occurs while initializing parameters
     */
    protected void _init (JobExecutionContext context) throws JobExecutionException {
        JobDataMap map = context.getJobDetail().getJobDataMap();
        setEntityId(map.getInt("entityId"));

        parameters = new HashMap<String, String>();
        for (Object key : map.keySet())
            parameters.put((String) key, map.get(key).toString());
    }

    /**
     * Return this plug-ins schedule as a readable string. Can be used as part of
     * {@link IScheduledTask#getTaskName()} to make the task name unique to the schedule
     * allowing multiple plug-ins of the same type to be added with different schedules.
     *
     * @return schedule string
     */
    public abstract String getScheduleString ();

    /**
     * This method is used for the chained scheduledTasks.
     * It executes the current task and calls the following in the list,
     * the actual task logic goes to {@link this#doExecute(JobExecutionContext)}
     *
     * @param context executing job context
     * @throws JobExecutionException
     */
    public void execute (JobExecutionContext context) throws JobExecutionException {
        //Executes current task logic
        doExecute(context);
        //Calls the next task in the chain.
        handleChainedTasks(context);
    }

    /**
     * Here goes the actual task logic to execute for chained tasks.
     *
     * @param context
     * @throws JobExecutionException
     */
    public void doExecute (JobExecutionContext context) throws JobExecutionException {}

    /**
     * Handles the call of the following task in the list
     * taken from the JOB_CHAIN_IDS parameter.
     *
     * @param context
     * @throws JobExecutionException
     */
    private void handleChainedTasks(JobExecutionContext context) throws JobExecutionException{
        JobDataMap jdMap = context.getJobDetail().getJobDataMap();
        String jobList = (String) jdMap.get(JOB_LIST_KEY);
        if (StringUtils.isNotBlank(jobList)) {
            jobList = jobList.trim();
            String[] jobListArr = jobList.split(JOB_LIST_SEPARATOR);
            if (!ArrayUtils.isEmpty(jobListArr)) {
                if (jobList.contains(JOB_LIST_SEPARATOR)) {
                    jobList = jobList.substring(jobList.indexOf(JOB_LIST_SEPARATOR) + 1, jobList.length());
                } else {
                    jobList = "";
                }
                jdMap.put(JOB_LIST_KEY, jobList);
                try {
                    Integer jobId = Integer.parseInt(jobListArr[0].trim());

                    IPluggableTaskSessionBean iPluggableTaskSessionBean = (IPluggableTaskSessionBean) Context.getBean(Context.Name.PLUGGABLE_TASK_SESSION);
                    PluggableTaskDTO pluggableTaskDTO = iPluggableTaskSessionBean.getDTO(jobId, Integer.parseInt(jdMap.get("entityId").toString()));
                    PluggableTaskBL<ScheduledTask> taskLoader = new PluggableTaskBL<ScheduledTask>();
                    taskLoader.set(pluggableTaskDTO);
                    LOG.info("Executing task from a chain with puggableTaskTypeId=%s",  jobId);
                    taskLoader.instantiateTask().execute(context);
                }catch (NumberFormatException e) {
                    LOG.error("Error getting the jobId from the %s parameter.", JOB_LIST_KEY);
                    e.printStackTrace();
                }
                catch (PluggableTaskException e) {
                    LOG.error("Error executing a task from a chain.");
                    e.printStackTrace();
                }
            }
        }
    }
}

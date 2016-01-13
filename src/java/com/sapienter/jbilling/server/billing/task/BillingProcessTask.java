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

package com.sapienter.jbilling.server.billing.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.task.AbstractBackwardSimpleScheduledTask;
import com.sapienter.jbilling.server.util.Context;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.util.Date;



/**
 * Scheduled billing process plug-in, executing the billing process on a simple schedule.
 * <p>
 * This plug-in accepts the standard {@link com.sapienter.jbilling.server.process.task.AbstractSimpleScheduledTask} plug-in parameters
 * for scheduling. If these parameters are omitted (all parameters are not defined or blank)
 * the plug-in will be scheduled using the jbilling.properties "process.time" and
 * "process.frequency" values.
 *
 * @author
 * @see com.sapienter.jbilling.server.process.task.AbstractBackwardSimpleScheduledTask
 */
public class BillingProcessTask extends AbstractBackwardSimpleScheduledTask {

    private static final FormatLogger LOG = new FormatLogger(BillingProcessTask.class);

    private static final String PROPERTY_RUN_BILLING = "process.run_billing";

    public String getTaskName() {
        return "billing process: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        super.execute(context);//_init(context);

        IBillingProcessSessionBean billing = (IBillingProcessSessionBean) Context.getBean(Context.Name.BILLING_PROCESS_SESSION);

        if (Util.getSysPropBooleanTrue(PROPERTY_RUN_BILLING)) {
            LOG.info("Starting billing at " + new Date() + " for " + getEntityId());
            billing.trigger(new Date(), getEntityId());
            LOG.info("Ended billing at " + new Date());
        }
    }

    /**
     * Returns the scheduled trigger for the billing process. If the plug-in is missing
     * the {@link com.sapienter.jbilling.server.process.task.AbstractSimpleScheduledTask}
     * parameters use the the default jbilling.properties process schedule instead.
     *
     * @return billing trigger for scheduling
     * @throws PluggableTaskException thrown if properties or plug-in parameters could not be parsed
     */
    @Override
    public SimpleTrigger getTrigger() throws PluggableTaskException {
        SimpleTrigger trigger = super.getTrigger();

        // trigger start time and frequency using jbilling.properties unless plug-in
        // parameters have been explicitly set to define the billing schedule
        if (useProperties()) {
            LOG.debug("Scheduling billing process from jbilling.properties ...");
            trigger = setTriggerFromProperties((SimpleTriggerImpl) trigger);
        } else {
            LOG.debug("Scheduling billing process using plug-in parameters ...");
        }

        return trigger;
    }

}

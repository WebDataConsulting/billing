/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.IMediationSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.task.AbstractBackwardSimpleScheduledTask;
import com.sapienter.jbilling.server.util.Context;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduled mediation process plug-in, executing the mediation process on a simple schedule.
 *
 * This plug-in accepts the standard {@link AbstractSimpleScheduledTask} plug-in parameters
 * for scheduling. If these parameters are omitted (all parameters are not defined or blank)
 * the plug-in will be scheduled using the jbilling.properties "process.time" and
 * "process.frequency" values.
 *
 * @see com.sapienter.jbilling.server.process.task.AbstractBackwardSimpleScheduledTask
 *
 * @author Brian Cowdery
 * @since 25-05-2010
 */
public class MediationProcessTask extends AbstractBackwardSimpleScheduledTask {
	private static final FormatLogger LOG = new FormatLogger(MediationProcessTask.class);

    private static final AtomicBoolean running = new AtomicBoolean(false);

    private static final String PROPERTY_RUN_MEDIATION = "process.run_mediation";

    public String getTaskName() {
        return "mediation process: " + getScheduleString();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        super.execute(context);//_init(context);

        if (running.compareAndSet(false, true)) {
            IMediationSessionBean mediation = (IMediationSessionBean) Context.getBean(Context.Name.MEDIATION_SESSION);

            try {
                if (Util.getSysPropBooleanTrue(PROPERTY_RUN_MEDIATION)) {
                    LOG.info("Starting mediation at %s", new Date());
                    mediation.trigger(getEntityId());
                    LOG.info("Ended mediation at %s", new Date());
                }
            } finally {
                running.set(false);
            }
        } else {
            LOG.warn("Failed to trigger mediation process at %s, another process is already running.", context.getFireTime());
        }
    }

    /**
     * Returns the scheduled trigger for the mediation process. If the plug-in is missing
     * the {@link com.sapienter.jbilling.server.process.task.AbstractSimpleScheduledTask}
     * parameters use the the default jbilling.properties process schedule instead.
     *
     * @return mediation trigger for scheduling
     * @throws PluggableTaskException thrown if properties or plug-in parameters could not be parsed
     */
    @Override
    public SimpleTrigger getTrigger() throws PluggableTaskException {
        SimpleTrigger trigger = super.getTrigger();

        // trigger start time and frequency using jbilling.properties unless plug-in
        // parameters have been explicitly set to define the mediation schedule
        if (useProperties()) {
            LOG.debug("Scheduling mediation process from jbilling.properties ...");
            trigger= setTriggerFromProperties((SimpleTriggerImpl)trigger);
        } else {
            LOG.debug("Scheduling mediation process using plug-in parameters ...");
        }

        return trigger;
    }

}

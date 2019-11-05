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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.TimePeriod;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.ServerConstants;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.util.concurrent.TimeUnit;

/**
 * A simple Scheduled process plug-in, executing the extending process class on a simple schedule.
 *
 * This plug-in accepts the standard {@link AbstractSimpleScheduledTask} plug-in parameters
 * for scheduling. If these parameters are omitted (all parameters are not defined or blank)
 * the plug-in will be scheduled using the jbilling.properties "process.time" and
 * "process.frequency" values, therefore named as 'AbstractBackward' because of this backward
 * compatility in scheduling the same process via the old jbilling.properties files.
 *
 * @see com.sapienter.jbilling.server.process.task.AbstractSimpleScheduledTask
 *
 * @author Vikas Bodani
 * @since 02-08-2010
 */

public abstract class AbstractBackwardSimpleScheduledTask extends
        AbstractSimpleScheduledTask {
    private static final FormatLogger LOG = new FormatLogger(AbstractBackwardSimpleScheduledTask.class);
    private static final String PROPERTY_PROCESS_TIME = "process.time";
    private static final String PROPERTY_PROCESS_FREQ = "process.frequency";

    public void execute(JobExecutionContext context) throws JobExecutionException {
        _init(context);
    }

    @Override
    public String getScheduleString() {
        StringBuilder builder = new StringBuilder();

        try {
            builder.append("start: ");
            builder.append(useProperties()
                           ? Util.getSysProp(PROPERTY_PROCESS_TIME)
                           : getParameter(PARAM_START_TIME.getName(), DEFAULT_START_TIME).toString());
            builder.append(", ");

            builder.append("end: ");
            builder.append(getParameter(PARAM_END_TIME.getName(), DEFAULT_END_TIME));
            builder.append(", ");

            Integer repeat = getParameter(PARAM_REPEAT.getName(), DEFAULT_REPEAT);
            builder.append("repeat: ");
            builder.append((repeat == SimpleTrigger.REPEAT_INDEFINITELY ? "infinite" : repeat));
            builder.append(", ");

            builder.append("interval: ");
            builder.append(useProperties()
                           ? Util.getSysProp(PROPERTY_PROCESS_FREQ) + " mins"
                           : getParameter(PARAM_INTERVAL.getName(), DEFAULT_INTERVAL) + " hrs");

        } catch (PluggableTaskException e) {
            LOG.error("Exception occurred parsing plug-in parameters", e);
        }

        return builder.toString();
    }

    protected SimpleTrigger setTriggerFromProperties(SimpleTriggerImpl trigger) throws PluggableTaskException {
    try {
    		
            // set process.time as trigger start time if set
            String start = Util.getSysProp(PROPERTY_PROCESS_TIME);
            if (StringUtils.isNotBlank(start))
                trigger.setStartTime(DATE_FORMAT.parseDateTime(start).toDate());

            // set process.frequency as trigger repeat interval if set
            String repeat = Util.getSysProp(PROPERTY_PROCESS_FREQ);
            if (StringUtils.isNotBlank(repeat))
                trigger.setRepeatInterval(Long.parseLong(repeat) * 60 * 1000);

        } catch (NumberFormatException e) {
            throw new PluggableTaskException("Exception parsing process.frequency for schedule", e);
        } catch (IllegalArgumentException e) {
            throw new PluggableTaskException("Exception parsing process.time for schedule", e);
        }

        return trigger;
    }

    /**
     * Returns true if the billing process should be scheduled using values from jbilling.properties
     * or if the schedule should be derived from plug-in parameters.
     *
     * @return true if properties should be used for scheduling, false if schedule from plug-ins
     */
    protected boolean useProperties() {
        return StringUtils.isBlank(parameters.get(PARAM_START_TIME.getName()))
            && StringUtils.isBlank(parameters.get(PARAM_END_TIME.getName()))
            && StringUtils.isBlank(parameters.get(PARAM_REPEAT.getName()))
            && StringUtils.isBlank(parameters.get(PARAM_INTERVAL.getName()));
    }
    
    
    public TimePeriod getTimePeriod() {

    	TimePeriod period = new TimePeriod();
    	Long schedulerIntervalInDays = Long.valueOf(0);

    	try {
    		if (useProperties()) {
    			String schedulerIntervalInMinutes = Util.getSysProp(PROPERTY_PROCESS_FREQ);
    			if (StringUtils.isNotBlank(schedulerIntervalInMinutes)) {
    				schedulerIntervalInDays = TimeUnit.MINUTES.toDays(Integer.valueOf(schedulerIntervalInMinutes));
    			}

    		} else {
    			Integer schedulerIntervalInHours = getParameter(PARAM_INTERVAL.getName(), DEFAULT_INTERVAL);
    			if (schedulerIntervalInHours != null) {
    				schedulerIntervalInDays = TimeUnit.HOURS.toDays(schedulerIntervalInHours);
    			}
    		}
    	}   catch (PluggableTaskException e) {
    		LOG.error("Exception occurred parsing plug-in parameters", e);
    	}

    	period.setUnitId(ServerConstants.PERIOD_UNIT_DAY);
    	period.setValue(schedulerIntervalInDays.compareTo(Long.valueOf(0)) > 0 ? schedulerIntervalInDays
    			.intValue() : 1);

    	return period;
    }
}

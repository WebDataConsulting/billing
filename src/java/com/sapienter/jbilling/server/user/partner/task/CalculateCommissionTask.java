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
package com.sapienter.jbilling.server.user.partner.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.user.IUserSessionBean;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Scheduled task to trigger the Partner Commission Process
 */
public class CalculateCommissionTask extends AbstractCronTask {
	
    private static final FormatLogger LOG = new FormatLogger(CalculateCommissionTask.class);
    @Override
    public String getTaskName () {
        return this.getClass().getName() + "-" + getEntityId();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        if(Util.getSysPropBooleanTrue(ServerConstants.PROPERTY_RUN_COMMISION)) {
        	_init(context);
        	IUserSessionBean userSessionBean = (IUserSessionBean) Context.getBean(
        				Context.Name.USER_SESSION);
		   
		        // get a session for the remote interfaces
			userSessionBean.calculatePartnerCommissions(getEntityId());
		 } else {
					LOG.warn("Failed to trigger CalculateCommission process at %s, another process is already running.", context.getFireTime());
			} 
      }
}

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

package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 * @author Igor Poteryaev
 */
public class EmailAndPaymentProcessor extends JobContextHandler implements InitializingBean,
        ItemProcessor<Integer, Integer> {

    private static final FormatLogger LOG = new FormatLogger(EmailAndPaymentProcessor.class);

    private IBillingProcessSessionBean local;

    private Map<Integer, Integer[]> map;

    private Integer entityId;
    private Integer billingProcessId;
    private boolean review;

    @Override
    public Integer process(Integer userId) {
        long enteringTime = System.currentTimeMillis();
        LOG.debug("BillingProcessId # %s || UserId # %s +++ Enter process(Integer userId)", billingProcessId, userId);
        Integer[] result = map.get(userId);
        if (!review) {
            LOG.debug("Sending email and processing payments for UserId # %s", userId);
            for (int f = 0; f < result.length; f++) {
                local.email(entityId, result[f], billingProcessId);
            }
            LOG.debug("BillingProcessId # %s || UserId # %s +++ User %s done email & payment.", billingProcessId,
                    userId, userId);
        }
        LOG.debug("BillingProcessId # %s || UserId # %s +++ Leaving process(Integer userId)", billingProcessId, userId);
        long exitTime = System.currentTimeMillis();
        LOG.debug("User # %s executed in # %s secs", userId, (exitTime - enteringTime) / 1000);
        return userId;
    }

    @Override
    public void afterPropertiesSet() {
        LOG.debug("Entering afterPropertiesSet()");

        map = this.getMapFromContext(ServerConstants.JOBCONTEXT_PROCESS_USER_RESULT_KEY);

        billingProcessId = this.getIntegerFromContext(ServerConstants.JOBCONTEXT_BILLING_PROCESS_ID_KEY);
        LOG.debug("billing process id from context: %s", billingProcessId);

        local = (IBillingProcessSessionBean) Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
    }

    public void setEntityId(String entityId) {
        this.entityId = Integer.parseInt(entityId);
    }

    public void setReview(String review) {
        this.review = Integer.parseInt(review) == 1;
    }
}

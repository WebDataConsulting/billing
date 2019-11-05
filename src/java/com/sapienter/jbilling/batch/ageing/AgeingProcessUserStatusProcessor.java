package com.sapienter.jbilling.batch.ageing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.util.Context;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;

import java.util.Date;



/**
 * @author Khobab
 */
public class AgeingProcessUserStatusProcessor implements InitializingBean, ItemProcessor<Integer, AgeingStatusResult> {

    private static final FormatLogger LOG = new FormatLogger(AgeingProcessUserStatusProcessor.class);

    private IBillingProcessSessionBean local = null;

    private Date ageingDate;
    private Integer entityId;

    /**
     * Gets BillingProcessSessionBean bean from context for processor use
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        local = (IBillingProcessSessionBean)
                Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
    }

    /**
     * Gets users id from reader and then done status reviewing
     */
    @Override
    public AgeingStatusResult process(Integer userId) throws Exception {
        AgeingStatusResult result = new AgeingStatusResult();
        LOG.debug("Review Status of the user # " + userId);
        result.setOverdueInvoices(local.reviewUserStatus(entityId, userId, ageingDate));
        result.setUserId(userId);
        return result;
    }

    /*
     * setters
     */
    public void setEntityId(String entityId) {
        this.entityId = Integer.parseInt(entityId);
    }

    public void setAgeingDate(Date billingDate) {
        this.ageingDate = billingDate;
    }

}

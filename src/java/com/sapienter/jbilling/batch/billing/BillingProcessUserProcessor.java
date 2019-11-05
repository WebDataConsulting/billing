package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.BillingProcessRunBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.ProcessRunUserDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;

import java.util.Date;
import java.util.List;
import java.util.Map;



/**
 * @author Khobab
 */
public class BillingProcessUserProcessor extends JobContextHandler implements InitializingBean, ItemProcessor<Integer, Integer> {

    private static final FormatLogger logger = new FormatLogger(BillingProcessUserProcessor.class);

    private IBillingProcessSessionBean local;
    private ConfigurationBL conf;

    private List<Integer> successfullUsers;
    private boolean onlyRecurring;
    private Integer billingProcessId;

    private Integer entityId;
    private Date billingDate;
    private boolean review;

    /**
     * receives user id, processes it and then returns a user id.
     */
    @Override
    public Integer process(Integer userId) throws Exception {
        long enteringTime = System.currentTimeMillis();
        logger.debug("BillingProcessId # " + billingProcessId + " || UserId # " + userId + " +++ Entering process(Integer userId)");
        Integer totalInvoices = 0;
        if (!successfullUsers.contains(userId)) { // TODO: change this by a query to the DB
            logger.debug("BillingProcessId # " + billingProcessId + " || UserId # " + userId + " +++ Processing user #:" + userId);

            Integer[] result = local.processUser(billingProcessId, billingDate, userId, review, onlyRecurring);
            logger.debug("User " + userId + " was processed sucessfully.");
            if (result != null) {
                logger.debug("BillingProcessId # " + billingProcessId + " || UserId # " + userId + " +++ User %s done invoice generation.", userId);
                List<Integer> list = this.getIntegerListFromContext(ServerConstants.JOBCONTEXT_SUCCESSFULL_USERS_LIST_KEY);
                synchronized (list) {
                    if (!list.contains(userId)) {
                        list.add(userId);
                    }
                    this.addIntegerListToContext(ServerConstants.JOBCONTEXT_SUCCESSFULL_USERS_LIST_KEY, list);
                }

                Map<Integer, Integer[]> invoicesMap = this.getMapFromContext(ServerConstants.JOBCONTEXT_PROCESS_USER_RESULT_KEY);
                synchronized (invoicesMap) {
                    invoicesMap.put(userId, result);
                    this.addMapToContext(ServerConstants.JOBCONTEXT_PROCESS_USER_RESULT_KEY, invoicesMap);
                }

                local.addProcessRunUser(billingProcessId, userId, ProcessRunUserDTO.STATUS_SUCCEEDED);
                logger.debug("BillingProcessId # " + billingProcessId + " || UserId # " + userId + " +++ STATUS_SUCCEEDED");
            }

            totalInvoices = result.length;
            this.addIntegerToContext(ServerConstants.JOBCONTEXT_TOTAL_INVOICES_KEY, this.getIntegerFromContext(ServerConstants.JOBCONTEXT_TOTAL_INVOICES_KEY) + totalInvoices);
            logger.debug("BillingProcessId # " + billingProcessId + " || UserId # " + userId + " +++ Total invoices count from job context # " + this.getIntegerFromContext(ServerConstants.JOBCONTEXT_TOTAL_INVOICES_KEY));

            logger.debug("BillingProcessId # " + billingProcessId + " || UserId # " + userId + " +++ Total # " + totalInvoices + " invoices generated for user # " + userId);
        }

        logger.debug("BillingProcessId # " + billingProcessId + " || UserId # " + userId + " +++ Leaving process(Integer userId)");
        long exitTime = System.currentTimeMillis();
        logger.debug("User # " + userId + " executed in # " + (exitTime - enteringTime) / 1000 + " secs");
        return userId;
    }

    /**
     * runs immediately after values have set to calculate some values that will be used during processing.
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        logger.debug("Entering afterPropertiesSet()");

        conf = new ConfigurationBL(entityId);
        local = (IBillingProcessSessionBean) Context.getBean(Context.Name.BILLING_PROCESS_SESSION);

        billingProcessId = this.getIntegerFromContext(ServerConstants.JOBCONTEXT_BILLING_PROCESS_ID_KEY);

        BillingProcessRunBL billingProcessRunBL = new BillingProcessRunBL();
        billingProcessRunBL.setProcess(billingProcessId);

        // TODO: all the customer's id in memory is not a good idea. 1M customers would be 4MB of memory
        logger.debug("Finding successful user for billing process");
        successfullUsers = billingProcessRunBL.findSuccessfullUsers();

        logger.debug("Calcualting isRecurring variable");
        onlyRecurring = conf.getEntity().getOnlyRecurring() == 1;

        logger.debug("Leaving afterPropertiesSet()");
    }

    public void setEntityId(String entityId) {
        this.entityId = Integer.parseInt(entityId);
    }

    public void setBillingDate(Date billingDate) {
        this.billingDate = billingDate;
    }

    public void setReview(String review) {
        this.review = Integer.parseInt(review) == 1;
    }
}

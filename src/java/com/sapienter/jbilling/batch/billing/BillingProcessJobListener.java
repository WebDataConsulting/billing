package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.BatchProcessInfoBL;
import com.sapienter.jbilling.server.process.BillingProcessFailedUserBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask;
import com.sapienter.jbilling.server.process.task.IBillingProcessFilterTask;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import org.hibernate.ScrollableResults;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;

import java.sql.SQLException;
import java.util.*;

;

/**
 * @author Khobab
 */
public class BillingProcessJobListener implements JobExecutionListener {

    private static final FormatLogger logger = new FormatLogger(BillingProcessJobListener.class);

    /**
     * Moves job execution context data to database tables at the end of billing process
     */
    @SuppressWarnings("unchecked")
    @Override
    public void afterJob(JobExecution jobExec) {
        logger.debug("Entering afterJob()");
        ExecutionContext ec = jobExec.getExecutionContext();

        //saving info to database
        logger.debug("Getting contents from job execution context");
        List<Integer> successfulUsers = (List<Integer>) ec.get(ServerConstants.JOBCONTEXT_SUCCESSFULL_USERS_LIST_KEY);
        List<Integer> failedUsers = (List<Integer>) ec.get(ServerConstants.JOBCONTEXT_FAILED_USERS_LIST_KEY);
        Integer billingProcessId = (Integer) ec.getInt(ServerConstants.JOBCONTEXT_BILLING_PROCESS_ID_KEY);
        Integer totalUsersFailed = (Integer) ec.getInt(ServerConstants.JOBCONTEXT_TOTAL_USERS_FAILED_KEY);

        logger.debug("Data from context is: successful_users # " + successfulUsers.toString() + " ,failed_users # " + failedUsers.toString() +
                " , billing_process_id # " + billingProcessId + " , no_of_failed_users # " + totalUsersFailed);

        BatchProcessInfoBL infoBL = new BatchProcessInfoBL();
        BillingProcessFailedUserBL failedUserBL = new BillingProcessFailedUserBL();

        Integer batchProcessId = infoBL.create(billingProcessId, jobExec.getId().intValue(), totalUsersFailed, successfulUsers.size()).getId();
        for (Integer failed : failedUsers) {
            failedUserBL.create(batchProcessId, failed);
        }

        // if there are failed users mark the job as FAILED so it could be restarted
        if (totalUsersFailed > 0) {
            logger.debug("There are failed users in job # " + jobExec.getJobId() + " , marking job as failed.");
            jobExec.setStatus(BatchStatus.FAILED);
        }

        logger.debug("Destroying ids object from BillingProcessUsersLoader");
        jobExec.getExecutionContext().remove(ServerConstants.JOBCONTEXT_USERS_LIST_KEY);

    }

    /**
     * will be called before every billing to clear values stored in job execution context or initialize maps
     * this method also creates billing process dto and
     */
    @Override
    public void beforeJob(JobExecution jobExec) {
        ExecutionContext ec = jobExec.getExecutionContext();
        ec.putInt(ServerConstants.JOBCONTEXT_TOTAL_USERS_FAILED_KEY, 0);
        ec.putInt(ServerConstants.JOBCONTEXT_TOTAL_INVOICES_KEY, 0);
        ec.put(ServerConstants.JOBCONTEXT_FAILED_USERS_LIST_KEY, new ArrayList<Integer>());
        ec.put(ServerConstants.JOBCONTEXT_SUCCESSFULL_USERS_LIST_KEY, new ArrayList<Integer>());
        ec.put(ServerConstants.JOBCONTEXT_PROCESS_USER_RESULT_KEY, new HashMap<Integer, Integer[]>());

        JobParameters jobParams = jobExec.getJobParameters();

        Integer entityId = Integer.parseInt(jobParams.getString(ServerConstants.BATCH_JOB_PARAM_ENTITY_ID));
        Integer periodType = Integer.parseInt(jobParams.getString(ServerConstants.BATCH_JOB_PARAM_PERIOD_TYPE));
        Integer periodValue = Integer.parseInt(jobParams.getString(ServerConstants.BATCH_JOB_PARAM_PERIOD_VALUE));
        boolean review = "1".equals(jobParams.getString(ServerConstants.BATCH_JOB_PARAM_REVIEW)) ;
        Date billingDate = jobParams.getDate(ServerConstants.BATCH_JOB_PARAM_BILLING_DATE);

        ConfigurationBL conf = new ConfigurationBL(entityId);
        IBillingProcessSessionBean local = (IBillingProcessSessionBean) Context.getBean(Context.Name.BILLING_PROCESS_SESSION);

        BillingProcessDTO billingProcessDTO = null;
        try {
            billingProcessDTO = local.createProcessRecord(
                    entityId, billingDate, periodType, periodValue, review,
                    conf.getEntity().getRetries());
        } catch (SQLException e) {
            logger.error("Error occurred while creating BillingProcessDTO: ", e);
        }

        new BillingProcessDAS().reattachUnmodified(billingProcessDTO);
        billingProcessDTO.getId();
        logger.debug("created billing process with id: " + billingProcessDTO.getId());

        logger.debug("Loading user ids for entityId # " + entityId + " and billingdate # " + billingDate);
        jobExec.getExecutionContext().put(ServerConstants.JOBCONTEXT_USERS_LIST_KEY, loadAndSort(entityId, billingDate));
        ec.putInt(ServerConstants.JOBCONTEXT_BILLING_PROCESS_ID_KEY, billingProcessDTO.getId());
    }

    /**
     * Finds user ids for company id and billing date using suitable find users method
     *
     * @param entityId    :   company id for which users will be found
     * @param billingDate :   billing date of users
     */
    @SuppressWarnings("rawtypes")
    private List<Integer> loadAndSort(Integer entityId, Date billingDate) {
        logger.debug("Entering loadAndStore() where entityId # " + entityId + " and billingDate # " + billingDate);
        PluggableTaskManager taskManager = null;
        try {
            taskManager = new PluggableTaskManager(entityId, ServerConstants.PLUGGABLE_TASK_BILL_PROCESS_FILTER);
        } catch (PluggableTaskException e) {
            // eat it
        }
        logger.debug("PluggableTaskManager initialized");

        IBillingProcessFilterTask task = null;
        try {
            if (taskManager != null) {
                task = (IBillingProcessFilterTask) taskManager.getNextClass();
            }
        } catch (PluggableTaskException e) {
            // eat it
        }
        // if one was not configured just use the basic task by default
        if (task == null) {
            logger.debug("No filter was found, initializing basic filter");
            task = new BasicBillingProcessFilterTask();
        }

        logger.debug("Finding user ids and setting them to scrollable results cursor");
        ScrollableResults results = task.findUsersToProcess(entityId, billingDate);
        List<Integer> ids = new ArrayList<Integer>();
        // put the items of scrollableresults in a list
        if (results != null) {
            while (results.next()) {
                ids.add((Integer) results.get(0));
            }
            results.close();
        }
        Collections.sort(ids);
        return ids;
    }
}

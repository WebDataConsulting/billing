package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BatchProcessInfoDAS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;

import java.util.Date;



/**
 * @author Khobab
 */
public class BatchContextHandler {

    private static final FormatLogger logger = new FormatLogger(BatchContextHandler.class);

    private JobExplorer jobExplorer;

    public BatchContextHandler() {
        jobExplorer = ((JobExplorer) Context.getBean(Context.Name.BATCH_JOB_EXPLORER));
    }

    /**
     * returns start date of the execution provided execution id
     *
     * @param jobExecutionId :	id of the job execution
     * @return Date                :	start date of execution
     */
    public Date getStartDate(Integer jobExecutionId) {
        logger.debug("Entering getStartDate()");
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId.longValue());
        logger.debug("Found execution: " + execution);
        return execution == null ? null : execution.getStartTime();
    }

    /**
     * returns end date of the execution provided execution id
     *
     * @param jobExecutionId :	id of the job execution
     * @return Date                :	end date of execution
     */
    public Date getEndDate(Integer jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId.longValue());
        return execution == null ? null : execution.getEndTime();
    }

    /**
     * restarts a failed job
     *
     * @param billingProcessId :	id of the failed billing process
     * @param entityId         :	id of the entity to which billing process belongs
     * @return                    : true - if restart was successful
     */
    public boolean restartFailedJobByBillingProcessId(Integer billingProcessId, final Integer entityId) {
        logger.debug("Entering restartFailedJobByBillingProcessId() with id # " + billingProcessId);
        final Date jobRunDate = this.getDateFromJbParametersByExecutionId(this.getExecutionIdByBillingProcessId(billingProcessId));
        if (jobRunDate != null) {
            Thread restartThread = new Thread() {
                @Override
                public void run() {
                    getProcessBean().trigger(jobRunDate, entityId);
                }
            };
            restartThread.start();
            return true;
        }
        logger.debug("Job Restart was successful...job running in background");
        return false;
    }

    /**
     * Gets BillingProcessSessionBean bean from context
     *
     * @return    :	BillingProcessSessionBean
     */
    private IBillingProcessSessionBean getProcessBean() {
        return Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
    }

    /**
     * Get latest job execution id of given billing process
     *
     * @param billingProcessId :	id of the billing process
     * @return                        :	latest execution id
     */
    private Integer getExecutionIdByBillingProcessId(Integer billingProcessId) {
        return new BatchProcessInfoDAS().getEntitiesByBillingProcessId(billingProcessId).get(0).getJobExecutionId();
    }

    /**
     * gets date value from job parameters
     *
     * @param executionId
     * @return
     */
    private Date getDateFromJbParametersByExecutionId(Integer executionId) {
        return jobExplorer.getJobExecution(executionId.longValue()).getJobParameters().getDate(ServerConstants.BATCH_JOB_PARAM_BILLING_DATE);
    }
}

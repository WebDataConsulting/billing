package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.BillingProcessRunBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;



/**
 * @author Khobab
 */
public class BillingProcessFailedTasklet implements Tasklet {

    private static final FormatLogger logger = new FormatLogger(BillingProcessFailedTasklet.class);

    private IBillingProcessSessionBean local;
    private BillingProcessRunBL billingProcessRunBL;

    private Integer entityId;

    /**
     * Marks process as failed, Send notification of failure along with total numer of users failed
     * and updates total number of invoices.
     */
    @Override
    public RepeatStatus execute(StepContribution stepContr, ChunkContext chunkCont)
            throws Exception {
        logger.debug("Entering execute(StepContribution, ChunkContext)");

        ExecutionContext jobContext = chunkCont.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        Integer billingProcessId = jobContext.getInt(ServerConstants.JOBCONTEXT_BILLING_PROCESS_ID_KEY);
        Integer usersFailed = jobContext.getInt(ServerConstants.JOBCONTEXT_TOTAL_USERS_FAILED_KEY);

        local
                = (IBillingProcessSessionBean) Context.getBean(Context.Name.BILLING_PROCESS_SESSION);

        logger.debug("Getting process by billing process id # " + billingProcessId);
        billingProcessRunBL = new BillingProcessRunBL();
        billingProcessRunBL.setProcess(billingProcessId);

        logger.debug("Setting process # " + billingProcessId + " as failed");
        local.updateProcessRunFinished(
                billingProcessId, ServerConstants.PROCESS_RUN_STATUS_FAILED);
        billingProcessRunBL.notifyProcessRunFailure(entityId, usersFailed);

        BillingProcessRunBL runBL = new BillingProcessRunBL();
        runBL.setProcess(billingProcessId);
        // update the totals
        runBL.updateTotals(billingProcessId);

        logger.debug("**** ENTITY %s DONE. Failed users = %s", entityId, usersFailed);

        return RepeatStatus.FINISHED;
    }

    /* Setters */
    public void setEntityId(String entityId) {
        this.entityId = Integer.parseInt(entityId);
    }
}

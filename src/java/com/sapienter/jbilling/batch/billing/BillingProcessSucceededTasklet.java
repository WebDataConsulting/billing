package com.sapienter.jbilling.batch.billing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.PaperInvoiceBatchBL;
import com.sapienter.jbilling.server.payment.event.EndProcessPaymentEvent;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.PaperInvoiceBatchDTO;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


//import com.sapienter.jbilling.client.util.Constants;

/**
 * @author Khobab
 */
public class BillingProcessSucceededTasklet implements Tasklet, InitializingBean {

    private static final FormatLogger logger = new FormatLogger(BillingProcessSucceededTasklet.class);

    private IBillingProcessSessionBean local;
    private ConfigurationBL conf;

    private Integer entityId;
    private boolean review;
    private Date billingDate;
    private Integer periodType;
    private Integer periodValue;

    /**
     * Returns the maximum value that Month if if period unit monthly and lastDayOfMonth flag is true,
     * For example, if the date of this instance is February 1, 2004 the actual maximum value of the DAY_OF_MONTH field
     * is 29 because 2004 is a leap year, and if the date of this instance is February 1, 2005, it's 28.
     *
     * @param billingDate
     * @return
     */
    public static Date calculateNextRunDateForEndOfMonth(Date billingDate) {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(billingDate);
        Integer dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) <= dayOfMonth) {
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
        } else {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        }

        return cal.getTime();
    }

    /**
     * Set billing process as Successful, marks parallel payment processing finished and sets next billing date.
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
            throws Exception {
        logger.debug("Entering execute(StepContribution, ChunkContext)");

        ExecutionContext jobContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        Integer billingProcessId = 0, totalInvoices = 0;
        if (jobContext.containsKey(ServerConstants.JOBCONTEXT_BILLING_PROCESS_ID_KEY)) {
            billingProcessId = jobContext.getInt(ServerConstants.JOBCONTEXT_BILLING_PROCESS_ID_KEY);
        }
        if (jobContext.containsKey(ServerConstants.JOBCONTEXT_TOTAL_INVOICES_KEY)) {
            totalInvoices = jobContext.getInt(ServerConstants.JOBCONTEXT_TOTAL_INVOICES_KEY);
        }

        logger.debug("billingProcessId:" + billingProcessId + " ,totalInvoices:" + totalInvoices);

        // only if all got well processed
        // if some of the invoices were paper invoices, a new file with all
        // of them has to be generated
        BillingProcessDAS bpDas = new BillingProcessDAS();
        try {
            //ref #4800. The process entity is this session does not
            //have the changes made with the paper invoice
            //notification about the paper invoice batch
            //so we first evict the old object and reattach
            //new one to get the information about the batch process
            bpDas.detach(new BillingProcessBL(billingProcessId).getEntity());
            BillingProcessBL process = new BillingProcessBL(billingProcessId);
            PaperInvoiceBatchDTO batch = process.getEntity().getPaperInvoiceBatch();
            if (totalInvoices > 0 && batch != null) {
                PaperInvoiceBatchBL batchBl = new PaperInvoiceBatchBL(batch);

                logger.debug("Compiling Invoices for entityId:" + entityId);
                batchBl.compileInvoiceFilesForProcess(entityId);

                logger.debug("Sending Emails");
                // send the file as an attachment
                batchBl.sendEmail();
            }
        } catch (Exception e) {
            logger.error("Error generetaing batch file", e);
        }

        Integer processRunId = local.updateProcessRunFinished(
                billingProcessId, ServerConstants.PROCESS_RUN_STATUS_SUCCESS);

        if (!review) {
            // the payment processing is happening in parallel
            // this event marks the end of it
            EndProcessPaymentEvent event = new EndProcessPaymentEvent(processRunId, entityId);
            EventManager.process(event);
            // and finally the next run date in the config
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(billingDate);

            /**
             * 	  Calculate billing next run date as per billing configuration next run date and billing period.
             * A. In first if calculation of Next Run Date in case Semi Monthly period in calculateNextRunDateForSemiMonthly method.
             * B. In second else if calculation of Next Run Date in case Monthly period and lastDayOfMonth flag is true.
             * 	  Calculation in calculateNextRunDateForEndOfMonth method.
             * C. In last else calculate Next Run Date as per other period and period value 1.
             */
            BillingProcessConfigurationDTO billingProcesssConfig = conf.getDTO();
            Integer periodUnit = billingProcesssConfig.getPeriodUnit().getId();
            if (CalendarUtils.isSemiMonthlyPeriod(periodUnit)) {
                cal.setTime(CalendarUtils.addSemiMonthyPeriod(billingDate));
            } else if (periodUnit.compareTo(ServerConstants.PERIOD_UNIT_MONTH) == 0
                    && billingProcesssConfig.getLastDayOfMonth().equals(true)) {
                cal.setTime(calculateNextRunDateForEndOfMonth(billingDate));
            } else {
                cal.add(MapPeriodToCalendar.map(periodUnit), new Integer(1));
            }

            conf.getEntity().setNextRunDate(cal.getTime());
            logger.debug("Updated run date to %s", cal.getTime());
        }


        return RepeatStatus.FINISHED;
    }

    /*
     * setters
     */
    public void setEntityId(String entityId) {
        this.entityId = Integer.parseInt(entityId);
    }

    public void setPeriodType(String periodType) {
        this.periodType = Integer.parseInt(periodType);
    }

    public void setPeriodValue(String periodValue) {
        this.periodValue = Integer.parseInt(periodValue);
    }

    public void setBillingDate(Date billingDate) {
        this.billingDate = billingDate;
    }

    public void setReview(String review) {
        this.review = Integer.parseInt(review) == 1;
    }

    /**
     * This method runs when bean is initialized to set some values that will be used in bean methods
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        logger.debug("Entering afterPropertiesSet()");

        conf = new ConfigurationBL(entityId);
        local
                = (IBillingProcessSessionBean) Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
    }
}

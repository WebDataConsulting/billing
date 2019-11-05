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

package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.billing.task.BillingProcessTask;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.db.*;
import com.sapienter.jbilling.server.process.event.NoNewInvoiceEvent;
import com.sapienter.jbilling.server.process.task.IScheduledTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * This is the session facade for the all the billing process and its 
 * related services. 
 */
@Transactional( propagation = Propagation.REQUIRED )
public class BillingProcessSessionBean implements IBillingProcessSessionBean {

    private static final FormatLogger LOG = new FormatLogger(BillingProcessSessionBean.class);

    private static final ConcurrentMap<Integer, Boolean> running = new ConcurrentHashMap<Integer, Boolean>();

    private static final ConcurrentMap<Integer, Boolean> ageingRunning = new ConcurrentHashMap<Integer, Boolean>();

    /**
     * Gets the invoices for the specified process id. The returned collection
     * is of extended dtos (InvoiceDTO).
     * @param processId
     * @return A collection of InvoiceDTO objects
     * @throws SessionInternalError
     */
    public Collection getGeneratedInvoices(Integer processId) {
        // find the billing_process home interface
        BillingProcessDAS processHome = new BillingProcessDAS();
        Collection<InvoiceDTO> invoices =  new InvoiceDAS().findByProcess(processHome.find(processId));
        
        for (InvoiceDTO invoice : invoices) {
            invoice.getOrderProcesses().iterator().next().getId(); // it is a touch
        }
        return invoices;
    }
    
    /**
     * @param entityId
     * @param languageId
     * @return
     * @throws SessionInternalError
     */
    public AgeingDTOEx[] getAgeingSteps(Integer entityId, 
            Integer executorLanguageId, Integer languageId) 
            throws SessionInternalError {
        try {
            AgeingBL ageing = new AgeingBL();
            return ageing.getSteps(entityId, executorLanguageId, languageId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }
    
    /**
     * @param entityId
     * @param languageId
     * @param steps
     * @throws SessionInternalError
     */
    @Transactional( propagation = Propagation.REQUIRES_NEW)
    public void setAgeingSteps(Integer entityId, Integer languageId, 
            AgeingDTOEx[] steps) 
            throws SessionInternalError {
        try {
            AgeingBL ageing = new AgeingBL();
            ageing.setSteps(entityId, languageId, steps);
        } catch (SessionInternalError e) {
            throw e;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void generateReview(Integer entityId, Date billingDate,
            Integer periodType, Integer periodValue) throws SessionInternalError {

        LOG.debug("Generating review entity %s", entityId);
        IBillingProcessSessionBean local = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
        local.processEntity(entityId, billingDate, periodType, periodValue, true);
        // let know this entity that a new review is now pending approval

        try {
            String params[] = new String [] { entityId.toString() };
            NotificationBL.sendSapienterEmail(entityId, "process.new_review", null, params);
        } catch (Exception e) {
            LOG.warn("Exception sending email about a generateReview run to entity", e);
        }
    }

    /**
     * Creates the billing process record. This has to be done in its own
     * transaction (thus, in its own method), so new invoices can link to
     * an existing process record in the db.
     */
    public BillingProcessDTO createProcessRecord(Integer entityId, Date billingDate,
            Integer periodType, Integer periodValue, boolean isReview,
            Integer retries) 
            throws  SQLException {
        BillingProcessBL bpBL = new BillingProcessBL();
        BillingProcessDTO dto = new BillingProcessDTO();

        // process can't leave reviews behind, and a review has to 
        // delete the previous one too            
        bpBL.purgeReview(entityId, isReview);
        
        //I need to find the entity
        CompanyDAS comDas = new CompanyDAS();
        CompanyDTO company = comDas.find(entityId);
        //I need to find the PeriodUnit
        PeriodUnitDAS periodDas = new PeriodUnitDAS();
        PeriodUnitDTO period = periodDas.find(periodType);
        
        dto.setEntity(company);
        dto.setBillingDate(Util.truncateDate(billingDate));
        dto.setPeriodUnit(period);
        dto.setPeriodValue(periodValue);
        dto.setIsReview(isReview ? new Integer(1) : new Integer(0));
        dto.setRetriesToDo(retries);
        
        bpBL.findOrCreate(dto);
        return bpBL.getEntity();
    }

    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public Integer createRetryRun(Integer processId) {
        BillingProcessBL process = new BillingProcessBL(processId);
        // create a new run record
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        runBL.create(process.getEntity(), process.getEntity().getBillingDate());
        LOG.debug("created process run %s", runBL.getEntity().getId());

        return runBL.getEntity().getId();
    }
    
    @Transactional( propagation = Propagation.NOT_SUPPORTED )
    public void processEntity(Integer entityId, Date billingDate, Integer periodType, Integer periodValue,
                              boolean isReview) throws SessionInternalError {    
    	LOG.debug("Entering processEntity(entityId: %s, billingDate: %s, periodType: %s, periodValue: %s)", entityId, billingDate, periodValue, periodValue);
    	if (entityId == null || billingDate == null) {
            throw new SessionInternalError("entityId and billingDate can't be null");
        }
        
    	JobLauncher launcher = (JobLauncher) Context.getBean(Context.Name.BATCH_SYNC_JOB_LAUNCHER);
        LOG.debug("Loaded job launcher bean # %s", launcher);
        
        Job job = (Job) Context.getBean(Context.Name.BATCH_JOB_GENERATE_INVOICES);
        LOG.debug("Loaded job bean # %s", job.toString());
        
        JobParametersBuilder paramBuilder = new JobParametersBuilder().addString(ServerConstants.BATCH_JOB_PARAM_ENTITY_ID, entityId.toString())
    									.addDate(ServerConstants.BATCH_JOB_PARAM_BILLING_DATE, billingDate)
    									.addString(ServerConstants.BATCH_JOB_PARAM_PERIOD_VALUE, periodValue.toString())
    									.addString(ServerConstants.BATCH_JOB_PARAM_PERIOD_TYPE, periodType.toString())
    									.addString(ServerConstants.BATCH_JOB_PARAM_REVIEW, (isReview ? "1" : "0"));
        if(isReview) {
        	paramBuilder.addDate(ServerConstants.BATCH_JOB_PARAM_UNIQUE, new Date());
        }
        JobParameters jobParameters = paramBuilder.toJobParameters();
        
        try {
			launcher.run(job, jobParameters);
		} catch (Exception e) {
			LOG.error("Job # %s with parameters # %s colud not be launched:", job.getName(), jobParameters.toString(), e);
		}
        LOG.debug("Job for entity id # %s has finished successfully", entityId);
    }

	/**
     * This method process a payment synchronously. It is a wrapper to the payment processing  
     * so it runs in its own transaction
     */
    public void processPayment(Integer processId, Integer runId, Integer invoiceId) {
    	LOG.debug("Entering processPayment()");
        try {
            BillingProcessBL bl = new BillingProcessBL();
            bl.generatePayment(processId, runId, invoiceId);
        } catch (Exception e) {
            LOG.error("Exception processing a payment ", e);
        }
    }

    /**
     * This method marks the end of payment processing. It is a wrapper
     * so it runs in its own transaction
     */
    public void endPayments(Integer runId) {
    	LOG.debug("Entering endPayment()");
        BillingProcessRunBL run = new BillingProcessRunBL(runId);
        run.updatePaymentsFinished();
        // update the totals
        run.updateTotals(run.getEntity().getBillingProcess().getId());
        run.updatePaymentsStatistic(run.getEntity().getId());
        LOG.debug("Leaving endPayment()");
    }

    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public boolean verifyIsRetry(Integer processId, int retryDays, Date today) {
        GregorianCalendar cal = new GregorianCalendar();
        // find the last run date
        BillingProcessBL process = new BillingProcessBL(processId);
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        ProcessRunDTO lastRun = Collections.max(process.getEntity().getProcessRuns(), runBL.new DateComparator());
        cal.setTime(Util.truncateDate(lastRun.getStarted()));
        LOG.debug("Retry evaluation lastrun = %s", cal.getTime());
        cal.add(GregorianCalendar.DAY_OF_MONTH, retryDays);
        LOG.debug("Added days = %s today = %s", cal.getTime(), today);
        if (!cal.getTime().after(today)) {
            return true;
        } else {
            return false;
        }
    }    

    
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public void email(Integer entityId, final Integer invoiceId, final Integer processId) {
        try {
            InvoiceBL invoice = new InvoiceBL(invoiceId);
            Integer userId = invoice.getEntity().getBaseUser().getUserId();
            LOG.debug("email for user %s invoice %s", userId, invoiceId);
            UserBL userBL = new UserBL(userId);
            userBL.getEntity();

            // last but not least, let this user know about his/her new invoice.
            NotificationBL notif = new NotificationBL();
            try {
                List<MessageDTO> invoiceMessages= notif.getInvoiceMessages(entityId, processId,
                                                    userBL.getEntity().getLanguageIdField(), invoice.getEntity());
                INotificationSessionBean notificationSessionBean = Context.getBean(Context.Name.NOTIFICATION_SESSION);
                invoiceMessages.forEach( msg -> {
                    try {
                        notificationSessionBean.notify(userId, msg);
                    } catch (Exception e) {
                        //handle failure to notify
                        if ( MessageDTO.TYPE_INVOICE_EMAIL.equals( msg.getTypeId() ) ) {
                            try {
                                String params[] = new String [] { processId.toString(), invoiceId.toString() };
                                NotificationBL.sendSapienterEmail(Util.getSysProp("process.failed.email.to"), entityId, "process.failed.new.invoice", null, params);
                            } catch (Exception ex) {
                                LOG.warn("Exception sending email to entity", ex);
                            }
                        }
                    }
                } );
            } catch (NotificationNotFoundException e) {
                LOG.warn("Invoice message not defined for entity %s Invoice email not sent", entityId);
            } catch (Exception e) {
                LOG.error(e.getMessage());
                LOG.warn("Email notification of Invoice " + invoiceId + " failed for user " + userId);
            }
        } catch (Exception e) {
            LOG.error("sending email and processing payment", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }
    
    /**
     * Process a user, generating the invoice/s
     * @param userId
     */
    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public Integer[] processUser(Integer processId, Date billingDate, Integer userId, boolean isReview, boolean onlyRecurring) {
        int invoiceGenerated = 0;
        Integer[] retValue = null;

        try {
            UserBL user = new UserBL(userId);
            
            if (!user.canInvoice()) {
                LOG.debug("Skipping non-customer / subaccount user %s", userId);
                return new Integer[0];
            }
            
            if (!user.isBillable(billingDate)) {
            	LOG.debug("Skipping non billable user %s", userId);
            	return new Integer[0];
            }

            BillingProcessBL processBL = new BillingProcessBL(processId);
            BillingProcessDTO process = processBL.getEntity();
            
            // payment and notification only needed if this user gets a 
            // new invoice.
            InvoiceDTO newInvoices[] = processBL.generateInvoice(process, null, user.getEntity(), isReview, onlyRecurring, null);
            
            //Update Next Invoice Date of Customer.
            if (!isReview) {
            	//Update parent next invoice date.
            	updateNextInvoiceDate(user.getDto());

            	//Update childern next invoice date.
            	updateChildrenNextInvoiceDate(user.getDto());
            }
            
            if (newInvoices == null) {
                if (!isReview) {
                    NoNewInvoiceEvent event = new NoNewInvoiceEvent(
                            user.getEntityId(userId), userId, 
                            process.getBillingDate(), 
                            user.getEntity().getSubscriberStatus().getId());
                    EventManager.process(event);
                }
                return new Integer[0];
            }

            retValue = new Integer[newInvoices.length];
            for (int f = 0; f < newInvoices.length; f++) {
                retValue[f] = newInvoices[f].getId();
                invoiceGenerated++;
            }
            LOG.info("The user %s has been processed. %s invoice generated", userId, invoiceGenerated);

        } catch (Throwable e) {
            LOG.error("Exception caught when processing the user %s", userId, e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // rollback !
            return null; // the user was not processed
        }

        return retValue;
    }

    public void updateNextInvoiceDate(UserDTO user) {
        UserBL userBl = new UserBL(user.getId());
    	userBl.setCustomerNextInvoiceDate(user);
    }

    public BillingProcessDTOEx getDto(Integer processId, Integer languageId) {
        BillingProcessDTOEx retValue = null;
        
        BillingProcessBL process = new BillingProcessBL(processId);
        retValue = process.getDtoEx(languageId);
        if (retValue != null) retValue.toString(); // as a form of touch
           
        return retValue;            
    }

    public BillingProcessConfigurationDTO getConfigurationDto(Integer entityId) 
            throws SessionInternalError {
        BillingProcessConfigurationDTO retValue = null;
        
        try {
            ConfigurationBL config = new ConfigurationBL(entityId);
            retValue = config.getDTO();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }            
           
        return retValue;            
    }

    public Integer createUpdateConfiguration(Integer executorId,
            BillingProcessConfigurationDTO dto) 
            throws SessionInternalError {
        Integer retValue;
        
        try {
            LOG.debug("Updating configuration %s", dto);
            ConfigurationBL config = new ConfigurationBL();
            retValue = config.createUpdate(executorId, dto);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }            
           
        return retValue;            
    }

    public Integer getLast(Integer entityId) 
            throws SessionInternalError {
        int retValue;
        
        try {
            BillingProcessBL process = new BillingProcessBL();
            retValue = process.getLast(entityId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }            
        
        return retValue > 0 ? new Integer(retValue) : null;
    }  

    public BillingProcessDTOEx getReviewDto(Integer entityId, Integer languageId) {
        BillingProcessDTOEx dto = null;
        BillingProcessBL process = new BillingProcessBL();
        dto = process.getReviewDTO(entityId, languageId);
        if (dto != null) dto.toString(); // as a touch
        
        return dto;           
    }    

    public BillingProcessConfigurationDTO setReviewApproval(
            Integer executorId, Integer entityId, 
            Boolean flag) throws SessionInternalError {
        try {
            LOG.debug("Setting review approval : %s", flag);
            ConfigurationBL config = new ConfigurationBL(entityId);
            config.setReviewApproval(executorId, flag.booleanValue());
            return getConfigurationDto(entityId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public boolean trigger(Date pToday, Integer entityId) throws SessionInternalError {

        running.putIfAbsent(entityId, false);
        if (!running.replace(entityId, false, true)) {
            LOG.warn("Failed to trigger billing process at %s, another process is already running.", pToday.getTime());
            return false;
        }
        LOG.debug("Billing trigger for %s entity %s", pToday, entityId);

        try {
            Date today = Util.truncateDate(pToday);
            processEntity(entityId, today);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        } finally {
            running.put(entityId, false);
        }
        return true;
    }

    private BillingProcessTask findOrCreateBillingProcessTask (Integer entityId) throws PluggableTaskException {

        // find the billing process task
        PluggableTaskManager<IScheduledTask> taskManager =
                new PluggableTaskManager<IScheduledTask>(entityId,
                ServerConstants.PLUGGABLE_TASK_SCHEDULED);

        for (IScheduledTask task = taskManager.getNextClass(); task != null; task = taskManager.getNextClass()) {
            if (task instanceof BillingProcessTask) {
                return (BillingProcessTask) task;
            }
        }
        return new BillingProcessTask();
    }

    private void processEntity (Integer entityId, Date currentRunDate) throws Exception {

        BillingProcessBL processBL = new BillingProcessBL();

        BillingProcessConfigurationDTO config = new ConfigurationBL(entityId).getDTO();
        Integer periodUnitId = config.getPeriodUnit().getId();
        Integer periodValue = new Integer(1);

        Date nextRunDate = config.getNextRunDate();
        boolean isReviewRequired = config.getGenerateReport() == 1;

        if (! nextRunDate.after(currentRunDate)) {
            EventLogger eLogger = EventLogger.getInstance();
            LOG.debug("A process has to be done for entity %s", entityId);

            // check that: the configuration requires a review
            // AND, there is no partial run already there (failed)
            if (isReviewRequired && new BillingProcessDAS().isPresent(entityId, 0, nextRunDate) == null) {
                // a review had to be done for the run to go ahead
                if ( ServerConstants.REVIEW_STATUS_DISAPPROVED.equals(config.getReviewStatus())
                        || !( processBL.isReviewPresent(entityId) )
                    ) {  // review wasn't generated

                    if ( ServerConstants.REVIEW_STATUS_DISAPPROVED.equals(config.getReviewStatus()) ) {
                        LOG.debug("The process should run, but the review has been disapproved");
                    } else {
                        LOG.warn("Review is required but not present for entity %s", entityId);
                        eLogger.warning(entityId, null, config.getId(),
                                EventLogger.MODULE_BILLING_PROCESS,
                                EventLogger.BILLING_REVIEW_NOT_GENERATED,
                                ServerConstants.TABLE_BILLING_PROCESS_CONFIGURATION);
                    }

                    generateReview(entityId, nextRunDate, periodUnitId, periodValue);

                } else if (ServerConstants.REVIEW_STATUS_GENERATED.equals(config.getReviewStatus())) {
                    // the review has to be reviewed yet
                    int hourOfDay = new GregorianCalendar().get(GregorianCalendar.HOUR_OF_DAY);
                    LOG.warn("Review is required but is not approved. Entity %s hour is %s", entityId, hourOfDay);

                    eLogger.warning(entityId, null, config.getId(),
                            EventLogger.MODULE_BILLING_PROCESS,
                            EventLogger.BILLING_REVIEW_NOT_APPROVED,
                            ServerConstants.TABLE_BILLING_PROCESS_CONFIGURATION);
                    try {
                        // only once per day please
                        if (hourOfDay < 1) {
                            String params[] = new String[]{entityId.toString()};
                            NotificationBL.sendSapienterEmail(entityId, "process.review_waiting", null, params);
                        }
                    } catch (Exception e) {
                        LOG.warn("Exception sending an entity email", e);
                    }
                }
            } else {
                LOG.debug("There should be a real run today");
                IBillingProcessSessionBean local = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
                local.processEntity(entityId, nextRunDate, periodUnitId, periodValue, false);
            }

        } else {
            // no run, may be then a review generation
            LOG.debug("No run was scheduled. Next run on %s", nextRunDate);
            if (isReviewRequired) {
                Date reviewDate = new DateTime(nextRunDate).minusDays(config.getDaysForReport()).toDate();
                if (! reviewDate.after(currentRunDate)) {
                    if (!processBL.isReviewPresent(entityId)
                            || ServerConstants.REVIEW_STATUS_DISAPPROVED.equals(config.getReviewStatus())) {
                        LOG.debug("Review is absent or disapproved. Regenerating.");
                        generateReview(entityId, nextRunDate, periodUnitId, periodValue);
                    }
                }
            }
        } // else (no run)
    }

    /**
     * @return the id of the invoice generated
     */
    public InvoiceDTO generateInvoice(Integer orderId, Integer invoiceId, Integer languageId, Integer executorUserId)
            throws SessionInternalError {
        
        try {
            BillingProcessBL process = new BillingProcessBL();
            InvoiceDTO invoice = process.generateInvoice(orderId, invoiceId, executorUserId);

            if (null != invoice) {
                invoice.touch();
            } 

            return invoice;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        } 
    }

    @Transactional( propagation = Propagation.NOT_SUPPORTED )
    @Override
    public void reviewUsersStatus(Integer entityId, Date today) throws SessionInternalError {

        ageingRunning.putIfAbsent(entityId, Boolean.FALSE);

        if (ageingRunning.get(entityId)) {
            LOG.warn("Failed to trigger ageing review process at %s, another process is already running.", today);
            return;

        } else {
            ageingRunning.put(entityId, Boolean.TRUE);
        }
        
        JobLauncher launcher = (JobLauncher) Context.getBean(Context.Name.BATCH_SYNC_JOB_LAUNCHER);
        LOG.debug("Loaded job launcher bean # %s", launcher);
        
        Job job = (Job) Context.getBean(Context.Name.BATCH_JOB_AGEING_PROCESS);
        LOG.debug("Loaded job bean # %s", job.toString());
        
        JobParameters jobParameters = new JobParametersBuilder().addString(ServerConstants.BATCH_JOB_PARAM_ENTITY_ID, entityId.toString())
    									.addDate(ServerConstants.BATCH_JOB_PARAM_AGEING_DATE, today)
    									// TODO: following parameter was added to make ageing job unique each time
    									.addDate(ServerConstants.BATCH_JOB_PARAM_UNIQUE, new Date())
    									.toJobParameters();
        try {
			launcher.run(job, jobParameters);
		} catch (Exception e) {
			LOG.error("Job # %s with parameters # %s colud not be launched:", job.getName(), jobParameters.toString(), e);	
		}
        
        ageingRunning.put(entityId, Boolean.FALSE);
        LOG.debug("Job for entity id # %s has finished successfully", entityId);
    }

    public List<InvoiceDTO> reviewUserStatus(Integer entityId, Integer userId, Date today) {
            LOG.debug("Trying to review user %s for date %s", userId, today);
            AgeingBL age = new AgeingBL();
            return age.reviewUserForAgeing(entityId, userId, today);
    }

    /**
     * Update status of BillingProcessRun in new transaction
     * for accessing updated entity from other thread
     * @param billingProcessId id of billing process for searching ProcessRun
     * @return id of updated ProcessRunDTO
     */
    public Integer updateProcessRunFinished(Integer billingProcessId, Integer processRunStatusId) {
    	LOG.debug("Entering updateRunFinished()");
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        runBL.setProcess(billingProcessId);
        runBL.updateFinished(processRunStatusId);
        LOG.debug("Leaving updateRunFinished()");
        return runBL.getEntity().getId();
    }

    public Integer addProcessRunUser(Integer billingProcessId, Integer userId, Integer status) {
    	LOG.debug("Entering addProcessRunUser()");
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        runBL.setProcess(billingProcessId);
        return runBL.addProcessRunUser(userId, status).getId();
    }

    /**
     * Returns true if the Billing Process is running.
     * @param entityId
     */
    public boolean isBillingRunning(Integer entityId) {
        if(entityId==null)
            return false;

        running.putIfAbsent(entityId, false);
    	return running.get(entityId);
    }

    public ProcessStatusWS getBillingProcessStatus(Integer entityId) {
        BillingProcessRunBL runBL = new BillingProcessRunBL();
        return runBL.getBillingProcessStatus(entityId);
    }
    
    public boolean isAgeingProcessRunning(Integer entityId) {
        return ageingRunning.getOrDefault(entityId, Boolean.FALSE);
    }

    public ProcessStatusWS getAgeingProcessStatus(Integer entityId) {
        ProcessStatusWS result = new ProcessStatusWS();
        if (isAgeingProcessRunning(entityId)) {
            result.setState(ProcessStatusWS.State.RUNNING);
        } else {
            result.setState(ProcessStatusWS.State.FINISHED);
        }
        return result;
    }
    
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
     * To Update Child Accounts Next Invoice Date
     *
     * @param userDto
     */
    public void updateChildrenNextInvoiceDate(UserDTO userDto) {
    	
    	Iterator subAccountsIt = null;
        if (userDto.getCustomer().getIsParent() != null &&
        		userDto.getCustomer().getIsParent().intValue() == 1) {
            UserBL parent = new UserBL(userDto.getUserId());
            //update child next invoice date
            updateChildNextInvoiceDate(parent.getEntity().getCustomer().getChildren(), parent);
        }
    }
    
    /**
     * This function updates the next invoice date of all customers in a hierarchy of parent - child and further children relationship, 
     * in a recursive manner till there is no customer left out from the hierarchy." Also, sub-accounts next invoice date update
     * happen for every sub account that does not have invoice if child check box checked and parent and child  billing cycle are same.
     * @param subAccountsIt
     * @param user
     */
    public void updateChildNextInvoiceDate(Collection<CustomerDTO> subAccountsIt, UserBL user) {
    	
    	MainSubscriptionDTO parentMainSubscription = user.getDto().getCustomer().getMainSubscription();
        Integer parentBillingCycleUnit = parentMainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
        Integer parentBillingCycleValue = parentMainSubscription.getSubscriptionPeriod().getValue();
        
    	if ( CollectionUtils.isNotEmpty(subAccountsIt) ) {
            for (CustomerDTO customer: subAccountsIt) {

                MainSubscriptionDTO childMainSubscription = customer.getBaseUser().getCustomer().getMainSubscription();
                Integer childBillingCycleUnit = childMainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
                Integer childBillingCycleValue = childMainSubscription.getSubscriptionPeriod().getValue();
                
                if ((customer.getInvoiceChild() == null || customer.getInvoiceChild().intValue() == 0) && 
                		(parentBillingCycleUnit.equals(childBillingCycleUnit) && parentBillingCycleValue.equals(childBillingCycleValue))) {
                	//update user next invoice date
                	updateNextInvoiceDate(customer.getBaseUser());
                }
                if (customer.getIsParent() != null &&
                		customer.getIsParent().intValue() == 1) {
                    UserBL parent = new UserBL(customer.getBaseUser().getUserId());
                    if (checkIfUserhasAnychildren(parent)) {
                    	Collection<CustomerDTO> subAccounts = parent.getEntity().getCustomer().getChildren();
                    	updateChildNextInvoiceDate(subAccounts, parent); // Recursive function
                    }
                }
            }
		}
	}

    public boolean checkIfUserhasAnychildren(UserBL parent) {
        return (parent != null && parent.getEntity() != null
                && parent.getEntity().getCustomer().getChildren() != null
                && !parent.getEntity().getCustomer().getChildren().isEmpty());
    }

}

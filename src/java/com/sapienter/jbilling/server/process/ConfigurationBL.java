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



;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDAS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProcessRunDAS;
import com.sapienter.jbilling.server.process.db.ProcessRunDTO;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.audit.EventLogger;

public class ConfigurationBL {
    private BillingProcessConfigurationDAS configurationDas = null;
    private BillingProcessConfigurationDTO configuration = null;
    private EventLogger eLogger = null;
    private static final FormatLogger LOG = new FormatLogger(ConfigurationBL.class);

    public ConfigurationBL(Integer entityId)  {
        init();
        configuration = configurationDas.findByEntity(new CompanyDAS().find(entityId));
    }

    public ConfigurationBL() {
        init();
    }

    public ConfigurationBL(BillingProcessConfigurationDTO cfg) {
        init();
        configuration = cfg;
    }

    private void init() {
        eLogger = EventLogger.getInstance();
        configurationDas = new BillingProcessConfigurationDAS();

    }

    public BillingProcessConfigurationDTO getEntity() {
        return configuration;
    }

    public void set(Integer entityId) {
        configuration = configurationDas.findByEntity(new CompanyDAS().find(entityId));
    }

    public Integer createUpdate(Integer executorId,
            BillingProcessConfigurationDTO dto) {
        configuration = configurationDas.findByEntity(dto.getEntity());
        if (configuration != null) {

            if (!configuration.getGenerateReport().equals(
                    dto.getGenerateReport())) {
                eLogger.audit(executorId, null,
                        ServerConstants.TABLE_BILLING_PROCESS_CONFIGURATION,
                        configuration.getId(),
                        EventLogger.MODULE_BILLING_PROCESS,
                        EventLogger.ROW_UPDATED, configuration
                                .getGenerateReport(), null, null);
                configuration.setGenerateReport(dto.getGenerateReport());
                configuration
                        .setReviewStatus(dto.getGenerateReport() == 1 ? ServerConstants.REVIEW_STATUS_GENERATED
                                : ServerConstants.REVIEW_STATUS_APPROVED);
            } else {
                eLogger.audit(executorId, null,
                        ServerConstants.TABLE_BILLING_PROCESS_CONFIGURATION,
                        configuration.getId(),
                        EventLogger.MODULE_BILLING_PROCESS,
                        EventLogger.ROW_UPDATED, null, null, null);
            }

            configuration.setNextRunDate(dto.getNextRunDate());
        } else {
            configuration = configurationDas.create(dto.getEntity(), dto
                    .getNextRunDate(), dto.getGenerateReport());
        }

        configuration.setDaysForReport(dto.getDaysForReport());
        configuration.setDaysForRetry(dto.getDaysForRetry());
        configuration.setPeriodUnit(dto.getPeriodUnit());
        configuration.setDueDateUnitId(dto.getDueDateUnitId());
        configuration.setDueDateValue(dto.getDueDateValue());
        configuration.setDfFm(dto.getDfFm());
        configuration.setOnlyRecurring(dto.getOnlyRecurring());
        configuration.setInvoiceDateProcess(dto.getInvoiceDateProcess());
        configuration.setAutoPaymentApplication(dto.getAutoPaymentApplication());
        configuration.setMaximumPeriods(dto.getMaximumPeriods());
        configuration.setLastDayOfMonth(dto.getLastDayOfMonth());
        configuration.setProratingType(dto.getProratingType());

        return configuration.getId();
    }

    public BillingProcessConfigurationDTO getDTO() {
        BillingProcessConfigurationDTO dto = new BillingProcessConfigurationDTO();

        dto.setDaysForReport(configuration.getDaysForReport());
        dto.setDaysForRetry(configuration.getDaysForRetry());
        dto.setEntity(configuration.getEntity());
        dto.setGenerateReport(configuration.getGenerateReport());
        dto.setId(configuration.getId());
        dto.setNextRunDate(configuration.getNextRunDate());
        dto.setPeriodUnit(configuration.getPeriodUnit());
        dto.setReviewStatus(configuration.getReviewStatus());
        dto.setDueDateUnitId(configuration.getDueDateUnitId());
        dto.setDueDateValue(configuration.getDueDateValue());
        dto.setDfFm(configuration.getDfFm());
        dto.setOnlyRecurring(configuration.getOnlyRecurring());
        dto.setInvoiceDateProcess(configuration.getInvoiceDateProcess());
        dto.setMaximumPeriods(configuration.getMaximumPeriods());
        dto.setAutoPaymentApplication(configuration.getAutoPaymentApplication());
        dto.setLastDayOfMonth(configuration.getLastDayOfMonth());
        dto.setProratingType(configuration.getProratingType());

        return dto;
    }

    public void setReviewApproval(Integer executorId, boolean flag) {

        eLogger.audit(executorId, null,
                ServerConstants.TABLE_BILLING_PROCESS_CONFIGURATION, configuration
                        .getId(), EventLogger.MODULE_BILLING_PROCESS,
                EventLogger.ROW_UPDATED, configuration.getReviewStatus(), null,
                null);
        configuration.setReviewStatus(flag ? ServerConstants.REVIEW_STATUS_APPROVED
                : ServerConstants.REVIEW_STATUS_DISAPPROVED);

    }

    /**
     * Convert a given BillingProcessConfigurationDTO into a BillingProcessConfigurationWS web-service object.
     *
     * @param dto dto to convert
     * @return converted web-service object
     */
    public static BillingProcessConfigurationWS getWS(BillingProcessConfigurationDTO dto) {
		if (null == dto)
			return null;

		BillingProcessConfigurationWS ws = new BillingProcessConfigurationWS();
		ws.setId(dto.getId());
		ws.setPeriodUnitId(dto.getPeriodUnit() != null ? dto.getPeriodUnit()
				.getId() : null);
		ws.setEntityId(dto.getEntity() != null ? dto.getEntity().getId() : null);
		ws.setNextRunDate(dto.getNextRunDate());
		ws.setGenerateReport(dto.getGenerateReport());
		ws.setRetries(dto.getRetries());
		ws.setDaysForRetry(dto.getDaysForRetry());
		ws.setDaysForReport(dto.getDaysForReport());
		ws.setReviewStatus(dto.getReviewStatus());
		ws.setDueDateUnitId(dto.getDueDateUnitId());
		ws.setDueDateValue(dto.getDueDateValue());
		ws.setDfFm(dto.getDfFm());
		ws.setOnlyRecurring(dto.getOnlyRecurring());
		ws.setInvoiceDateProcess(dto.getInvoiceDateProcess());
		ws.setMaximumPeriods(dto.getMaximumPeriods());
		ws.setAutoPaymentApplication(dto.getAutoPaymentApplication());
		ws.setLastDayOfMonth(dto.getLastDayOfMonth());
		ws.setProratingType(null != dto.getProratingType() ? dto
				.getProratingType().getOptionText() : ServerConstants.BLANK_STRING);
		return ws;
    }

    /**
     * Convert a given BillingProcessConfigurationWS web-service object into a BillingProcessConfigurationDTO entity.
     *
     * The BillingProcessConfigurationWS must have an entity and period unit ID or an exception will be thrown.
     *
     * @param ws ws object to convert
     * @return converted DTO object
     * @throws SessionInternalError if required field is missing
     */
    public static BillingProcessConfigurationDTO getDTO(BillingProcessConfigurationWS ws) {
        if (ws != null) {

            if (ws.getEntityId() == null)
                    throw new SessionInternalError("BillingProcessConfigurationDTO must have an entity id.");
            
            if (ws.getPeriodUnitId() == null)
            	throw new SessionInternalError("BillingProcessConfigurationDTO must have a period unit id.");
            
            // billing process entity
            CompanyDTO entity = new EntityBL(ws.getEntityId()).getEntity();
            
            // billing process period unit
            PeriodUnitDTO periodUnit = new PeriodUnitDAS().find(ws.getPeriodUnitId());
            
            return new BillingProcessConfigurationDTO(ws, entity, periodUnit);
        }
        return null;
    }

    public static boolean validate(BillingProcessConfigurationWS ws) {
    	boolean retValue = true;
 
    	//validate nextRunDate - Unique if there is already a successful run for that date 
    	//(if a process failed, it is fine to run it again)
    	//TODO Should I Util.truncateDate before using the ws.nextRunDate?
    	BillingProcessDTO billingProcessDTO=new BillingProcessDAS().isPresent(ws.getEntityId(), 0, ws.getNextRunDate()); 
    	if ( billingProcessDTO != null) {
    		for (ProcessRunDTO run: billingProcessDTO.getProcessRuns()) {
    			//if status is not failed i.e. for the same date, if the process is either running or finished
    			if (!ServerConstants.PROCESS_RUN_STATUS_FAILED.equals(run.getStatus().getId()) ) {
    			    LOG.error("Trying to set this configuration: %s but already has this: %s", ws, run);
    				SessionInternalError exception = new SessionInternalError(
    				        "There is already a billing process for the give date." + ws.getNextRunDate());
    	            String messages[] = new String[1];
    	            messages[0] = "BillingProcessConfigurationWS,nextRunDate,billing.configuration.error.unique.nextrundate,";
    	            exception.setErrorMessages(messages);
    	            throw exception;
    			}
    		}
    	}
    	
    	ProcessRunDTO run = new ProcessRunDAS().getLatestSuccessful(ws.getEntityId());
    	
    	//The nextRunDate must be greater than the latest successful one
    	if (run != null
            && run.getBillingProcess().getBillingDate() != null
            && !run.getBillingProcess().getBillingDate().before(ws.getNextRunDate())) {

    		LOG.error("Trying to set this configuration: %s but the it should be in the future %s", ws, run.getBillingProcess());
			SessionInternalError exception = new SessionInternalError("The new next date needs to be in the future from the last successful run");
			String messages[] = new String[1];
			messages[0] = "BillingProcessConfigurationWS,nextRunDate,"
                            + "billing.configuration.error.past.nextrundate,"
                            + run.getBillingProcess().getBillingDate();

			exception.setErrorMessages(messages);
			throw exception;
		}

    	return retValue;
    }
    
}

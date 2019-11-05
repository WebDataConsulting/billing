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
package com.sapienter.jbilling.server.user.partner;

import java.math.BigDecimal;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDAS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDTO;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDTO;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDAS;
import com.sapienter.jbilling.server.user.partner.db.InvoiceCommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionExceptionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;

public class CommissionProcessConfigurationBL {

    /**
     * Validates that the nextRunDate is greater than the last commission process run.
     * @param ws
     * @return
     */
    public static boolean validate(CommissionProcessConfigurationWS ws) throws SessionInternalError {
        //validate nextRunDate greatest than last commission process run.
        CommissionProcessRunDTO commissionProcessRun = new CommissionProcessRunDAS().findLatestByDate(new CompanyDAS().find(ws.getEntityId()));
        if(commissionProcessRun != null && commissionProcessRun.getPeriodEnd().after(ws.getNextRunDate())){
            return false;
        }

        if(ws.getPeriodValue()==null) {
            throw new SessionInternalError("Error: Invalid configuration",
                    new String[] { "partner.error.commissionProcess.invalidPeriodValue.nan" });
        }

        if(ws.getPeriodValue()<=0) {
            throw new SessionInternalError("Error: Invalid configuration",
                    new String[] { "partner.error.commissionProcess.invalidPeriodValue.negative.or.zero" });
        }

        return true;
    }

    /**
     * Convenient method to create or update the commission process configuration.
     * @param dto
     * @return
     */
    public static Integer createUpdate(CommissionProcessConfigurationDTO dto){
        CommissionProcessConfigurationDAS configurationDAS = new CommissionProcessConfigurationDAS();

        CommissionProcessConfigurationDTO configuration = configurationDAS.findByEntity(dto.getEntity());

        if(configuration != null){
            configuration.setPeriodUnit(dto.getPeriodUnit());
            configuration.setPeriodValue(dto.getPeriodValue());
            configuration.setNextRunDate(dto.getNextRunDate());
        }else{
            configuration = dto;
        }

        configurationDAS.save(configuration);

        return configuration.getId();
    }
   
    public static final  CommissionWS getCommissionWS(CommissionDTO dto) {
        //todo: add invoice commissions
    	
    	CommissionWS ws = new CommissionWS();
        ws.setId(dto.getId());
        ws.setAmount(dto.getAmount());
        ws.setType(dto.getType().name());
        ws.setPartnerId((dto.getPartner() != null) ? dto.getPartner().getId() : null);
        ws.setCommissionProcessRunId((dto.getCommissionProcessRun() != null) ? dto.getCommissionProcessRun().getId() : null);
        ws.setCurrencyId(dto.getCurrency().getId());
        ws.setOwningEntityId(getOwningEntityId(ws));
        return ws;
    }
    
    public static final CommissionDTO getDTO(CommissionWS ws){
        CommissionDTO commission = new CommissionDTO();
        commission.setId(0);
        commission.setPartner(new PartnerDAS().find(ws.getPartnerId()));
        commission.setType(CommissionType.valueOf(ws.getType()));
        commission.setCurrency(new CurrencyDAS().find(ws.getCurrencyId()));
        commission.setAmount(new BigDecimal(ws.getAmount()));
        commission.setCommissionProcessRun(new CommissionProcessRunDAS().find(ws.getCommissionProcessRunId()));
        commission.setInvoiceCommissions(new InvoiceCommissionDAS().findByCommission(commission));
        return commission;
    }
    
	private static final Integer getOwningEntityId(CommissionWS ws) {
		if (ws.getCommissionProcessRunId() != null
				&& ws.getCommissionProcessRunId() > 0) {
			return new CommissionProcessRunDAS()
					.find(ws.getCommissionProcessRunId()).getEntity().getId();
		} else {
			return null;
		}
	}
    
    
	public static final CommissionProcessConfigurationWS getCommissionProcessConfigurationWS(
													CommissionProcessConfigurationDTO dto) {
	
		CommissionProcessConfigurationWS ws = new CommissionProcessConfigurationWS();
		ws.setId(dto.getId());
		ws.setEntityId((dto.getEntity() != null) ? dto.getEntity().getId()
				: null);
		ws.setNextRunDate(dto.getNextRunDate());
		ws.setPeriodUnitId((dto.getPeriodUnit() != null) ? dto.getPeriodUnit()
				.getId() : null);
		ws.setPeriodValue(dto.getPeriodValue());
		return ws;
	}
	
	public static final CommissionProcessConfigurationDTO getDTO(CommissionProcessConfigurationWS ws){
        CommissionProcessConfigurationDTO commissionProcessConfiguration = new CommissionProcessConfigurationDTO();

        commissionProcessConfiguration.setId(0);
        commissionProcessConfiguration.setEntity(new CompanyDAS().find(ws.getEntityId()));
        commissionProcessConfiguration.setNextRunDate(ws.getNextRunDate());
        commissionProcessConfiguration.setPeriodUnit(new PeriodUnitDAS().find(ws.getPeriodUnitId()));
        commissionProcessConfiguration.setPeriodValue(ws.getPeriodValue());

        return commissionProcessConfiguration;
    }
	
	public static final CommissionProcessRunWS getCommissionProcessRunWS(
			CommissionProcessRunDTO dto) {

		CommissionProcessRunWS ws = new CommissionProcessRunWS();
		ws.setId(dto.getId());
		ws.setEntityId((dto.getEntity() != null) ? dto.getEntity().getId()
				: null);
		ws.setRunDate(dto.getRunDate());
		ws.setPeriodStart(dto.getPeriodStart());
		ws.setPeriodEnd(dto.getPeriodEnd());
		return ws;

	}

	public static final CommissionProcessRunDTO getDTO(CommissionProcessRunWS ws) {
		CommissionProcessRunDTO commissionProcessRun = new CommissionProcessRunDTO();

		commissionProcessRun.setId(0);
		commissionProcessRun.setEntity(new CompanyDAS().find(ws.getEntityId()));
		commissionProcessRun.setRunDate(ws.getRunDate());
		commissionProcessRun.setPeriodStart(ws.getPeriodStart());
		commissionProcessRun.setPeriodEnd(ws.getPeriodEnd());

		return commissionProcessRun;
	}
	
	public static final PartnerCommissionExceptionWS getPartnerCommissionExceptionWS(
			PartnerCommissionExceptionDTO dto) {

		PartnerCommissionExceptionWS ws = new PartnerCommissionExceptionWS();
		ws.setId(dto.getId());
		ws.setPartnerId(dto.getPartner().getId());
		ws.setStartDate(dto.getStartDate());
		ws.setEndDate(dto.getEndDate());
		ws.setPercentage(dto.getPercentage());
		ws.setItemId(dto.getItem().getId());
		ws.setOwningUserId(getOwningUserId(ws));
		return ws;
	}
	
	 private static final Integer getOwningUserId (PartnerCommissionExceptionWS ws) {
	        if (ws.getPartnerId() != null && ws.getPartnerId() > 0) {
	            return new PartnerBL(ws.getPartnerId()).getEntity().getBaseUser().getId();
	        } else {
	            return null;
	        }
	    }

	
	 public static final PartnerCommissionExceptionDTO getDTO(PartnerCommissionExceptionWS ws){
	        PartnerCommissionExceptionDTO commissionException = new PartnerCommissionExceptionDTO();
	        commissionException.setId(0);
	        commissionException.setPartner(new PartnerDAS().find(ws.getPartnerId()));
	        commissionException.setItem(new ItemDAS().find(ws.getItemId()));
	        commissionException.setStartDate(ws.getStartDate());
	        commissionException.setEndDate(ws.getEndDate());
	        commissionException.setPercentage(ws.getPercentageAsDecimal());
	        return commissionException;
	    }

}

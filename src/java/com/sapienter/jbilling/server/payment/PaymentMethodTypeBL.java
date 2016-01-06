package com.sapienter.jbilling.server.payment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Util;

public class PaymentMethodTypeBL {
	
	PaymentMethodTypeWS paymentMethodTypeWS = null;
	
	PaymentMethodTypeDAS pmtDAS = null;
	AccountTypeDAS accountTypeDAS = null;
	PaymentMethodTypeDTO paymentMethodType = null;
	
	public PaymentMethodTypeBL() {
		init();
	}
	
	public PaymentMethodTypeBL(PaymentMethodTypeWS paymentMethodTypeWS) {
		init();
		this.paymentMethodTypeWS = paymentMethodTypeWS;
	}
	
	public PaymentMethodTypeBL (Integer paymentMethodTypeId) {
		init();
		setPaymentMethodType(paymentMethodTypeId);
	}
	
	public void init() {
		pmtDAS = new PaymentMethodTypeDAS();
		accountTypeDAS = new AccountTypeDAS();
	}
	
	public void setPaymentMethodTypeWS (PaymentMethodTypeWS paymentMethodTypeWS) {
		this.paymentMethodTypeWS = paymentMethodTypeWS;
	}
	
	public void setPaymentMethodType (Integer id) {
		paymentMethodType = pmtDAS.find(id);
	}
	
	public void setPaymentMethodType (PaymentMethodTypeDTO dto) {
		paymentMethodType = dto;
	}
	
	/**
	 * Convert payment method template dto to ws, it also populate only entity related meta
	 * fields in ws object
	 * 
	 * @param dto	PaymentMethodTemplateDTO
	 * @param entityId	Integer
	 * @return	PaymentMethodTemplateWS
	 */
	public PaymentMethodTemplateWS getWS(PaymentMethodTemplateDTO dto, Integer entityId) {
		PaymentMethodTemplateWS ws = new PaymentMethodTemplateWS();
		
		ws.setId(dto.getId());
		ws.setTemplateName(dto.getTemplateName());
		
		Set<MetaFieldWS> metaFields = new HashSet<MetaFieldWS>();
		for(MetaField mf : dto.getPaymentTemplateMetaFields()) {
			if(mf.getEntity().getId() == entityId.intValue()) {
				metaFields.add(MetaFieldBL.getWS(mf));
			}
		}
		
		ws.setMetaFields(metaFields);
		
		return ws;
	}
	
	public PaymentMethodTypeDTO getDTO(Integer entityId) {
		
		if(paymentMethodTypeWS == null) {
			return null;
		}
		
		PaymentMethodTypeDTO dto = new PaymentMethodTypeDTO();
		
		if(paymentMethodTypeWS.getId() != null) {
			dto.setId(paymentMethodTypeWS.getId());
		}
		
		dto.setIsRecurring(paymentMethodTypeWS.getIsRecurring());
		dto.setMethodName(paymentMethodTypeWS.getMethodName());
		dto.setAllAccountType(paymentMethodTypeWS.isAllAccountType());
		if (paymentMethodTypeWS.isAllAccountType()) {
			paymentMethodTypeWS.setAccountTypes(getAllAccountTypes(entityId));
		}
		dto.setEntity(new CompanyDAS().findNow(entityId));
		dto.setPaymentMethodTemplate(new PaymentMethodTemplateDAS().findNow(paymentMethodTypeWS.getTemplateId()));
		
		// set account types
		for (Integer accountTypeId : paymentMethodTypeWS.getAccountTypes()) {
			if (accountTypeId != null) {
				dto.getAccountTypes().add(accountTypeDAS.find(accountTypeId));
			}
		}
		
		Set<MetaField> metaFields = new HashSet<MetaField>(0);
		for(MetaFieldWS metaFieldWS : paymentMethodTypeWS.getMetaFields()) {
			MetaField metaField = MetaFieldBL.getDTO(metaFieldWS,entityId);
			metaFields.add(metaField);
		}
		dto.setMetaFields(metaFields);
		
		return dto;
	}
	
	public PaymentMethodTypeDTO create(PaymentMethodTypeDTO dto) {
		saveMetaFields(dto);
		
		dto = pmtDAS.save(dto);
		pmtDAS.flush();
		pmtDAS.clear();
		
		return dto;
	}
	
	public void update(PaymentMethodTypeDTO dto){
		PaymentMethodTypeDTO persisted = pmtDAS.find(dto.getId());
		
		saveMetaFields(persisted, dto);

		persisted.getAccountTypes().clear();
		for (AccountTypeDTO accountTypeDTO : dto.getAccountTypes()) {
			persisted.getAccountTypes().add(accountTypeDTO);
		}
		persisted.setIsRecurring(dto.getIsRecurring());
		persisted.setAllAccountType(dto.isAllAccountType());
		persisted.setMethodName(dto.getMethodName());
		
		pmtDAS.save(persisted);
		pmtDAS.flush();
		pmtDAS.clear();
		
	}
	
	public boolean delete() {
		Integer count = pmtDAS.countInstrumentsAttached(paymentMethodType.getId());
        if(count < 1) {
			pmtDAS.delete(paymentMethodType);
	        pmtDAS.flush();
	        pmtDAS.clear();
	        return true;
        }
        return false;
	}
	
	private void saveMetaFields(PaymentMethodTypeDTO paymentMethodType) {

        Set<MetaField> metafields = new HashSet<MetaField>(paymentMethodType.getMetaFields());
        paymentMethodType.getMetaFields().clear();
        MetaFieldBL metaFieldBl = new MetaFieldBL();
        MetaFieldDAS mfDas = new MetaFieldDAS();
        
        for (MetaField mf : metafields) {
            if (mf.getId() == 0) {
            	paymentMethodType.getMetaFields().add(metaFieldBl.create(mf));
            } else {
                new MetaFieldBL().update(mf);
                paymentMethodType.getMetaFields().add(mfDas.find(mf.getId()));
            }
        }
    }
	
	private void saveMetaFields(PaymentMethodTypeDTO persistedPaymentMethodType,
            PaymentMethodTypeDTO paymentMethodType) {

		Map<Integer, Collection<MetaField>> diffMap = Util.calculateCollectionDifference(
				persistedPaymentMethodType.getMetaFields(),
				paymentMethodType.getMetaFields(),
				new Util.IIdentifier<MetaField>() {
		
					@Override
					public boolean evaluate(MetaField input, MetaField output) {
					    if (input.getId() != 0 && output.getId() != 0) {
					        return input.getId() == output.getId();
					    } else {
					        return input.getName().equals(output.getName());
					    }
					}
					
					@Override
					public void setIdentifier(MetaField input, MetaField output) {
					    output.setId(input.getId());
					}
				});
		
		persistedPaymentMethodType.getMetaFields().clear();
		
		for (MetaField mf : diffMap.get(-1)) {
			new MetaFieldBL().delete(mf.getId());
			persistedPaymentMethodType.getMetaFields().remove(mf);
		}
		
		for (MetaField mf : diffMap.get(0)) {
			new MetaFieldBL().update(mf);
			persistedPaymentMethodType.getMetaFields().add(new MetaFieldDAS().find(mf.getId()));
		}
		
		for (MetaField mf : diffMap.get(1)) {
			persistedPaymentMethodType.getMetaFields().add(new MetaFieldBL().create(mf));
		}
	}
	
	public PaymentMethodTypeWS getWS() {
		
		if(paymentMethodType == null) {
			return null;
		}
		
		PaymentMethodTypeWS ws = new PaymentMethodTypeWS();
		
		ws.setId(paymentMethodType.getId());
		ws.setIsRecurring(paymentMethodType.getIsRecurring());
		ws.setMethodName(paymentMethodType.getMethodName());
		ws.setTemplateId(paymentMethodType.getPaymentMethodTemplate().getId());
		ws.setAllAccountType(paymentMethodType.isAllAccountType());
		
		// set account type ids
		List<Integer> accountTypes = new ArrayList<Integer>();
		for (AccountTypeDTO accountType : paymentMethodType.getAccountTypes()) {
			accountTypes.add(accountType.getId());
		}
		ws.setAccountTypes(accountTypes);

		// set meta fields
		List<MetaFieldWS> metaFields = new ArrayList<MetaFieldWS>();
		for(MetaField mf : paymentMethodType.getMetaFields()) {
			metaFields.add(MetaFieldBL.getWS(mf));
		}
		ws.setMetaFields(metaFields.toArray(new MetaFieldWS[paymentMethodType.getMetaFields().size()]));
		
		return ws;	
	}
	
	private List<Integer> getAllAccountTypes(Integer entityId) {
		List<Integer> accountTypes = new ArrayList<Integer>();
		for (AccountTypeDTO accountType : new AccountTypeDAS()
				.findAll(entityId)) {
			accountTypes.add(accountType.getId());
		}
		return accountTypes;
	}
}

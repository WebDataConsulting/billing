package com.sapienter.jbilling.server.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;

public class CustomerAccountTypeMetaFieldWS {

	private Date effectiveDate;
	private Integer accountInfoTypeId;
	private List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>(0);
	
	public Date getEffectiveDate() {
		return effectiveDate;
	}

	public void setEffectiveDate(Date effectiveDate) {
		this.effectiveDate = effectiveDate;
	}

	public Integer getAccountInfoTypeId() {
		return accountInfoTypeId;
	}

	public void setAccountInfoTypeId(Integer accountInfoTypeId) {
		this.accountInfoTypeId = accountInfoTypeId;
	}

	public List<MetaFieldValueWS> getMetaFieldValues() {
		return metaFieldValues;
	}

	public void setMetaFieldValues(List<MetaFieldValueWS> metaFieldValues) {
		this.metaFieldValues = metaFieldValues;
	}
}

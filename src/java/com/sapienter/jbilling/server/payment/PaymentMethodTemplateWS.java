package com.sapienter.jbilling.server.payment;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.sapienter.jbilling.server.metafields.MetaFieldWS;

public class PaymentMethodTemplateWS implements Serializable{
	
	private Integer id;
	private String templateName;
	
	private Set<MetaFieldWS> metaFields = new HashSet<MetaFieldWS>(0);

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public Set<MetaFieldWS> getMetaFields() {
		return metaFields;
	}

	public void setMetaFields(Set<MetaFieldWS> metaFields) {
		this.metaFields = metaFields;
	}
}

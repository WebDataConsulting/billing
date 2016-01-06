package com.sapienter.jbilling.client.suretax.response;

import org.codehaus.jackson.annotate.JsonProperty;

public class TaxItem {
	@JsonProperty("TaxTypeCode")
	public String taxTypeCode;
	@JsonProperty("TaxTypeDesc")
	public String taxTypeDesc;
	@JsonProperty("TaxAmount")
	public String taxAmount;

	public void setTaxTypeCode(String taxTypeCode) {
		this.taxTypeCode = taxTypeCode;
	}

	public void setTaxTypeDesc(String taxTypeDesc) {
		this.taxTypeDesc = taxTypeDesc;
	}

	public void setTaxAmount(String taxAmount) {
		this.taxAmount = taxAmount;
	}

	@Override
	public String toString() {
		return "TaxItem [taxTypeCode=" + taxTypeCode + ", taxTypeDesc="
				+ taxTypeDesc + ", taxAmount=" + taxAmount + "]";
	}

}

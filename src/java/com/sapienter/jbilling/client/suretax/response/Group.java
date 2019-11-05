package com.sapienter.jbilling.client.suretax.response;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;


public class Group {
	@JsonProperty("StateCode")
	public String stateCode;
	@JsonProperty("InvoiceNumber")
	public String invoiceNumber;
	@JsonProperty("CustomerNumber")
	public String customerNumber;
	@JsonProperty("TaxList")
	public List<TaxItem> taxList;

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public void setCustomerNumber(String customerNumber) {
		this.customerNumber = customerNumber;
	}

	public void setTaxList(List<TaxItem> taxList) {
		this.taxList = taxList;
	}

	@Override
	public String toString() {
		return "Group [stateCode=" + stateCode + ", invoiceNumber="
				+ invoiceNumber + ", customerNumber=" + customerNumber
				+ ", taxList=" + taxList + "]";
	}
}

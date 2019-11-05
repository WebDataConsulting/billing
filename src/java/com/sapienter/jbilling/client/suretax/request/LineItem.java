package com.sapienter.jbilling.client.suretax.request;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class LineItem {
	// Used to identify an item within the request. If no value is provided,
	// requests are numbered sequentially.
	// Max Len: 20
	@JsonProperty("LineNumber")
	private String lineNumber;
	// Used for tax aggregation by Invoice. Must be alphanumeric.
	// Max Len: 20
	@JsonProperty("InvoiceNumber")
	private String invoiceNumber;
	// Used for tax aggregation by Customer. Must be alphanumeric.
	// Max Len: 10
	@JsonProperty("CustomerNumber")
	private String customerNumber;
	// Required when using Tax Situs Rule 01 or 03.
	// Format: NPANXXNNNN
	@JsonProperty("OrigNumber")
	private String origNumber;
	// Required when using Tax Situs Rule 01.
	// Format: NPANXXNNNN
	@JsonProperty("TermNumber")
	private String termNumber;
	// Required when using Tax Situs Rule 01 or 02.
	// Format: NPANXXNNNN
	@JsonProperty("BillToNumber")
	private String billToNumber;
	// Billing zip code
	// Required when using Tax Situs Rule 04 or 05.
	// Zip code in format: 99999 (US) or X9X9X9 (Canadian)
	@JsonProperty("Zipcode")
	private String zipcode;
	// Billing zip code extension
	// Zip code extension in format: 9999
	@JsonProperty("Plus4")
	private String plus4;
	// Secondary zip code
	// Secondary Zip code in format: 99999 (US) or X9X9X9 (Canadian)
	@JsonProperty("P2PZipcode")
	private String p2PZipcode;
	// Secondary zip code extension
	// Secondary zip code extension in format: 99999 (US) or X9X9X9
	// (Canadian)
	@JsonProperty("P2PPlus4")
	private String p2PPlus4;
	// Transaction date
	// Required. Date of transaction. Valid date formats include:
	// MM/DD/YYYY
	// MMDDYYYY
	// YYYYMMDDTHH:MM:SS
	@JsonProperty("TransDate")
	private String transDate;
	// Required. Format: $$$$$$$$$.CCCC
	// For Negative charges, the first position should have a minus
	// indicator.
	@JsonProperty("Revenue")
	private float revenue;
	// Required. Units in format: 99999. Default should be 1
	@JsonProperty("Units")
	private int units;
	// Required.
	// 00  Default (Number of unique lines)
	@JsonProperty("UnitType")
	private String unitType = "00";
	// Call duration
	// Required. Duration of call in seconds. Format 99999.
	@JsonProperty("Seconds")
	private int seconds = 0;
	// Required. Values:
	// 0  Default (No Tax Included)
	// 1  Tax Included in Revenue
	@JsonProperty("TaxIncludedCode")
	private String taxIncludedCode = "0";
	// Required. Values:
	// 01  TwooutofThree test using NPANXX (default)
	// 02  Billed to number
	// 03  Origination number
	// 04  Billing Zip code
	// 05  Billing Zip code + 4
	// 06  Private Line (uses Origination & Termination numbers)
	// 07  Point to Point Zip codes
	@JsonProperty("TaxSitusRule")
	private String taxSitusRule;
	@JsonProperty("TransTypeCode")
	private String transTypeCode;
	// Required. Values:
	// R  Residential customer (default)
	// B  Business customer
	@JsonProperty("SalesTypeCode")
	private String salesTypeCode;
	// Required. Values:
	// 99  Default (should be used for nonTelecom services.)
	// 00  ILEC
	// 01  IXC
	// 02  CLEC
	// 03  VOIP
	// 04  ISP
	// 05  Wireless
	@JsonProperty("RegulatoryCode")
	private String regulatoryCode;
	@JsonProperty("TaxExemptionCodeList")
	private List<String> taxExemptionCodeList;

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public void setCustomerNumber(String customerNumber) {
		this.customerNumber = customerNumber;
	}

	public void setOrigNumber(String origNumber) {
		this.origNumber = origNumber;
	}

	public void setTermNumber(String termNumber) {
		this.termNumber = termNumber;
	}

	public void setBillToNumber(String billToNumber) {
		this.billToNumber = billToNumber;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}

	public void setPlus4(String plus4) {
		this.plus4 = plus4;
	}

	public void setP2PZipcode(String p2pZipcode) {
		p2PZipcode = p2pZipcode;
	}

	public void setP2PPlus4(String p2pPlus4) {
		p2PPlus4 = p2pPlus4;
	}

	public void setTransDate(String transDate) {
		this.transDate = transDate;
	}

	public void setRevenue(float revenue) {
		this.revenue = revenue;
	}

	public void setUnits(int units) {
		this.units = units;
	}

	public void setUnitType(String unitType) {
		this.unitType = unitType;
	}

	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}

	public void setTaxSitusRule(String taxSitusRule) {
		this.taxSitusRule = taxSitusRule;
	}

	public void setTransTypeCode(String transTypeCode) {
		this.transTypeCode = transTypeCode;
	}

	public void setSalesTypeCode(String salesTypeCode) {
		this.salesTypeCode = salesTypeCode;
	}

	public void setRegulatoryCode(String regulatoryCode) {
		this.regulatoryCode = regulatoryCode;
	}

	public void setTaxExemptionCodeList(List<String> taxExemptionCodeList) {
		this.taxExemptionCodeList = taxExemptionCodeList;
	}

	public void setLineNumber(String lineNumber) {
		this.lineNumber = lineNumber;
	}

	public void setTaxIncludedCode(String taxIncludedCode) {
		this.taxIncludedCode = taxIncludedCode;
	}

}

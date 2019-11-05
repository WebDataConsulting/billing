package com.sapienter.jbilling.client.suretax.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class SuretaxRequest {
	@JsonProperty("ItemList")
	public List<LineItem> itemList;
	@JsonProperty("ClientNumber")
	public String clientNumber;
	@JsonProperty("BusinessUnit")
	public String businessUnit;
	@JsonProperty("ValidationKey")
	public String validationKey;
	@JsonProperty("DataYear")
	public String dataYear;
	@JsonProperty("DataMonth")
	public String dataMonth;
	@JsonProperty("TotalRevenue")
	public float totalRevenue;
	@JsonProperty("ReturnFileCode")
	public String returnFileCode;
	@JsonProperty("ClientTracking")
	public String clientTracking;
	@JsonProperty("IndustryExemption")
	public String industryExemption;
	@JsonProperty("ResponseType")
	public String responseType;
	@JsonProperty("ResponseGroup")
	public String responseGroup;

	public void setItemList(List<LineItem> itemList) {
		this.itemList = itemList;
	}

	public void setClientNumber(String clientNumber) {
		this.clientNumber = clientNumber;
	}

	public void setBusinessUnit(String businessUnit) {
		this.businessUnit = businessUnit;
	}

	public void setValidationKey(String validationKey) {
		this.validationKey = validationKey;
	}

	public void setDataYear(String dataYear) {
		this.dataYear = dataYear;
	}

	public void setDataMonth(String dataMonth) {
		this.dataMonth = dataMonth;
	}

	public void setTotalRevenue(float totalRevenue) {
		this.totalRevenue = totalRevenue;
	}

	public void setReturnFileCode(String returnFileCode) {
		this.returnFileCode = returnFileCode;
	}

	public void setClientTracking(String clientTracking) {
		this.clientTracking = clientTracking;
	}

	public void setIndustryExemption(String industryExemption) {
		this.industryExemption = industryExemption;
	}

	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}

	public void setResponseGroup(String responseGroup) {
		this.responseGroup = responseGroup;
	}

	public static void main(String[] args) {
		LineItem li = new LineItem();
		li.setBillToNumber("billToNumber");
		li.setCustomerNumber("customerNumber");
		li.setInvoiceNumber("invoiceNumber");
		li.setLineNumber("lineNumber");
		li.setOrigNumber("origNumber");
		li.setP2PPlus4("");
		li.setP2PZipcode("");
		li.setPlus4("");
		li.setRegulatoryCode("99");
		li.setRevenue(40.0f);
		li.setSalesTypeCode("R");
		li.setSeconds(55);
		List<String> taxExemptionCodes = new ArrayList<String>();
		taxExemptionCodes.add("00");
		taxExemptionCodes.add("00");
		li.setTaxExemptionCodeList(taxExemptionCodes);
		li.setTaxIncludedCode("0");
		li.setTaxSitusRule("01");
		li.setTermNumber("termNumber");
		li.setTransDate("2010-10-10T00:00:00");
		li.setTransTypeCode("010101");
		li.setUnits(1);
		li.setUnitType("00");
		li.setZipcode("");

		ItemList il = new ItemList();
		List<LineItem> lis = new ArrayList<LineItem>();
		lis.add(li);
		il.setItemList(lis);

		SuretaxRequest request = new SuretaxRequest();
		request.setBusinessUnit("businessUnit");
		request.setClientNumber("000000350");
		request.setClientTracking("track");
		request.setValidationKey("a4be5e95-1034-4b73-a9d5-23ba4b4ca2e7");
		request.setDataMonth("10");
		request.setDataYear("2010");
		request.setIndustryExemption("");
		request.setItemList(lis);
		request.setResponseGroup("00");
		request.setResponseType("S");
		request.setReturnFileCode("0");
		request.setTotalRevenue(40.0f);

		ObjectMapper mapper = new ObjectMapper();
		try {
			String json = mapper.writeValueAsString(request);
			System.out.println("json:" + json);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

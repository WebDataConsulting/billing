package com.sapienter.jbilling.client.suretax.response;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class SuretaxResponse {
	@JsonProperty("Successful")
	public String successful;
	@JsonProperty("ResponseCode")
	public String responseCode;
	@JsonProperty("HeaderMessage")
	public String headerMessage;
	@JsonProperty("ItemMessages")
	public List<ItemMessage> itemMessages;
	@JsonProperty("ClientTracking")
	public String clientTracking;
	@JsonProperty("TotalTax")
	public String totalTax;
	@JsonProperty("TransId")
	public String transId;
	@JsonProperty("GroupList")
	public List<Group> groupList;
	public String jsonString;

	public void setSuccessful(String successful) {
		this.successful = successful;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public void setHeaderMessage(String headerMessage) {
		this.headerMessage = headerMessage;
	}

	public void setItemMessages(List<ItemMessage> itemMessages) {
		this.itemMessages = itemMessages;
	}

	public void setClientTracking(String clientTracking) {
		this.clientTracking = clientTracking;
	}

	public void setTotalTax(String totalTax) {
		this.totalTax = totalTax;
	}

	public void setTransId(String transId) {
		this.transId = transId;
	}

	public void setGroupList(List<Group> groupList) {
		this.groupList = groupList;
	}

	public void setJsonString(String jsonString) {
		this.jsonString = jsonString;
	}

	@Override
	public String toString() {
		return "SuretaxResponse [successful=" + successful + ", responseCode="
				+ responseCode + ", headerMessage=" + headerMessage
				+ ", itemMessages=" + itemMessages + ", clientTracking="
				+ clientTracking + ", totalTax=" + totalTax + ", transId="
				+ transId + ", groupList=" + groupList + "]";
	}

}

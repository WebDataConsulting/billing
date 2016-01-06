package com.sapienter.jbilling.client.suretax.request;

import org.codehaus.jackson.annotate.JsonProperty;

public class SuretaxCancelRequest {
	@JsonProperty("ClientNumber")
	private String clientNumber;
	@JsonProperty("ValidationKey")
	private String validationKey;
	@JsonProperty("TransId")
	private String transId;
	@JsonProperty("ClientTracking")
	private String clientTracking;

	public void setClientNumber(String clientNumber) {
		this.clientNumber = clientNumber;
	}

	public void setValidationKey(String validationKey) {
		this.validationKey = validationKey;
	}

	public void setTransId(String transId) {
		this.transId = transId;
	}

	public void setClientTracking(String clientTracking) {
		this.clientTracking = clientTracking;
	}

}

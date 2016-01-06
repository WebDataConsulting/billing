package com.sapienter.jbilling.client.suretax.response;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

public class SuretaxCancelResponse {
	@JsonProperty("Successful")
	private String successful;
	@JsonProperty("ResponseCode")
	private String responseCode;
	@JsonProperty("HeaderMessage")
	private String headerMessage;
	@JsonProperty("ClientTracking")
	private String clientTracking;
	@JsonProperty("TransId")
	private String transId;

	public String getSuccessful() {
		return successful;
	}

	public void setSuccessful(String successful) {
		this.successful = successful;
	}

	public String getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public String getHeaderMessage() {
		return headerMessage;
	}

	public void setHeaderMessage(String headerMessage) {
		this.headerMessage = headerMessage;
	}

	public String getClientTracking() {
		return clientTracking;
	}

	public void setClientTracking(String clientTracking) {
		this.clientTracking = clientTracking;
	}

	public String getTransId() {
		return transId;
	}

	public void setTransId(String transId) {
		this.transId = transId;
	}

}

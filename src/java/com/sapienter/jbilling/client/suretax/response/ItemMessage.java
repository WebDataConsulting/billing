package com.sapienter.jbilling.client.suretax.response;

import org.codehaus.jackson.annotate.JsonProperty;

public class ItemMessage {
	@JsonProperty("LineNumber")
	public String lineNumber;
	@JsonProperty("ResponseCode")
	public String responseCode;
	@JsonProperty("Message")
	public String message;

	public void setLineNumber(String lineNumber) {
		this.lineNumber = lineNumber;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "ItemMessage [lineNumber=" + lineNumber + ", responseCode="
				+ responseCode + ", message=" + message + "]";
	}

}

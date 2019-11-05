package com.sapienter.jbilling.server.order;

public enum CancellationFeeType {
	FLAT("FLAT"),PERCENTAGE("PERCENTAGE");
	
	private String cancellationFeeType;
	 
	private CancellationFeeType(String cancellationFeeType) {
		this.cancellationFeeType = cancellationFeeType;
	}
 
	public String getCancellationFeeType() {
		return cancellationFeeType;
	}
}

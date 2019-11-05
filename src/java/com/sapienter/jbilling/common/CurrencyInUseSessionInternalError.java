package com.sapienter.jbilling.common;

public class CurrencyInUseSessionInternalError extends SessionInternalError {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CurrencyInUseSessionInternalError() {
		super();
	}
	
	public CurrencyInUseSessionInternalError(String s) {
		super(s);
	}
	
	public CurrencyInUseSessionInternalError(String message, String[] errors) {
		super(message, errors);
	}
	
}

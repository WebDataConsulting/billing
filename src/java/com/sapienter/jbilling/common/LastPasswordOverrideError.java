package com.sapienter.jbilling.common;

public class LastPasswordOverrideError extends SessionInternalError{

	public LastPasswordOverrideError(){
		super();
	}
	public LastPasswordOverrideError(String s) {
		super(s);
	}
	public LastPasswordOverrideError(String s, Class className, Exception e) {
		super(s, className, e);
	}

	public LastPasswordOverrideError(Exception e) {
		super(e);
	}

	public LastPasswordOverrideError(String message, Throwable e) {
		super(message, e);
	}

	public LastPasswordOverrideError(String message, Throwable e,
			String[] errors) {
		super(message, e, errors);
	}

	public LastPasswordOverrideError(String message, String[] errors) {
		super(message, errors);
	}
}

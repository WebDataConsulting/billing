/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.common;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.ws.WebFault;

@WebFault(name = "SessionInternalError", targetNamespace = "http://jbilling/")
public class SessionInternalError extends RuntimeException {

    protected SessionInternalErrorMessages sessionInternalErrorMessages = new SessionInternalErrorMessages();
	private String errorMessages[] = null;
	private String params[] = null;
	private String uuid;
	
    public SessionInternalError() {
    }

    public SessionInternalError(String s) {
        super(s);
    }
    
    public SessionInternalError(String s, Class className, Exception e) {
        super(e);
        FormatLogger log = new FormatLogger(className);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();

        log.fatal(s + e.getMessage() + "\n" + sw.toString());
        
    }

    public SessionInternalError(Exception e) {
        super(e);

        if (e instanceof SessionInternalError) {
            setErrorMessages(((SessionInternalError) e).getErrorMessages());
        }

        FormatLogger log = new FormatLogger(Logger.getLogger("com.sapienter.jbilling"));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        log.fatal("Internal error: %s \n %s", e.getMessage(), sw.toString());
    }

    public SessionInternalError(String message, Throwable e) {
        super(message + " Cause: " + e.getMessage(), e);
    }

    public SessionInternalError(String message, Throwable e, String[] errors) {
        super(message + getErrorsAsString(errors), e);
        setErrorMessages(errors);
    }

    public SessionInternalError(String message, String[] errors) {
        super(message + getErrorsAsString(errors));
        setErrorMessages(errors);
    }
    
    public SessionInternalError(String message, String[] errors, String[] params) {
        super(message + getErrorsAsString(errors));
        setErrorMessages(errors);
        setParams(params);
    }

    private static String getErrorsAsString(String[] errors){
        StringBuilder builder = new StringBuilder();
        if (errors != null) {
            builder.append(". Errors: ");
            for (String error : errors) {
                builder.append(error);
                builder.append(System.getProperty("line.separator"));
            }
        }
        return builder.toString();
    }

	public void setErrorMessages(String errors[]) {
        sessionInternalErrorMessages.setErrorMessages(errors);
	}

	public String[] getErrorMessages() {
		return sessionInternalErrorMessages.getErrorMessages();
	}

    public SessionInternalErrorMessages getFaultInfo() {
        return this.sessionInternalErrorMessages;
    }
	
    public String[] getParams() {
		return params;
	}

	public void setParams(String[] params) {
		this.params = params;
	}
	
	public boolean hasParams() {
		return getParams() != null && getParams().length > 0;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void copyErrorInformation(Throwable throwable) {
		if(throwable instanceof SessionInternalError){
			SessionInternalError internal = (SessionInternalError) throwable;
			this.setErrorMessages(internal.getErrorMessages());
			this.setParams(internal.getParams());
		}
	}

}

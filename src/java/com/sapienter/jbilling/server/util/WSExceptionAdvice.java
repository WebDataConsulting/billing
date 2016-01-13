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

package com.sapienter.jbilling.server.util;

import org.springframework.aop.ThrowsAdvice;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

/**
 * Re-throws any exceptions from the API as SessionInternalErrors to
 * prevent server exception classes being required on the client.
 * Useful for remoting protocols such as Hessian which propagate the
 * exception stack trace from the server to the client.
 */
public class WSExceptionAdvice implements ThrowsAdvice {

    private static final FormatLogger LOG = new FormatLogger(WSExceptionAdvice.class);

    public void afterThrowing(Method method, Object[] args, Object target, Exception throwable) {
    	// Avoid catching automatic validation exceptions
	    String uuid = UUID.randomUUID().toString();
	    String message = null;
	    if (throwable instanceof SessionInternalError) {
		    //someone explicitly throws SessionInternalError
		    SessionInternalError sie = (SessionInternalError)throwable;
		    message = "uuid=" + uuid + ", message=" + sie.getMessage();

    		String messages[] = sie.getErrorMessages();
    		if (messages != null && messages.length > 0) {
    			LOG.debug("uuid=%s, message=Validation Errors, errors= %s", uuid, Arrays.toString(messages));
    		} else {
			    LOG.debug(message);
		    }
    	} else {
		    //unexpected exception happens
	        StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    throwable.printStackTrace(pw);
		    pw.close();
		    message = throwable.getMessage();
		    LOG.debug("uuid=%s, message=%s, method=%s \n %s", uuid, message, method.getName(), sw.toString());

	        message = "uuid=" + uuid + ", message=Error calling jBilling API, method=" + method.getName();
	    }

	    //here we create a new exception and we are only including error information
	    //for the exception. We are not giving away the original place from where the
	    //exception was created since it can be viewed as security risk. It could
	    //inadvertently reveal critical place in code, db table structure etc.
	    //we are generating and giving away an UUID so that clients can gives a piece
	    //if information that will help us track the original exception in our logs
        SessionInternalError publicException =  new SessionInternalError(message);
	    publicException.copyErrorInformation(throwable);
	    publicException.setUuid(uuid);
	    throw publicException;
    }
}

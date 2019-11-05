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

package com.sapienter.jbilling.client

import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.context.NoSuchMessageException

import java.util.List;

import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope 
import org.hibernate.StaleObjectStateException;
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import com.sapienter.jbilling.common.SessionInternalError;

class ViewUtils {
    // thanks Groovy for adding the setters and getters for me
    PluginAwareResourceBundleMessageSource messageSource;

    /**
     * Trims all string parameters.
     *
     * @param params
     * @return
     */
    def trimParameters(params) {
        params.each{
            if(it.value instanceof String) {
                it.value = it.value.trim()
            }
        }
    }

    /**
     * Will add to flash.errorMessages a list of string with each error message, if any.
     * @param flash
     * @param locale
     * @param exception
     * @return
     * true if there are validation errors, otherwise false
     */

    boolean resolveException(flash, Locale locale, Exception exception,List <String> emptyFields=null) {
        List<String> messages = new ArrayList<String>();
        if (exception instanceof SessionInternalError && exception.getErrorMessages()?.length > 0) {
            int i=0
            List <String> errorLabelList = [];
            for (String message : exception.getErrorMessages()) {
                List<String> fields = message.split(",");
                if (fields.size() <= 2) {
                    List restOfFields = null;
                    if (fields.size() >= 2) {
                        restOfFields = fields[1..fields.size()-1];
                    }
                    String errorMessage
                    try {
                        errorMessage = messageSource.getMessage(fields[0], restOfFields as Object[] , locale);
                    } catch (NoSuchMessageException e) {
                        errorMessage = fields[0]
                    }
                    messages.add errorMessage
                } else {
                    String type = messageSource.getMessage("bean." + fields[0], null, locale);
                    String propertyCode = "bean." + fields[0] + "." + fields[1];
                    String property
                    try {
                        property = messageSource.getMessage(propertyCode, null, locale);
                    } catch (NoSuchMessageException e) {
                        if (propertyCode.startsWith("bean.OrderWS.")) {
                            propertyCode = propertyCode.replaceAll(".parentOrder.", ".")
                            propertyCode = propertyCode.replace(".childOrders.", ".")
                            property = messageSource.getMessage(propertyCode, null, locale);
                        } else {
                            throw e
                        }
                    }
                    List restOfFields = null;
                    if (fields.size() >= 4) {
                        restOfFields = fields[3..fields.size()-1];
                    }
                    String errorMessage
                    try {
                        errorMessage = messageSource.getMessage(fields[2], restOfFields as Object[] , locale);
                    } catch (NoSuchMessageException e) {
                        errorMessage = fields[2]
                    }
                    String finalMessage
                    if (emptyFields){
                        if (emptyFields.getAt(i)){
                            errorLabelList.add(messageSource.getMessage("validation.error.email.preference.${emptyFields.getAt(i)}",
                                    [type, property, errorMessage] as Object[], locale))
                        }
                    }else{
                        try {
                            errorMessage = messageSource.getMessage(fields[2], restOfFields as Object[] , locale);
                        } catch (NoSuchMessageException e) {
                            errorMessage = fields[2]
                        }
                        finalMessage = messageSource.getMessage("validation.message",
                                [type, property, errorMessage] as Object[], locale);
                        finalMessage = type.equals("Meta Field") ? errorMessage : finalMessage
                        messages.add finalMessage;
                    }
                }
                i++
            }
            if (emptyFields){
                errorLabelList.sort();
                errorLabelList.each {messages.add(it)}
            }
            flash.errorMessages = messages;
            return true;
        } else if (exception.getCause() instanceof StaleObjectStateException) {
            // this is two people trying to update the same data
            StaleObjectStateException ex = exception.getCause();
            flash.error = messageSource.getMessage("error.dobule_update", null, locale);
        } else {
            // generic error
            flash.error = messageSource.getMessage("error.exception", [exception?.getMessage()] as Object[], locale);
        }

        return false;
    }

	/**
	 * This method was added to address issue #7346.
	 * Earlier, while trying to delete a refunded payment the error message being shown to user was - 'The payment has an error in the payment Id
	 * field: This payment cannot be deleted since it has been refunded'.
	 * The error above is not related to payment id or any particular payment field but to a payment as a whole.
	 * This method takes care of the problem and would return the error message as 'This payment cannot be deleted since it has been refunded'    
	 * @param flash
	 * @param locale
	 * @param exception
	 * @return
	 * true if there are validation errors, otherwise false
	 */
	boolean resolveExceptionMessage(flash, Locale locale, Exception exception) {
		List<String> messages = new ArrayList<String>();
		if (exception instanceof SessionInternalError && exception.getErrorMessages()?.length > 0) {
			int i=0
			for (String message : exception.getErrorMessages()) {
				List<String> fields = message.split(",");
				List restOfFields = null;
				if (fields.size() >= 2) {
					restOfFields = fields[1..fields.size()-1];
				}
				String errorMessage = messageSource.getMessage(fields[0], restOfFields as Object[] , locale);
				messages.add errorMessage;
			}
			i++
		flash.errorMessages = messages;
		return true;
	} else if (exception.getCause() instanceof StaleObjectStateException) {
		// this is two people trying to update the same data
		StaleObjectStateException ex = exception.getCause();
		flash.error = messageSource.getMessage("error.dobule_update", null, locale);
	} else {
		// generic error
		flash.error = messageSource.getMessage("error.exception", [exception.getCause().getMessage()] as Object[], locale);
	}

	return false;
}
    /*
    * When exception throw from plugin then it will wrapped into another object of exception like PluggableTaskException/TaskException
    * This method will loop over the exception and find the root object of exception
    * */

    public static Throwable getRootCause(Exception exception) {
        //if error is object of sessionInternalError and has error fields return to controller
        if (exception instanceof SessionInternalError && exception.getErrorMessages()) {
            return exception
        }
        if (exception.getCause() != null)
            return getRootCause(exception.getCause())

        return null;
    }

}

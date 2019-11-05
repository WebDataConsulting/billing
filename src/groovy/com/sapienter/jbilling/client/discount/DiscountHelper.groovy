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

package com.sapienter.jbilling.client.discount

import com.sapienter.jbilling.common.FormatLogger
import com.sapienter.jbilling.server.discount.DiscountWS
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.apache.commons.lang.StringUtils

/**
 * DiscountHelper 
 *
 * @author Amol Gadre
 * @since 29/11/12
 */
class DiscountHelper {

	private static def log = new FormatLogger(this.class)

    static def DiscountWS bindDiscount(DiscountWS discount, GrailsParameterMap params) {        
        def oldType = ""
        def newType = ""
        
        // sort price model parameters by index
        def sorted = new TreeMap<Integer, GrailsParameterMap>()
        params.discount.each{ k, v ->
            // all other fields are populated using bindData
            // lets only bind attributes, also we need oldType, newType below
            if ((k.startsWith("attribute.") && (k.endsWith(".name") || k.endsWith (".value")))
                    || k.equals("oldType") || k.equals("type")) {
                sorted.put(k, v)
                // lets not remove this debug, may be useful for debugging
                log.debug("******* k=${k}		v=${v}")
            }
        }
        if (discount == null) {
        	discount = new DiscountWS()
		}
		
		def attributeIndex = 0
		SortedMap<String, String> newSortedMap = new TreeMap<String, String>(sorted)
		while (newSortedMap.size() != 0) {
    		attributeIndex++
    		String key = newSortedMap.remove("attribute." + attributeIndex + ".name")
    		String value = newSortedMap.remove("attribute." + attributeIndex + ".value")
			newSortedMap.remove("attribute." + attributeIndex + "._value")
    		oldType = newSortedMap.remove("oldType")
    		newType = newSortedMap.remove("type")
    		if (key && !key.trim()?.isEmpty())
    			discount.attributes.put(key, value)
    	}
    	if (newType && newType.equalsIgnoreCase("RECURRING_PERIODBASED")) {
    		if (discount.attributes == null || 
    			(discount.attributes && 
    			 discount.attributes.get("isPercentage") == null)) {
    			discount.attributes.put("isPercentage", "No")
    		}
    	}

        return discount
    }

}

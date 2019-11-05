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

package jbilling

import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO

/**
* JBillingDisplayTagLib
*
* @author Vikas Bodani
* @since 03/09/11
*/

class JBillingDisplayTagLib {

    
    def showProperCase = { attrs, body -> 
        
        StringBuffer sb= new StringBuffer("")
        
        String str= attrs.value
        if (str) {
            sb.append(str.charAt(0).toUpperCase())
            if (str.length() > 1) 
                sb.append(str.substring(1))
        }
        
        out << sb.toString()
    }
    
    /**
     * Prints the phone number is a nice format
     */
    def phoneNumber = { attrs, body ->
        
        def countryCode= attrs.countryCode
        def areaCode= attrs.areaCode 
        def number= attrs.number
        
        StringBuffer sb= new StringBuffer("");
        
        if (countryCode) {
            sb.append(countryCode).append("-")
        }
        
        if (areaCode) {
            sb.append(areaCode).append("-")
        }
        if (number) {
            
            if (number.length() > 4) {
                char[] nums= number.getChars()
                
                int i=0;
                for(char c: nums) {
                   //check if this value is a number between 0 and 9
                   if (c < 58 && c > 47 ) {
                       if (i<3) {
                           sb.append(c)
                           i++
                       } else if (i == 3) {
                           sb.append("-").append(c)
                           i++
                       } else {
                           sb.append(c)
                           i++
                       }
                   }
                }
            } else {
                sb.append(number)
            }
        }
        
        out << sb.toString()
    }

    def isSubsProd = { attrs, body ->
        for (def type : attrs?.plan.itemTypes) {
            if (type.orderLineTypeId == ServerConstants.ORDER_LINE_TYPE_SUBSCRIPTION.intValue()) {
                out << "true"
                break
            }
        }
    }

   def hasAssetProduct = { attrs, body ->
       for (def planItem : attrs?.plan.plans.asList()?.first().planItems) {
           def item = planItem?.item
           def bundle = planItem?.bundle
           if (item.assetManagementEnabled == 1 && bundle?.quantity.compareTo(BigDecimal.ZERO) > 0) {
               out << item.id
               break
           }
       }
    }

    def accountTypeMetaFields = { attrs, body ->
        def filter=attrs.filter
        out << "<select style='float:left;' name=${filter.field}.fieldKeyData>"
        AccountTypeDTO.findAllByCompany(new CompanyDTO(session['company_id'])).each {
            Map<Integer, List<MetaField>> mfList = MetaFieldBL.getAvailableAccountTypeFieldsMap(it.id)

            out << "<option value>--${it.description}</option>"
            mfList.each { Integer key, List value ->
                value.each {
                    out << " <option value=${it.id} ${filter?.fieldKeyData?.equals(it?.id?.toString())?'selected=\"selected\"':''}>${it.name}</option>"
                }
            }
        }
        out << '</select>'
    }

    def formatPriceForDisplay = { attrs, body ->
        def outputString= '0.0000'

        if ( attrs.price ) {
            def price= attrs.price

            outputString= Util.formatRateForDisplay(price.rate)

            if ( price.type == PriceModelStrategy.LINE_PERCENTAGE ) {
                outputString= '%' + outputString
            } else {
                outputString= price.currency?.symbol + outputString
            }
        } else {
            // no price, show 0.0000
        }
        out << outputString
    }


}
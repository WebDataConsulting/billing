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

/*
 * Created on Jan 15, 2005
 *
 */
package com.sapienter.jbilling.server.user;

/**
 * @author Emil
 *
 */
public interface CreditCardSQL {
    
    static final String expiring = 
    		"select bu.id, pi.id, to_date(mf.string_value,'MM/YY') " +
	            " from base_user bu inner join user_status st on bu.status_id = st.id " +
	    		" left outer join ageing_entity_step step on st.id = step.status_id " +
	    		" inner join payment_information pi on bu.id = pi.user_id " +
	    		" inner join payment_information_meta_fields_map pimf on pi.id = pimf.payment_information_id " +
	    		" inner join meta_field_value mf on pimf.meta_field_value_id = mf.id " +
	    		" inner join meta_field_name mfn  on (mf.meta_field_name_id = mfn.id and mfn.field_usage = 'DATE' and  mf.string_value ~ '(?:0[1-9]|1[0-2])/[0-9]{2}') " +
		    		" where bu.deleted = 0 " +
		    		" and (bu.status_id =  " + UserDTOEx.STATUS_ACTIVE + " or step.suspend = 0) " +
		    		" and (pi.processing_order IS NOT NULL) " +
		    		" and to_date(mf.string_value,'MM/YY') <= ? " +
		    		" and pi.id in ( " +
		    			" select p.id from payment_information p  " +
						    " inner join payment_information_meta_fields_map pimf on p.id = pimf.payment_information_id " +
						    " inner join meta_field_value mf on pimf.meta_field_value_id = mf.id " +
						    " inner join meta_field_name mfn  on (mf.meta_field_name_id = mfn.id and mfn.field_usage = 'PAYMENT_CARD_NUMBER')" +
						    " )";
}

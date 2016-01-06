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
package com.sapienter.jbilling.server.payment.blacklist;

import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.*;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDAS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Util;

/**
 * Filters contact names.
 */
public class NameFilter implements BlacklistFilter {

    public Result checkPayment(PaymentDTOEx paymentInfo) {
        return checkUser(paymentInfo.getUserId());
    }

    public Result checkUser(Integer userId) {
        Integer entityId = new UserDAS().find(userId).getCompany().getId();

        ContactDTO userContact = new ContactDAS().findContact(userId);
        //check in the contact
        if (userContact != null) {
            if(null != userContact.getFirstName() || null != userContact.getLastName()){
                List<BlacklistDTO> blacklist = new BlacklistDAS().filterByName(
                        entityId, userContact.getFirstName(), userContact.getLastName());

                if (!blacklist.isEmpty()) {
                    ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
                    return new Result(true,
                            bundle.getString("payment.blacklist.name_filter"));
                }
            }
        }

        //check against meta fields
        CustomerDAS customerDAS = new CustomerDAS();
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        Integer customerId = customerDAS.getCustomerId(userId);
        if(null != customerId){
            List<Integer> aitIds = customerDAS.getCustomerAccountInfoTypeIds(customerId);
            for(Integer ait : aitIds){
                String firstName = null;
                String lastName = null;
                Date effectiveDate = new Date();
                
                List<Integer> firstNameIds =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.FIRST_NAME, ait, effectiveDate);
                Integer firstNameId = null != firstNameIds && firstNameIds.size() > 0 ?
                        firstNameIds.get(0) : null;

                List<Integer> lastNameIds =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.LAST_NAME, ait, effectiveDate);
                Integer lastNameId = null != lastNameIds && lastNameIds.size() > 0 ?
                        lastNameIds.get(0) : null;

                if(null != firstNameId) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(firstNameId);
                    firstName = null != value.getValue() ? (String) value.getValue() : null;
                }

                if(null != lastNameId) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(lastNameId);
                    lastName = null != value.getValue() ? (String) value.getValue() : null;
                }

                if(null != firstName || null != lastName) {

                    List<BlacklistDTO> blacklist = new BlacklistDAS().filterByName(
                            entityId,
                            firstName,
                            lastName);

                    if (!blacklist.isEmpty()) {
                        ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
                        return new Result(true,
                                bundle.getString("payment.blacklist.name_filter"));
                    }
                }
            }
        }

        return new Result(false, null);
    }

    public String getName() {
        return "Name blacklist filter";
    }
}

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
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDAS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Util;

/**
 * Filters contact phone numbers.
 */
public class PhoneFilter implements BlacklistFilter {

    public Result checkPayment(PaymentDTOEx paymentInfo) {
        return checkUser(paymentInfo.getUserId());
    }

    public Result checkUser(Integer userId) {
        ContactDTO contact = new ContactDAS().findContact(userId);
        Integer entityId = new UserDAS().find(userId).getCompany().getId();

        //check against a contact
        if (contact != null) {
            if (contact.getPhoneCountryCode() != null ||
                    contact.getPhoneAreaCode() != null ||
                    contact.getPhoneNumber() != null) {


                List<BlacklistDTO> blacklist = new BlacklistDAS().filterByPhone(
                        entityId,
                        contact.getPhoneCountryCode(),
                        contact.getPhoneAreaCode(),
                        contact.getPhoneNumber());

                if (!blacklist.isEmpty()) {
                    ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
                    return new Result(true,
                            bundle.getString("payment.blacklist.phone_filter"));
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

                Integer phoneCountryCode = null;
                Integer phoneAreaCode = null;
                String phoneNumber = null;
                Date effectiveDate = new Date();
                
                List<Integer> phoneCountryCodes =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.PHONE_COUNTRY_CODE, ait, effectiveDate);
                Integer phoneCountryCodeId = null != phoneCountryCodes && phoneCountryCodes.size() > 0 ?
                        phoneCountryCodes.get(0) : null;
                List<Integer> phoneAreaCodes =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.PHONE_AREA_CODE, ait, effectiveDate);
                Integer phoneAreaCodeId = null != phoneAreaCodes && phoneAreaCodes.size() > 0 ?
                        phoneAreaCodes.get(0) : null;
                List<Integer> phoneNumbers =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.PHONE_NUMBER, ait, effectiveDate);
                Integer phoneNumberId = null != phoneNumbers && phoneNumbers.size() > 0 ?
                        phoneNumbers.get(0) : null;


                if (null != phoneCountryCodeId) {
                    MetaFieldValue phoneCountryCodeValue =
                            metaFieldDAS.getIntegerMetaFieldValue(phoneCountryCodeId);
                    phoneCountryCode = null != phoneCountryCodeValue.getValue() ?
                            (Integer) phoneCountryCodeValue.getValue() : null;
                }

                if (null != phoneAreaCodeId) {
                    MetaFieldValue phoneAreaCodeVlaue =
                            metaFieldDAS.getIntegerMetaFieldValue(phoneAreaCodeId);
                    phoneAreaCode = null != phoneAreaCodeVlaue.getValue() ?
                            (Integer) phoneAreaCodeVlaue.getValue() : null;
                }

                if (null != phoneNumberId) {
                    MetaFieldValue phoneNumberValue =
                            metaFieldDAS.getStringMetaFieldValue(phoneNumberId);
                    phoneNumber = null != phoneNumberValue.getValue() ?
                            (String) phoneNumberValue.getValue() : null;
                }

                if(null != phoneCountryCode ||
                        null != phoneAreaCode ||
                        null != phoneNumber){

                    List<BlacklistDTO> blacklist = new BlacklistDAS().filterByPhone(
                            entityId,
                            phoneCountryCode,
                            phoneAreaCode,
                            phoneNumber);

                    if (!blacklist.isEmpty()) {
                        ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
                        return new Result(true,
                                bundle.getString("payment.blacklist.phone_filter"));
                    }
                }
            }
        }

        return new Result(false, null);
    }

    public String getName() {
        return "Phone blacklist filter";
    }
}

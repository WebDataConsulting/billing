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
import com.sapienter.jbilling.server.metafields.MetaFieldType;
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
 * Filters contact addresses.
 */
public class AddressFilter implements BlacklistFilter {

    public Result checkPayment(PaymentDTOEx paymentInfo) {
        return checkUser(paymentInfo.getUserId());
    }

    public Result checkUser(Integer userId) {
        Integer entityId = new UserDAS().find(userId).getCompany().getId();

        ContactDTO contact = new ContactDAS().findContact(userId);

        //check against a contact
        if (contact != null) {
            if (contact.getAddress1() != null || contact.getAddress2() != null ||
                    contact.getCity() != null || contact.getStateProvince() != null ||
                    contact.getPostalCode() != null || contact.getCountryCode() != null) {

                List<BlacklistDTO> blacklist = new BlacklistDAS().filterByAddress(
                        entityId,
                        contact.getAddress1(),
                        contact.getAddress2(),
                        contact.getCity(),
                        contact.getStateProvince(),
                        contact.getPostalCode(),
                        contact.getCountryCode());

                if (!blacklist.isEmpty()) {
                    ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
                    return new Result(true,
                            bundle.getString("payment.blacklist.address_filter"));
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

                String address1 = null;
                String address2 = null;
                String city = null;
                String stateProvince = null;
                String postalCode = null;
                String countryCode = null;
                Date effectiveDate = new Date();

                List<Integer> address1s =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.ADDRESS1, ait, effectiveDate);
                Integer address1Id = null != address1s && address1s.size() > 0 ?
                        address1s.get(0) : null;

                List<Integer> address2s =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.ADDRESS2, ait, effectiveDate);
                Integer address2Id = null != address2s && address2s.size() > 0 ?
                        address2s.get(0) : null;

                List<Integer> cities =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.CITY, ait, effectiveDate);
                Integer cityId = null != cities && cities.size() > 0 ?
                        cities.get(0) : null;

                List<Integer> stateProvinces =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.STATE_PROVINCE, ait, effectiveDate);
                Integer stateProvinceId = null != stateProvinces && stateProvinces.size() > 0 ?
                        stateProvinces.get(0) : null;

                List<Integer> postalCodes =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.POSTAL_CODE, ait, effectiveDate);
                Integer postalCodeId = null != postalCodes && postalCodes.size() > 0 ?
                        postalCodes.get(0) : null;

                List<Integer> countryCodes =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.COUNTRY_CODE, ait, effectiveDate);
                Integer countryCodeId = null != countryCodes && countryCodes.size() > 0 ?
                        countryCodes.get(0) : null;

                if (null != address1Id) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(address1Id);
                    address1 = null != value.getValue() ? (String) value.getValue() : null;
                }

                if (null != address2Id) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(address2Id);
                    address2 = null != value.getValue() ? (String) value.getValue() : null;
                }

                if (null != cityId) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(cityId);
                    city = null != value.getValue() ? (String) value.getValue() : null;
                }

                if (null != stateProvinceId) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(stateProvinceId);
                    stateProvince = null != value.getValue() ? (String) value.getValue() : null;
                }

                if (null != postalCodeId) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(postalCodeId);
                    postalCode = null != value.getValue() ? (String) value.getValue() : null;
                }

                if (null != countryCodeId) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(countryCodeId);
                    countryCode =  null != value && null != value.getValue() ?
                            (String) value.getValue() : null;
                }

                List<BlacklistDTO> blacklist = new BlacklistDAS().filterByAddress(
                        entityId, address1, address2, city,
                        stateProvince, postalCode, countryCode);

                if (!blacklist.isEmpty()) {
                    ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
                    return new Result(true,
                            bundle.getString("payment.blacklist.address_filter"));
                }
            }
        }
        return new Result(false, null);
    }

    public String getName() {
        return "Address blacklist filter";
    }
}

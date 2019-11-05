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

package com.sapienter.jbilling.server.payment.blacklist.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.blacklist.BlacklistBL;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;

import java.util.Collection;
import java.util.Date;

/**
 * Blacklists users and all their data when their status moves to 
 * suspended or higher. 
 */
public class BlacklistUserStatusTask extends PluggableTask 
        implements IInternalEventsTask {
    private static final FormatLogger LOG = new FormatLogger(BlacklistUserStatusTask.class);

    private static final Class<Event> events[] = new Class[] { 
            NewUserStatusEvent.class };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) {
        NewUserStatusEvent myEvent = (NewUserStatusEvent) event;
        // only process suspended or higher events
        if (myEvent.getNewStatusId().equals(UserDTOEx.STATUS_ACTIVE)) {
            return;
        }

        UserStatusDTO status = new UserStatusDAS().find(myEvent.getNewStatusId());
        if (!status.isSuspended()) {
            return;
        }

        UserStatusDTO oldStatus = new UserStatusDAS().find(myEvent.getOldStatusId());
        // If user was already suspended or higher, then only blacklist user
        // & their info if their user id isn't already blacklisted.
        if (oldStatus.isSuspended() && BlacklistBL.isUserIdBlacklisted(myEvent.getUserId())) {
            LOG.warn("User id is blacklisted for an already suspended or " +
                    "higher user, returning");
            return;
        }

        UserDTO user = new UserDAS().find(myEvent.getUserId());
        BlacklistBL blacklistBL = new BlacklistBL();

        LOG.debug("Adding blacklist records for user id: %s", user.getId());

        // blacklist user id
        blacklistBL.create(user.getCompany(), BlacklistDTO.TYPE_USER_ID,
                BlacklistDTO.SOURCE_USER_STATUS_CHANGE, null, null, user, null);

                // blacklist ip address
        Integer ipAddressCcf =
                BlacklistBL.getIpAddressCcfId(user.getCompany().getId());

        if (ipAddressCcf == null) {
            // blacklist preference or payment filter plug-in
            // not configured properly
            LOG.warn("Null ipAddressCcf - skipping adding IpAddress contact info");
        } else if (user.getCustomer() != null && user.getCustomer().getMetaFields() != null) {
            Object ipAddress = null;
            MetaField metaField = new MetaFieldDAS().find(ipAddressCcf);
            if (metaField != null) {
                MetaFieldValue metaFieldValue = user.getCustomer().getMetaField(metaField.getName());
                if (metaFieldValue != null) {
                    ipAddress = metaFieldValue.getValue();
                }
            }
            // blacklist the ip address if it was found
            if (ipAddress != null) {
                MetaFieldValue newValue = metaField.createValue();
                newValue.setValue(ipAddress);
                blacklistBL.create(user.getCompany(), BlacklistDTO.TYPE_IP_ADDRESS,
                        BlacklistDTO.SOURCE_USER_STATUS_CHANGE, null, null, null, newValue);
            }
        }

        // user's contact
        ContactDTO contact = new ContactDAS().findContact(myEvent.getUserId());

        if (contact == null) {
            contact = ContactBL.buildFromMetaField(myEvent.getUserId(), new Date());
        }

        if (contact == null) {
            LOG.warn("User %s does not have contact information to blacklist.", myEvent.getUserId());
            return;
        }

        // contact to be added to blacklist
        ContactDTO newContact = null;

        // blacklist name
        if (contact.getFirstName() != null || contact.getLastName() != null) {
            newContact = new ContactDTO();
            newContact.setCreateDate(new Date());
            newContact.setDeleted(0);
            newContact.setFirstName(contact.getFirstName());
            newContact.setLastName(contact.getLastName());
            blacklistBL.create(user.getCompany(), BlacklistDTO.TYPE_NAME,
                    BlacklistDTO.SOURCE_USER_STATUS_CHANGE, null, newContact, 
                    null, null);
        }

        // blacklist address
        if (contact.getAddress1() != null || contact.getAddress2() != null ||
                contact.getCity() != null || contact.getStateProvince() != null ||
                contact.getPostalCode() != null || contact.getCountryCode() != null) {
            newContact = new ContactDTO();
            newContact.setCreateDate(new Date());
            newContact.setDeleted(0);
            newContact.setAddress1(contact.getAddress1());
            newContact.setAddress2(contact.getAddress2());
            newContact.setCity(contact.getCity());
            newContact.setStateProvince(contact.getStateProvince());
            newContact.setPostalCode(contact.getPostalCode());
            newContact.setCountryCode(contact.getCountryCode());
            blacklistBL.create(user.getCompany(), BlacklistDTO.TYPE_ADDRESS,
                    BlacklistDTO.SOURCE_USER_STATUS_CHANGE, null, newContact, null, null);
        }

        // blacklist phone number
        if (contact.getPhoneCountryCode() != null || 
                contact.getPhoneAreaCode() != null || 
                contact.getPhoneNumber() != null) {
            newContact = new ContactDTO();
            newContact.setCreateDate(new Date());
            newContact.setDeleted(0);
            newContact.setPhoneCountryCode(contact.getPhoneCountryCode());
            newContact.setPhoneAreaCode(contact.getPhoneAreaCode());
            newContact.setPhoneNumber(contact.getPhoneNumber());
            blacklistBL.create(user.getCompany(), BlacklistDTO.TYPE_PHONE_NUMBER,
                    BlacklistDTO.SOURCE_USER_STATUS_CHANGE, null, newContact, null, null);
        }

        // blacklist cc numbers
        UserBL userBl = new UserBL(user);
        PaymentInformationBL piBl = new PaymentInformationBL();
        Collection<PaymentInformationDTO> creditCards = userBl.getAllCreditCards();
        if(creditCards != null) {
	        for (PaymentInformationDTO cc : creditCards) {
	        	String cardNumber = piBl.getStringMetaFieldByType(cc, MetaFieldType.PAYMENT_CARD_NUMBER);
	            if (cardNumber != null) {
	                PaymentInformationDTO creditCard = piBl.getCreditCardObject(cardNumber, user.getCompany());
	                piBl.updateStringMetaField(creditCard, piBl.getStringMetaFieldByType(cc, MetaFieldType.DATE), MetaFieldType.DATE);
	                piBl.updateStringMetaField(creditCard, piBl.getStringMetaFieldByType(cc, MetaFieldType.PAYMENT_CARD_NUMBER), MetaFieldType.PAYMENT_CARD_NUMBER);
	                blacklistBL.create(user.getCompany(), BlacklistDTO.TYPE_CC_NUMBER,
	                        BlacklistDTO.SOURCE_USER_STATUS_CHANGE, creditCard, 
	                        null, null, null);
	            }
	        }
        }
    }
}

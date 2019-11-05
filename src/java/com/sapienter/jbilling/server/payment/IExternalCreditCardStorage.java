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
package com.sapienter.jbilling.server.payment;

import java.util.List;

import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;

public interface IExternalCreditCardStorage {

    /**
     * Store the given credit card using the payment gateways storage mechanism.
     *
     * This method should return null for storage failures, so that the
     * {@link com.sapienter.jbilling.server.payment.tasks.SaveCreditCardExternallyTask }
     * can perform failure handling.
     *
     * If an obscured and stored credit card is encountered, this method should still return a
     * gateway key for the card and not a null value. It is up to the implementation
     * to decide whether or not to re-store the card or to leave it as-is.
     *
     * @param contact ContactDTO from NewContactEvent, may be null if triggered by NewCreditCardEvent
     * @param Credit Card or/and Ach instrument
     * @return gateway key of stored credit card, null if storage failed
     */
    public String storeCreditCard(ContactDTO contact, PaymentInformationDTO instrument);
    
    /**
     * Delete the existing credit card details or the Ach payment details.
     * 
     * This method should return null for storage failures, so that the
     * {@link com.sapienter.jbilling.server.payment.tasks.SaveCreditCardExternallyTask }
     * can perform failure handling.
     *
     * @param contact contact to process
     * @param payment instruments to process
     * @return resulting unique gateway key for the credit card/contact
     */
    public String deleteCreditCard(ContactDTO contact, PaymentInformationDTO instrument);
}

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

package com.sapienter.jbilling.server.notification;



import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.db.UserDTO;

public interface INotificationSessionBean {
    
    /**
     * Sends an email with the invoice to a customer.
     * This is used to manually send an email invoice from the GUI
     * @param invoiceId
     * @return
    */
    public Boolean emailInvoice(Integer invoiceId) throws SessionInternalError;
    
    /**
     * Sends an email with the invoice to a customer.
     * This is used to manually send an email invoice from the GUI
     * @param paymentId
     * @return
    */
    public Boolean emailPayment(Integer paymentId) throws SessionInternalError;

    public void notify(Integer userId, MessageDTO message) 
            throws SessionInternalError;

    public void asyncNotify(Integer userId, MessageDTO message)
            throws SessionInternalError;
    
   /**
    * Sends a notification to a user. Returns true if no exceptions were
    * thrown, otherwise false. This return value could be considered
    * as if this message was sent or not for most notifications (emails).
    */
    public Boolean notify(UserDTO user, MessageDTO message) 
            throws SessionInternalError;

    public MessageDTO getDTO(Integer typeId, Integer languageId,
            Integer entityId) throws SessionInternalError;

    public Integer createUpdate(MessageDTO dto, Integer entityId) 
            throws SessionInternalError;

    public String getEmails(Integer entityId, String separator) 
            throws SessionInternalError;
}

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

package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.PaperInvoiceBatchBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.db.PaperInvoiceBatchDTO;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author Emil
 */
public class PaperInvoiceNotificationTask
        extends PluggableTask implements NotificationTask {

    private static final FormatLogger LOG = new FormatLogger(PaperInvoiceNotificationTask.class);
    // pluggable task parameters names
    public static final ParameterDescription PARAMETER_DESIGN = 
    	new ParameterDescription("design", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_TEMPLATE =
            new ParameterDescription("template", false, ParameterDescription.Type.INT);
    public static final ParameterDescription PARAMETER_LANGUAGE_OPTIONAL =
    	new ParameterDescription("language", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_SQL_QUERY_OPTIONAL = 
    	new ParameterDescription("sql_query", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_COLOUR_CODE_OPTIONAL =
            new ParameterDescription("colour_code", false, ParameterDescription.Type.STR);


    //initializer for pluggable params
    { 
    	descriptions.add(PARAMETER_DESIGN);
    	descriptions.add(PARAMETER_TEMPLATE);
        descriptions.add(PARAMETER_LANGUAGE_OPTIONAL);
        descriptions.add(PARAMETER_SQL_QUERY_OPTIONAL);
    }



    private String design;
    private Integer templateId;
    private boolean language;
    private boolean sqlQuery;
    private ContactBL contact;
    private ContactDTOEx to;
    private Integer entityId;
    private InvoiceDTO invoice;
    private ContactDTOEx from;
    private String colourCode;

    /* (non-Javadoc)
     * @see com.sapienter.jbilling.server.pluggableTask.NotificationTask#deliver(com.sapienter.betty.interfaces.UserEntityLocal, com.sapienter.betty.server.notification.MessageDTO)
     */
    private void init(UserDTO user, MessageDTO message)
            throws TaskException {
        design = parameters.get(PARAMETER_DESIGN.getName());

        invoice = (InvoiceDTO) message.getParameters().get(
                "invoiceDto");
        try {
            String templateIdStr = parameters.get(PARAMETER_TEMPLATE.getName());
            templateId = ((templateIdStr != null) && !templateIdStr.isEmpty()) ? Integer.valueOf(parameters.get(PARAMETER_TEMPLATE.getName())) : null;
            language = Boolean.valueOf((String) parameters.get(
                    PARAMETER_LANGUAGE_OPTIONAL.getName()));
            sqlQuery = Boolean.valueOf((String) parameters.get(
                    PARAMETER_SQL_QUERY_OPTIONAL.getName()));
        	colourCode = parameters.get(PARAMETER_COLOUR_CODE_OPTIONAL.getName());
            contact = new ContactBL();
            contact.setInvoice(invoice.getId());
            if (contact.getEntity() != null) {
                to = contact.getDTO();
                if (to.getUserId() == null) {
                    to.setUserId(invoice.getBaseUser().getUserId());
                }
            }

            entityId = user.getEntity().getId();
            contact.setEntity(entityId);
            LOG.debug("Found Entity %s contact %s", entityId, contact.getEntity());
            if (contact.getEntity() != null) {
                from = contact.getDTO();
                LOG.debug("Retrieved entity contact i.e. from %s", from);
                if (from.getUserId() == null) {
                    from.setUserId(new EntityBL().getRootUser(entityId));
                }
                LOG.debug("Entity Contact User ID %s", from.getUserId());
            }
        } catch (Exception e) {
            throw new TaskException(e);
        }
    }

    public boolean deliver(UserDTO user, MessageDTO message)
            throws TaskException {
        if (!message.getTypeId().equals(MessageDTO.TYPE_INVOICE_PAPER)) {
            // this task is only to notify about invoices
            return false;
        }
        try {
            init(user, message);
            NotificationBL.generatePaperInvoiceAsFile(getDesign(user), sqlQuery,
                    invoice, from, to, message.getContent()[0].getContent(),
                    message.getContent()[1].getContent(), entityId,
                    user.getUserName(), user.getPassword(), colourCode);
            // update the batch record
            Integer processId = (Integer) message.getParameters().get(
                    "processId");
            PaperInvoiceBatchBL batchBL = new PaperInvoiceBatchBL();
            PaperInvoiceBatchDTO record = batchBL.createGet(processId);
            record.setTotalInvoices(record.getTotalInvoices() + 1);
            // link the batch to this invoice
            // lock the row, the payment MDB will update too
            InvoiceDTO myInvoice = new InvoiceDAS().findForUpdate(invoice.getId());
            myInvoice.setPaperInvoiceBatch(record);
            record.addInvoice(myInvoice);
        } catch (Exception e) {
            throw new TaskException(e);
        }

        return true;
    }

    public byte[] getPDF(UserDTO user, MessageDTO message)
            throws SessionInternalError {
        try {
            init(user, message);
            LOG.debug("now message1 = %s", message.getContent()[0].getContent());

                return NotificationBL.generatePaperInvoiceAsStream(this.getDesign(user), sqlQuery, invoice, from, to,
                        message.getContent()[0].getContent(), message.getContent()[1].getContent(), entityId,
                        user.getUserName(), user.getPassword(), colourCode);
            } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public String getPDFFile(UserDTO user, MessageDTO message)
            throws SessionInternalError {
        try {
            init(user, message);
            
                return NotificationBL.generatePaperInvoiceAsFile(this.getDesign(user), sqlQuery, invoice, from, to,
                        message.getContent()[0].getContent(), message.getContent()[1].getContent(), entityId,
                        user.getUserName(), user.getPassword(), colourCode);
            } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public int getSections() {
        return 2;
    }

    @Override
    public List<NotificationMediumType> mediumHandled() {
        return Arrays.asList(NotificationMediumType.PDF);
    }

    private String getDesign(UserDTO user) {
        String customerDesign = null;
		if ( null != user.getCustomer()) {
        	
        	if ( !StringUtils.isEmpty(user.getCustomer().getInvoiceDesign())) {
        		customerDesign = user.getCustomer().getInvoiceDesign();
        	} else if (null != user.getCustomer().getAccountType()) {
        		customerDesign= user.getCustomer().getAccountType().getInvoiceDesign();
        	} 
        }

        if (StringUtils.isBlank(customerDesign) && user.getCustomer() != null && user.getCustomer().getAccountType() != null) {
            customerDesign = user.getCustomer().getAccountType().getInvoiceDesign();
        }

        if (StringUtils.isBlank(customerDesign)) {
            customerDesign = design;
        }
        	
        return language ? customerDesign + user.getLanguage().getCode() :  customerDesign;
    }
}

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

package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
;

import java.math.BigDecimal;
import java.util.Set;

/**
 * This plug-in calculates taxes for invoice.
 *
 * Plug-in parameters:
 *
 *      tax_item_id                 (required) The item that will be added to an invoice with the taxes
 *
 *      customer_exempt_field_id     (optional) The id of CCF that if its value is 'true' or 'yes' for a customer,
 *                                  then the customer is considered exempt. Exempt customers do not get the tax
 *                                  added to their invoices.
 *      item_exempt_category_id     (optional) The id of an item category that, if the item belongs to, it is
 *                                  exempt from taxes
 *
 * @author Alexander Aksenov, Vikas Bodani
 * @since 30.04.11
 */
public class SimpleTaxCompositionTask extends AbstractChargeTask {

    private static final FormatLogger LOG = new FormatLogger(SimpleTaxCompositionTask.class);

    protected Integer exemptItemCategoryID = null;
    protected Integer exemptCustomerAttributeID = null;
    
    // plug-in parameters
    
    // optional, may be empty
    public static final ParameterDescription PARAM_CUSTOM_CONTACT_FIELD_ID =
        new ParameterDescription("customer_exempt_field_id", false, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAM_ITEM_EXEMPT_CATEGORY_ID = 
		new ParameterDescription("item_exempt_category_id", false, ParameterDescription.Type.STR);

    //initializer for pluggable params
    {
        descriptions.add(PARAM_CUSTOM_CONTACT_FIELD_ID);
        descriptions.add(PARAM_ITEM_EXEMPT_CATEGORY_ID);
    }

    /**
     * Set the current set of plugin params
     */
    protected void setPluginParameters()  throws TaskException {
        LOG.debug("setPluginParameters()");
        super.setPluginParameters();
        try {
            String paramValue = getParameter(PARAM_ITEM_EXEMPT_CATEGORY_ID.getName(), "");
            if (paramValue != null && !"".equals(paramValue.trim())) {
                exemptItemCategoryID = new Integer(paramValue);
            }
            paramValue = getParameter(PARAM_CUSTOM_CONTACT_FIELD_ID.getName(), "");
            if (paramValue != null && !"".equals(paramValue.trim())) {
                exemptCustomerAttributeID = new Integer(paramValue);
            }
        } catch (NumberFormatException e) {
            LOG.error("Incorrect plugin configuration", e);
            throw new TaskException(e);
        }
    }

    public boolean isTaxCalculationNeeded(NewInvoiceContext invoice, Integer userId) {
    	LOG.debug("isTaxCalculationNeeded for user %s having exemptProperty %s", userId, exemptCustomerAttributeID );
    	//default true
    	boolean retVal= true;
    	if ( null != exemptCustomerAttributeID ) { 
	    	UserDTO user= UserBL.getUserEntity(userId);
	        CustomerDTO customer = user.getCustomer();
	        if (null != customer) {
	        	LOG.debug ("User and Customer resolved. ");
		        MetaFieldValue customField = customer.getMetaField(exemptCustomerAttributeID);
		        if ( null != customField) {
		        	LOG.debug("Exempt field value %s", customField.getValue());
			        String value = (String) customField.getValue();
			        if ("yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) {
			            retVal= false;
			        }
		        }
	        }
	        
    	}
        return retVal;
    }

    /**
    * Used to set primarily the line type id for tax item or any other customization
    */
    protected BigDecimal calculateAndApplyTax(NewInvoiceContext invoice, Integer userId) { 
        
        LOG.debug("calculateAndApplyTax");
        
        BigDecimal invoiceAmountSum= super.calculateAndApplyTax(invoice, userId);
        
        LOG.debug("Exempt Category %s", exemptItemCategoryID);
        if (exemptItemCategoryID != null) {
            // find exemp items and subtract price
            for (int i = 0; i < invoice.getResultLines().size(); i++) {
                InvoiceLineDTO invoiceLine = (InvoiceLineDTO) invoice.getResultLines().get(i);
                ItemDTO item = invoiceLine.getItem();

                if (item != null) {
                    Set<ItemTypeDTO> itemTypes = new ItemDAS().find(item.getId()).getItemTypes();
                    for (ItemTypeDTO itemType : itemTypes) {
                        if (itemType.getId() == exemptItemCategoryID) {
                            LOG.debug("Item %s is Exempt. Category %s", item.getDescription(), itemType.getId());
                            invoiceAmountSum = invoiceAmountSum.subtract(invoiceLine.getAmount());
                            break;
                        }
                    }
                }
            }
        }
        
        this.invoiceLineTypeId= ServerConstants.INVOICE_LINE_TYPE_TAX;
        
        return invoiceAmountSum;
    }
}

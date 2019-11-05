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

import java.math.BigDecimal;
import java.util.List;
import java.util.Date;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.CustomerDAS;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.ServerConstants;

/**
 * This plug-in calculates taxes for invoice.
 *
 * Plug-in parameters:
 * 
 *      tax_country_code .(required) 'country code' for which the above tax item id is applicable 
 * 
 * @author Vikas Bodani
 * @since 27-Jul-2011
 *
 */
public class CountryTaxCompositionTask extends AbstractChargeTask {

    private static final FormatLogger LOG = new FormatLogger(CountryTaxCompositionTask.class);
    
    // Plug-in Parameters
    // Mandatory parameters
    protected static final ParameterDescription PARAM_TAX_COUNTRY_CODE = new ParameterDescription("tax_country_code", true, ParameterDescription.Type.STR);
    
    protected String strTaxCountryCode=null; 
    
    //initializer for pluggable params
    {
        descriptions.add(PARAM_TAX_COUNTRY_CODE);
    }
    
    protected BigDecimal calculateAndApplyTax(NewInvoiceContext invoice, Integer userId) { 
        
        LOG.debug("calculateAndApplyTax");
        
        BigDecimal invoiceAmountSum= super.calculateAndApplyTax(invoice, userId);
        
        this.invoiceLineTypeId= ServerConstants.INVOICE_LINE_TYPE_TAX;
        
        return invoiceAmountSum;
    }
    
    /**
     * Set the current set of plugin params
     */
    protected void setPluginParameters()  throws TaskException {
        LOG.debug("setPluginParameters()");
        super.setPluginParameters();
        try {
            String paramValue = getParameter(PARAM_TAX_COUNTRY_CODE.getName(), "");
            if (paramValue == null || "".equals(paramValue.trim())) {
                throw new TaskException("Tax Country Code is not defined!");
            }
            strTaxCountryCode= paramValue;
            LOG.debug("Param country code is set.");
        } catch (NumberFormatException e) {
            LOG.error("Incorrect plugin configuration", e);
            throw new TaskException(e);
        }
    }
    
    /**
     * Custom logic to determine if the tax should be applied to this user's invoice
     * @param userId The user_id of the Invoice
     * @return
     */
    protected boolean isTaxCalculationNeeded(NewInvoiceContext invoice, Integer userId) {
        LOG.debug("isTaxCalculationNeeded()");
        
        //get parent user
        UserDTO user= UserBL.getUserEntity(userId);
        if (null != user) {
            CustomerDAS customerDAS = new CustomerDAS();
            Integer customerId = customerDAS.getCustomerId(user.getUserId());
            if(null != customerId){
                List<Integer> aits = customerDAS.getCustomerAccountInfoTypeIds(customerId);
                if(null != aits && aits.size()>0){
                    boolean result = false;
                    for(Integer ait : aits){
                        String countryCode = getStringMetaFieldValue(customerId, MetaFieldType.COUNTRY_CODE, ait, new Date());
                        if(null != countryCode && !countryCode.trim().isEmpty()){
                            LOG.debug("Contact Country Code is %s. for AIT id: %s",
                                    countryCode, ait);
                            result |= strTaxCountryCode.equals(countryCode);
                        }
                    }
                    return result;
                }
            }

        }
        return false;
    }

    private String getStringMetaFieldValue(Integer customerId, MetaFieldType type, Integer group, Date effectiveDate){
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        List<Integer> values = metaFieldDAS.getCustomerFieldValues(customerId, type, group, effectiveDate);
        Integer valueId = null != values && values.size() > 0 ? values.get(0) : null;
        MetaFieldValue valueField = null != valueId ? metaFieldDAS.getStringMetaFieldValue(valueId) : null;
        return null != valueField ? (String) valueField.getValue() : null;
    }
}

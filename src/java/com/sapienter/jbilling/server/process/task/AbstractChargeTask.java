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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.pluggableTask.InvoiceCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.util.ServerConstants;

import java.math.BigDecimal;

/**
 * This plug-in calculates taxes for invoice.
 * <p/>
 * Plug-in parameters:
 * <p/>
 * charge_carrying_item_id (required) The item that will be added to an invoice with the
 * taxes
 *
 * @author Vikas Bodani
 * @since 28-Jul-2011
 */
public abstract class AbstractChargeTask extends PluggableTask implements InvoiceCompositionTask {

    private static final FormatLogger LOG = new FormatLogger(AbstractChargeTask.class);

    //mandatory plugin parameters
    protected static final ParameterDescription PARAM_TAX_ITEM_ID =
            new ParameterDescription("charge_carrying_item_id", true, ParameterDescription.Type.STR);

    /**
     * The tax item initialized via a plugin-parameter holds the charge that'll be applied
     * to the invoice provided other conditions are met. The price may be percetage or fixed rate.
     */
    protected ItemDTO taxItem = null;

    /**
     * The Invoice LIne Type of the Invoice Item added as a result of this charge task
     * Default initialized to a type Tax.
     */
    protected Integer invoiceLineTypeId = ServerConstants.INVOICE_LINE_TYPE_TAX;

    // initializer for pluggable params
    {
        descriptions.add(PARAM_TAX_ITEM_ID);
    }

    /**
     *
     */
    public AbstractChargeTask() {
        super();
    }

    public void apply(NewInvoiceContext invoice, Integer userId) throws TaskException {
        LOG.debug("apply()");
        this.setPluginParameters();

        if (!this.isTaxCalculationNeeded(invoice, userId)) {
            return;
        }

        BigDecimal taxOrPenaltyBaseAmnt = this.calculateAndApplyTax(invoice, userId);

        this.applyCharge(invoice, userId, taxOrPenaltyBaseAmnt, invoiceLineTypeId);
    }

    /**
     * Apply the percetage or flat rate from the taxItem and apply it to the invoice.
     *
     * @param invoice
     * @param userId
     */
    protected void applyCharge(NewInvoiceContext invoice, Integer userId, BigDecimal taxOrPenaltyBaseAmt, Integer INVOICE_LINE_TYPE) {
        LOG.debug("applyCharge()");
        BigDecimal taxOrPenaltyValue;
        BigDecimal taxOrPenaltyRate= BigDecimal.ZERO;
        if (taxItem.getPercentage()!=null) {
            LOG.debug("Percentage: " + taxItem.getPercentage());
            LOG.debug("Calculating tax on = " + taxOrPenaltyBaseAmt);
            taxOrPenaltyValue = taxOrPenaltyBaseAmt.multiply(taxItem.getPercentage()).divide(
                    BigDecimal.valueOf(100L), CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND);
            taxOrPenaltyRate= taxItem.getPercentage();
        } else {
            LOG.debug("Flat Price.");
            ItemBL itemBL = new ItemBL(taxItem);
            taxOrPenaltyValue = itemBL.getPriceByCurrency(invoice.getCreateDatetime(), taxItem, userId, invoice.getCurrency().getId());
            taxOrPenaltyRate= taxOrPenaltyValue;
        }
        
        if (taxOrPenaltyValue.compareTo(BigDecimal.ZERO) != 0 ) {
	        LOG.debug("Adding Tax Or Penalty as additional Invoice Line");
	        String itemDescription = taxItem.getDescription();
	        InvoiceLineDTO invoiceLine = new InvoiceLineDTO(null, itemDescription,
	                taxOrPenaltyValue, taxOrPenaltyRate, BigDecimal.ONE, INVOICE_LINE_TYPE, 0,
	                taxItem.getId(), userId, taxItem.isPercentage() ? 1 :0);
	        
	        invoice.addResultLine(invoiceLine);
        } else {
        	LOG.debug("Tax or penalty amount = 0, adding no line.");
        }
    }

    /**
     * Set all the current plugin params
     */
    protected void setPluginParameters() throws TaskException {
        LOG.debug("setPluginParameters()");
        try {
            String paramValue = getParameter(PARAM_TAX_ITEM_ID.getName(), "");// mandatory
            if (paramValue == null || "".equals(paramValue.trim())) {
                throw new TaskException("Tax item id is not defined!");
            }
            this.taxItem = new ItemDAS().find(Integer.valueOf(paramValue));
            if (taxItem == null) {
                throw new TaskException("Tax item not found!");
            }
            LOG.debug("The Tax Item is set.");
        } catch (NumberFormatException e) {
            LOG.error("Incorrect plugin configuration", e);
            throw new TaskException(e);
        }
    }

    /**
     * @param invoice
     * @param userId
     * @return
     */
    protected abstract boolean isTaxCalculationNeeded(NewInvoiceContext invoice, Integer userId);

    /**
     * @param invoice
     * @param userId
     */
    protected BigDecimal calculateAndApplyTax(NewInvoiceContext invoice, Integer userId) {

        //calculate TOTAL to include result lines
        invoice.calculateTotal();
        BigDecimal invoiceAmountSum = invoice.getTotal();

        //For a Flat Rate Charge, below calculation is not required.
        if (taxItem.getPercentage()!=null) {
            //Remove CARRIED BALANCE from tax calculation to avoid double taxation
            LOG.debug("Percentage Price. Carried balance is %s", invoice.getCarriedBalance());
            if (null != invoice.getCarriedBalance()) {
                invoiceAmountSum = invoiceAmountSum.subtract(invoice.getCarriedBalance());
            }

            // Remove TAX ITEMS from Invoice to avoid calculating tax on tax
            for (int i = 0; i < invoice.getResultLines().size(); i++) {
                InvoiceLineDTO invoiceLine = invoice.getResultLines().get(i);
                if (null != invoiceLine.getInvoiceLineType() && invoiceLine.getInvoiceLineType().getId() == ServerConstants.INVOICE_LINE_TYPE_TAX) {
                    invoiceAmountSum = invoiceAmountSum.subtract(invoiceLine.getAmount());
                }
            }
        }

        return invoiceAmountSum;
    }
}

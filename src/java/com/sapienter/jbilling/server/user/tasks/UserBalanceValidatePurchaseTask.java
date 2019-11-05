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

package com.sapienter.jbilling.server.user.tasks;

import java.math.BigDecimal;
import java.util.List;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.ServerConstants;

/**
 * Pluggable task determines result for validatePurchase API method 
 * according to the user's dynamic balance.
 */
public class UserBalanceValidatePurchaseTask extends PluggableTask 
        implements IValidatePurchaseTask {

    public ValidatePurchaseWS validate(CustomerDTO customer, List<ItemDTO> items, List<BigDecimal> amounts, 
                                       ValidatePurchaseWS result, List<List<PricingField>> fields) throws TaskException {

        if (!result.getAuthorized()) {
            return result;
        }

        BigDecimal amount = BigDecimal.ZERO;
        for (BigDecimal a : amounts) {
            amount = amount.add(a);
        }

        // avoid divide by zero exception
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            result.setQuantity(new BigDecimal(Integer.MAX_VALUE));
            return result;
        }

        // get the parent customer that pays, if it exists
        while (customer.getParent() != null
                && (customer.getInvoiceChild() == null || customer.getInvoiceChild() == 0)) {
            // go up one level
            customer =  customer.getParent();
        }
        BigDecimal quantity = customer.getCreditLimit().add(customer.getDynamicBalance()).divide(amount, ServerConstants.BIGDECIMAL_SCALE, ServerConstants.BIGDECIMAL_ROUND);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0)
        result.setAuthorized(false);

        result.setQuantity(quantity);
        return result;
    }
}

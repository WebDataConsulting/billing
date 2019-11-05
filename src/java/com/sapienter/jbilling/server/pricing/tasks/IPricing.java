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
package com.sapienter.jbilling.server.pricing.tasks;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pricing.PricingContext;

import java.math.BigDecimal;

public interface IPricing {

    /**
     * Get the price for the given item, user, and quantity being purchased. Pricing fields can be
     * provided to define specific pricing scenarios to be handled by the implementing class.
     *
     * @param pricingContext
     * @param defaultPrice
     * @param pricingOrder
     * @param orderLine
     * @param singlePurchase
     * @return
     * @throws TaskException
     */
    // For a price determination we really require: user, currency, item, eventDate, pricingFields and quantity in that order
    public BigDecimal getPrice(PricingContext pricingContext, BigDecimal defaultPrice, OrderDTO pricingOrder, OrderLineDTO orderLine, boolean singlePurchase)
            throws TaskException;
}

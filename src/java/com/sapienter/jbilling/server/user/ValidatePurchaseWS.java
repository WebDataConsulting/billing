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

package com.sapienter.jbilling.server.user;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sapienter.jbilling.common.Util;

/**
 * Result object for validatePurchase API method.
 */
public class ValidatePurchaseWS implements Serializable {

    private Boolean success = true;
    private Boolean authorized = true;
    private String quantity = "0.0";
    private List<String> message = new ArrayList<String>();

    public ValidatePurchaseWS() {
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getAuthorized() {
        return authorized;
    }

    public void setAuthorized(Boolean authorized) {
        this.authorized = authorized;
    }

    public String getQuantity() {
        return quantity;
    }

    public BigDecimal getQuantityAsDecimal() {
        return Util.string2decimal(quantity);
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public void setQuantity(Double quantity) {
        this.setQuantity(new BigDecimal(quantity));
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = (quantity != null ? quantity.toString() : null);
    }

    public String[] getMessage() {
        return message.toArray(new String[message.size()]);
    }

    public void setMessage(String[] message) {
        this.message = Arrays.asList(message);
    }

    public void addMessage(String message) {
        this.message.add(message);
    }

    @Override
    public String toString() {
        return "ValidatePurchaseWS{" +
                "success=" + success +
                ", authorized=" + authorized +
                ", quantity='" + quantity + '\'' +
                '}';
    }
}

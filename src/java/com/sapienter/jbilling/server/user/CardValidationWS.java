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
import java.util.ArrayList;
import java.util.List;

import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;

/**
 * CardValidationWS
 *
 * @author Brian Cowdery
 * @since 07/09/11
 */
public class CardValidationWS implements Serializable {

    private boolean valid = true;
    private int level = 0;
    private int levelFailed = 0;
    private List<String> errors = new ArrayList<String>();
    private PaymentAuthorizationDTOEx preAuthorization = null;
    public CardValidationWS() {
    }

    public CardValidationWS(int level) {
        this.level = level;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevelFailed() {
        return levelFailed;
    }

    public void setLevelFailed(int levelFailed) {
        this.levelFailed = levelFailed;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error, int level) {
        this.errors.add(error);
        this.valid = false;
        this.levelFailed = level;
    }
    public PaymentAuthorizationDTOEx getPreAuthorization() {
		return preAuthorization;
	}

	public void setPreAuthorization(PaymentAuthorizationDTOEx preAuthorization) {
		this.preAuthorization = preAuthorization;
	}

    @Override
    public String toString() {
        return "CardValidationWS{"
               + "valid=" + valid
               + ", level=" + level
               + ", levelFailed=" + levelFailed
               + ", errors=" + errors
               + ", preAuthorization=" + preAuthorization
               + '}';
    }
}

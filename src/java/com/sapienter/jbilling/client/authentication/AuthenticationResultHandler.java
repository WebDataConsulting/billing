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

package com.sapienter.jbilling.client.authentication;

import com.sapienter.jbilling.server.user.IUserSessionBean;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

/**
 * @author carevski
 * @since 10/24/12
 */
public class AuthenticationResultHandler {

    private IUserSessionBean userSession;

    public void setUserSession(IUserSessionBean userSession) {
        this.userSession = userSession;
    }

    public void loginSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        update(authentication.getPrincipal(), true);
    }

    public void loginFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        update(authentication.getPrincipal(), false);
    }

    private boolean update(Object details, boolean success){
        boolean doUpdate = false;
        String username = null;
        Integer entityId = null;

        if (details instanceof String) {
            String[] tokens = ((String) details).split(";");
            if (tokens.length < 2) {
                return false; //??
            }
            username = tokens[0];
            entityId = Integer.valueOf(tokens[1]);
            doUpdate = true;

        } else if (details instanceof CompanyUserDetails) {

            CompanyUserDetails companyDetails = (CompanyUserDetails) details;
            username = companyDetails.getPlainUsername();
            entityId = companyDetails.getCompanyId();
            doUpdate = true;

        }

        if (doUpdate) {
            if (success) {
                userSession.loginSuccess(username, entityId);
            } else {
                boolean locked = userSession.loginFailure(username, entityId);
            }
            return true;
        }

        return false;

    }

}

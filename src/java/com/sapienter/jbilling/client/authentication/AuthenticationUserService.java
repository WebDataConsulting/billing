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
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.ServerConstants;


public class AuthenticationUserService {


	private IUserSessionBean userSession;

	public AuthenticationUserService() {
	}

	public void setUserSession(IUserSessionBean userSession) {
		this.userSession = userSession;
	}

	public boolean isLockoutEnforced(UserDTO user) {
		String result = userSession.getEntityPreference(user.getEntity().getId(), ServerConstants.PREFERENCE_FAILED_LOGINS_LOCKOUT);
		if (null != result && !result.trim().isEmpty()) {
			int allowedRetries = Integer.parseInt(result);
			return allowedRetries > 0;
		}
		return false;
	}

	/**
	 * method to save new encrypted password and encryption scheme for a user
	 */
    public void saveUser(String userName, Integer entityId, String newPasswordEncoded, Integer newScheme){
    	UserBL bl = new UserBL(userName, entityId);
    	bl.saveUserWithNewPasswordScheme(bl.getEntity().getId(), entityId, newPasswordEncoded, newScheme);
    }

	/**
	 * check if user's encryption scheme and jbilling.properties encryption scheme is same or different
	 */
	public Boolean isEncryptionSchemeSame(Integer entityId, String userName, Integer schemeId){
		UserBL bl = new UserBL(userName, entityId);
		return bl.isEncryptionSchemeSame(schemeId);
	}

	/**
	 * get encryption scheme of the user
	 */
	public Integer getEncryptionSchemeOfUser(Integer entityId, String userName){
		UserBL bl = new UserBL(userName, entityId);
		return bl.getEncryptionSchemeOfUser(userName, entityId);
	}
}

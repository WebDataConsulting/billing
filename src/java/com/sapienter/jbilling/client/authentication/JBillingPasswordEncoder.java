/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.client.authentication;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.security.JBCrypto;
import com.sapienter.jbilling.common.Util;

/**
 * Implementation of the Spring Security {@link PasswordEncoder} using jBilling's own
 * cryptology algorithm.
 *
 * @author Bryan Cowdery
 * @author Khurram M Cheema
 * @author Vladimir Carevski
 *
 * @since 17-06-2014
 * @version 4.0
 */
public class JBillingPasswordEncoder implements PasswordEncoder {

	private AuthenticationUserService userService;

	public JBillingPasswordEncoder() {
	}

	/**
	 * Encodes a password using jBillings own cryptology algorithm. This implementation may
	 * use of a random secure salt value according to configurations of used algorithm.
	 * Given companyUserDetails values will be used to calculated right digesting algorithm.
	 *
	 * @param password password to encode
	 * @param companyUserDetails company user details
	 * @return encoded password
	 * @throws DataAccessException
	 */
	public String encodePassword(String password, Object companyUserDetails) throws DataAccessException {
		Integer encoderId = getPasswordEncoderId(companyUserDetails);
		return JBCrypto.encodePassword(encoderId, password);
	}

	/**
	 * Returns true if the 2 given encoded passwords match.
	 *
	 * @param encPass encoded password from stored user
	 * @param rawPass plain-text password from authentication form
	 * @param companyUserDetails company user details
	 * @return true if passwords match, false if not
	 * @throws DataAccessException
	 */
	public boolean isPasswordValid(String encPass, String rawPass, Object companyUserDetails) throws DataAccessException {
		Boolean match = false;

		Integer configPassEncoderId = Integer.parseInt(Util.getSysProp(Constants.PASSWORD_ENCRYPTION_SCHEME));

		if (null != companyUserDetails && companyUserDetails instanceof CompanyUserDetails) {

			String userName = ((CompanyUserDetails) companyUserDetails).getPlainUsername();
			Integer entityId = ((CompanyUserDetails) companyUserDetails).getCompanyId();

			if (!userService.isEncryptionSchemeSame(entityId, userName, configPassEncoderId)) {

				// encryption scheme for the user is change from what is mentioned in
				// jbilling.properties so we need to update encrypted password and hash method
				Integer userPassEncoderId = userService.getEncryptionSchemeOfUser(entityId, userName);

				//check for password validity
				match = JBCrypto.passwordsMatch(userPassEncoderId, encPass, rawPass);

				//only update if the customer is providing the correct password
				//if we update when user is providing wrong password we would
				//effectively reset the password to something valid
				if (null != match && match.booleanValue()) {
					//create new encrypted password
					String newPassword = JBCrypto.encodePassword(configPassEncoderId, rawPass);
					//saving changes to the base_user table
					userService.saveUser(userName, entityId, newPassword, configPassEncoderId);
				}
			} else {

				//both the encryption schemes are the same so no need for new encryption
				match = JBCrypto.passwordsMatch(configPassEncoderId, encPass, rawPass);
			}
		}
		return match;
	}

	/**
	 * Returns the configured password encoding scheme.
	 *
	 * @param salt - used to pass company user details
	 * @return configured password encoder
	 */
	private Integer getPasswordEncoderId(Object salt) {
		Integer mainRoleId = null;
		if (null != salt && salt instanceof CompanyUserDetails) {
			CompanyUserDetails companyUserDetails = (CompanyUserDetails) salt;
			mainRoleId = companyUserDetails.getMainRoleId();
		}
		return JBCrypto.getPasswordEncoderId(mainRoleId);
	}


	public void setUserService(AuthenticationUserService userService) {
		this.userService = userService;
	}
}

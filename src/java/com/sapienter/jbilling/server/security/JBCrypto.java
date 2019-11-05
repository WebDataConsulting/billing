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
package com.sapienter.jbilling.server.security;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.util.Context;

import com.sapienter.jbilling.server.util.ServerConstants;
import org.springframework.security.authentication.encoding.PasswordEncoder;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Map;

/**
 * JBilling Utility class to help hashing of passwords and in future possibly
 * encryption of other pieces of information such as credit cards or encryption
 * of uploaded documents.
 *
 * Regarding password hashing this class implement the security practices and
 * policies that JBilling has adopted.
 *
 * WARNING: In JBilling, dealing with passwords (encoding and comparing) should
 * be done through this class and not with custom code.
 *
 * Usage:
 *
 * Hashing a Password with a given hashing method ID (methodId).
 *
 * String dbPassword = JBCrypto.encodePassword(methodId, plainPassword);
 *
 * Always use JBCrypto.encodePassword(..) for encoding password. The method will
 * deal with salt generation, if the method requires it and it will also do
 * password and salt merging to make the resulting string ready to be preserved
 * into database. In JBilling we follow the Spring Security way of storing
 * passwords in database. We generate different salt for each user and before
 * we store that into database we concatenate the password and salt with format
 * 'password{salt}'.
 *
 * Comparing two passwords, hashed password and plain text password with a
 * given method (methodId)
 *
 * boolean match = JBCrypto.passwordsMatch(methodId, encodedPassword, plainPassword)
 *
 * Always use JBCrypto.passwordsMatch(..) for comparing two passwords for a given
 * hashing method. The method will deal with demerging the salt from the encoded
 * password, if the hashing method requires it, and using that salt to hash the
 * plain text password so they can be compared. When comparing the two password we
 * rely on the Spring Security way of comparing two passwords which gives us
 * SlowEquals (comparing passwords with 'length-constant' time) way of comparing
 * password for better security.
 *
 * Note for Improvement:
 *
 * For the hashing methods that require salt this class will generate random salt
 * using the class SecureRandom with 24 byte
 *
 *
 * @author Bryan Cowdery
 * @author Emiliano Conde
 * @author Vladimir Carevski
 * @version 4.0
 */
public class JBCrypto {

	public static final int MIN_UNDIGESTED_ROLE = CommonConstants.TYPE_PARTNER;
	public static final JBillingHashingMethod DEFAULT_PASSWORD_ENCODER = JBillingHashingMethod.BCRYPT;

	// we will be letting Spring create the PasswordEncoders
	// but this is good for testing this class
	private static Map<String, PasswordEncoder> encoders = null;

	//we don't need any instances from this class for now
	private JBCrypto() {}

	/**
	 * Returns PasswordEncoder ID that will be used to hashing (encoding) the password.
	 * <p/>
	 * If the role of the user is INTERNAL, ROOT or CLERK and configured password is PLAIN
	 * then the system fall back to using internal default hashing algorithm. ROOT and CLERKS
	 * can not have un-hashed passwords.
	 *
	 * @param role - the role of the user that we are trying to find out PasswordEncoder
	 * @return the calculated password encoder object
	 */
	public static Integer getPasswordEncoderId(Integer role) {
		Integer passwordScheme = Integer.parseInt(Util.getSysProp(ServerConstants.PASSWORD_ENCRYPTION_SCHEME));
		JBillingHashingMethod passwordEncoder = JBillingHashingMethod.getById(passwordScheme);

		if (passwordEncoder.equals(JBillingHashingMethod.PLAIN) && null != role && role < MIN_UNDIGESTED_ROLE) {
			passwordEncoder = DEFAULT_PASSWORD_ENCODER;
		}
		return passwordEncoder.getId();
	}

	/**
	 * Returns PasswordEncoder object by id
	 *
	 * @param id - password encoder id
	 * @return PasswordEncoder object
	 * @throws java.lang.IllegalArgumentException - in case where the id is not valid
	 */
	private static PasswordEncoder getPasswordEncoderById(Integer id) {
		JBillingHashingMethod encoder = JBillingHashingMethod.getById(id);
		if (null == encoder) {
			throw new IllegalArgumentException("PasswordEncoder with id " + id + " does not exist");
		}
		return null != encoders && !encoders.isEmpty()
				? encoders.get(encoder.getEncoderBeanName())
				: Context.<PasswordEncoder>getBean(encoder.getEncoderBeanName());
	}

	/**
	 * Checks if the password encoder with given ID requires salt.
	 *
	 * @param passwordEncoderId - the ID of the password encoder
	 * @return true if the password encoder uses salt, false otherwise
	 */
	public static boolean requiresSalt(Integer passwordEncoderId) {
		return JBillingHashingMethod.getById(passwordEncoderId).isSalted();
	}

	/**
	 * Generates a random salt represented in byte array
	 *
	 * @param saltSize - the size of the generated salt in bytes
	 * @return String - the generated salt
	 */
	private static String generateRandomSalt(final int saltSize) {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[saltSize];
		random.nextBytes(salt);
		String saltString = null;
		try {
			saltString = new String(salt, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			saltString = new String(salt);
		}
		return saltString;
	}

	/**
	 * A helper method that extracts the password and salt from a string
	 * that has both of this pieces of information. JBilling follows the
	 * SpringSecurity way of merging password and salt i.e the resulting
	 * string from merging password and salt is:
	 * <p/>
	 * dbPassword = password{salt}
	 *
	 * @param mergedPasswordSalt - string that contains password merged with salt
	 * @return an arrays with two elements, first element is password, second element is salt
	 */
	private static String[] demergePasswordAndSalt(String mergedPasswordSalt) {
		if ((mergedPasswordSalt == null) || "".equals(mergedPasswordSalt)) {
			throw new IllegalArgumentException("Cannot pass a null or empty String");
		}

		String password = mergedPasswordSalt;
		String salt = "";

		int saltBegins = mergedPasswordSalt.lastIndexOf("{");

		if ((saltBegins != -1) && ((saltBegins + 1) < mergedPasswordSalt.length())) {
			salt = mergedPasswordSalt.substring(saltBegins + 1, mergedPasswordSalt.length() - 1);
			password = mergedPasswordSalt.substring(0, saltBegins);
		}

		return new String[]{password, salt};
	}

	private static String mergePasswordAndSalt(String password, String salt) {
		return mergePasswordAndSalt(password, salt, false);
	}

	/**
	 * Helper method that will merge password and salt. In JBilling we adopt
	 * the SpringSecurity way of merging password and salt before they are
	 * preserved in the database i.e. the resulting string that we get by
	 * merging has the format:
	 * <p/>
	 * dbPassword = password{salt}
	 *
	 * @param password - password to be merged with salt
	 * @param salt     - salt to be merged with password
	 * @param strict   - true if characters { and } are not allowed in the salt, false if they are allowed
	 * @return merged password and salt
	 */
	private static String mergePasswordAndSalt(String password, String salt, boolean strict) {
		if (password == null) {
			password = "";
		}

		if (strict && (salt != null)) {
			if ((salt.lastIndexOf("{") != -1) || (salt.lastIndexOf("}") != -1)) {
				throw new IllegalArgumentException("Cannot use { or } in salt.toString()");
			}
		}

		if ((salt == null) || "".equals(salt)) {
			return password;
		} else {
			return password + "{" + salt + "}";
		}
	}

	/**
	 * It will return encoded password ready to be preserved into database. If
	 * the hashing method that is used to encode the password requires salting
	 * then the salt is generated and merged with the password so that both of
	 * them can be preserved in the database. The format of the merged password
	 * and salt is 'password{salt}' (no quotes).
	 *
	 * @param methodId - hashing method to be used for encoding the password
	 * @param password - the plain text password
	 * @return a string representing encoded password, possibly merged with the
	 * generated salt value.
	 */
	public static String encodePassword(Integer methodId, String password) {
		JBillingHashingMethod hashingMethod = JBillingHashingMethod.getById(methodId);
		PasswordEncoder passwordEncoder = JBCrypto.getPasswordEncoderById(methodId);
		password = null != password ? password.trim() : "";
		String encodedPassword = null;
		if (JBCrypto.requiresSalt(methodId)) {
			String salt = JBCrypto.generateRandomSalt(hashingMethod.getSaltSize());
			encodedPassword = passwordEncoder.encodePassword(password, salt);
			encodedPassword = JBCrypto.mergePasswordAndSalt(encodedPassword, salt);
		} else {
			encodedPassword = passwordEncoder.encodePassword(password, null);
		}
		return encodedPassword;
	}

	/**
	 * This method should be used for comparing two passwords.
	 *
	 * @param methodId        - method that was used to has the encodedPassword, will also be used to hash the plainPassword
	 * @param encodedPassword - encodedPassword hashed with methodId, potentially contains merged salt
	 * @param plainPassword   - plan text password to compare against.
	 * @return true if the passwords match, false otherwise.
	 */
	public static boolean passwordsMatch(Integer methodId, String encodedPassword, String plainPassword) {
		PasswordEncoder passwordEncoder = JBCrypto.getPasswordEncoderById(methodId);
		plainPassword = null != plainPassword ? plainPassword.trim() : "";

		String salt = null;

		//if the method that was used to encode the password requires
		//salt then separate the salt and encoded password
		if (JBCrypto.requiresSalt(methodId)) {
			String[] tokens = JBCrypto.demergePasswordAndSalt(encodedPassword);
			encodedPassword = tokens[0];
			salt = tokens[1];
		}

		return passwordEncoder.isPasswordValid(encodedPassword, plainPassword, salt);
	}

	/**
	 * Password encoders to be used
	 *
	 * @param encoders
	 */
	public static void setEncoders(Map<String, PasswordEncoder> encoders) {
		JBCrypto.encoders = encoders;
	}
}

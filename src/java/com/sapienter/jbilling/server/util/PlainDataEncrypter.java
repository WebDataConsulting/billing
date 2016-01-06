package com.sapienter.jbilling.server.util;

/**
 * Plain data encryption mechanism. This mechanism does not
 * actually encrypt sensitive data.
 * Actual encryption implementations should implement the
 * {@linkplain DataEncrypter} interface accordingly and
 * redefine the <code>dataEncrypter</code> Spring bean.
 */
public class PlainDataEncrypter implements DataEncrypter {

	@Override
	public String encrypt(String plain) {
		return plain;
	}

	@Override
	public String decrypt(String encrypted) {
		return encrypted;
	}

}

package com.sapienter.jbilling.server.util;

/**
 * Data encryption interface. Encryption procedures should
 * implement this interface.
 */
public interface DataEncrypter {

	String encrypt(String plain);
	String decrypt(String encrypted);
}

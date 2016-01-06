package com.sapienter.jbilling.client.authentication;

import com.sapienter.jbilling.server.security.JBCrypto;
import com.sapienter.jbilling.server.security.JBillingHashingMethod;
import junit.framework.TestCase;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Maeis Gharibjanian
 * @author Vladimir Carevski
 * @since 10/28/13
 */
public class JBCryptoTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
		initEncoders();
	}

	private void initEncoders(){
		Map<String, PasswordEncoder> encoders = new HashMap<String, PasswordEncoder>();
		encoders.put(JBillingHashingMethod.PLAIN.getEncoderBeanName(), new PlaintextPasswordEncoder());
		encoders.put(JBillingHashingMethod.MD5.getEncoderBeanName(), new Md5PasswordEncoder());
		encoders.put(JBillingHashingMethod.SHA1.getEncoderBeanName(), new ShaPasswordEncoder());
		encoders.put(JBillingHashingMethod.SHA256.getEncoderBeanName(), new ShaPasswordEncoder(256));
//		encoders.put("bCryptPasswordEncoder", new BCryptPasswordEncoder()); //part of grails plugin which is not on test classpath
		JBCrypto.setEncoders(encoders);
	}

	public void testEncodePassword() {
        System.out.println("#testEncodePassword");
        String encodedPassword1 = JBCrypto.encodePassword(2, "123qwe");
        String encodedPassword2 = JBCrypto.encodePassword(2, "123qwe");

        System.out.println("encodedPassword1: " + encodedPassword1);
        System.out.println("encodedPassword2: " + encodedPassword2);

        assertTrue(encodedPassword1.equals(encodedPassword2));
    }

    public void testIsPasswordValid() {
        System.out.println("#testIsPasswordValid");
        String encodedPassword = JBCrypto.encodePassword(2, "123qwe");

	    System.out.println("Password: 123qwe encoded as: " + encodedPassword);

        assertTrue(JBCrypto.passwordsMatch(2, encodedPassword, "123qwe"));
        assertFalse(JBCrypto.passwordsMatch(2, encodedPassword, "123qwf"));
    }

}

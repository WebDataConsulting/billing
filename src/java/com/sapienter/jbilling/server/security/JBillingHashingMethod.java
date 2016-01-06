package com.sapienter.jbilling.server.security;

/**
 * Password hashing methods supported by JBilling
 *
 * When adding new methods only append them as next in the line.
 * Do not change password encoders sequence.
 *
 * @author Vladimir Carevski
 * @since 22-OCT-2014
 */
public enum JBillingHashingMethod {

	PLAIN       (1, "PLAIN",    "plainTextPasswordEncoder",     false),
	MD5         (2, "MD5",      "md5PasswordEncoder",           false),
	MD5_SALT    (3, "MD5_SALT", "md5PasswordEncoder",           true    ,16),
	SHA1        (4, "SHA1",     "sha1PasswordEncoder",          true    ,20),
	SHA256      (5, "SHA256",   "sha256PasswordEncoder",        true    ,32),
	BCRYPT      (6, "BCRYPT",   "bCryptPasswordEncoder",        false); //built-in salt management

	private Integer id;
	private String methodName;
	private String encoderBeanName;
	private boolean salted;
	private int saltSize; //in bytes

	JBillingHashingMethod(Integer id, String methodName, String encoderBeanName, boolean salted, int saltSize){
		this.id = id;
		this.methodName = methodName;
		this.encoderBeanName = encoderBeanName;
		this.salted = salted;
		this.saltSize = saltSize;
	}

	/**
	 * Sets default salt size of 32 bytes.
	 */
	JBillingHashingMethod(Integer id, String methodName, String encoderBeanName, boolean salted){
		this(id, methodName, encoderBeanName, salted, 32);
	}

	public Integer getId() {
		return id;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getEncoderBeanName() {
		return encoderBeanName;
	}

	public boolean isSalted() {
		return salted;
	}

	public int getSaltSize() {
		return saltSize;
	}

	public static JBillingHashingMethod getById(Integer id) {
		for (JBillingHashingMethod passwordEncoder : values()) {
			if (0 == id.compareTo(passwordEncoder.getId())) {
				return passwordEncoder;
			}
		}
		return null;
	}

}

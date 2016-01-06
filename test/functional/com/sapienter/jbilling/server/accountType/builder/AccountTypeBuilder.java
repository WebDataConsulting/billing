package com.sapienter.jbilling.server.accountType.builder;

import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladimir Carevski
 * @since 12-FEB-2015
 */
public class AccountTypeBuilder {

	public static final Integer MONTHLY_PERIOD = Integer.valueOf(2);//fixed constant for now
	public static final Integer DEFAULT_INVOICE_DELIVERY_METHOD = Integer.valueOf(1);

	private Integer id;
	private Integer entityId;
	private Integer languageId;
	private Integer currencyId;
	private BigDecimal creditLimitNotification1;
	private BigDecimal creditLimitNotification2;
	private Integer invoiceDeliveryMethodId;
	private BigDecimal creditLimit;
	private String invoiceDesign;
	private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>();
	private MainSubscriptionWS mainSubscription;
	private Integer[] informationTypeIds;
	private Integer[] paymentMethodTypeIds;

	public AccountTypeBuilder id(Integer id) {
		this.id = id;
		return this;
	}

	public AccountTypeBuilder entityId(Integer entityId) {
		this.entityId = entityId;
		return this;
	}

	public AccountTypeBuilder languageId(Integer languageId) {
		this.languageId = languageId;
		return this;
	}

	public AccountTypeBuilder currencyId(Integer currencyId) {
		this.currencyId = currencyId;
		return this;
	}

	public AccountTypeBuilder creditLimitNotification1(BigDecimal creditLimitNotif) {
		this.creditLimitNotification1 = creditLimitNotif;
		return this;
	}

	public AccountTypeBuilder creditLimitNotification2(BigDecimal creditLimitNotif) {
		this.creditLimitNotification2 = creditLimitNotif;
		return this;
	}

	public AccountTypeBuilder invoiceDeliveryMethod(Integer invoiceDeliveryMethodId) {
		this.invoiceDeliveryMethodId = invoiceDeliveryMethodId;
		return this;
	}

	public AccountTypeBuilder creditLimit(BigDecimal creditLimit) {
		this.creditLimit = creditLimit;
		return this;
	}

	public AccountTypeBuilder invoiceDesign(String invoiceDesign) {
		this.invoiceDesign = invoiceDesign;
		return this;
	}

	public AccountTypeBuilder addDescription(String name, Integer languageId) {
		InternationalDescriptionWS description = new InternationalDescriptionWS(languageId, name);
		return addDescription(description);
	}

	public AccountTypeBuilder addDescription(InternationalDescriptionWS description) {
		descriptions.add(description);
		return this;
	}

	public AccountTypeBuilder noDescriptions() {
		this.descriptions = null;
		return this;
	}

	public AccountTypeBuilder mainSubscription(MainSubscriptionWS mainSubscription) {
		this.mainSubscription = mainSubscription;
		return this;
	}

	public AccountTypeBuilder informationTypeIds(Integer[] informationTypeIds) {
		this.informationTypeIds = informationTypeIds;
		return this;
	}

	public AccountTypeBuilder paymentMethodTypeIds(Integer[] paymentMethodTypeIds) {
		this.paymentMethodTypeIds = paymentMethodTypeIds;
		return this;
	}

	public AccountTypeWS build() {
		BigDecimal ZERO = new BigDecimal("0");
		AccountTypeWS accountType = new AccountTypeWS();
		accountType.setId(null != id ? id : 0);
		accountType.setEntityId(null != entityId ? entityId : ApiTestCase.TEST_ENTITY_ID);
		accountType.setLanguageId(null != languageId ? languageId : ApiTestCase.TEST_LANGUAGE_ID);
		accountType.setCurrencyId(null != currencyId ? currencyId : Integer.valueOf(1));
		accountType.setCreditNotificationLimit1(null != creditLimitNotification1 ? creditLimitNotification1 : ZERO);
		accountType.setCreditNotificationLimit2(null != creditLimitNotification2 ? creditLimitNotification2 : ZERO);
		accountType.setInvoiceDeliveryMethodId(null != invoiceDeliveryMethodId ? invoiceDeliveryMethodId : DEFAULT_INVOICE_DELIVERY_METHOD);
		accountType.setCreditLimit(null != creditLimit ? creditLimit : ZERO);
		accountType.setInvoiceDesign(null != invoiceDesign ? invoiceDesign : null);
		if (null != descriptions && descriptions.isEmpty()) {
			addDescription("Test Account Type #" + System.currentTimeMillis(), accountType.getLanguageId());
		}
		accountType.setDescriptions(null != descriptions ? descriptions : null);
		accountType.setMainSubscription(null != mainSubscription ? mainSubscription : new MainSubscriptionWS(MONTHLY_PERIOD, 1));
		accountType.setInformationTypeIds(null != informationTypeIds ? informationTypeIds : null);
		accountType.setPaymentMethodTypeIds(null != paymentMethodTypeIds ? paymentMethodTypeIds : null);
		return accountType;
	}

	public AccountTypeWS create(JbillingAPI api) {
		return create(api, false);
	}

	public AccountTypeWS create(JbillingAPI api, boolean refresh) {
		if (null == api) throw new IllegalStateException("API not initialized in Builder");

		AccountTypeWS accountType = build();
		Integer id = api.createAccountType(accountType);
		accountType.setId(id);
		if (refresh) {
			accountType = api.getAccountType(id);
		}
		return accountType;
	}

}

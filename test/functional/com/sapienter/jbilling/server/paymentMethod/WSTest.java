package com.sapienter.jbilling.server.paymentMethod;


import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by anandkushwaha on 12/2/15.
 */
@Test(groups = {"web-services", "paymentMethod"})
public class WSTest extends ApiTestCase {

	private static Integer MONTHLY_PERIOD;
	private static Integer CURRENCY_ID;
	private static Integer LANGUAGE_ID;
	private static final Integer INVOICE_DELIVERY_ID = Integer.valueOf(1);
	private static final Integer PAYMENT_TEMPLATE_CARD = Integer.valueOf(1);


	@Override
	protected void prepareTestInstance() throws Exception {
		super.prepareTestInstance();
		CURRENCY_ID = Constants.PRIMARY_CURRENCY_ID;
		LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
		MONTHLY_PERIOD = getOrCreateMonthlyOrderPeriod(api);
	}

	@Test
	public void test001CreatePaymentMethodType() {
		AccountTypeWS accountType = createAccountType(MONTHLY_PERIOD);
		Integer newAccountTypeId = api.createAccountType(accountType);
		accountType = api.getAccountType(newAccountTypeId);
		Assert.assertEquals("New account type should be created", newAccountTypeId, accountType.getId());

		PaymentMethodTypeWS paymentMethod = createPaymentMethod();
		paymentMethod.setAllAccountType(false);
		ArrayList accountTypes = new ArrayList<Integer>();
		accountTypes.add(api.getAccountType(newAccountTypeId).getId());
		paymentMethod.setAccountTypes(accountTypes);
		Integer paymentMethodTypeId = api.createPaymentMethodType(paymentMethod);
		Assert.assertEquals("New payment method type should be created", paymentMethodTypeId,
				api.getPaymentMethodType(paymentMethodTypeId).getId());
		PaymentMethodTypeWS existing = api.getPaymentMethodType(paymentMethodTypeId);
		Assert.assertEquals("Account type should be associated with payment method type",
				existing.getAccountTypes().size(), 1);

		//cleanup
		boolean deleteAccountType = api.deleteAccountType(newAccountTypeId);
		Assert.assertEquals(deleteAccountType, true);
		boolean deletePaymentMethodType = api.deletePaymentMethodType(paymentMethodTypeId);
		Assert.assertEquals(deletePaymentMethodType, true);
	}

	@Test
	public void test002UpdatePaymentMethodType() {
		AccountTypeWS accountTypeWS1 = createAccountType(MONTHLY_PERIOD);
		Integer newAccountTypeId1 = api.createAccountType(accountTypeWS1);
		Assert.assertEquals(newAccountTypeId1, api.getAccountType(newAccountTypeId1).getId());

		AccountTypeWS accountTypeWS2 = createAccountType(MONTHLY_PERIOD);
		Integer newAccountTypeId2 = api.createAccountType(accountTypeWS2);
		Assert.assertEquals(newAccountTypeId2, api.getAccountType(newAccountTypeId2).getId());

		PaymentMethodTypeWS paymentMethod = createPaymentMethod();
		paymentMethod.setAllAccountType(false);
		ArrayList accountType = new ArrayList<Integer>();
		accountType.add(api.getAccountType(newAccountTypeId1).getId());
		accountType.add(api.getAccountType(newAccountTypeId2).getId());
		paymentMethod.setAccountTypes(accountType);
		Integer paymentMethodTypeId = api.createPaymentMethodType(paymentMethod);
		Assert.assertEquals(paymentMethodTypeId, api.getPaymentMethodType(paymentMethodTypeId).getId());

		PaymentMethodTypeWS existing = api.getPaymentMethodType(paymentMethodTypeId);
		Assert.assertEquals("Both account Type should be associated with payment method type",
				existing.getAccountTypes().size(), 2);

		existing.setAllAccountType(true);
		api.updatePaymentMethodType(existing);
		Assert.assertEquals("Payment method type should be global", existing.isAllAccountType(), true);
		PaymentMethodTypeWS existing1 = api.getPaymentMethodType(paymentMethodTypeId);
		Assert.assertEquals("All account type should be associated with payment method type",
				existing1.getAccountTypes().size(), api.getAllAccountTypes().length);

		//cleanup
		boolean deleteAccountType1 = api.deleteAccountType(newAccountTypeId1);
		Assert.assertEquals(deleteAccountType1, true);
		boolean deleteAccountType2 = api.deleteAccountType(newAccountTypeId2);
		Assert.assertEquals(deleteAccountType2, true);
		boolean deletePaymentMethodType = api.deletePaymentMethodType(paymentMethodTypeId);
		Assert.assertEquals(deletePaymentMethodType, true);

	}

	//check paymentMethodType with allAccountType/true will be available for new account type
	@Test
	public void test003CreateAccountType() {
		PaymentMethodTypeWS paymentMethod = createPaymentMethod();
		paymentMethod.setAllAccountType(true);
		Integer paymentMethodTypeId = api.createPaymentMethodType(paymentMethod);

		PaymentMethodTypeWS paymentMethod1 = createPaymentMethod();
		paymentMethod1.setAllAccountType(true);
		Integer paymentMethodTypeId1 = api.createPaymentMethodType(paymentMethod1);


		AccountTypeWS accountTypeWS = createAccountType(MONTHLY_PERIOD);
		Integer newAccountTypeId = api.createAccountType(accountTypeWS);
		Assert.assertEquals(newAccountTypeId, api.getAccountType(newAccountTypeId).getId());

		Assert.assertTrue("Payment method type with setAllAccountType/true should be associated with new account type",
				Arrays.asList(api.getAccountType(newAccountTypeId).getPaymentMethodTypeIds()).contains(paymentMethodTypeId));
		Assert.assertTrue("Payment method type with setAllAccountType/true should be associated with new account type",
				Arrays.asList(api.getAccountType(newAccountTypeId).getPaymentMethodTypeIds()).contains(paymentMethodTypeId1));

		boolean deletePaymentMethodType1 = api.deletePaymentMethodType(paymentMethodTypeId1);
		Assert.assertEquals(deletePaymentMethodType1, true);

		//cleanup
		boolean deletePaymentMethodType = api.deletePaymentMethodType(paymentMethodTypeId);
		Assert.assertEquals(deletePaymentMethodType, true);
		boolean deleteAccountType = api.deleteAccountType(newAccountTypeId);
		Assert.assertEquals(deleteAccountType, true);
	}

	private MetaFieldWS copyMetaField(MetaFieldWS metaField) {
		MetaFieldWS mf = new MetaFieldWS();

		mf.setDataType(metaField.getDataType());
		mf.setDefaultValue(metaField.getDefaultValue());
		mf.setDisabled(metaField.isDisabled());
		mf.setDisplayOrder(metaField.getDisplayOrder());
		mf.setFieldUsage(metaField.getFieldUsage());
		mf.setFilename(metaField.getFilename());
		mf.setMandatory(metaField.isMandatory());
		mf.setName(metaField.getName());
		mf.setValidationRule(metaField.getValidationRule());
		mf.setPrimary(metaField.isPrimary());

		// set rule id to 0 so a new rule will be created
		if (mf.getValidationRule() != null) {
			mf.getValidationRule().setId(0);
		}

		return mf;
	}

	private AccountTypeWS createAccountType(Integer periodId) {
		AccountTypeWS accountType = new AccountTypeWS();
		accountType.setCreditLimit(new BigDecimal(0));
		accountType.setCurrencyId(CURRENCY_ID);
		accountType.setEntityId(api.getCallerCompanyId());
		accountType.setInvoiceDeliveryMethodId(INVOICE_DELIVERY_ID);
		accountType.setLanguageId(LANGUAGE_ID);
		accountType.setMainSubscription(new MainSubscriptionWS(periodId, 1));
		accountType.setCreditNotificationLimit1("0");
		accountType.setCreditNotificationLimit2("0");
		accountType.setCreditLimit("0");
		accountType.setName("account_type_" + System.currentTimeMillis(), LANGUAGE_ID);
		return accountType;
	}

	private PaymentMethodTypeWS createPaymentMethod() {
		PaymentMethodTypeWS paymentMethod = new PaymentMethodTypeWS();
		PaymentMethodTemplateWS paymentMethodTemplateWS = api.getPaymentMethodTemplate(PAYMENT_TEMPLATE_CARD);
		Set<MetaFieldWS> templateMetaFields = paymentMethodTemplateWS.getMetaFields();
		MetaFieldWS[] metaFields;
		if (templateMetaFields != null && templateMetaFields.size() > 0) {
			metaFields = new MetaFieldWS[templateMetaFields.size()];
			Integer i = 0;
			for (MetaFieldWS metaField : templateMetaFields) {
				MetaFieldWS mf = copyMetaField(metaField);
				mf.setEntityId(api.getCallerCompanyId());
				mf.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
				metaFields[i] = mf;

				i++;
			}
		} else {
			metaFields = new MetaFieldWS[0];
		}
		paymentMethod.setMetaFields(metaFields);
		paymentMethod.setTemplateId(paymentMethodTemplateWS.getId());
		paymentMethod.setMethodName(Long.toString(System.currentTimeMillis()));
		paymentMethod.setIsRecurring(false);
		return paymentMethod;
	}

	private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
		OrderPeriodWS[] periods = api.getOrderPeriods();
		for(OrderPeriodWS period : periods){
			if(1 == period.getValue().intValue() &&
					PeriodUnitDTO.MONTH == period.getPeriodUnitId().intValue()){
				return period.getId();
			}
		}
		//there is no monthly order period so create one
		OrderPeriodWS monthly = new OrderPeriodWS();
		monthly.setEntityId(api.getCallerCompanyId());
		monthly.setPeriodUnitId(1);//monthly
		monthly.setValue(1);
		monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(LANGUAGE_ID, "PYM:MONTHLY")));
		return api.createOrderPeriod(monthly);
	}

}

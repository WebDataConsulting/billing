package com.sapienter.jbilling.server.payment;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * The following class helps building payment methods from existing templates in databse
 *
 * @author Vladimir Carevski
 * @since 24-FEB-2014
 */
public final class PaymentMethodHelper {

	public static final Integer PAYMENT_TEMPLATE_CARD = Integer.valueOf(1);
	public static final Integer PAYMENT_TEMPLATE_ACH = Integer.valueOf(2);
	public static final Integer PAYMENT_TEMPLATE_CHEQUE = Integer.valueOf(3);

	public final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	public final static String CC_MF_NUMBER = "cc.number";
	public final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	public final static String CC_MF_TYPE = "cc.type";

	public final static String ACH_MF_ROUTING_NUMBER = "ach.routing.number";
	public final static String ACH_MF_BANK_NAME = "ach.bank.name";
	public final static String ACH_MF_CUSTOMER_NAME = "ach.customer.name";
	public final static String ACH_MF_ACCOUNT_NUMBER = "ach.account.number";
	public final static String ACH_MF_ACCOUNT_TYPE = "ach.account.type";

	public final static String CHEQUE_MF_BANK_NAME = "cheque.bank.name";
	public final static String CHEQUE_MF_DATE = "cheque.date";
	public final static String CHEQUE_MF_NUMBER = "cheque.number";

	public static PaymentMethodTypeWS buildCCTemplateMethod(JbillingAPI api) {
		return buildPaymentMethod(api, PAYMENT_TEMPLATE_CARD);
	}

	public static PaymentMethodTypeWS buildACHTemplateMethod(JbillingAPI api) {
		return buildPaymentMethod(api, PAYMENT_TEMPLATE_ACH);
	}

	public static PaymentMethodTypeWS buildChequeTemplateMethod(JbillingAPI api) {
		return buildPaymentMethod(api, PAYMENT_TEMPLATE_CHEQUE);
	}

	private static PaymentMethodTypeWS buildPaymentMethod(JbillingAPI api, Integer templateId) {
		PaymentMethodTypeWS paymentMethod = new PaymentMethodTypeWS();
		paymentMethod.setAllAccountType(Boolean.TRUE);
		PaymentMethodTemplateWS paymentMethodTemplateWS = api.getPaymentMethodTemplate(templateId);
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
		paymentMethod.setMethodName(paymentMethodTemplateWS.getTemplateName() + ":" + (System.currentTimeMillis() % 10000));
		paymentMethod.setIsRecurring(false);
		return paymentMethod;
	}


	private static MetaFieldWS copyMetaField(MetaFieldWS metaField) {
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

	public static PaymentInformationWS createCreditCard(Integer methodId, String cardHolderName, String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(methodId);
		cc.setProcessingOrder(Integer.valueOf(1));
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.STRING, 1, cardHolderName);
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.STRING, 2, cardNumber);
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true, DataType.STRING, 3,
				 DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).print(date.getTime()));
		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, true, false, DataType.INTEGER, 4, new Integer(0));
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}

	public static PaymentInformationWS createACH(Integer methodId, String customerName, String bankName,
	                                             String routingNumber, String accountNumber, Integer accountType) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(methodId);
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_ACH);
		cc.setProcessingOrder(new Integer(2));

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, ACH_MF_ROUTING_NUMBER, false, true,
				DataType.STRING, 1, routingNumber);
		addMetaField(metaFields, ACH_MF_CUSTOMER_NAME, false, true,
				DataType.STRING, 2, customerName);
		addMetaField(metaFields, ACH_MF_ACCOUNT_NUMBER, false, true,
				DataType.STRING, 3, accountNumber);
		addMetaField(metaFields, ACH_MF_BANK_NAME, false, true,
				DataType.STRING, 4, bankName);
		addMetaField(metaFields, ACH_MF_ACCOUNT_TYPE, false, true,
				DataType.ENUMERATION, 5, accountType == 1 ? Constants.ACH_CHECKING : Constants.ACH_SAVING);

		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}

	public static PaymentInformationWS createCheque(Integer methodId, String bankName, String chequeNumber, Date date) {
		PaymentInformationWS cheque = new PaymentInformationWS();
		cheque.setPaymentMethodTypeId(methodId);
		cheque.setPaymentMethodId(Constants.PAYMENT_METHOD_CHEQUE);
		cheque.setProcessingOrder(new Integer(3));

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CHEQUE_MF_BANK_NAME, false, true,
				DataType.STRING, 1, bankName);
		addMetaField(metaFields, CHEQUE_MF_NUMBER, false, true,
				DataType.STRING, 2, chequeNumber);
		addMetaField(metaFields, CHEQUE_MF_DATE, false, true,
				DataType.DATE, 3, date);
		cheque.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cheque;
	}

	private static void addMetaField(List<MetaFieldValueWS> metaFields,
	                                 String fieldName, boolean disabled, boolean mandatory,
	                                 DataType dataType, Integer displayOrder, Object value) {
		MetaFieldValueWS ws = new MetaFieldValueWS();
		ws.setFieldName(fieldName);
		ws.setDisabled(disabled);
		ws.setMandatory(mandatory);
		ws.setDataType(dataType);
		ws.setDisplayOrder(displayOrder);
		ws.setValue(value);

		metaFields.add(ws);
	}

}

package com.sapienter.jbilling.server.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import org.joda.time.DateTime;
import com.sapienter.jbilling.server.metafields.DataType;
import org.joda.time.format.DateTimeFormat;

/**
 * User: Nikhil
 * Date: 10/8/12
 * Description: A utility class written to help developers write test cases quickly covering some common tasks such as
 * a) Creating An OrderWS
 * b) Adding An Order Line to an OrderWS
 * c) Creating A PaymentWS
 * d) Creating A Customer returning UserWS
 * e) Creating A Customer ConatctWS
 * f) Pausing the thread for some seconds
 * g) Updating the billing configuration
 * h) Creating PlanWS
 * i) Get Contact Field Content
 */
public class CreateObjectUtil {

    /**
     * Creates an OrderWS object
     * @param userId
     * @param currencyId
     * @param billingType
     * @param orderPeriod
     * @param activeSince
     * @return
     */
	private final static int CC_PM_ID = 1;
	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	
    public static OrderWS createOrderObject(
            Integer userId, Integer currencyId, Integer billingType, Integer orderPeriod,  Date activeSince) {
        /*
        * Create
        */
        OrderWS newOrder = new OrderWS();

        newOrder.setUserId(userId);
        newOrder.setCurrencyId(currencyId);
        newOrder.setBillingTypeId(billingType);
        newOrder.setPeriod(orderPeriod);

        //Defaults
        newOrder.setNotes("Domain: www.test.com");

        newOrder.setActiveSince(activeSince);
//         On some branches this field is present, please uncomment if required
//        newOrder.setCycleStarts(cal.getTime());

        return newOrder;
    }

    /**
     * To add a line to an order
     * @param order
     * @param lineQty
     * @param lineTypeId
     * @param lineItemId
     * @param linePrice
     * @param description
     * @return
     */
    public static OrderWS addLine(OrderWS order, Integer lineQty, Integer lineTypeId, Integer lineItemId, BigDecimal linePrice, String description) {

        // store the existing lines
        OrderLineWS[] existingLines = order.getOrderLines();
        List<OrderLineWS> finalLines = new ArrayList<OrderLineWS>();
        // iterate over the array and add to the ArrayList
        if (null != existingLines) {
	        for( OrderLineWS oneItem : existingLines ) {
	            finalLines.add(oneItem);
	        }
        }
        // Now add some 1 line
        OrderLineWS line;
        line = new OrderLineWS();
        line.setTypeId(lineTypeId);
        line.setItemId(lineItemId);
        if (null != linePrice) {
            line.setPrice(linePrice);
        }
        line.setAmount(linePrice);
        line.setQuantity(lineQty);
        line.setDescription(description);
        finalLines.add(line);
        OrderLineWS[] simpleArray = new OrderLineWS[ finalLines.size() ];
        finalLines.toArray( simpleArray );
        order.setOrderLines(simpleArray);
        return order;
    }

    /**
     * To create a payment object
     */
    public static PaymentWS createPaymentObject(Integer userId, BigDecimal amount, Integer currencyId, boolean isRefund, Integer paymentMethodId,
                                                Date paymentDate, String paymentNotes, PaymentInformationWS instrument) {
        PaymentWS payment = new PaymentWS();
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setIsRefund((isRefund ? new Integer(1) : new Integer(0)));
        payment.setCurrencyId(currencyId);
        payment.setMethodId(paymentMethodId);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentNotes(paymentNotes);

        if(instrument != null) {
        	payment.getPaymentInstruments().add(instrument);
        }
        
        return payment;
    }

    /**
     * To create a customer
     */
    public static UserWS createCustomer( Integer currencyId, String userName, String password, Integer languageId, Integer mainRoleId, boolean isParent, Integer statusID,
                                         PaymentInformationWS instrument, ContactWS contact, MainSubscriptionWS subscriptionWS) {

        UserWS newUser = new UserWS();
        newUser.setUserName(userName);
        newUser.setLanguageId(languageId);
        newUser.setCurrencyId(currencyId);

        //Provide Defaults
        newUser.setPassword(password);
        newUser.setMainRoleId(mainRoleId);//customer
        newUser.setIsParent(isParent);//not parent
        newUser.setStatusId(statusID); //active user
        
        newUser.setAccountTypeId(Integer.valueOf(1));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor


        //contact info
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);
        
        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(1);
        
        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(1);
        
        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });

        // add a contact
        if(contact != null) {
            newUser.setContact(contact);
        }

        // instrument can be credit card or ach
        if(instrument != null) {
        	newUser.getPaymentInstruments().add(instrument);
        }
        

        // not on some branches currently, so remove this and also the parameter :(
        if(subscriptionWS != null) {
            newUser.setMainSubscription(subscriptionWS);
        }

        return newUser;
    }

    /**
     * To Create Customer Contact
     * @param email
     * @return
     */
    public static ContactWS createCustomerContact(String email) {
        ContactWS contact = new ContactWS();
        contact.setEmail(email);
        // rest of the fields are not mandatory
        return contact;
    }

    /**
     * To Pause the thread
     * @param t
     */
    public static void pause(long t) {

        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * To update the billing configuration
     * @param nextRunDate
     * @param maxPeriods
     * @param entityID
     * @param generateReport
     * @param onlyRecurring
     * @param invoiceDateProcess
     * 
     * @return
     */
    public static BillingProcessConfigurationWS updateBillingConfig(Date nextRunDate, Integer maxPeriods, Integer entityID,
                                                                    Integer generateReport, Integer onlyRecurring,
                                                                    Integer invoiceDateProcess) {

        BillingProcessConfigurationWS config = new BillingProcessConfigurationWS();

        config.setNextRunDate(nextRunDate);
        config.setMaximumPeriods(maxPeriods);
        config.setEntityId(entityID);
        config.setGenerateReport(generateReport);
        config.setInvoiceDateProcess(invoiceDateProcess);
        config.setOnlyRecurring(onlyRecurring);
        // present in some branches, pls uncomment if required
//        config.setPeriodUnitId(Constants.PERIOD_UNIT_MONTH);
//        config.setPeriodValue(1);
        return config;

    }

    // Present on some branches, uncomment if required
    /*public static String getContactFieldContent(ContactWS contact, Integer fieldId) {
        Integer index = 0;
        for(Integer id : contact.getFieldIDs()){
            if(id==fieldId){
                break;
            }
            index++;
        }

        return contact.getFieldValues()[index];
    }*/
    
    
    /**
	* To create a customer
	*/
    public static UserWS createCustomer(Integer currencyId, String userName,
            String password, Integer languageId, Integer mainRoleId,
            boolean isParent, Integer statusID, PaymentInformationWS instrument,
            ContactWS contact) {

        UserWS newUser = new UserWS();
        newUser.setUserName(userName);
        newUser.setLanguageId(languageId);
        newUser.setCurrencyId(currencyId);

        //defautl account type id
        newUser.setAccountTypeId(Integer.valueOf(1));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor


        //contact info
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);
        
        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(1);
        
        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(1);
        
        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });
        
        // Provide Defaults
        newUser.setPassword(password);
        newUser.setMainRoleId(mainRoleId);// customer
        newUser.setIsParent(isParent);// not parent
        newUser.setStatusId(statusID); // active user

        // add a contact
        if (contact != null) {
            newUser.setContact(contact);
        }

        // instrument can be credit card or ach
        if(instrument != null) {
        	newUser.getPaymentInstruments().add(instrument);
        }

        return newUser;
    }
    
    /**
	* To create a product without calling the createItem API.
	*
	* @param entityId
	* @param cost
	* @param currencyID
	* @param itemType
	* @return ItemDTOEx
	*/
    public static ItemDTOEx createItem(Integer entityId, BigDecimal cost, Integer currencyID, Integer itemType, String description) {
	     ItemDTOEx item = new ItemDTOEx();
	     item.setNumber(String.valueOf(new Date().getTime()));
	     item.setEntityId(entityId);
	     item.setDescription(description + Constants.SINGLE_SPACE + new Date().getTime());
	     item.setCurrencyId(currencyID);
	     item.setTypes(new Integer[] { itemType });
	     item.setPrice(cost);
	     return item;
    }
    
    public static ItemDTOEx createPercentageItem(Integer entityId, BigDecimal percentage, Integer currencyID, Integer itemType, String description) {
	     ItemDTOEx item = new ItemDTOEx();
	     item.setNumber(String.valueOf(new Date().getTime()));
	     item.setEntityId(entityId);
	     item.setDescription("Percentage Item: " + description + Constants.SINGLE_SPACE + new Date().getTime());
	     item.setCurrencyId(currencyID);
	     item.setTypes(new Integer[] { itemType });
         item.getDefaultPrices().put(new DateTime(2014, 12, 15, 0, 0, 0, 0).withTime(0, 0, 0, 0).toDate(), new PriceModelWS(PriceModelStrategy.LINE_PERCENTAGE
                .name(), percentage, currencyID));
	     return item;
    }   
    
    public static UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId) throws JbillingAPIException, IOException {
        return createUser(goodCC, parentId, currencyId, true);
    }

    public static UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId, boolean doCreate) throws JbillingAPIException, IOException {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        /*
        * Create - This passes the password validation routine.
        */
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("As$fasdf1");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(Integer.valueOf(1));
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setCreditLimit("1");
        newUser.setInvoiceChild(new Boolean(false));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("FrodoRecharge");
        metaField4.setGroupId(1);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("BagginsRecharge");
        metaField5.setGroupId(1);

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });

        // valid credit card must have a future expiry date to be valid for payment processing
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
        
        // add a credit card
        PaymentInformationWS cc = createCreditCard("Frodo Rech Baggins", goodCC ? "4929974024420784" : "4111111111111111",
				expiry.getTime());

        newUser.getPaymentInstruments().add(cc);

        if (doCreate) {
            System.out.println("Creating user ...");
	        Integer userId = api.createUser(newUser);
	        newUser = api.getUserWS(userId);
        }

        return newUser;
    }
    
    public static PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
		
		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true,
				DataType.STRING, 1, cardHolderName);
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.STRING, 2,
				cardNumber);
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.STRING, 3, DateTimeFormat.forPattern(
                        Constants.CC_DATE_FORMAT).print(date.getTime()));
		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, true, false,
				DataType.INTEGER, 4, new Integer(0));
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}
    
    public static void addMetaField(List<MetaFieldValueWS> metaFields,
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

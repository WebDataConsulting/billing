package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.partner.*;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.Asserts;
;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertEquals;
import com.sapienter.jbilling.server.util.CreateObjectUtil;

@Test(groups = { "web-services", "partner" })
public class CommissionTest {

    private static final Integer ADMIN_USER_ID = 2;
    private static final Integer PARTNER_ROLE_ID = 4;
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;

    Date startDate = new DateTime().minusMonths(2).toDate();
    Date endDate = new DateTime().plusMonths(2).toDate();
    DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");

    private static final FormatLogger LOG = new FormatLogger(CommissionTest.class);
    private JbillingAPI api = null;
    private final static String AGENT_NAME = "AgentTest";
    private final static String ITEM_NAME = "ItemTest";

    private void init() {
        try {
            api = JbillingAPIFactory.getAPI();
        } catch (Exception e) {
            LOG.error("Cannot initialize JBilling API.");
        }
    }

    @Test
    public void testCommissionCalculation() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer commissionItemId = null;
        PartnerWS standardPartner = null;
        PartnerWS masterPartner = null;
        PartnerWS referrerPartner = null;
        PartnerWS referrerPartnerWithCommission = null;
        PartnerWS childPartner = null;
        PartnerWS parentPartner = null;
        PartnerWS paymentPartner = null;
        PartnerWS exceptionPartner = null;
        PartnerWS dueExceptionPartner = null;
        PartnerWS unlimitedExceptionPartner = null;
        try {
            ItemDTOEx commissionItem = createItem(api, "commissionItem-" + new Date().getTime(), new BigDecimal("25.00"), new BigDecimal("50.00"));

            commissionItemId = commissionItem.getId();

            //Standard Partner
            standardPartner = createPartner(api, "Standard", PartnerType.STANDARD, null);
            createInvoiceForPartner(api, standardPartner, commissionItemId);
            System.out.println("standard finished");

            //Master Partner
            masterPartner = createPartner(api, "Master", PartnerType.MASTER, null);
            createInvoiceForPartner(api, masterPartner, commissionItemId);
            System.out.println("master finished");

            //Exception Partner
            exceptionPartner = createExceptionPartner(api, commissionItemId, null, null);
            createInvoiceForPartner(api, exceptionPartner, commissionItemId);
            System.out.println("exception finished");

            //Referrer Partner
            referrerPartner = createReferrerPartner(api, standardPartner);
            System.out.println("referrer finished");

            //Payment Partner
            paymentPartner = createPartner(api, "Payment", PartnerType.STANDARD, PartnerCommissionType.PAYMENT);
            Integer invoiceId = createInvoiceForPartner(api, paymentPartner, commissionItemId);
            applyPayment(api, paymentPartner.getUserId(), BigDecimal.ONE, invoiceId);
            System.out.println("payment finished");

            //Parent Partner
            parentPartner = createPartner(api, "Parent", PartnerType.STANDARD, null);
            System.out.println("parent finished");

            //Child Partner
            childPartner = createPartner(api, "Child", PartnerType.STANDARD, null);
            childPartner.setParentId(parentPartner.getId());
            updatePartner(api, childPartner);
            createInvoiceForPartner(api, childPartner, commissionItemId);
            System.out.println("child finished");

            //Due exception partner
            dueExceptionPartner = createExceptionPartner(api, commissionItemId, null, new DateTime().minusMonths(1).toDate());
            createInvoiceForPartner(api, dueExceptionPartner, commissionItemId);
            System.out.println("dueException finished");

            //Unlimited exception partner
            unlimitedExceptionPartner = createExceptionPartner(api, commissionItemId, dtf.parseDateTime("01/01/2004").toDate(), null);
            createInvoiceForPartner(api, unlimitedExceptionPartner, commissionItemId);
            System.out.println("unlimitedException finished");

            //Referrer Partner with own commission
            referrerPartnerWithCommission = createReferrerPartner(api, unlimitedExceptionPartner);
            createInvoiceForPartner(api, referrerPartnerWithCommission, commissionItemId);
            System.out.println("referrer with commission finished");

            //Configure commission process
            CommissionProcessConfigurationWS configurationWS = new CommissionProcessConfigurationWS();
            configurationWS.setEntityId(1);
            configurationWS.setNextRunDate(new DateTime().withDayOfMonth(1).toDate());
            configurationWS.setPeriodUnitId(PeriodUnitDTO.MONTH);
            configurationWS.setPeriodValue(1);
            //TODO: We don't have a way to remove for now the commission process.
            api.createUpdateCommissionProcessConfiguration(configurationWS);
            System.out.println("commission process configuration finished");

            //Trigger commission process.
            api.calculatePartnerCommissions();
            System.out.println("triggering the commission process finished.");

            //getting the commissionsRuns
            CommissionProcessRunWS[] commissionRuns = api.getAllCommissionRuns();

            CommissionProcessRunWS thisRun = null;
            for(CommissionProcessRunWS commissionRun : commissionRuns){
                if(new DateTime(commissionRun.getRunDate()).toDateMidnight().equals(new DateMidnight()) ){
                    System.out.println("This commission run found.");
                    thisRun = commissionRun;
                    break;
                }
            }

            if(thisRun == null){
                fail("Couldn't get the commission process run generated");
            }

            //Getting the commissions
            CommissionWS[] commissions = api.getCommissionsByProcessRunId(thisRun.getId());

            for(CommissionWS commission : commissions){
                //Standard Partner
                if(commission.getPartnerId().equals(standardPartner.getId())){
                    Asserts.assertEquals("Wrong standard partner commission amount", new BigDecimal("2.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type", CommissionType.DEFAULT_STANDARD_COMMISSION.name(), commission.getType());
                    //Master Partner
                }else if (commission.getPartnerId().equals(masterPartner.getId())){
                    Asserts.assertEquals("Wrong master partner commission amount",new BigDecimal("5.00"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_MASTER_COMMISSION.name(), commission.getType());
                    //Exception Partner
                }else if (commission.getPartnerId().equals(exceptionPartner.getId())){
                    Asserts.assertEquals("Wrong exception partner commission amount",new BigDecimal("7.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.EXCEPTION_COMMISSION.name(), commission.getType());
                    //Referrer Partner
                }else if (commission.getPartnerId().equals(referrerPartner.getId())){
                    Asserts.assertEquals("Wrong referrer partner commission amount",new BigDecimal("1.25"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.REFERRAL_COMMISSION.name(), commission.getType());
                    //Parent Partner
                }else if (commission.getPartnerId().equals(parentPartner.getId())){
                    Asserts.assertEquals("Wrong parent partner commission amount",new BigDecimal("2.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_STANDARD_COMMISSION.name(), commission.getType());
                    //Payment Partner
                }else if (commission.getPartnerId().equals(paymentPartner.getId())){
                    Asserts.assertEquals("Wrong payment partner commission amount",new BigDecimal("0.25"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_STANDARD_COMMISSION.name(), commission.getType());
                    //Due Exception Partner
                }else if (commission.getPartnerId().equals(dueExceptionPartner.getId())){
                    Asserts.assertEquals("Wrong dueException partner commission amount",new BigDecimal("2.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_STANDARD_COMMISSION.name(), commission.getType());
                    //Unlimited Exception Partner
                }else if (commission.getPartnerId().equals(unlimitedExceptionPartner.getId())){
                    Asserts.assertEquals("Wrong unlimitedException partner commission amount",new BigDecimal("7.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.EXCEPTION_COMMISSION.name(), commission.getType());
                    //Referrer partner with commission
                }else if (commission.getPartnerId().equals(referrerPartnerWithCommission.getId())){
                    Asserts.assertEquals("Wrong referrer partner with commission, commission amount",new BigDecimal("5.00"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_MASTER_COMMISSION.name(), commission.getType());
                }
            }
        } finally {
            api.deleteItem(commissionItemId);
            deletePartner(api, standardPartner);
            api.deleteUser(standardPartner.getUserId());

            deletePartner(api, masterPartner);
            api.deleteUser(masterPartner.getUserId());

            deletePartner(api, referrerPartner);
            api.deleteUser(referrerPartner.getUserId());

            deletePartner(api, referrerPartnerWithCommission);
            api.deleteUser(referrerPartnerWithCommission.getUserId());

            deletePartner(api, childPartner);
            api.deleteUser(childPartner.getUserId());

            deletePartner(api, parentPartner);
            api.deleteUser(parentPartner.getUserId());

            deletePartner(api, paymentPartner);
            api.deleteUser(paymentPartner.getUserId());

            deletePartner(api, exceptionPartner);
            api.deleteUser(exceptionPartner.getUserId());

            deletePartner(api, dueExceptionPartner);
            api.deleteUser(dueExceptionPartner.getUserId());

            deletePartner(api, unlimitedExceptionPartner);
            api.deleteUser(unlimitedExceptionPartner.getUserId());
        }
    }

    private PartnerWS createPartner(JbillingAPI api, String name, PartnerType partnerType, PartnerCommissionType partnerCommissionType){
        // new partner
        String version = ""+new Date().getTime();
        UserWS user = new UserWS();
        user.setUserName("partner-01-" + version);
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(PARTNER_ROLE_ID);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        ContactWS contact = new ContactWS();
        contact.setEmail(user.getUserName() + "@test.com");
        contact.setFirstName("Partner" + name);
        contact.setLastName(version);
        user.setContact(contact);

        PartnerWS partner = new PartnerWS();
        partner.setType(partnerType.name());
        if(partnerCommissionType != null){
            partner.setCommissionType(partnerCommissionType.name());
        }

        // create partner
        partner.setId(api.createPartner(user, partner));

        return api.getPartner(partner.getId());
    }

    private void updatePartner(JbillingAPI api, PartnerWS partner){
        api.updatePartner(api.getUserWS(partner.getUserId()), partner);
    }

    private void deletePartner(JbillingAPI api, PartnerWS partner) {
        partner.setCommissions(new CommissionWS[0]);
        updatePartner(api, partner);
        api.deletePartner(partner.getId());
    }

    private PartnerWS createExceptionPartner(JbillingAPI api, Integer commissionItemId, Date startDate, Date endDate){
        PartnerWS exceptionPartner = createPartner(api, "Exception", PartnerType.STANDARD, null);
        PartnerCommissionExceptionWS commissionException = new PartnerCommissionExceptionWS();
        commissionException.setPercentage(new BigDecimal("75.00"));
        commissionException.setStartDate(startDate != null ? startDate : this.startDate);
        commissionException.setEndDate(endDate != null ? endDate : this.endDate);
        commissionException.setItemId(commissionItemId);
        exceptionPartner.setCommissionExceptions(new PartnerCommissionExceptionWS[]{commissionException});
        updatePartner(api, exceptionPartner);
        return  exceptionPartner;
    }

    private PartnerWS createReferrerPartner(JbillingAPI api, PartnerWS referralPartner){
        PartnerWS referrerPartner = createPartner(api, "Referrer", PartnerType.MASTER, null);
        PartnerReferralCommissionWS referralCommission = new PartnerReferralCommissionWS();
        referralCommission.setPercentage(new BigDecimal("50.00"));
        referralCommission.setStartDate(startDate);
        referralCommission.setEndDate(endDate);
        referralCommission.setReferralId(referralPartner.getId());
        referrerPartner.setReferrerCommissions(new PartnerReferralCommissionWS[]{referralCommission});
        updatePartner(api, referrerPartner);
        return referrerPartner;
    }

    private UserWS createCustomer(JbillingAPI api, PartnerWS partner){
        UserWS user = null;
        try{
            user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
            user.setPartnerId(partner.getId());
            user.setPassword(null);
            api.updateUser(user);
            System.out.println("userUpdated with id:" + user.getUserId());
        }catch (Exception e){
            fail("Exception creating customer");
        }
        return user;
    }

    private Integer createInvoiceForPartner (JbillingAPI api, PartnerWS partner, Integer itemId) {
        UserWS user = createCustomer(api, partner);

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(ServerConstants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(1); // once
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(itemId);
        line.setQuantity(1);
        line.setUseItem(true);

        order.setOrderLines(new OrderLineWS[] { line });

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        return invoiceId;
    }

    private ItemDTOEx createItem(JbillingAPI api, String description, BigDecimal standardPercentage, BigDecimal masterPercentage){
        try {
            ItemDTOEx newItem = new ItemDTOEx();

            List<InternationalDescriptionWS> descriptions = new java.util.ArrayList<InternationalDescriptionWS>();
            InternationalDescriptionWS enDesc = new InternationalDescriptionWS(1, description);
            descriptions.add(enDesc);

            newItem.setPriceModelCompanyId(new Integer(1));
            newItem.setDescriptions(descriptions);
            newItem.setPriceManual(0);
            newItem.setPrices(CreateObjectUtil.setItemPrice(new BigDecimal("10.0"), new DateMidnight(1970, 1, 1).toDate(), Integer.valueOf(1), Integer.valueOf(1)));
            newItem.setNumber(description);
            newItem.setHasDecimals(0);
            newItem.setAssetManagementEnabled(0);
            newItem.setStandardPartnerPercentage(standardPercentage);
            newItem.setMasterPartnerPercentage(masterPercentage);
            Integer types[] = new Integer[1];
            types[0] = new Integer(1);
            newItem.setTypes(types);

            System.out.println("Creating item ..." + newItem);
            Integer ret = api.createItem(newItem);
            assertNotNull("The item was not created", ret);
            System.out.println("Done!");
            newItem.setId(ret);

            return newItem;
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
            return null;
        }
    }

    private void applyPayment(JbillingAPI api, Integer userId, BigDecimal amount, Integer invoiceId){

        PaymentWS payment = new PaymentWS();
        payment.setAmount(amount);
        payment.setIsRefund(new Integer(0));
        payment.setMethodId(ServerConstants.PAYMENT_METHOD_CHEQUE);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setResultId(ServerConstants.RESULT_ENTERED);
        payment.setCurrencyId(new Integer(1));
        payment.setUserId(userId);
        payment.setPaymentNotes("Notes");
        payment.setPaymentPeriod(new Integer(1));

        PaymentInformationWS cheque = com.sapienter.jbilling.server.user.WSTest.createCheque("ws bank", "2232-2323-2323", Calendar.getInstance().getTime());
        payment.getPaymentInstruments().add(cheque);

        api.applyPayment(payment, invoiceId);
    }

    @Test
    public void testCommissionCurrencyConversion() {
        init();
        ItemDTOEx product = null;
        PartnerWS agent = null;
        PartnerWS japanAgent = null;
        Integer invoiceID = null;
        Integer invoiceID2 = null;
        try {
            //Create a product
            String itemName = ITEM_NAME + new Date();
            product = createItem(api,itemName,new BigDecimal(50),new BigDecimal(80));

            //Create an agent with USD currency
            Integer currencyID = null;
            for(CurrencyWS currency : api.getCurrencies()) {
                if(currency.getCode().equals("USD")) {
                    currencyID = currency.getId();
                }
            }
            String name = "COMMON_2_" + AGENT_NAME;
            agent = createPartnerWithCurrency(name,PartnerType.STANDARD, PartnerCommissionType.INVOICE, currencyID);

            //Create a customer
            createCustomer(api,agent);
            //Create an order & generate the invoice in USD
            invoiceID = createInvoiceForPartner(api,agent,product.getId());


            //Create japan agent with currency set to YEN
            for(CurrencyWS currency : api.getCurrencies()) {
                if(currency.getCode().equals("JPY")) {
                    currencyID = currency.getId();
                }
            }
            System.out.println("Currency " + currencyID) ;
            name = "JAPAN_6_" + AGENT_NAME;
            japanAgent = createPartnerWithCurrency(name,PartnerType.STANDARD, PartnerCommissionType.INVOICE, currencyID);
            //Create an order & generate the invoice in USD
            invoiceID2 = createInvoiceForPartner(api,japanAgent,product.getId());


            //Trigger the commission process
            prepareAndTriggerCommissionPocess();
            CommissionWS[] commissions = getGeneratedCommissions2();
            for(CommissionWS commission : commissions) {
                Integer userID = api.getPartner(commission.getPartnerId()).getUserId();
                System.out.println("Integer agentID = " + userID);
                Integer agentCurrencyID = api.getUserWS(userID).getCurrencyId();
                System.out.println("Commission currencyID: " + commission.getCurrencyId());
                System.out.println("Agent currencyID: " + agentCurrencyID);
                assertEquals("The commission currency equals the agent currency", commission.getCurrencyId(), agentCurrencyID);
                if(commission.getPartnerId().equals(agent.getId())) {
                    assertEquals(new BigDecimal(5.00).floatValue(),commission.getAmountAsDecimal().floatValue(),0.001);
                }
                if(commission.getPartnerId().equals(japanAgent.getId())) {
                    assertEquals(new BigDecimal(557.00).floatValue(),commission.getAmountAsDecimal().floatValue(),0.001);
                }
            }
        } finally {
            deleteInvoice(api, invoiceID2);
            deleteInvoice(api, invoiceID);
            deletePartner(api, agent);
            api.deleteUser(agent.getUserId());
            deletePartner(api, japanAgent);
            api.deleteUser(japanAgent.getUserId());
            api.deleteItem(product.getId());
        }
    }

    private void deleteInvoice(JbillingAPI api, Integer invoiceId) {
        Integer[] orderIds = api.getInvoiceWS(invoiceId).getOrders();
        api.deleteInvoice(invoiceId);
        for (Integer orderId: orderIds) {
            api.deleteOrder(orderId);
        }
    }

    private CommissionWS[] getGeneratedCommissions() {
        CommissionProcessRunWS[] commissionRuns = api.getAllCommissionRuns();
        CommissionProcessRunWS thisRun = null;
        for(CommissionProcessRunWS commissionRun : commissionRuns){
            if(new DateTime(commissionRun.getRunDate()).toDateMidnight().equals(new DateMidnight()) ){
                System.out.println("This commission run found.");
                thisRun = commissionRun;
                break;
            }
        }
        if(thisRun == null){
            fail("Couldn't get the commission process run generated");
        }
        //Getting the commissions
        return api.getCommissionsByProcessRunId(thisRun.getId());
    }

    private void prepareAndTriggerCommissionPocess() {
        try {
            CommissionProcessConfigurationWS configurationWS = new CommissionProcessConfigurationWS();
            configurationWS.setEntityId(1);
            configurationWS.setNextRunDate(new DateTime().withDayOfMonth(1).toDate());
            configurationWS.setPeriodUnitId(PeriodUnitDTO.MONTH);
            configurationWS.setPeriodValue(1);
            api.createUpdateCommissionProcessConfiguration(configurationWS);
            System.out.println("commission process configuration finished");
        } catch(Exception e) {
            System.out.println("The commission process is already running.");
        }
        //Trigger commission process.
        api.calculatePartnerCommissions();
        System.out.println("triggering the commission process finished.");
    }

    private PartnerWS createPartnerWithCurrency(String name, PartnerType partnerType,
                                                PartnerCommissionType partnerCommissionType, Integer currencyID) {
        // new partner
        String version = ""+new Date().getTime();
        UserWS user = new UserWS();
        user.setUserName(name);
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(currencyID);
        user.setMainRoleId(PARTNER_ROLE_ID);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        /*
        ContactWS contact = new ContactWS();
        contact.setEmail(user.getUserName() + "@test.com");
        contact.setFirstName("Partner" + name);
        contact.setLastName(version);
        user.setContact(contact);
*/
        PartnerWS partner = new PartnerWS();
        partner.setType(partnerType.name());

        // create partner
        partner.setId(api.createPartner(user, partner));

        return api.getPartner(partner.getId());
    }

    private CommissionWS[] getGeneratedCommissions2() {
        CommissionProcessRunWS[] commissionRuns = api.getAllCommissionRuns();
        //Getting the commissions
        return api.getCommissionsByProcessRunId(commissionRuns[commissionRuns.length-1].getId());
    }

}

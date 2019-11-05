package com.sapienter.jbilling.server.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.BillingProcessTestCase;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderProcessWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import junit.framework.AssertionFailedError;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;

import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
/**
 * BillingProcessTestCase
 * This is the base class for all Billing process test classes that have 
 * been written to test Billing Process functionality. It has all the methods 
 * that are reusable across all Billing cycle Test.
 * @author mazhar
 * @since 15-JUNE-2014
 */
public abstract class BillingProcessTestCase {
	
	private static JbillingAPI api = null;
	private static ArrayList<String> failures = null;
    private static SimpleDateFormat formatDate = new SimpleDateFormat("MM/dd/yyyy");
    
	public OrderPeriodWS getOrderPeriod(JbillingAPI api2, OrderPeriodWS[] periods, Integer periodUnit, Integer value, String description) throws Exception{
		OrderPeriodWS orderPeriodWS = null;
		api = JbillingAPIFactory.getAPI();
		for (OrderPeriodWS orderPeriodWS1 : periods) {
            if ( orderPeriodWS1.getPeriodUnitId().intValue()==periodUnit.intValue() && orderPeriodWS1.getValue().intValue()==value.intValue()){
                orderPeriodWS = orderPeriodWS1;
                break;
            }
        }
        if(orderPeriodWS == null){
        	orderPeriodWS = new OrderPeriodWS(999,1, periodUnit, value);
        	
        	InternationalDescriptionWS descr=
                    new InternationalDescriptionWS(1, description);
            List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>();
            descriptions.add(descr) ;
            orderPeriodWS.setDescriptions(descriptions);
            Integer id = api.createOrderPeriod(orderPeriodWS);
            orderPeriodWS.setId(id);
        }
        return orderPeriodWS;
	}

    public OrderWS createMockOrder(int userId,int orderPeriod, Date activeSince,int billingType) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingType);
        order.setPeriod(orderPeriod);
        order.setCurrencyId(1);
        order.setActiveSince(activeSince);
        order.setProrateFlag(false);
        order.setNextBillableDay(activeSince);

        ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(1);
        for (int i = 0; i < 1; i++){
            OrderLineWS nextLine = new OrderLineWS();
            nextLine.setTypeId(ServerConstants.ORDER_LINE_TYPE_ITEM);
            nextLine.setDescription("Order line: " + i);
            nextLine.setItemId(i + 1);
            nextLine.setQuantity(1);
            nextLine.setPrice( new BigDecimal(60));
            nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply( new BigDecimal(60)));

            lines.add(nextLine);
        }
        order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
        return order;
    }

    
    public UserWS createUser(int orderPeriodId,Integer day,Date nextInvoiceDate) throws JbillingAPIException, IOException {
        JbillingAPI api = JbillingAPIFactory.getAPI();


        
        MainSubscriptionWS newMainSubscription = new MainSubscriptionWS(orderPeriodId, day);
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("Asdfasdf@1");
        newUser.setLanguageId(new Integer(1));
        newUser.setMainRoleId(new Integer(5));
        newUser.setParentId(null); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(null);
        newUser.setInvoiceChild(new Boolean(false));
        newUser.setMainSubscription(newMainSubscription);
        newUser.setNextInvoiceDate(nextInvoiceDate);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor

        newUser.setMetaFields(new MetaFieldValueWS[]{metaField1, metaField2});

        // add a contact
        ContactWS contact = new ContactWS();
        contact.setEmail(newUser.getUserName() + "@shire.com");
        contact.setFirstName("J");
        contact.setLastName("Biller");
        newUser.setContact(contact);

        System.out.println("Creating user ...");
        newUser.setUserId(api.createUser(newUser));

        newUser = api.getUserWS(newUser.getUserId());
        newUser.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(newUser);
        newUser = api.getUserWS(newUser.getUserId());

        return newUser;
    }
    public void initErrorList(){
    	failures = new ArrayList<String>();
    }
    public ArrayList<String> getErrors(){
    	return failures;
    }
    public void assertEqualsBilling(String message, Date expected, Date actual){
    	try{
    		assertEquals(message, expected, actual);
    	}catch(AssertionFailedError error){
    		System.out.println("===============================================\n"+error.getMessage());
    	}catch (AssertionError error) {
    		failures.add(error.getMessage());
		}
    }
    
    public void assertEqualsBilling(String message, Integer expected, Integer actual){
    	try{
    		assertEquals(message, expected, actual);
    	}catch(AssertionFailedError error){
    		System.out.println("===============================================\n"+error.getMessage());
    	}catch (AssertionError error) {
    		failures.add(error.getMessage());
		}
    }
    public void assertEqualsBilling(String message, String expected, String actual){
    	try{
    		assertEquals(message, expected, actual);
    	}catch(AssertionFailedError error){
    		System.out.println("===============================================\n"+error.getMessage());
    	}catch (AssertionError error) {
    		failures.add(error.getMessage());
		}
    }
    
    public void assertContainsErrorBilling(SessionInternalError errors, String expected, String errorMessage){
    	try{
    		assertContainsError(errors, expected, errorMessage);
    	}catch(AssertionFailedError error){
    		System.out.println("===============================================\n"+error.getMessage());
    	}catch (AssertionError error) {
    		failures.add(error.getMessage());
		}
    }
    
    public Integer enableNoInvoiceFilterTask(JbillingAPI api,Integer pluginTypeId) {
        PluggableTaskWS plugin = new PluggableTaskWS();
        plugin.setTypeId(pluginTypeId);
        plugin.setProcessingOrder(99);
        Integer pluginId = api.createPlugin(plugin);
        System.out.println("Returning pluginId" + pluginId);
        return pluginId;
    }

    public void disableNoInvoiceFilterTask(JbillingAPI api, Integer pluginId) {
    	try {
    		if (pluginId != null) {
    			api.deletePlugin(pluginId);
    		}
    		pluginId = null;

    	} catch (Exception e) {
    		/* ignore */
    	}
    }
    
    public void triggerBilling(Date runDate) {
    	BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();

        config.setNextRunDate(runDate);
        config.setRetries(new Integer(1));
        config.setDaysForRetry(new Integer(5));
        config.setGenerateReport(new Integer(0));
        config.setAutoPaymentApplication(new Integer(0));
        config.setDfFm(new Integer(0));
        config.setPeriodUnitId(new Integer(ServerConstants.PERIOD_UNIT_DAY));
        config.setDueDateUnitId(ServerConstants.PERIOD_UNIT_DAY);
        config.setDueDateValue(new Integer(0));
        config.setInvoiceDateProcess(new Integer(0));
        config.setMaximumPeriods(new Integer(99));
        config.setOnlyRecurring(new Integer(0));
        config.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());

        System.out.println("B - Setting config to: " + config);
        api.createUpdateBillingProcessConfiguration(config);

        System.out.println("Running Billing Process for  "  + runDate );
        api.triggerBilling(runDate);
    }
    
    public void assertScenarioChecks(UserWS userC1, int orderIdC1, Date orderNextBillableDate, Date userNextInvoiceDate, 
    		int totalInvoices, Calendar periodStartDate, Calendar periodEndDate, int previous) throws Exception {

    	Integer languageId = new Integer(1);
    	
        OrderWS orderWSC1=api.getOrder(orderIdC1);
        String periodStartDateInvoice = formatDate.format(periodStartDate.getTime());
        String periodEndDateInvoice = formatDate.format(periodEndDate.getTime());
        
        OrderPeriodWS [] allPeriod=api.getOrderPeriods();
        OrderPeriodWS daily = getOrderPeriod(api,allPeriod, ServerConstants.PERIOD_UNIT_DAY,languageId,"Daily");
        OrderPeriodWS weekly = getOrderPeriod(api, allPeriod, ServerConstants.PERIOD_UNIT_WEEK, 1, "Weekly");
        OrderPeriodWS semiMonthly = getOrderPeriod(api,allPeriod, ServerConstants.PERIOD_UNIT_SEMI_MONTHLY,languageId,"Semi-Monthly");
        OrderPeriodWS monthly = getOrderPeriod(api,allPeriod, ServerConstants.PERIOD_UNIT_MONTH,languageId,"Monthly");

        System.out.println("Generated Invoices of orders : " +orderWSC1.getGeneratedInvoices().length);

        Integer[] invoiceIdsWSC1At6Jan= api.getLastInvoices(userC1.getUserId(), 1);

        if ((invoiceIdsWSC1At6Jan.length-previous) != 0){

            InvoiceWS invoiceWSC1At6Jan = api.getInvoiceWS(invoiceIdsWSC1At6Jan[0]);

            System.out.println("Generated Invoice Id for " +orderIdC1 +":" + invoiceWSC1At6Jan.getId());
            System.out.println("Total Invoice lines for  " +invoiceWSC1At6Jan.getId() +"  :" + invoiceWSC1At6Jan.getInvoiceLines().length);
            assertEquals("Invoice Lines for " + orderIdC1 + " At6Jan should be" + totalInvoices, totalInvoices, invoiceWSC1At6Jan.getInvoiceLines().length-previous);
            assertEqualsBilling("Invoice Lines for " + orderIdC1 + " At6Jan should be " + totalInvoices, totalInvoices, invoiceWSC1At6Jan.getInvoiceLines().length-previous);

            Arrays.parallelSort(invoiceWSC1At6Jan.getInvoiceLines(), (line1, line2) -> line1.getId() - line2.getId() );
            
            for (com.sapienter.jbilling.server.entity.InvoiceLineDTO invoiceLineDTO:invoiceWSC1At6Jan.getInvoiceLines()){
                if(invoiceLineDTO.getItemId()!=null){
                    if (orderWSC1.getPeriod().equals(daily.getId())) {
                        System.out.println("Getting description for "+orderWSC1.getId()+" 6Jan:"+invoiceLineDTO.getDescription());
                        assertEquals("1st description for " + orderWSC1.getId() + " Invoice Id: " + invoiceWSC1At6Jan.getId() + " should be: ", "Order line: 0 Period from " + periodStartDateInvoice + " to " + periodEndDateInvoice, invoiceLineDTO.getDescription());
                        periodStartDate.add(Calendar.DAY_OF_MONTH, 1);
                        periodStartDateInvoice = formatDate.format(periodStartDate.getTime());
                        periodEndDate.add(Calendar.DAY_OF_MONTH, 1);
                        periodEndDateInvoice = formatDate.format(periodEndDate.getTime());
                    }
                    if (orderWSC1.getPeriod().equals(weekly.getId())) {
                        System.out.println("Getting description for "+orderWSC1.getId()+"  6Jan:"+invoiceLineDTO.getDescription());
                        assertEquals("1st description for " + orderWSC1.getId() + " At6Jan should be: ", "Order line: 0 Period from " + periodStartDateInvoice + " to " + periodEndDateInvoice, invoiceLineDTO.getDescription());
                        periodStartDate.add(Calendar.WEEK_OF_MONTH, 1);
                        periodStartDateInvoice = formatDate.format(periodStartDate.getTime());
                        periodEndDate.add(Calendar.WEEK_OF_MONTH, 1);
                        periodEndDateInvoice = formatDate.format(periodEndDate.getTime());
                    }
                    if (orderWSC1.getPeriod().equals(monthly.getId())) {
                        System.out.println("Getting description for "+orderWSC1.getId()+":"+invoiceLineDTO.getDescription());
                        assertEquals("1st description for " + orderWSC1.getId() + " At6Jan should be: ", "Order line: 0 Period from " + periodStartDateInvoice + " to " + periodEndDateInvoice, invoiceLineDTO.getDescription());
                        periodStartDate.add(Calendar.MONTH, 1);
                        periodStartDateInvoice = formatDate.format(periodStartDate.getTime());
                        periodEndDate.add(Calendar.MONTH, 1);
                        periodEndDateInvoice = formatDate.format(periodEndDate.getTime());
                    }
                    if (orderWSC1.getPeriod().equals(semiMonthly.getId())) {
                        System.out.println("Getting description for :"+invoiceLineDTO.getDescription());
                        assertEqualsBilling("1st description for " + orderWSC1.getId() + " At6Jan should be: ", "Order line: 0 Period from " + periodStartDateInvoice + " to " + periodEndDateInvoice, invoiceLineDTO.getDescription());
                        periodStartDate.add(Calendar.DAY_OF_MONTH, 14);
                        periodStartDateInvoice = formatDate.format(periodStartDate.getTime());
                        periodEndDate.add(Calendar.DAY_OF_MONTH, 14);
                        periodEndDateInvoice = formatDate.format(periodEndDate.getTime());
                    }
                }
            }
        }

        System.out.println("Processed Order Next Billable Day : " + orderWSC1.getNextBillableDay());
        assertEquals(orderIdC1 + " Next Billable date should be " + orderNextBillableDate, orderNextBillableDate, orderWSC1.getNextBillableDay());
        UserWS userWSC1= api.getUserWS(userC1.getId());
        System.out.println("User Next Invoice Date After Billing process: " + userWSC1.getNextInvoiceDate());
        assertEquals(userC1.getId() + " next invoice date should be " + userNextInvoiceDate, userNextInvoiceDate, userWSC1.getNextInvoiceDate());
    }
    
    public void assertScenarioChecks(UserWS userC1 ,int orderIdC1,Date orderNextBillableDate,
    		Date userNextInvoiceDate,int totalInvoices,  Calendar periodStartDate, Calendar periodEndDate) throws Exception {
    	
    	assertScenarioChecks(userC1,orderIdC1,orderNextBillableDate,userNextInvoiceDate,totalInvoices,periodStartDate,periodEndDate,0);
    	
    }
    
    public void assertScenarioChecksForParentChild(UserWS userC1, int orderIdC1, Date orderNextBillableDate, Date userNextInvoiceDate, 
    		int totalInvoices, Calendar periodStartDate, Calendar periodEndDate, int previous) throws Exception {

    	Integer languageId = new Integer(1);
    	
        OrderWS orderWSC1=api.getOrder(orderIdC1);
        String periodStartDateInvoice = formatDate.format(periodStartDate.getTime());
        String periodEndDateInvoice = formatDate.format(periodEndDate.getTime());
        
        OrderPeriodWS [] allPeriod=api.getOrderPeriods();
        OrderPeriodWS monthly = getOrderPeriod(api,allPeriod, ServerConstants.PERIOD_UNIT_MONTH,languageId,"Monthly");

        System.out.println("Generated Invoices of orders : " +orderWSC1.getGeneratedInvoices().length);

        Integer[] invoiceIdsWSC1At6Jan= api.getLastInvoices(userC1.getUserId(), 1);

        if ((invoiceIdsWSC1At6Jan.length-previous) != 0){

            InvoiceWS invoiceWSC1At6Jan = api.getInvoiceWS(invoiceIdsWSC1At6Jan[0]);

            System.out.println("Generated Invoice Id for " +orderIdC1 +":" + invoiceWSC1At6Jan.getId());
            System.out.println("Total Invoice lines for  " +invoiceWSC1At6Jan.getId() +"  :" + invoiceWSC1At6Jan.getInvoiceLines().length);
            assertEquals("Invoice Lines for " + orderIdC1 + " parent child should be" + totalInvoices, totalInvoices, invoiceWSC1At6Jan.getInvoiceLines().length-previous);
            assertEqualsBilling("Invoice Lines for " + orderIdC1 + " parent child should be " + totalInvoices, totalInvoices, invoiceWSC1At6Jan.getInvoiceLines().length-previous);

            for (com.sapienter.jbilling.server.entity.InvoiceLineDTO invoiceLineDTO:invoiceWSC1At6Jan.getInvoiceLines()){
                if(invoiceLineDTO.getItemId()!=null){
                    if (orderWSC1.getPeriod().equals(monthly.getId())) {
                        System.out.println("Getting description for "+orderWSC1.getId()+":"+invoiceLineDTO.getDescription());
                        assertEquals("1st description for " + orderWSC1.getId() + " Billibg Period should be: ", "Order line: 0 Period from " + periodStartDateInvoice + " to " + periodEndDateInvoice, invoiceLineDTO.getDescription());
                    }
                }
            }
        }

        System.out.println("Processed Order Next Billable Day : " + orderWSC1.getNextBillableDay());
        assertEquals(orderIdC1 + " Next Billable date should be " + orderNextBillableDate, orderNextBillableDate, orderWSC1.getNextBillableDay());
        UserWS userWSC1= api.getUserWS(userC1.getId());
        System.out.println("User Next Invoice Date After Billing process: " + userWSC1.getNextInvoiceDate());
        assertEquals(userC1.getId() + " next invoice date should be " + userNextInvoiceDate, userNextInvoiceDate, userWSC1.getNextInvoiceDate());
    }
    
    public void assertScenarioChecksForParentChild(UserWS userC1 ,int orderIdC1,Date orderNextBillableDate,
    		Date userNextInvoiceDate,int totalInvoices,  Calendar periodStartDate, Calendar periodEndDate) throws Exception {
    	
    	assertScenarioChecksForParentChild(userC1,orderIdC1,orderNextBillableDate,userNextInvoiceDate,totalInvoices,periodStartDate,periodEndDate,0);
    	
    }
}

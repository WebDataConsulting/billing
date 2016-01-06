package com.sapienter.jbilling.server.task;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;

import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.List;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

/**
 * Tests the notification task: {@CreditLimitationNotificationTask}
 * @author Maeis Gharibjanian
 * @since 03-09-2013
 */
@Test(groups = { "integration", "task" })
public class CreditLimitationNotificationTaskTest extends ApiTestCase {

    // id of credit limitation plugin at table: pluggable_task_type
    private static final Integer CREDIT_LIMITATION_NOTIFICATION_PLUGIN_ID = 134;

    private static Integer ORDER_CHANGE_STATUS_APPLY_ID;
	private static Integer CURRENCY_USD;
	private static Integer CURRENCY_GBP;
	private static Integer LANGUAGE_ID;
	private static Integer CUSTOMER_MAIN_ROLE;
	private static Integer PRANCING_PONY_ACCOUNT_TYPE;
	private static Integer pluginId;

	protected void prepareTestInstance() throws Exception {
		super.prepareTestInstance();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
		CURRENCY_USD = Constants.PRIMARY_CURRENCY_ID;
		CURRENCY_GBP = Integer.valueOf(5);
		CUSTOMER_MAIN_ROLE = Integer.valueOf(5);
		LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
		enablePlugin();
	}

	protected void afterTestClass() throws Exception {
		if (pluginId != null) {
			api.deletePlugin(pluginId);
			pluginId = null;
		}
	}

	private void enablePlugin() {
		PluggableTaskTypeWS type = api.getPluginTypeWS(CREDIT_LIMITATION_NOTIFICATION_PLUGIN_ID);

		//check if the static number for the plugin is the one we need
		if(!type.getClassName().equals("com.sapienter.jbilling.server.user.tasks.CreditLimitationNotificationTask")){
			fail("The plugin with id:" + CREDIT_LIMITATION_NOTIFICATION_PLUGIN_ID + ", is not with class name CreditLimitationNotificationTask");
		}

		PluggableTaskWS plugin = new PluggableTaskWS();
		plugin.setTypeId(CREDIT_LIMITATION_NOTIFICATION_PLUGIN_ID);
		plugin.setProcessingOrder(10);

		Hashtable<String, String> parameters = new Hashtable<String, String>();
		plugin.setParameters(parameters);

		pluginId = api.createPlugin(plugin);
	}

    @Test
    public void testCreditLimitationNotification() throws IOException {
	    assertNotNull("Plugin not configured", pluginId);
	    System.out.println("CreditLimitationNotificationTaskTest, plugin id: " + pluginId);

	    PluggableTaskWS plugin = api.getPluginWS(pluginId);
	    assertEquals("Plugin not configured correctly", CREDIT_LIMITATION_NOTIFICATION_PLUGIN_ID.intValue(), plugin.getTypeId().intValue());

	    BigDecimal orglDynamicBal = new BigDecimal("10.00");

	    //test if emails_sent.txt does not exist
	    String directory = Util.getSysProp("base_dir");

	    System.out.println("Base Directory: " + directory);
	    try {
		    File f = new File(directory + "emails_sent.txt");
		    f.delete();
		    assertFalse("The emails_sent file exists", f.exists());
	    } catch (Exception e) {
		    System.out.println(e.getMessage());
		    fail("Exception. Can not remove the emails_sent.txt file." + e.getMessage());
	    }

	    UserWS newUser = new UserWS();
	    newUser.setUserName("creditLimit-test" + System.currentTimeMillis());
	    newUser.setPassword("Admin123@");
	    newUser.setLanguageId(LANGUAGE_ID);
	    newUser.setCurrencyId(CURRENCY_GBP);
	    newUser.setMainRoleId(CUSTOMER_MAIN_ROLE);
	    newUser.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
	    newUser.setIsParent(Boolean.FALSE);
	    newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

	    //balance type and dynamic balance to 10.
	    newUser.setDynamicBalance(orglDynamicBal);

	    //we have balance 10 and we make purchase of 15 which drops under 0
	    newUser.setCreditLimitNotification1(BigDecimal.ZERO);

	    String email = newUser.getUserName() + "@gmail.com";
	    //email contact meta field
	    MetaFieldValueWS metaField2 = new MetaFieldValueWS();
	    metaField2.setFieldName("contact.email");
	    metaField2.setValue(email);
	    metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

	    newUser.setMetaFields(new MetaFieldValueWS[]{
			    metaField2
	    });

	    // do the creation
	    Integer newUserId = api.createUser(newUser);
	    System.out.println("User created : " + newUserId);
	    assertNotNull("User created", newUserId);

	    // verify that the dynamic balance of the saved users is correctly preserved
	    UserWS createdUser = api.getUserWS(newUserId);
	    assertEquals("Dynamic Balance Not Save Correctly", orglDynamicBal, createdUser.getDynamicBalanceAsDecimal());

	    //create Order for $15,
	    OrderWS order = new OrderWS();
	    order.setUserId(newUserId);
	    order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);//pre-paid
	    order.setPeriod(Constants.ORDER_PERIOD_ONCE);//one time
	    order.setCurrencyId(CURRENCY_GBP);
	    order.setActiveSince(new java.util.Date());

	    OrderLineWS lines[] = new OrderLineWS[1];
	    OrderLineWS line = new OrderLineWS();
	    line.setPrice(new BigDecimal("5.00"));
	    line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);//item
	    line.setQuantity(new Integer(3));
	    line.setAmount(new BigDecimal("5.00"));
	    line.setDescription("Example Item");
	    line.setItemId(new Integer(3));
	    line.setUseItem(false);
	    lines[0] = line;

	    order.setOrderLines(lines);
	    Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    System.out.println("Order created : " + orderId);
	    assertNotNull("Order created", orderId);

	    //verify that the order total is indeed 15$
	    order = api.getOrder(orderId);
	    assertEquals("Order total is not correct", new BigDecimal("15.00"), order.getTotalAsDecimal());

	    //dynamic balance should reduce and drop from $10 to -5$
	    createdUser = api.getUserWS(newUserId);
	    System.out.println("Original Dynamic bal : " + orglDynamicBal);
	    System.out.println("Current Dynamic bal : " + createdUser.getDynamicBalanceAsDecimal());

	    assertEquals("The dynamic balance should be -5.00$", new BigDecimal("-5.00"), createdUser.getDynamicBalanceAsDecimal());
	    assertTrue("The dynamic balance is not less than before", createdUser.getDynamicBalanceAsDecimal().compareTo(orglDynamicBal) < 0);

	    System.out.println("Base Directory: " + directory);
	    File f = new File(directory + "emails_sent.txt");

	    assertTrue("File does not exists. File:" + f.getName(), f.exists());

	    System.out.println("File Name: " + f.getName());

	    FileReader fr = new FileReader(f);

	    BufferedReader reader = new BufferedReader(fr);

	    String strLine = reader.readLine();

	    while (strLine != null) {
		    if (strLine.startsWith("Subject")) {
			    assertTrue(strLine.indexOf("Your pre-paid balance is below Water mark.") > 0);
		    }
		    strLine = reader.readLine();
	    }

	    //cleanup
	    api.deleteOrder(orderId);
	    api.deleteUser(newUserId);
    }

	private Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
		OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
		for (OrderChangeStatusWS status : statuses) {
			if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
				return status.getId();
			}
		}
		//there is no APPLY status in db so create one
		OrderChangeStatusWS apply = new OrderChangeStatusWS();
		String status1Name = "APPLY: " + System.currentTimeMillis();
		OrderChangeStatusWS status1 = new OrderChangeStatusWS();
		status1.setApplyToOrder(ApplyToOrder.YES);
		status1.setDeleted(0);
		status1.setOrder(1);
		status1.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}

}

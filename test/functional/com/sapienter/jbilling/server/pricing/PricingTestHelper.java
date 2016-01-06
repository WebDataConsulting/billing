package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.List;

/**
 * Helper for writing pricing tests. Provides constants and factory methods to produce
 * web-service objects used in integration tests.
 *
 * @author Brian Cowdery
 * @since 10-Jul-2012
 */
public class PricingTestHelper {

    private static final DateTimeFormatter TS = DateTimeFormat.forPattern("-HHmmss");
    // plug-in configuration
    private static final Integer PRICING_PLUGIN_ID = 410;
    private static final Integer RULES_PRICING_PLUGIN_TYPE_ID = 61; // RulesPricingTask2
    private static final Integer MODEL_PRICING_PLUGIN_TYPE_ID = 79; // PriceModelPricingTask

	public static Integer PRANCING_PONY_BASIC_ACCOUNT_TYPE = Integer.valueOf(1);
	public static Integer MORDOR_BASIC_ACCOUNT_TYPE = Integer.valueOf(2);

	public static Integer CURRENCY_USD = Integer.valueOf(1);
	public static Integer LANGUAGE_US = Integer.valueOf(1);
	public static Integer MAIN_ROLE_ID = 5;

    private static String timestamp() {
        return TS.print(new LocalTime());
    }

    public static UserWS buildUser(String username, Integer periodId, Integer accountTypeId) {
        UserWS user = new UserWS();
	    String timestamp = timestamp();
        user.setUserName(username + timestamp);
        user.setPassword("Admin123@");
        user.setLanguageId(LANGUAGE_US);
        user.setCurrencyId(CURRENCY_USD);
        user.setMainRoleId(MAIN_ROLE_ID);
        user.setAccountTypeId(accountTypeId);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);
        user.setMainSubscription(new MainSubscriptionWS(periodId, new Date().getDate()));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue("test-" + timestamp + "@test.com");
        metaField1.setGroupId(accountTypeId);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Pricing First:" + timestamp);
        metaField2.setGroupId(accountTypeId);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Pricing Last:" + timestamp);
        metaField3.setGroupId(accountTypeId);

        user.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
        });

        return user;
    }

    public static OrderWS buildMonthlyOrder(Integer userId, Integer orderPeriodId) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        OrderStatusWS statusWS = new OrderStatusWS();
        statusWS.setId(1); //OrderStatusFlag INVOICE = 1
        order.setOrderStatusWS(statusWS);
        order.setPeriod(orderPeriodId);
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(new Date());

        return order;
    }

	public static OrderWS buildOrder(Integer userId, Integer itemId, Integer periodId) {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(periodId);
		order.setCurrencyId(CURRENCY_USD);
		order.setActiveSince(new Date());

		OrderLineWS planLine = new OrderLineWS();
		planLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		planLine.setItemId(itemId);
		planLine.setUseItem(true);
		planLine.setQuantity(1);
		order.setOrderLines(new OrderLineWS[]{planLine});
		return order;
	}

    public static OrderWS buildOneTimeOrder(Integer userId) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(Constants.ORDER_PERIOD_ONCE);
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(new Date());

        return order;
    }

    public static OrderLineWS buildOrderLine(Integer itemId, Integer quantity) {
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(itemId);
        line.setUseItem(true);
        line.setQuantity(quantity);

        return line;
    }

    public static ItemDTOEx buildItem(String number, String desc, Integer type) {
        ItemDTOEx item = new ItemDTOEx();
        item.setNumber(number);
        item.setDescription(desc);
        item.setTypes(new Integer[]{type});
        item.setAssetManagementEnabled(0);
        return item;
    }


    //Enable/disable the PricingModelPricingTask plug-in.
    public void enablePricingPlugin(JbillingAPI api) {
        PluggableTaskWS plugin = api.getPluginWS(PRICING_PLUGIN_ID);
        plugin.setTypeId(MODEL_PRICING_PLUGIN_TYPE_ID);

        api.updatePlugin(plugin);
    }

    public void disablePricingPlugin(JbillingAPI api) {
        PluggableTaskWS plugin = api.getPluginWS(PRICING_PLUGIN_ID);
        plugin.setTypeId(RULES_PRICING_PLUGIN_TYPE_ID);

        api.updatePlugin(plugin);
    }
    
    public static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api){
    	OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
    	Integer statusId = null;
    	for(OrderChangeStatusWS orderChangeStatus : list){
    		if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)){
    			statusId = orderChangeStatus.getId();
    			break;
    		}
    	}
    	if(statusId != null){
    		return statusId;
    	}else{
    		OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
    		newStatus.setApplyToOrder(ApplyToOrder.YES);
    		newStatus.setDeleted(0);
    		newStatus.setOrder(1);
    		newStatus.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status1"));
    		return api.createOrderChangeStatus(newStatus);
    	}
    }
    
    public static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
    	OrderPeriodWS[] periodsList = api.getOrderPeriods();
    	Integer periodId = getOrderPeriodFromList(periodsList);
    	if(periodId != null){
    		return periodId;
    	}else{
    		OrderPeriodWS period = new OrderPeriodWS();
    		period.setPeriodUnitId(PeriodUnitDTO.MONTH);
    		period.setValue(1);
    		period.getDescriptions().add(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "Monthly Period"));
    		api.updateOrCreateOrderPeriod(period);
    		periodsList = api.getOrderPeriods();
    		return getOrderPeriodFromList(periodsList);
    	}
    }
    
    private static Integer getOrderPeriodFromList(OrderPeriodWS[] list){
    	Integer periodId = null;
    	for(OrderPeriodWS period : list){
    		if(period.getPeriodUnitId() == PeriodUnitDTO.MONTH){
    			periodId = period.getId();
    			break;
    		}
    	}
    	if(periodId != null){
    		return periodId;
    	}else{
    		return null;
    	}
    }
    
    public static Integer createItemCategory(JbillingAPI api){
    	ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("category"+Short.toString((short)System.currentTimeMillis()));
        itemType.setOrderLineTypeId(1);
        return api.createItemCategory(itemType);
    }
}

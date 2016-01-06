package com.sapienter.jbilling.server.discounts;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;

import java.math.BigDecimal;
import java.util.*;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Vladimir Carevski
 * @since 25-FEB-2015
 */
public abstract class BaseDiscountApiTest extends ApiTestCase {

	protected static final Calendar today = Calendar.getInstance();
	protected String random = String.valueOf(new Random().nextInt(100));

	protected static final Integer CUSTOMER_MAIN_ROLE = Integer.valueOf(5);

	protected static final Integer CUSTOMER_ACTIVE = Integer.valueOf(1);
	protected static final Integer CURRENCY_USD = Integer.valueOf(1);

	protected static final Integer LANGUAGE_US = Integer.valueOf(1);

	protected static Integer ORDER_CHANGE_STATUS_APPLY_ID;
	protected static Integer ONE_TIME_ORDER_PERIOD = Integer.valueOf(1);
	protected static Integer MONTHLY_ORDER_PERIOD;
	protected static Integer THREE_MONTHLY_ORDER_PERIOD;

	protected static final BigDecimal TEN = BigDecimal.TEN;

	protected Integer TEST_ITEM_CATEGORY;
	protected BillingProcessConfigurationWS prevBillingConfig;

	@Override
	protected void prepareTestInstance() throws Exception {
		System.out.println("prepareTestInstance:" + this.getClass().getSimpleName());
		super.prepareTestInstance();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
		MONTHLY_ORDER_PERIOD = getOrCreateMonthlyOrderPeriod(api, 1);
		THREE_MONTHLY_ORDER_PERIOD = getOrCreateMonthlyOrderPeriod(api, 3);
		TEST_ITEM_CATEGORY = createItemCategory(api);

		//backup the old billing configuration and
		//setup new suitable to testing discounts
		setupBillingConfiguration();
	}

	@Override
	protected void afterTestClass() throws Exception {
		System.out.println("afterTestClass:" + this.getClass().getSimpleName());
		try {
			if (null != TEST_ITEM_CATEGORY) {
				api.deleteItemCategory(TEST_ITEM_CATEGORY);
				TEST_ITEM_CATEGORY = null;}

			//restore the original billing configuration
			restoreBillingConfiguration();
		} catch (Exception e) {
		}
	}

	private void setupBillingConfiguration() {
		prevBillingConfig = api.getBillingProcessConfiguration();

		//setup billing configuration suitable for testing discounts
		BillingProcessConfigurationWS config = new BillingProcessConfigurationWS(prevBillingConfig);
		config.setGenerateReport(0);
		config.setDaysForReport(3);
		config.setRetries(0);
		config.setDaysForRetry(1);
		config.setReviewStatus(0);

		config.setDueDateUnitId(PeriodUnitDTO.MONTH);
		config.setDueDateValue(1);

		config.setOnlyRecurring(0);
		config.setInvoiceDateProcess(0);
		config.setMaximumPeriods(1);

		System.out.print("Setting up Billing Configuration for Discounts");
		api.createUpdateBillingProcessConfiguration(config);
	}

	private void restoreBillingConfiguration() {
		if (null != prevBillingConfig) {
			System.out.print("Restoring Billing Configuration");
			api.createUpdateBillingProcessConfiguration(prevBillingConfig);
		} else {
			System.out.println("Failed To Restore Billing Configuration");
		}
	}

	protected Integer createItemCategory(JbillingAPI api) {
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("discount category: " + random);
		itemType.setOrderLineTypeId(1);
		return api.createItemCategory(itemType);
	}

	protected Integer createItem(String description, String price, String number, Integer itemTypeId) {
		ItemDTOEx newItem = new ItemDTOEx();
		newItem.setDescription(description + ":" + random);
		newItem.setPrice(new BigDecimal(price));
		newItem.setNumber(number + ":" + random);
		newItem.setTypes(new Integer[]{itemTypeId});

		System.out.println("Creating item ..." + newItem);
		return api.createItem(newItem);
	}

	protected List<OrderWS> getDiscountOrders(Integer parentOrderId) {
		List<OrderWS> discountOrders = new ArrayList<OrderWS>();
		OrderWS[] linkedOrders = api.getLinkedOrders(parentOrderId);
		assertNotNull("parent order is null.", linkedOrders);
		for (OrderWS childOrder : linkedOrders) {
			for (OrderLineWS orderLine : childOrder.getOrderLines()) {
				if (Constants.ORDER_LINE_TYPE_DISCOUNT == orderLine.getTypeId().intValue()) {
					discountOrders.add(childOrder);
					break;
				}
			}
		}
		return discountOrders;
	}

	private static Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
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
		status1.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}

	private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api, int months){
		OrderPeriodWS[] periods = api.getOrderPeriods();
		for(OrderPeriodWS period : periods){
			if(months == period.getValue().intValue() &&
					PeriodUnitDTO.MONTH == period.getPeriodUnitId().intValue()){
				return period.getId();
			}
		}
		//there is no monthly order period so create one
		OrderPeriodWS monthly = new OrderPeriodWS();
		monthly.setEntityId(api.getCallerCompanyId());
		monthly.setPeriodUnitId(1);//monthly
		monthly.setValue(months);
		monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "DSC:MONTHLY:"+months)));
		return api.createOrderPeriod(monthly);
	}
}

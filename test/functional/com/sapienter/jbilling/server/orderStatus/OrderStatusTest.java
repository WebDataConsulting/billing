package com.sapienter.jbilling.server.orderStatus;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.test.ApiTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Created by vivekmaster146 on 25/3/14.
 */
@Test(groups = {"web-services", "orderStatus"})
public class OrderStatusTest extends ApiTestCase {

	private OrderStatusWS refOrderStatus;
	private static CompanyWS company;

	@Override
	protected void prepareTestInstance() throws Exception {
		super.beforeTestClass();
		company = api.getCompany();
	}

	@AfterMethod
	public void cleanUp() {
		if (null != refOrderStatus) {
			api.deleteOrderStatus(refOrderStatus);
			refOrderStatus = null;
		}
	}

	// Create OrderStatus Pass Case. Case when description is unique and Flag is Invoice(Allowed)
	@Test
	public void createOrderStatus() {
		String statusName = "Test: " + System.currentTimeMillis();
		refOrderStatus = buildOrderStatus(OrderStatusFlag.INVOICE, statusName);
		refOrderStatus.setId(api.createUpdateOrderStatus(refOrderStatus));
		assertNotNull("Order status created", refOrderStatus.getId());
	}

	// Delete Order Status Pass case.
	@Test(expectedExceptions = SessionInternalError.class)
	public void deleteOrderStatus() {
		String statusName = "Test: " + System.currentTimeMillis();
		refOrderStatus = buildOrderStatus(OrderStatusFlag.INVOICE, statusName);
		Integer orderStatusId = api.createUpdateOrderStatus(refOrderStatus);

		refOrderStatus = api.findOrderStatusById(orderStatusId);
		api.deleteOrderStatus(refOrderStatus);
		refOrderStatus = null;//mark as deleted

		//explodes with exception because the order status does not exist anymore
		refOrderStatus = api.findOrderStatusById(orderStatusId);
	}

	// Create OrderStatus Fail case. Case when Status flag is Finished.(It already exist)
	@Test(expectedExceptions = SessionInternalError.class)
	public void createOrderStatus2() {
		String statusName = "Test2: " + System.currentTimeMillis();
		OrderStatusWS orderStatusWS = buildOrderStatus(OrderStatusFlag.FINISHED, statusName);
		api.createUpdateOrderStatus(orderStatusWS);
	}

	// Create OrderStatus Fail case. Case when Status flag is SUSPENDED_AGEING.(It already exist)
	@Test(expectedExceptions = SessionInternalError.class)
	public void createOrderStatus3() {
		String statusName = "Test3: " + System.currentTimeMillis();
		OrderStatusWS orderStatus = buildOrderStatus(OrderStatusFlag.SUSPENDED_AGEING, statusName);
		api.createUpdateOrderStatus(orderStatus);
	}

	// Create OrderStatus Fail case. Case when description already exist.
	@Test(expectedExceptions = SessionInternalError.class)
	public void createOrderStatus4() {
		String statusName = "Active";
		OrderStatusWS orderStatusWS = buildOrderStatus(OrderStatusFlag.INVOICE, statusName);
		api.createUpdateOrderStatus(orderStatusWS);
	}

	// Update Order Status Description Pass case. Case when description is unique.
	@Test
	public void createUpdateOrderStatus() {
		//create vanilla Invoice order status
		String statusName = "Update Test: " + System.currentTimeMillis();
		refOrderStatus = buildOrderStatus(OrderStatusFlag.INVOICE, statusName);
		refOrderStatus.setId(api.createUpdateOrderStatus(refOrderStatus));

		//refresh and verify description
		refOrderStatus = api.findOrderStatusById(refOrderStatus.getId());
		assertEquals("Description should match", statusName, refOrderStatus.getDescription());

		//modify the description of the order status
		String newDescription = "New Update Test: " + System.currentTimeMillis();
		refOrderStatus.setDescription(newDescription);
		refOrderStatus.setDescriptions(buildDescriptions(newDescription));

		api.createUpdateOrderStatus(refOrderStatus);
		refOrderStatus = api.findOrderStatusById(refOrderStatus.getId());
		assertEquals("Updated Description should match", newDescription, refOrderStatus.getDescription());
	}

	// Update Order Status fail case. Case when status flag is set to finished(Already exist)
	@Test(expectedExceptions = SessionInternalError.class)
	public void createUpdateOrderStatus2() {
		//create vanilla Invoice order status
		String statusName = "Update Test: " + System.currentTimeMillis();
		refOrderStatus = buildOrderStatus(OrderStatusFlag.INVOICE, statusName);
		refOrderStatus.setId(api.createUpdateOrderStatus(refOrderStatus));

		//attempt to change flag from INVOICE to FINISHED
//		refOrderStatus = api.findOrderStatusById(refOrderStatus.getId());
		refOrderStatus.setOrderStatusFlag(OrderStatusFlag.FINISHED);

		api.createUpdateOrderStatus(refOrderStatus);
	}

	private OrderStatusWS buildOrderStatus(OrderStatusFlag flag, String statusName) {
		OrderStatusWS orderStatus = new OrderStatusWS(null, company, flag, statusName);
		orderStatus.setDescription(statusName);
		orderStatus.setDescriptions(buildDescriptions(statusName));
		return orderStatus;
	}

	private List<InternationalDescriptionWS> buildDescriptions(String description) {
		List descList = new ArrayList<InternationalDescriptionWS>();
		descList.add(new InternationalDescriptionWS(api.getCallerLanguageId(), description));
		return descList;
	}

    // Delete Order Status fail case.Case when Order Status is used in order_change table.
    @Test
    public void deleteOrderStatus4() {
        OrderStatusWS orderStatusWS = api.findOrderStatusById(400);
        try {
            api.deleteOrderStatus(orderStatusWS);

        } catch (Exception e) {
            OrderStatusWS orderStatusWS2 = api.findOrderStatusById(400);
            Boolean deleted = orderStatusWS2 != null;
            assertTrue(deleted);
        }
    }
}
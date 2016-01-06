package com.sapienter.jbilling.server.orderChangeStatus;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.ApiTestCase;

import org.testng.annotations.Test;

import java.util.*;

/**
 * @author Alexander Aksenov
 * @since 06.07.13
 */
@Test(groups = {"web-services", "orderChangeStatus"})
public class WSTest extends ApiTestCase {

    @Test
    public void test01GetOrderChangeStatuses() {
        List<OrderChangeStatusWS> currentStatuses = Arrays.asList(api.getOrderChangeStatusesForCompany());
        assertNotNull("Statuses should be presented", currentStatuses);
        assertFalse("Statuses should be presented", currentStatuses.isEmpty());
        OrderChangeStatusWS pendingStatus = findStatusById(currentStatuses, Constants.ORDER_CHANGE_STATUS_PENDING);
        OrderChangeStatusWS applyErrorStatus = findStatusById(currentStatuses, Constants.ORDER_CHANGE_STATUS_APPLY_ERROR);
        assertNotNull("PENDING status should be presented", pendingStatus);
        assertNotNull("APPLY_ERROR status should be presented", applyErrorStatus);
    }

	@Test
    public void test02CreateUpdateDeleteOrderChangeStatus() {
	    String status1Name = "Status 1:" + System.currentTimeMillis();
        OrderChangeStatusWS status1 = new OrderChangeStatusWS();
        status1.setApplyToOrder(ApplyToOrder.NO);
        status1.setDeleted(0);
        status1.setOrder(1);
        status1.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, status1Name));

        Integer status1Id = api.createOrderChangeStatus(status1);
        assertNotNull("Status should be created", status1Id);
        List<OrderChangeStatusWS> statuses = Arrays.asList(api.getOrderChangeStatusesForCompany());
        status1 = findStatusById(statuses, status1Id);
        assertNotNull("Status should be created", status1);
        assertEquals("Incorrect value for deleted field", 0, status1.getDeleted());
        assertEquals("Incorrect value for order field", 1, (int) status1.getOrder());
        assertEquals("Incorrect value for description field", status1Name, status1.getDescription(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID).getContent());
        if (statuses.size() == 3) { // 2 predefined and 1 created now
            assertEquals("Status should be created as default if first for company", ApplyToOrder.YES, status1.getApplyToOrder());
        }

	    //remember current apply status
	    OrderChangeStatusWS applyStatus = findApplyStatus(statuses);

        // try to create status with the same description
        OrderChangeStatusWS status2 = new OrderChangeStatusWS();
        status2.setApplyToOrder(ApplyToOrder.YES);
        status2.setDeleted(0);
        status2.setOrder(2);
        status2.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, status1Name));
        Integer status2Id;
        try {
            status2Id = api.createOrderChangeStatus(status2);
            fail("Status with same name should not be created");
        } catch (SessionInternalError ex) {
            assertEquals("Incorrect error", "OrderChangeStatusWS,descriptions,orderChangeStatusWS.error.unique.name", ex.getErrorMessages()[0]);
        }

	    String status2Name = "Status 2:" + System.currentTimeMillis();
        status2.getDescription(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID).setContent(status2Name);
        status2Id = api.createOrderChangeStatus(status2);
        assertNotNull("Status should be created", status2Id);
        statuses = Arrays.asList(api.getOrderChangeStatusesForCompany());
        status1 = findStatusById(statuses, status1Id);
        status2 = findStatusById(statuses, status2Id);

        assertNotNull("Status should be created", status2);
        assertEquals("Incorrect value for deleted field", 0, status2.getDeleted());
        assertEquals("Incorrect value for order field", 2, (int) status2.getOrder());
        assertEquals("Incorrect value for description field", status2Name, status2.getDescription(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID).getContent());
        assertEquals("Status should be created as ApplyToOrder", ApplyToOrder.YES, status2.getApplyToOrder());
        assertEquals("Status should be ApplyToOrder.NO now", ApplyToOrder.NO, status1.getApplyToOrder());

        status1.setApplyToOrder(ApplyToOrder.YES);
        status1.setOrder(5);
        status1.getDescription(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID).setContent(status1Name + "_updated");
        api.updateOrderChangeStatus(status1);
        statuses = Arrays.asList(api.getOrderChangeStatusesForCompany());
        status1 = findStatusById(statuses, status1Id);
        status2 = findStatusById(statuses, status2Id);

        assertEquals("Incorrect value for deleted field", 0, status1.getDeleted());
        assertEquals("Incorrect value for order field", 5, (int) status1.getOrder());
        assertEquals("Incorrect value for description field", status1Name + "_updated", status1.getDescription(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID).getContent());
        assertEquals("Status should be ApplyToOrder.YES", ApplyToOrder.YES, status1.getApplyToOrder());
        assertEquals("Status should be ApplyToOrder.NO now", ApplyToOrder.NO, status2.getApplyToOrder());

        api.deleteOrderChangeStatus(status2Id);
	    api.deleteOrderChangeStatus(status1Id);
        statuses = Arrays.asList(api.getOrderChangeStatusesForCompany());
        status2 = findStatusById(statuses, status2Id);
        assertNull("Status should be deleted", status2);

	    //make sure the original order change status is still marked as apply to order
	    if(null != applyStatus) {
		    applyStatus.setApplyToOrder(ApplyToOrder.YES);
		    api.updateOrderChangeStatus(applyStatus);
		    statuses = Arrays.asList(api.getOrderChangeStatusesForCompany());
		    assertEquals("Should be applied",
				    ApplyToOrder.YES,
				    findStatusById(statuses, applyStatus.getId()).getApplyToOrder());
	    }
    }

    private OrderChangeStatusWS findStatusById(List<OrderChangeStatusWS> statuses, Integer targetId) {
        for (OrderChangeStatusWS ws : statuses) {
            if (ws.getId().equals(targetId)) {
                return ws;
            }
        }
        return null;
    }

	private OrderChangeStatusWS findApplyStatus(List<OrderChangeStatusWS> statuses){
		for(OrderChangeStatusWS status : statuses){
			if(status.getApplyToOrder().equals(ApplyToOrder.YES)){
				return status;
			}
		}
		return null;
	}
}

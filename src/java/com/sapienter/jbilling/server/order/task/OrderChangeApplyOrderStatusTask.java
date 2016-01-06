/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.event.OrderChangeAppliedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

/**
 * This internal event plugin will change order status if this is allowed
 * by applied order change type.
 * Order change apply event is not equals to saving changed order to DB
 * (order change can be successfully applied to detached (or dto) order.
 * So, we should modify order from order change to change status.
 *
 * @author: Alexander Aksenov
 * @since: 27.02.14
 */
public class OrderChangeApplyOrderStatusTask extends PluggableTask implements IInternalEventsTask {

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
            OrderChangeAppliedEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof OrderChangeAppliedEvent) {
            OrderChangeAppliedEvent changeAppliedEvent = (OrderChangeAppliedEvent) event;
            OrderChangeDTO changeDTO = changeAppliedEvent.getOrderChangeDTO();
            if (changeDTO != null && changeDTO.getOrderChangeType() != null
                    && changeDTO.getOrderChangeType().isAllowOrderStatusChange() && changeDTO.getOrderStatusToApply() != null) {
                changeDTO.getOrder().setOrderStatus(changeDTO.getOrderStatusToApply());
            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}

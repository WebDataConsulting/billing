/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.item.tasks;

import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.event.NewStatusEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;


/**
 * Listens for {@link com.sapienter.jbilling.server.order.event.NewStatusEvent} events.
 * If the new status is FINISHED, the task will unlink all assets from the order and assign them the default
 * status.
 *
 * @author Gerhard
 * @since 13/5/2013
 */
public class RemoveAssetFromFinishedOrderTask extends PluggableTask implements IInternalEventsTask {

    private static final Class<Event> events[] = new Class[] {
            NewStatusEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        NewStatusEvent newStatusEvent = (NewStatusEvent) event;

        //load the order
        OrderDTO order = new OrderBL(newStatusEvent.getOrderId()).getDTO();

        //orders with a period of once will immediately go to a finished state after billing, ignore them
        if (order.isRecurring()
                && newStatusEvent.getNewStatusId().equals(new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.FINISHED, order.getUser().getCompany().getId()))) {
           new AssetBL().unlinkAssets(newStatusEvent.getOrderId(), newStatusEvent.getExecutorId());
        }
    }
}

package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.UserCreatedEvent;
import com.sapienter.jbilling.server.user.event.UserUpdatedEvent;
import com.sapienter.jbilling.server.util.Constants;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

public class UserUpdatedTask extends PluggableTask implements IInternalEventsTask {
    private Integer entityId;
    private Integer executorId;

    private static final Class<Event> events[] = new Class[]{
            UserCreatedEvent.class
    };


    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) throws PluggableTaskException {
        UserDTO userDTO;

        UserCreatedEvent userEvent = (UserCreatedEvent) event;
        userDTO = new UserBL(userEvent.getUserId()).getDto();
        entityId = userEvent.getEntityId();
        executorId = userEvent.getExecutorId();

        if (userDTO != null) {
            UserBL bl = new UserBL(userDTO);
            userDTO.getCustomer().setNotes("The customer was just created.");

            bl.update(executorId, new UserDTOEx(userDTO));
        }
    }
}

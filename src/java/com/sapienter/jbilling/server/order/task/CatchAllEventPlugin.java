package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.CatchAllEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

/**
 * @author Vikas Bodani
 * @since 05-Jan-2016.
 */
public class CatchAllEventPlugin extends PluggableTask implements IInternalEventsTask {


    @Override
    public void process(Event event) throws PluggableTaskException {
        //TODO
        System.out.printf("Event fired %s" , event);

        //TODO

        //call a callback url with parameters specific to each event

    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return new Class[]{CatchAllEvent.class};
    }
}

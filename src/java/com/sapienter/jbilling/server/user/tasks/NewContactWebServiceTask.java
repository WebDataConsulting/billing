package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.user.tasks.ws.GlobalWeather;
import com.sapienter.jbilling.server.user.tasks.ws.GlobalWeatherSoap;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.event.NewContactEvent;

/**
 * Simple plugin that invokes a remote web-service when a customer's contact information is changed.
 *
 * This plugin fetches the current weather from the webservicex.com Global Weather service and
 * updates the customer notes with the result.
 *
 * The JAX-WS web-service client was generated using Maven and the jaxws:wsimport tool. See the
 * Maven pom.xml file in the root jbilling directory for more information.
 *
 * @author Brian Cowdery
 * @since 17-Aug-2012
 */
public class NewContactWebServiceTask extends PluggableTask implements IInternalEventsTask {

    private static FormatLogger LOG = new FormatLogger(NewContactWebServiceTask.class);


    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            NewContactEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /**
     * Fetch the current weather for the user's set city & country code, and update the
     * customer notes field with the result.
     *
     * @param event event to process
     * @throws PluggableTaskException
     */
    public void process(Event event) throws PluggableTaskException {
        if (!(event instanceof NewContactEvent)) throw new PluggableTaskException("Cannot process event " + event);

        NewContactEvent contactEvent = (NewContactEvent) event;

        // get the current weather for the user
        ContactDTO contact = contactEvent.getContactDto();
        LOG.debug("Getting weather for %s, %s", contact.getCity(), contact.getCountryCode());

        String weather = getWeatherService().getWeather(contact.getCity(), contact.getCountryCode());
        LOG.debug("Weather = %s", weather);

    }

    private GlobalWeatherSoap getWeatherService() {
        return new GlobalWeather().getGlobalWeatherSoap();
    }
}

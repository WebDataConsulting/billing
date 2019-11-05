/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.server.order.event.OrderPreAuthorizedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;

/**
 *  Provision pre-authorized orders for partner customers to external system
 *
 *  Customer is a partner customer if the value for the partner metafield
 *  matches the partner value provided as a task parameter
 *
 *  @author Panche Isajeski
 *  @since 12/05/2012
 */
public class ExternalOrderProvisioningTask extends PluggableTask implements IInternalEventsTask {

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            OrderPreAuthorizedEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        // TODO (pai) implement order provisioning to external system

    }
}

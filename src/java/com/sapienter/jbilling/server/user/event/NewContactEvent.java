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

package com.sapienter.jbilling.server.user.event;

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;

/**
 *
 * @author emilc
 */
public class NewContactEvent implements Event {

    private final Integer entityId;
    private final ContactDTO contactDto;

    private final Integer userId;
    private final Integer groupId;

    public ContactDTO getContactDto() {
        return contactDto;
    }

    public NewContactEvent(Integer userId, ContactDTO contactDto, Integer entityId) {
        this.contactDto = contactDto;
        this.entityId = entityId;
        this.userId = userId;

        this.groupId = null;
    }

    public String getName() {
        return "New Contact Event";
    }

    public Integer getEntityId() {
        return entityId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getGroupId() {
        return groupId;
    }
}

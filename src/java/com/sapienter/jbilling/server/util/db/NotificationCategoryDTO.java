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
package com.sapienter.jbilling.server.util.db;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO;
import com.sapienter.jbilling.server.util.ServerConstants;

@Entity
@TableGenerator(
    name = "notification_category_GEN",
            table = "jbilling_seqs",
            pkColumnName = "name",
            valueColumnName = "next_id",
            pkColumnValue = "notification_category",
            allocationSize = 10)
@Table(name="notification_category")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class NotificationCategoryDTO extends AbstractDescription implements java.io.Serializable {

    private int id;
    private Set<NotificationMessageTypeDTO> messageTypes = new HashSet<NotificationMessageTypeDTO>(0);

    public NotificationCategoryDTO() {
    }

    // for stubs
    public NotificationCategoryDTO(Integer id) {
        this.id = id;
    }
    
    public NotificationCategoryDTO(Integer id, Set<NotificationMessageTypeDTO> messageTypes) {
        this.id = id;
        this.messageTypes= messageTypes;
    }

    @Transient
    protected String getTable() {
        return ServerConstants.TABLE_NOTIFICATION_CATEGORY;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "notification_category_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "category")
	public Set<NotificationMessageTypeDTO> getMessageTypes() {
		return messageTypes;
	}

	public void setMessageTypes(Set<NotificationMessageTypeDTO> messageTypes) {
		this.messageTypes = messageTypes;
	}

    public void addMessageType (NotificationMessageTypeDTO messageType ){this.messageTypes.add(messageType);}

    public void removeMessageType (NotificationMessageTypeDTO messageType ){this.messageTypes.remove(messageType);}

}



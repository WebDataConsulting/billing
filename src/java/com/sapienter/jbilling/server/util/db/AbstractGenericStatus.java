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

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;

/**
 * Abstract class for status classes. The get/setId() methods maps to
 * the status_value, instead of the primary key. Allows use of status
 * constants as the id.
 */
@Entity
@Table(name = "generic_status")
@TableGenerator(
        name = "generic_status_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "generic_status",
        allocationSize = 1
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="dtype",
    discriminatorType = DiscriminatorType.STRING
)
public abstract class AbstractGenericStatus extends AbstractDescription {

    protected int id;
    protected Integer statusValue;
    protected Integer order;

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "generic_status_GEN")
    @Column(name="id", unique=true, nullable=false)
    public Integer getPrimaryKey() {
        return id;
    }
    
    public void setPrimaryKey(Integer id) {
        this.id = id;
    }

    @Column(name="status_value", unique=true, nullable=false)
    public int getId() {
        return statusValue;
    }
    
    public void setId(int statusValue) {
        this.statusValue = statusValue;
    }

    @Column(name="ordr")
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

}

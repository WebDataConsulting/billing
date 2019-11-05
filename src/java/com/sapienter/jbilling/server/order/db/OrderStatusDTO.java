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
package com.sapienter.jbilling.server.order.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;


@Entity
@TableGenerator(name = "order_status_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "order_status",
        allocationSize = 100)
@Table(name = "order_status")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class OrderStatusDTO  extends AbstractDescription implements java.io.Serializable {
    private static final FormatLogger LOG = new FormatLogger(OrderStatusDTO.class);

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="order_status_GEN")
    @Column(name="id", unique=true, nullable=false)
    private int id;

    @Column(name="order_status_flag", unique=true, nullable=false)
    private OrderStatusFlag orderStatusFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private CompanyDTO entity;
    public CompanyDTO getEntity() {
        return entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    public String getStatusValue(){
        return getDescription();
    }

    public void setStatusValue(String text){
        setDescription(text);
    }


    public void setId(int id) {
        this.id = id;
    }


    public int getId() {
        return id;
    }


    public OrderStatusDTO() {

    }

    public OrderStatusFlag getOrderStatusFlag() {
        return orderStatusFlag;
    }

    public void setOrderStatusFlag(OrderStatusFlag orderStatusFlag) {
        this.orderStatusFlag = orderStatusFlag;
    }


    @Transient
    protected String getTable() {
        return ServerConstants.TABLE_ORDER_STATUS;
    }

	
    /*@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="status_id")
    public Set<OrderDTO> getPurchaseOrders() {
        return this.orderDTOs;
    }*/
    
    /*public void setPurchaseOrders(Set<OrderDTO> orderDTOs) {
        this.orderDTOs = orderDTOs;
    }*/
}



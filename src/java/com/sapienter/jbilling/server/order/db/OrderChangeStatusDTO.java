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

package com.sapienter.jbilling.server.order.db;

import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractGenericStatus;

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;

/**
 * @author Alexander Aksenov
 * @since 05.07.13
 */
@Entity
@DiscriminatorValue("order_change_status")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class OrderChangeStatusDTO extends AbstractGenericStatus implements java.io.Serializable {
    

    private ApplyToOrder applyToOrder;
    private CompanyDTO company;
    private int deleted;

    public OrderChangeStatusDTO() {
        this.statusValue = 0;
    }

    @Column(name = "attribute1")
    @Enumerated(EnumType.STRING)
    public ApplyToOrder getApplyToOrder() {
        return applyToOrder;
    }

    public void setApplyToOrder(ApplyToOrder applyToOrder) {
        this.applyToOrder = applyToOrder;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getCompany() {
        return company;
    }

    public void setCompany(CompanyDTO company) {
        this.company = company;
    }

    @Column(name = "deleted")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_ORDER_CHANGE_STATUS;
    }

    @Override
    public String toString () {
        return "OrderChangeStatusDTO [applyToOrder=" + applyToOrder + ", company=" + company + ", deleted=" + deleted
                + "]";
    }
}

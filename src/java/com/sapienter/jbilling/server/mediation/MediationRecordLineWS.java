/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.mediation.db.MediationRecordLineDTO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MediationRecordLineWS
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
public class MediationRecordLineWS implements Serializable {

    private int id;
    private Integer orderLineId;
    private Date eventDate;
    private String amount; // use strings instead of BigDecimal for WS compatibility
    private String quantity;
    private String description;

    public MediationRecordLineWS() {
    }

    public MediationRecordLineWS(MediationRecordLineDTO dto) {
        this.id = dto.getId();
        this.orderLineId = dto.getOrderLine() != null ? dto.getOrderLine().getId() : null;
        this.eventDate = dto.getEventDate();
        setAmount(dto.getAmount());
        setQuantity(dto.getQuantity());
        this.description = dto.getDescription();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(Integer orderLineId) {
        this.orderLineId = orderLineId;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public BigDecimal getAmount() {
        return amount != null ? new BigDecimal(amount) : null;
    }

    public void setAmount(BigDecimal amount) {
        if (amount != null)
            this.amount = amount.toString();
    }

    public BigDecimal getQuantity() {
        return quantity != null ? new BigDecimal(quantity) : null;
    }

    public void setQuantity(BigDecimal quantity) {
        if (quantity != null)
            this.quantity = quantity.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "MediationRecordLineWS{"
               + "id=" + id
               + ", orderLineId=" + orderLineId
               + ", eventDate=" + eventDate
               + ", amount=" + amount
               + ", quantity=" + quantity
               + ", description='" + description + '\''
               + '}';
    }
}

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
package com.sapienter.jbilling.server.mediation.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import com.sapienter.jbilling.server.mediation.MediationRecordLineWS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

/**
 * This table links a mediation record (CDR) to one or more order lines. It specified how much each
 * order line was affected (usually, added some $). The description is to facilitate showing a 
 * meaningful call details on the screen or invoice.
 * The only field that is copied from the CDR, beside the ID, is the date (again, for conveninance).
 * Other information, like the phone number called, is not here. That should come from the CDR
 * repository (DB hopefully). The description can be used as an alternative.
 * The idea is to keep this table CDR format agnostic, and industry agnostic.
 * @author emilc
 */
@Entity
@TableGenerator(
        name="mediation_record_line_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="mediation_record_line",
        allocationSize = 100
        )
@Table(name = "mediation_record_line")
// no cache : it is hardly ever re-read 
public class MediationRecordLineDTO implements Serializable {

    private int id;
    private MediationRecordDTO record;
    private OrderLineDTO orderLine;
    private Date eventDate;
    private BigDecimal amount;
    private BigDecimal quantity;
    private String description;
    private int optlock;

    // needed by Hibernate
    public MediationRecordLineDTO() {
    }

    public MediationRecordLineDTO(int key, MediationRecordDTO record, OrderLineDTO line, Date date,
                                  BigDecimal amount, BigDecimal quantity) {
        this.id = key;
        this.record = record;
        this.orderLine = line;
        this.eventDate = date;
        this.amount = amount;
        this.quantity = quantity;
    }

    public MediationRecordLineDTO(MediationRecordLineWS ws, MediationRecordDTO record, OrderLineDTO orderLine) {
        this.id = ws.getId();
        this.record = record;
        this.orderLine = orderLine;
        this.eventDate = ws.getEventDate();
        this.amount = ws.getAmount();
        this.quantity = ws.getQuantity();
        this.description = ws.getDescription();        
    }

    @Id @GeneratedValue(strategy=GenerationType.TABLE, generator="mediation_record_line_GEN")
    @Column(name = "id", nullable = false)
    public int  getId() {
        return id;
    }
    // needed by Hibernate
    protected void setId(int key) {
        this.id = key;
    }

    @Column(name = "amount", nullable = false)
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Column(name = "quantity", nullable = false)
    public BigDecimal getQuantity() {
        return quantity;
    }
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "event_date", nullable = false)
    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_id", nullable = false)
    public OrderLineDTO getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(OrderLineDTO orderLine) {
        this.orderLine = orderLine;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mediation_record_id", nullable = false)
    public MediationRecordDTO getRecord() {
        return record;
    }

    public void setRecord(MediationRecordDTO record) {
        this.record = record;
    }

    @Version
    @Column(name = "OPTLOCK", nullable = false)
    public int getOptlock() {
        return optlock;
    }

    protected void setOptlock(int optlock) {
        this.optlock = optlock;
    }
    
    public String toString() {
        return "Mediation record line: id " + id + " record " + record.getKey() + " date " + eventDate +
                " order line id " + orderLine.getId() + " amount " + amount + " optlock " + optlock;
    }
}

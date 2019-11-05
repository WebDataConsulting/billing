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
package com.sapienter.jbilling.server.invoice.db;

import java.io.Serializable;
import java.math.BigDecimal;

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

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;

import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;

import com.sapienter.jbilling.server.util.ServerConstants;

@Entity
@TableGenerator(name = "invoice_line_GEN", table = "jbilling_seqs", pkColumnName = "name", valueColumnName = "next_id", pkColumnValue = "invoice_line", allocationSize = 100)
@Table(name = "invoice_line")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class InvoiceLineDTO implements Serializable {

    private int                id;
    private InvoiceLineTypeDTO invoiceLineType;
    private ItemDTO            item;
    private InvoiceDTO         invoice;

    private BigDecimal         amount;
    private BigDecimal         quantity;
    private BigDecimal         price;

    private Integer            deleted;
    private String             description;
    private Integer            sourceUserId;
    private Integer            isPercentage;
    private int                versionNum;

    private OrderDTO           order;

    public InvoiceLineDTO () {
    }

    public InvoiceLineDTO (int id, BigDecimal amount, Integer deleted, Integer isPercentage) {
        this.id = id;
        this.amount = amount;
        this.deleted = deleted;
        this.isPercentage = isPercentage;
    }

    public InvoiceLineDTO (Integer id, String description, BigDecimal amount, BigDecimal price, BigDecimal quantity,
            Integer typeId, Integer deleted, Integer itemId, Integer sourceUserId, Integer isPercentage) {
        setId(id == null ? 0 : id);
        setDescription(description);
        setAmount(amount);
        setPrice(price);
        setQuantity(quantity);
        setDeleted(deleted);
        setItem(itemId == null ? null : new ItemDTO(itemId));
        setSourceUserId(sourceUserId);
        setIsPercentage(isPercentage);
        setInvoiceLineType(new InvoiceLineTypeDTO(typeId));

    }

    public InvoiceLineDTO (int id, InvoiceLineTypeDTO invoiceLineType, ItemDTO item, InvoiceDTO invoice,
            BigDecimal amount, BigDecimal quantity, BigDecimal price, Integer deleted, String description,
            Integer sourceUserId, Integer isPercentage) {
        this.id = id;
        this.invoiceLineType = invoiceLineType;
        this.item = item;
        this.invoice = invoice;
        this.amount = amount;
        this.quantity = quantity;
        this.price = price;
        this.deleted = deleted;
        this.description = description;
        this.sourceUserId = sourceUserId;
        this.isPercentage = isPercentage;
    }

    public InvoiceLineDTO (int id2, String description2, BigDecimal amount, BigDecimal price, BigDecimal quantity2,
            Integer deleted, ItemDTO item, Integer sourceUserId2, Integer isPercentage) {
        this.id = id2;
        this.description = description2;
        this.amount = amount;
        this.price = price;
        this.quantity = quantity2;
        this.deleted = deleted;
        this.item = item;
        this.sourceUserId = sourceUserId2;
        this.isPercentage = isPercentage;
    }

    public static class Builder {

        private String     description;
        private BigDecimal amount   = BigDecimal.ZERO;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal price    = BigDecimal.ZERO;
        private Integer    type;
        private Integer    itemId;
        private Integer    sourceUserId;
        private OrderDTO   order;
        private boolean isPercentage = false;
        public Builder description (String description) {
            this.description = description;
            return this;
        }

        public Builder amount (BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder quantity (BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder price (BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder type (Integer type) {
            this.type = type;
            return this;
        }

        public Builder itemId (Integer itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder sourceUserId (Integer sourceUserId) {
            this.sourceUserId = sourceUserId;
            return this;
        }

        public Builder order (OrderDTO order) {
            this.order = order;
            return this;
        }
        
         public Builder isPercentage (boolean isPercentage) {
            this.isPercentage = isPercentage;
            return this;
        }
        
        public InvoiceLineDTO build () {
            InvoiceLineDTO newLine = new InvoiceLineDTO();
            newLine.setDeleted(0);
            newLine.setDescription(description);
            newLine.setAmount(amount);
            newLine.setQuantity(quantity);
            newLine.setPrice(price);
            newLine.setItem(itemId == null ? null : new ItemDTO(itemId));
            newLine.setSourceUserId(sourceUserId);
            newLine.setInvoiceLineType(new InvoiceLineTypeDTO(type));
            newLine.setIsPercentage(isPercentage ? 1 :0);
            if (order != null) {
                newLine.setOrder(order);
            }
            return newLine;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "invoice_line_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId () {
        return this.id;
    }

    public void setId (int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    public InvoiceLineTypeDTO getInvoiceLineType () {
        return this.invoiceLineType;
    }

    public void setInvoiceLineType (InvoiceLineTypeDTO invoiceLineType) {
        this.invoiceLineType = invoiceLineType;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public ItemDTO getItem () {
        return this.item;
    }

    public void setItem (ItemDTO item) {
        this.item = item;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    public InvoiceDTO getInvoice () {
        return this.invoice;
    }

    public void setInvoice (InvoiceDTO invoice) {
        this.invoice = invoice;
    }

    /**
     * Returns the total amount for this line. Usually this would be the {@code price * quantity}
     *
     * @return amount
     */
    @Column(name = "amount", nullable = false, precision = 17, scale = 17)
    public BigDecimal getAmount () {
        return this.amount;
    }

    public void setAmount (BigDecimal amount) {
        this.amount = amount;
    }

    @Column(name = "quantity")
    public BigDecimal getQuantity () {
        return this.quantity;
    }

    public void setQuantity (BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setQuantity (Integer quantity) {
        setQuantity(new BigDecimal(quantity));
    }

    /**
     * Returns the price of a single unit of this item.
     *
     * @return unit price
     */
    @Column(name = "price", precision = 17, scale = 17)
    public BigDecimal getPrice () {
        return this.price;
    }

    public void setPrice (BigDecimal price) {
        this.price = price;
    }

    @Column(name = "deleted", nullable = false)
    public Integer getDeleted () {
        return this.deleted;
    }

    public void setDeleted (Integer deleted) {
        this.deleted = deleted;
    }

    @Column(name = "description", length = 1000)
    public String getDescription () {
        return this.description;
    }

    public void setDescription (String description) {
        this.description = description;
    }

    @Column(name = "source_user_id")
    public Integer getSourceUserId () {
        return this.sourceUserId;
    }

    public void setSourceUserId (Integer sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    /**
     * Indicates whether or not the item referenced by this line is a percentage item or not.
     *
     * 1 - Item is a percentage item 0 - Item is not a percentage item
     *
     * @return 1 if item is percentage, 0 if not
     */
    @Column(name = "is_percentage", nullable = false)
    public Integer getIsPercentage () {
        return this.isPercentage;
    }

    public void setIsPercentage (Integer isPercentage) {
        this.isPercentage = isPercentage;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum () {
        return versionNum;
    }

    public void setVersionNum (int versionNum) {
        this.versionNum = versionNum;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    public OrderDTO getOrder () {
        return this.order;
    }

    public void setOrder (OrderDTO order) {
        this.order = order;
    }

    @Transient
    public boolean isReviewInvoiceLine() { return ( null != getInvoice() && getInvoice().isReviewInvoice()); }

    @Transient
    public int getOrderPosition () {
        return (null != getInvoiceLineType()) ? getInvoiceLineType().getOrderPosition() : 0;
    }

    @Transient
    public int getTypeId () {
        return (null != getInvoiceLineType()) ? getInvoiceLineType().getId() : 0;
    }

    @Transient
    public boolean dueInvoiceLine () {
        return ServerConstants.INVOICE_LINE_TYPE_DUE_INVOICE.equals(getTypeId());
    }

}

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * This class describes changes in order that can be applied on target date to produce changes in order lines (create/updates)
 * Order change can be created for:
 *      Existed Order Line update - quantity, price, description, assets, order and orderLine fields will be filled
 *      New order line create - item, quantity, price, description, assets, order, useItem fields will be filled
 *      New order line create as child to another order line - parentOrderLine field will be filled additionally to usual line create
 *      New order line create as child for another line, that will be created later - parentOrderChange filed will be filled additionally to usual line create
 * Other fields:
 *      createDateTime - order change create time
 *      startDate - order change planned application to order date
 *      applicationDate - actual order change application to order date
 *      userAssignedStatus - user status of order change
 *      status - system status of order change (before apply, after apply)
 *      errorMessage - error message if some error was found during change application to order
 *      errorCodes - error code if some error was found during change application to order
 * @author Alexander Aksenov
 * @since 09.07.13
 */
@Entity
@TableGenerator(
        name="order_change_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="order_change",
        allocationSize = 100
)
@Table(name="order_change")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderChangeDTO extends CustomizedEntity implements java.io.Serializable {

    private static final FormatLogger LOG = new FormatLogger(OrderChangeDTO.class);

    private Integer id;
    private OrderChangeDTO parentOrderChange;
    private OrderLineDTO parentOrderLine;
    private OrderLineDTO orderLine;
    private OrderDTO order;
    private ItemDTO item;
    private BigDecimal quantity;
    private BigDecimal price;
    private String description;
    private Integer useItem;
    private UserDTO user;
    private Date createDatetime;
    private Date startDate;
    private Date applicationDate;
    private OrderChangeStatusDTO userAssignedStatus;
    private OrderChangeStatusDTO status;
    private Set<AssetDTO> assets = new HashSet<AssetDTO>();
    private String errorMessage;
    private String errorCodes;
    private int optLock;
    private OrderChangeTypeDTO orderChangeType;
    private OrderStatusDTO orderStatusToApply;

    private Integer appliedManually;
    private Integer removal;

    private Date nextBillableDate;
    private Date endDate;

    // non-persisted fields
    // this field filled after creation of orderLine during change apply
    private OrderLineDTO lineCreated;
    private boolean isTouched = false;
    private boolean isPercentage =false;
    
    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="order_change_GEN")
    @Column(name="id", unique=true, nullable=false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="parent_order_change_id", nullable=true)
    public OrderChangeDTO getParentOrderChange() {
        return parentOrderChange;
    }

    public void setParentOrderChange(OrderChangeDTO parentOrderChange) {
        this.parentOrderChange = parentOrderChange;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="parent_order_line_id", nullable=true)
    public OrderLineDTO getParentOrderLine() {
        return parentOrderLine;
    }

    public void setParentOrderLine(OrderLineDTO parentOrderLine) {
        this.parentOrderLine = parentOrderLine;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false)
    public OrderDTO getOrder() {
        return order;
    }

    public void setOrder(OrderDTO order) {
        this.order = order;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="item_id", nullable=false)
    public ItemDTO getItem() {
        return item;
    }

    public void setItem(ItemDTO item) {
        this.item = item;
    }
    @Column(name="quantity", precision=17, scale=17)
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @Column(name="price", precision=17, scale=17)
    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Column(name="description", length=1000)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null && description.length() > 1000) {
            description = description.substring(0, 1000);
            LOG.warn("Truncated an order change description to " + description);
        }

        this.description = description;
    }

    @Column(name = "use_item", nullable = false)
    public Integer getUseItem() {
        return useItem;
    }

    public void setUseItem(Integer useItem) {
        this.useItem = useItem;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Column(name = "create_datetime", nullable = false)
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @Column(name = "start_date", nullable = false)
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column(name = "application_date", nullable = true)
    public Date getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(Date applicationDate) {
        this.applicationDate = applicationDate;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_assigned_status_id", nullable=false)
    public OrderChangeStatusDTO getUserAssignedStatus() {
        return userAssignedStatus;
    }

    public void setUserAssignedStatus(OrderChangeStatusDTO userAssignedStatus) {
        this.userAssignedStatus = userAssignedStatus;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="status_id", nullable=false)
    public OrderChangeStatusDTO getStatus() {
        return status;
    }

    public void setStatus(OrderChangeStatusDTO status) {
        this.status = status;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_line_id", nullable=true)
    public OrderLineDTO getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(OrderLineDTO orderLine) {
        this.orderLine = orderLine;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @Cascade({org.hibernate.annotations.CascadeType.DETACH})
    @JoinTable(name = "order_change_asset_map",
               joinColumns = {@JoinColumn(name = "order_change_id", updatable = false)},
               inverseJoinColumns = {@JoinColumn(name = "asset_id", updatable = false)}
    )
    public Set<AssetDTO> getAssets() {
        return assets;
    }

    public void setAssets(Set<AssetDTO> assets) {
        this.assets = assets;
    }

    @Column(name="error_message", length=500)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        if (errorMessage != null && errorMessage.length() > 1000) {
            errorMessage = errorMessage.substring(0, 500);
            LOG.warn("Truncated an order change error message to " + errorMessage);
        }
        this.errorMessage = errorMessage;
    }

    @Column(name="error_codes", length=200)
    public String getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(String errorCodes) {
        if (errorCodes != null && errorCodes.length() > 1000) {
            errorCodes = errorCodes.substring(0, 500);
            LOG.warn("Truncated an order change error code to " + errorCodes);
        }
        this.errorCodes = errorCodes;
    }

    @Version
    @Column(name = "optlock")
    public int getOptLock() {
        return optLock;
    }

    public void setOptLock(int optLock) {
        this.optLock = optLock;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "order_change_meta_field_map",
            joinColumns = @JoinColumn(name = "order_change_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    @Override
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_change_type_id", nullable=true)
    public OrderChangeTypeDTO getOrderChangeType() {
        return orderChangeType;
    }

    public void setOrderChangeType(OrderChangeTypeDTO orderChangeType) {
        this.orderChangeType = orderChangeType;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_status_id", nullable=true)
    public OrderStatusDTO getOrderStatusToApply() {
        return orderStatusToApply;
    }

    public void setOrderStatusToApply(OrderStatusDTO orderStatusToApply) {
        this.orderStatusToApply = orderStatusToApply;
    }

    @Column(name = "applied_manually", nullable = true)
    public Integer getAppliedManually () {
        return appliedManually;
    }

    public void setAppliedManually (Integer appliedManually) {
        this.appliedManually = appliedManually;
    }

    @Column(name = "removal", nullable = true)
    public Integer getRemoval () {
        return removal;
    }

    public void setRemoval (Integer removal) {
        this.removal = removal;
    }

    @Column(name = "next_billable_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getNextBillableDate () {
        return nextBillableDate;
    }

    public void setNextBillableDate (Date nextBillableDate) {
        this.nextBillableDate = nextBillableDate;
    }

    @Column(name = "end_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getEndDate () {
        return endDate;
    }

    public void setEndDate (Date endDate) {
        this.endDate = endDate;
    }

    @Override
    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.ORDER_CHANGE };
    }
    
    @Transient
    public OrderLineDTO getLineCreated() {
        return lineCreated;
    }

    @Transient
    public void setLineCreated(OrderLineDTO lineCreated) {
        this.lineCreated = lineCreated;
    }

    @Column(name = "is_percentage", nullable = false)
    public boolean isPercentage() {
		return isPercentage;
	}

	public void setPercentage(boolean isPercentage) {
		this.isPercentage = isPercentage;
	}
    
    public void touch() {
        // touch entity with possible cycle dependencies only once
        if (isTouched) return;
        isTouched = true;

        getCreateDatetime();
        if (getItem() != null) {
            getItem().getInternalNumber();
        }
        for(AssetDTO asset: assets) {
            asset.getIdentifier();
        }
        if (getParentOrderLine() != null) {
            getParentOrderLine().touch();
        }
        if (getParentOrderChange() != null) {
            getParentOrderChange().touch();
        }
        if (getOrder() != null) {
            getOrder().touch();
        }
        if (getStatus() != null) {
            getStatus().getApplyToOrder();
        }
        if (getUser() != null) {
            getUser().touch();
        }
        if (getOrderLine() != null) {
            getOrderLine().touch();
        }
    }

    @Override
    public String toString() {
        return "OrderChangeDTO{" +
                "id=" + id +
                ", parentOrderChange=" + (parentOrderChange != null ? parentOrderChange.getId() : "null") +
                ", parentOrderLine=" + (parentOrderLine != null ? parentOrderLine.getId() : "null") +
                ", orderLine=" + (orderLine != null ? orderLine.getId() : "null") +
                ", order=" + (order != null ? order.getId() : "null") +
                ", item=" + (item != null ? item.getId() : "null") +
                ", quantity=" + quantity +
                ", isPercentage=" + isPercentage +
                ", price=" + price +
                ", description='" + description + '\'' +
                ", useItem=" + useItem +
                ", user=" + (user != null ? user.getId() : "null") +
                ", createDatetime=" + createDatetime +
                ", startDate=" + startDate +
                ", applicationDate=" + applicationDate +
                ", nextBillableDate=" + nextBillableDate +
                ", userAssignedStatus=" + userAssignedStatus +
                ", status=" + status +
                ", assets=" + assets +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorCodes='" + errorCodes + '\'' +
                ", lineCreated=" + lineCreated +
                '}';
    }

}

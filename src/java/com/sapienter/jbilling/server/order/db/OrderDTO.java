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


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.discount.db.DiscountLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.user.UserCodeAssociate;
import com.sapienter.jbilling.server.user.db.UserCodeLinkDTO;
import com.sapienter.jbilling.server.user.db.UserCodeOrderLinkDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.time.PeriodUnit;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.hibernate.annotations.OrderBy;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

@Entity
@org.hibernate.annotations.Entity(dynamicUpdate = true)
@TableGenerator(
        name="purchase_order_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="purchase_order",
        allocationSize = 100
)
@Table(name="purchase_order")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderDTO extends CustomizedEntity implements Serializable, Exportable, UserCodeAssociate<UserCodeOrderLinkDTO> {

    private static FormatLogger LOG = new FormatLogger(OrderDTO.class);

    private Integer id;
    private UserDTO baseUserByUserId;
    private UserDTO baseUserByCreatedBy;
    private CurrencyDTO currencyDTO;
    private OrderStatusDTO orderStatusDTO;
    private OrderPeriodDTO orderPeriodDTO;
    private OrderBillingTypeDTO orderBillingTypeDTO;
    private OrderDTO primaryOrderDTO;
    private Date activeSince;
    private Date activeUntil;
    private Date deletedDate;
    private Date cycleStarts;
    private Date createDate;
    private Date nextBillableDay;
    private int deleted;
    private Integer notify;
    private Date lastNotified;
    private Integer notificationStep;
    private Integer dueDateUnitId;
    private Integer dueDateValue;
    private Integer dfFm;
    private Integer anticipatePeriods;
    private Integer ownInvoice;
    // reseller entity order id
    private Integer resellerOrder;
    private String notes;
    private Integer notesInInvoice;
    private Set<OrderProcessDTO> orderProcesses = new HashSet<OrderProcessDTO>(0);
    private List<OrderLineDTO> lines = new ArrayList<OrderLineDTO>(0);
    private Set<DiscountLineDTO> discountLines = new HashSet<>(0);
    private Set<UserCodeOrderLinkDTO> userCodeLinks = new HashSet<UserCodeOrderLinkDTO>(0);

    private Integer versionNum;
    private OrderDTO parentOrder;
    private Set<OrderDTO> childOrders = new HashSet<OrderDTO>(0);

    // other non-persitent fields
    private Collection<OrderProcessDTO> nonReviewPeriods = new ArrayList<OrderProcessDTO>(0);
    private Collection<InvoiceDTO> invoices = new ArrayList<InvoiceDTO>(0);
    private Collection<BillingProcessDTO> billingProcesses = new ArrayList<BillingProcessDTO>(0);
    private String periodStr = null;
    private String billingTypeStr = null;
    private String statusStr = null;
    private String timeUnitStr = null;
    private String currencySymbol = null;
    private String currencyName = null;
    private BigDecimal total = null;
    private List<PricingField> pricingFields = null;
    private String cancellationFeeType;
    private Integer cancellationFee;
    private Integer cancellationFeePercentage;
    private Integer cancellationMaximumFee;
    private Integer cancellationMinimumPeriod;
    private boolean isTouched = false;
    private BigDecimal freeUsageQuantity;
    private Boolean prorateFlag = Boolean.FALSE;

    public OrderDTO() {
    }

    public OrderDTO(OrderDTO other) {
        init(other);
    }

    public void init(OrderDTO other) {
        this.id = other.getId();
        this.baseUserByUserId = other.getBaseUserByUserId();
        this.baseUserByCreatedBy = other.getBaseUserByCreatedBy();
        this.currencyDTO = other.getCurrency();
        this.orderStatusDTO = other.getOrderStatus();
        this.orderPeriodDTO = other.getOrderPeriod();
        this.orderBillingTypeDTO = other.getOrderBillingType();
        this.activeSince = other.getActiveSince();
        this.activeUntil = other.getActiveUntil();
        this.createDate = other.getCreateDate();
        this.nextBillableDay = other.getNextBillableDay();
        this.deleted = other.getDeleted();
        this.notify = other.getNotify();
        this.lastNotified = other.getLastNotified();
        this.notificationStep = other.getNotificationStep();
        this.dueDateUnitId = other.getDueDateUnitId();
        this.dueDateValue = other.getDueDateValue();
        this.dfFm = other.getDfFm();
        this.anticipatePeriods = other.getAnticipatePeriods();
        this.ownInvoice = other.getOwnInvoice();
        this.notes = other.getNotes();
        this.notesInInvoice = other.getNotesInInvoice();
        this.orderProcesses.addAll(other.getOrderProcesses());

        other.getLines().forEach( line -> this.lines.add(new OrderLineDTO(line)));
        other.getDiscountLines().forEach(discountLine -> this.discountLines.add(new DiscountLineDTO(discountLine)));

        this.versionNum = other.getVersionNum();
        this.pricingFields = other.getPricingFields();
        this.cancellationFeeType = other.getCancellationFeeType();
        this.cancellationFee = other.getCancellationFee();
        this.cancellationFeePercentage = other.getCancellationFeePercentage();
        this.cancellationMaximumFee = other.getCancellationMaximumFee();
        this.parentOrder = other.getParentOrder();
        for (OrderDTO childOrder : other.getChildOrders()) {
            this.childOrders.add(new OrderDTO(childOrder));
        }
        this.userCodeLinks = other.userCodeLinks;
        this.prorateFlag = (null != other.getProrateFlag() ? other.getProrateFlag() : false);
    }

    public OrderDTO(int id, UserDTO baseUserByCreatedBy, CurrencyDTO currencyDTO, OrderStatusDTO orderStatusDTO, OrderBillingTypeDTO orderBillingTypeDTO, OrderDTO primaryOrderDTO, Date createDatetime, Integer deleted) {
        this.id = id;
        this.baseUserByCreatedBy = baseUserByCreatedBy;
        this.currencyDTO = currencyDTO;
        this.orderStatusDTO = orderStatusDTO;
        this.orderBillingTypeDTO = orderBillingTypeDTO;
        this.primaryOrderDTO = primaryOrderDTO;
        this.createDate = createDatetime;
        this.deleted = deleted;
    }
    public OrderDTO(int id, UserDTO baseUserByUserId, UserDTO baseUserByCreatedBy, CurrencyDTO currencyDTO,
                    OrderStatusDTO orderStatusDTO, OrderPeriodDTO orderPeriodDTO,
                    OrderBillingTypeDTO orderBillingTypeDTO, OrderDTO primaryOrderDTO, Date activeSince, Date activeUntil, Date createDatetime,
                    Date nextBillableDay, Integer deleted, Integer notify, Date lastNotified, Integer notificationStep,
                    Integer dueDateUnitId, Integer dueDateValue, Integer dfFm, Integer anticipatePeriods,
                    Integer ownInvoice, String notes, Integer notesInInvoice, Set<OrderProcessDTO> orderProcesses,
                    List<OrderLineDTO> orderLineDTOs, Set<DiscountLineDTO> discountLineDTOs, Boolean prorateFlag) {
        this.id = id;
        this.baseUserByUserId = baseUserByUserId;
        this.baseUserByCreatedBy = baseUserByCreatedBy;
        this.currencyDTO = currencyDTO;
        this.orderStatusDTO = orderStatusDTO;
        this.orderPeriodDTO = orderPeriodDTO;
        this.orderBillingTypeDTO = orderBillingTypeDTO;
        this.primaryOrderDTO = primaryOrderDTO;
        this.activeSince = activeSince;
        this.activeUntil = activeUntil;
        this.createDate = createDatetime;
        this.nextBillableDay = nextBillableDay;
        this.deleted = deleted;
        this.notify = notify;
        this.lastNotified = lastNotified;
        this.notificationStep = notificationStep;
        this.dueDateUnitId = dueDateUnitId;
        this.dueDateValue = dueDateValue;
        this.dfFm = dfFm;
        this.anticipatePeriods = anticipatePeriods;
        this.ownInvoice = ownInvoice;
        this.notes = notes;
        this.notesInInvoice = notesInInvoice;
        this.orderProcesses = orderProcesses;
        this.lines = orderLineDTOs;
        this.discountLines = discountLineDTOs;
        this.prorateFlag = prorateFlag;
    }

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="purchase_order_GEN")
    @Column(name="id", unique=true, nullable=false)
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    public UserDTO getBaseUserByUserId() {
        return this.baseUserByUserId;
    }
    public void setBaseUserByUserId(UserDTO baseUserByUserId) {
        this.baseUserByUserId = baseUserByUserId;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="created_by")
    public UserDTO getBaseUserByCreatedBy() {
        return this.baseUserByCreatedBy;
    }

    public void setBaseUserByCreatedBy(UserDTO baseUserByCreatedBy) {
        this.baseUserByCreatedBy = baseUserByCreatedBy;
    }
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="currency_id", nullable=false)
    public CurrencyDTO getCurrency() {
        return this.currencyDTO;
    }

    public void setCurrency(CurrencyDTO currencyDTO) {
        this.currencyDTO = currencyDTO;
    }
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="status_id", nullable=false)
    public OrderStatusDTO getOrderStatus() {
        return this.orderStatusDTO;
    }

    public void setOrderStatus(OrderStatusDTO orderStatusDTO) {
        this.orderStatusDTO = orderStatusDTO;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="period_id")
    public OrderPeriodDTO getOrderPeriod() {
        return this.orderPeriodDTO;
    }
    public void setOrderPeriod(OrderPeriodDTO orderPeriodDTO) {
        this.orderPeriodDTO = orderPeriodDTO;
    }

    public void setOrderPeriodId(Integer id) {
        if (id != null) {
            setOrderPeriod(new OrderPeriodDAS().find(id));
        } else {
            setOrderPeriod(null);
        }
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="billing_type_id", nullable=false)
    public OrderBillingTypeDTO getOrderBillingType() {
        return this.orderBillingTypeDTO;
    }

    public void setOrderBillingType(OrderBillingTypeDTO orderBillingTypeDTO) {
        this.orderBillingTypeDTO = orderBillingTypeDTO;
    }
    
    public boolean hasPrimaryOrder() {
    	return this.parentOrder != null;
    }
    
    @Column(name="active_since", length=13)
    public Date getActiveSince() {
        return this.activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    @Column(name="active_until", length=13)
    public Date getActiveUntil() {
        return this.activeUntil;
    }

    public void setActiveUntil(Date activeUntil) {
        this.activeUntil = activeUntil;
    }

    @Column(name="deleted_date", length=13)
    public Date getDeletedDate() {
        return this.deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
    }

    @Column(name="create_datetime", nullable=false, length=29)
    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDatetime) {
        this.createDate = createDatetime;
    }
    @Column(name="next_billable_day", length=29)
    public Date getNextBillableDay() {
        return this.nextBillableDay;
    }

    public void setNextBillableDay(Date nextBillableDay) {
        this.nextBillableDay = nextBillableDay;
    }

    @Column(name="deleted", nullable=false)
    public int getDeleted() {
        return this.deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @Column(name="notify")
    public Integer getNotify() {
        return this.notify;
    }

    public void setNotify(Integer notify) {
        this.notify = notify;
    }
    @Column(name="last_notified", length=29)
    public Date getLastNotified() {
        return this.lastNotified;
    }

    public void setLastNotified(Date lastNotified) {
        this.lastNotified = lastNotified;
    }

    @Column(name="notification_step")
    public Integer getNotificationStep() {
        return this.notificationStep;
    }

    public void setNotificationStep(Integer notificationStep) {
        this.notificationStep = notificationStep;
    }

    @Column(name="due_date_unit_id")
    public Integer getDueDateUnitId() {
        return this.dueDateUnitId;
    }

    public void setDueDateUnitId(Integer dueDateUnitId) {
        this.dueDateUnitId = dueDateUnitId;
    }

    @Column(name="due_date_value")
    public Integer getDueDateValue() {
        return this.dueDateValue;
    }

    public void setDueDateValue(Integer dueDateValue) {
        this.dueDateValue = dueDateValue;
    }

    @Column(name="df_fm")
    public Integer getDfFm() {
        return this.dfFm;
    }

    public void setDfFm(Integer dfFm) {
        this.dfFm = dfFm;
    }

    @Column(name="anticipate_periods")
    public Integer getAnticipatePeriods() {
        return this.anticipatePeriods;
    }

    public void setAnticipatePeriods(Integer anticipatePeriods) {
        this.anticipatePeriods = anticipatePeriods;
    }

    @Column(name="own_invoice")
    public Integer getOwnInvoice() {
        return this.ownInvoice;
    }

    public void setOwnInvoice(Integer ownInvoice) {
        this.ownInvoice = ownInvoice;
    }

    @Column(name="notes", length=200)
    public String getNotes() {
        return this.notes;
    }

    public void setNotes(String notes) {
        // make sure this is fits in the DB
        if (notes == null || notes.length() <= 200) {
            this.notes = notes;
        } else {
            this.notes = notes.substring(0, 200);
            LOG.warn("Trimming notes to 200 lenght: from " + notes + " to " + this.notes);
        }
    }

    @Column(name="notes_in_invoice")
    public Integer getNotesInInvoice() {
        return this.notesInInvoice;
    }

    public void setNotesInInvoice(Integer notesInInvoice) {
        this.notesInInvoice = notesInInvoice;
    }

    /*
     * There might potentially hundreds of process records, but they are not read by the app.
     * They are only taken for display, and then all are needed
     */
    @CollectionOfElements
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="purchaseOrder")
    @OrderBy (
            clause = "id desc"
    )
    public Set<OrderProcessDTO> getOrderProcesses() {
        return this.orderProcesses;
    }

    public void setOrderProcesses(Set<OrderProcessDTO> orderProcesses) {
        this.orderProcesses = orderProcesses;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="purchaseOrder")
    @OrderBy(clause="id")
    public List<OrderLineDTO> getLines() {
        return this.lines;
    }

    public void setLines(List<OrderLineDTO> orderLineDTOs) {
        this.lines = orderLineDTOs;
    }
    
    @Column(name="reseller_order", updatable = false)
    public Integer getResellerOrder() {
		return resellerOrder;
	}

	public void setResellerOrder(Integer resellerOrder) {
		this.resellerOrder = resellerOrder;
	}

	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="purchaseOrder")
    @OrderBy(clause="id")
    public Set<DiscountLineDTO> getDiscountLines() {
		return discountLines;
	}

	public void setDiscountLines(Set<DiscountLineDTO> discountLines) {
		this.discountLines = discountLines;
	}
	
	public boolean hasDiscountLines() {
		return getDiscountLines() != null && !getDiscountLines().isEmpty();
	}

    @Version
    @Column(name="OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }
    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id")
    public OrderDTO getParentOrder() {
        return parentOrder;
    }

    public void setParentOrder(OrderDTO parentOrder) {
        this.parentOrder = parentOrder;
    }

    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "parentOrder")
    public Set<OrderDTO> getChildOrders() {
        return childOrders;
    }

    public void setChildOrders(Set<OrderDTO> childOrders) {
        this.childOrders = childOrders;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "order_meta_field_map",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }
   
    @Column(name = "prorate_flag", nullable = false)
	public Boolean getProrateFlag() {
		return prorateFlag;
	}

	public void setProrateFlag(Boolean prorateFlag) {
		this.prorateFlag = prorateFlag;
	}
	
	@Transient
	public boolean getProrateFlagValue() {
		return null != prorateFlag && prorateFlag.booleanValue();
	}

	
	@Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.ORDER };
    }
	 
    /*
     * Conveniant methods to ease migration from entity beans
     */
    @Transient
    public Integer getBillingTypeId() {
        return getOrderBillingType() == null ? null : getOrderBillingType().getId();
    }
    /*
    public void setBillingTypeId(Integer typeId) {
        if (orderBillingTypeDTO == null) {
            OrderBillingTypeDTO dto = new OrderBillingTypeDTO();
            dto.setId(typeId);
            setOrderBillingType(dto);
        } else {
            orderBillingTypeDTO.setId(id)
        }
    }
    */
    
    @Transient
    public Integer getStatusId() {
        return getOrderStatus() == null ? null : getOrderStatus().getId();
    }
    public void setStatusId(Integer statusId) {
        if (statusId == null) {
            setOrderStatus(null);
            return;
        }
        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setId(statusId);
        setOrderStatus(dto);
    }

    @Transient
    public Integer getCurrencyId() {
        return getCurrency().getId();
    }
    public void setCurrencyId(Integer currencyId) {
        if (currencyId == null) {
            setCurrency(null);
        } else {
            CurrencyDTO currency = new CurrencyDTO(currencyId);
            setCurrency(currency);
        }
    }

    @Transient
    public UserDTO getUser() {
        return getBaseUserByUserId();
    }

    @Formula("(select sum(ol.amount) from order_line ol where ol.order_id = id and ol.deleted=0)")
    public BigDecimal getTotal() {
        return null != total ? total : BigDecimal.ZERO;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Transient
    // all the periods, but excluding those from process reviews
    public Collection<OrderProcessDTO> getPeriods() {
        return nonReviewPeriods;
    }

    @Transient
    public Collection<InvoiceDTO> getInvoices() {
        return invoices;

    }

    @Transient
    public String getPeriodStr() {
        return periodStr;
    }
    public void setPeriodStr(String str) {
        periodStr = str;
    }

    @Transient
    public String getBillingTypeStr() {
        return billingTypeStr;
    }
    public void setBillingTypeStr(String str) {
        this.billingTypeStr = str;
    }

    @Transient
    public String getStatusStr() {
        return statusStr;
    }

    @Transient
    public String getTimeUnitStr() {
        return timeUnitStr;
    }

    @Transient
    public String getCurrencyName() {
        return currencyName;
    }

    @Transient
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void addExtraFields(Integer languageId) {
        invoices = new ArrayList<InvoiceDTO>();
        billingProcesses = new ArrayList<BillingProcessDTO>();
        nonReviewPeriods = new ArrayList<OrderProcessDTO>();

        for (OrderProcessDTO process: getOrderProcesses()) {
            if (process.getIsReview() == 1) continue;
            nonReviewPeriods.add(process);

            try {
                InvoiceBL invoiceBl = new InvoiceBL(process.getInvoice().getId());
                invoices.add(invoiceBl.getDTO());
            } catch (Exception e) {
                throw new SessionInternalError(e);
            }

            billingProcesses.add(process.getBillingProcess());
        }

        periodStr = getOrderPeriod().getDescription(languageId);
        billingTypeStr = getOrderBillingType().getDescription(languageId);
        statusStr = getOrderStatus().getDescription(languageId);
        timeUnitStr = Util.getPeriodUnitStr(
                getDueDateUnitId(), languageId);

        currencySymbol = getCurrency().getSymbol();
        currencyName = getCurrency().getDescription(languageId);

    }

    @Transient
    public Integer getPeriodId() {
        return getOrderPeriod().getId();
    }

    @Transient
    public Integer getUserId() {
        return (getBaseUserByUserId() == null) ? null : getBaseUserByUserId().getId();
    }

    @Transient
    public Integer getCreatedBy() {
        return (getBaseUserByCreatedBy() == null) ? null : getBaseUserByCreatedBy().getId();
    }

    @Transient
    public OrderLineDTO getLine(Integer itemId) {
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 0 && line.getItem() != null && line.getItem().getId() == itemId) {
                return line;
            }
        }

        return null;
    }

    @Transient
    public OrderLineDTO getLineById(Integer lineId) {
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 0 && line.getId() == lineId) {
                return line;
            }
        }

        return null;
    }

    @Transient
    public void removeLine(Integer itemId) {
        OrderLineDTO line = getLine(itemId);
        if (line != null) {
            lines.remove(line);
        }
    }

    @Transient
    public void removeLineById(Integer lineId) {
        OrderLineDTO line = getLineById(lineId);
        if (line != null) {
            lines.remove(line);
        }
    }

    @Transient
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    @Transient
    public int getNumberOfLines() {
        int count = 0;
        for (OrderLineDTO line: getLines()) {
            if (line.getDeleted() == 0) {
                count++;
            }
        }
        return count;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<UserCodeOrderLinkDTO> getUserCodeLinks() {
        return userCodeLinks;
    }

    public void setUserCodeLinks(Set<UserCodeOrderLinkDTO> userCodes) {
        this.userCodeLinks = userCodes;
    }

    public void addUserCodeLink(UserCodeOrderLinkDTO dto) {
        dto.setOrder(this);
        userCodeLinks.add(dto);
    }

    @Transient
    public List<PricingField> getPricingFields() {
        return this.pricingFields;
    }

    public void setPricingFields(List<PricingField> fields) {
        this.pricingFields = fields;
    }

    // default values
    @Transient
    public void setDefaults(Integer entityId) {
        if (getCreateDate() == null) {
            setCreateDate(Calendar.getInstance().getTime());
            setDeleted(0);
        }
        if (getOrderStatus() == null) {
        	OrderStatusDAS orderStatusDAS = new OrderStatusDAS();
            setOrderStatus(orderStatusDAS.find(orderStatusDAS.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, entityId)));
        }
        for (OrderLineDTO line : lines) {
            line.setDefaults();
        }
    }

    /**
     * Makes sure that all the proxies are loaded, so no session is needed to
     * use the pojo
     */
    public void touch() {
        // touch entity with possible cycle dependencies only once
        if (isTouched) return;
        isTouched = true;

        getActiveSince();
        if (getBaseUserByUserId() != null)
            getBaseUserByUserId().getCreateDatetime();
        if (getBaseUserByCreatedBy() != null)
            getBaseUserByCreatedBy().getCreateDatetime();
        for (OrderLineDTO line: getLines()) {
            line.touch();
        }
        for (DiscountLineDTO discountLine: getDiscountLines()) {
            discountLine.getDiscount();
        }
        for (InvoiceDTO invoice: getInvoices()) {
            invoice.getCreateDatetime();
        }
        for (OrderProcessDTO process: getOrderProcesses()) {
            process.getPeriodStart();
        }
        for (MetaFieldValue metaField : this.getMetaFields()) {
            metaField.touch();
        }
        if (getOrderBillingType() != null)
            getOrderBillingType().getId();
        if (getOrderPeriod() != null)
            getOrderPeriod().getId();
        if (getOrderStatus() != null)
            getOrderStatus().getId();
        if (getParentOrder() != null) {
            getParentOrder().touch();
        }
        for (OrderDTO childOrder : getChildOrders()) {
            childOrder.touch();
        }

        for(UserCodeLinkDTO userCodeLink : getUserCodeLinks()) {
            userCodeLink.touch();
        }
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer("Order = " +
                "id=" + id + "," +
                "baseUserByUserId=" + ((baseUserByUserId == null) ? null : baseUserByUserId.getId()) + "," +
                "baseUserByCreatedBy=" + ((baseUserByCreatedBy== null) ? null : baseUserByCreatedBy.getId()) + "," +
                "currencyDTO=" + currencyDTO + "," +
                "orderStatusDTO=" + ((orderStatusDTO == null) ? null : orderStatusDTO) + "," +
                "orderPeriodDTO=" + ((orderPeriodDTO == null) ? null : orderPeriodDTO) + "," +
                "orderBillingTypeDTO=" + ((orderBillingTypeDTO == null) ? null : orderBillingTypeDTO) + "," +
                "primaryOrderDTO=" + ((primaryOrderDTO == null) ? null : primaryOrderDTO.getId()) + "," +
                "activeSince=" + activeSince + "," +
                "activeUntil=" + activeUntil + "," +
                "createDate=" + createDate + "," +
                "nextBillableDay=" + nextBillableDay + "," +
                "deleted=" + deleted + "," +
                "notify=" + notify + "," +
                "lastNotified=" + lastNotified + "," +
                "notificationStep=" + notificationStep + "," +
                "dueDateUnitId=" + dueDateUnitId + "," +
                "dueDateValue=" + dueDateValue + "," +
                "dfFm=" + dfFm + "," +
                "anticipatePeriods=" + anticipatePeriods + "," +
                "ownInvoice=" + ownInvoice + "," +
                "notes=" + notes + "," +
                "notesInInvoice=" + notesInInvoice + "," +
                "orderProcesses=" + orderProcesses + "," +
                "versionNum=" + versionNum +
                " freeUsageQuantity=" +  freeUsageQuantity +
                " lines:[");

        for (OrderLineDTO line: getLines()) {
            str.append(line.toString()).append("-");
        }
        str.append(']');/*
        str.append(", parentOrder =").append(parentOrder != null ? parentOrder : null ).append(',');
        str.append(" childOrderIds:[");
        for (OrderDTO childOrder: getChildOrders()) {
            str.append(childOrder.getId()).append("-");
        }
        str.append("]");
      
    	str.append(", discountLines:[");
        for (DiscountLineDTO discountLine: getDiscountLines()) {
            str.append(discountLine.toString() + "-");
        }
        str.append(']');*/
        
        return str.toString();

    }

    @Transient
    public String[] getFieldNames() {
        String headers[]= new String[] {
                "id",
                "userId",
                "userName",
                "status",
                "period",
                "billingType",
                "currency",
                "total",
                "activeSince",
                "activeUntil",
                "cycleStart",
                "createdDate",
                "nextBillableDay",
                "isMainSubscription",
                "notes",

                // order lines
                "lineItemId",
                "lineProductCode",
                "lineQuantity",
                "linePrice",
                "lineAmount",
                "lineDescription"
        };
        
        List<String> list = new ArrayList<>(Arrays.asList(headers));
        for(String name : metaFieldsNames) {
        	list.add(name);
        }
        
        return list.toArray(new String[list.size()]);
    }

    @Transient
    public Object[][] getFieldValues() {
        List<Object[]> values = new ArrayList<Object[]>();

        // main invoice row
        values.add(
                new Object[] {
                        id,
                        (baseUserByUserId != null ? baseUserByUserId.getId() : null),
                        (baseUserByUserId != null ? baseUserByUserId.getUserName() : null),
                        (orderStatusDTO != null ? orderStatusDTO.getDescription() : null),
                        (orderPeriodDTO != null ? orderPeriodDTO.getDescription() : null),
                        (orderBillingTypeDTO != null ? orderBillingTypeDTO.getDescription() : null),
                        (currencyDTO != null ? currencyDTO.getDescription() : null),
                        getTotal(),
                        activeSince,
                        activeUntil,
                        cycleStarts,
                        createDate,
                        nextBillableDay,
                        notes
                }
        );

        // indented row for each order line
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 0) {
                values.add(
                        new Object[] {
                                // padding for the main invoice columns
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,

                                // order line
                                line.getItem().getId(),
                                line.getItem().getInternalNumber(),
                                line.getQuantity(),
                                line.getPrice(),
                                line.getAmount(),
                                line.getDescription()
                        }
                );
            }
        }
        
        Object value[] = new Object[metaFieldsNames.size()];
        int index = 0;
        for(String name : metaFieldsNames) {
        	MetaFieldValue metavalue = getMetaField(name);
        	if(metavalue == null) {
        		value[index] = null;
        	}
        	else {
        		value[index] = metavalue;
        	}
        	index++;
        }
        values.add(value);
        
        return values.toArray(new Object[values.size()][]);
    }

    @Transient
    public Date getPricingDate() {
        Date billingDate = getActiveSince();
        if (billingDate == null) {
            billingDate = getCreateDate();
        }
        return billingDate;
    }

    @Column(name = "cancellation_fee_type")
	public String getCancellationFeeType() {
		return cancellationFeeType;
	}

	public void setCancellationFeeType(String cancellationFeeType) {
		this.cancellationFeeType = cancellationFeeType;
	}

	@Column(name = "cancellation_fee")
	public Integer getCancellationFee() {
		return cancellationFee;
	}

    public void setCancellationFee(Integer cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    @Column(name = "cancellation_fee_percentage")
    public Integer getCancellationFeePercentage() {
        return cancellationFeePercentage;
    }

    public void setCancellationFeePercentage(Integer cancellationFeePercentage) {
        this.cancellationFeePercentage = cancellationFeePercentage;
    }
	
    @Column(name = "cancellation_maximum_fee")
    public Integer getCancellationMaximumFee(){
        return cancellationMaximumFee;
    }
	
    public void setCancellationMaximumFee(Integer cancellationMaximumFee){
        this.cancellationMaximumFee = cancellationMaximumFee;
    }

    @Column(name = "cancellation_minimum_period")
    public Integer getCancellationMinimumPeriod() {
        return cancellationMinimumPeriod;
    }

    public void setCancellationMinimumPeriod(Integer cancellationMinimumPeriod) {
        this.cancellationMinimumPeriod = cancellationMinimumPeriod;
    }

    @Transient
    public Date calcNextBillableDayFromChanges () {
        Date nextBillableDate = this.getNextBillableDay();
        for (OrderLineDTO line : this.getLines()) {
            for (OrderChangeDTO change : line.getOrderChanges()) {
                if ((nextBillableDate == null) || nextBillableDate.after(change.getNextBillableDate())) {
                    nextBillableDate = change.getNextBillableDate();
                }
            }
        }
        return nextBillableDate;
    }

    @Transient
    public PeriodUnit valueOfPeriodUnit () {

        int periodUnitId = this.getOrderPeriod().getPeriodUnit().getId();
        int dayOfMonth = this.getUser().getCustomer().getMainSubscription().getNextInvoiceDayOfPeriod();

        return PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);
    }

    @Transient
    public boolean isOneTime() {
        return (orderPeriodDTO.getId() == Constants.ORDER_PERIOD_ONCE.intValue());
    }

    @Transient
    public boolean isRecurring() {
        return (orderPeriodDTO.getId() != Constants.ORDER_PERIOD_ONCE.intValue());
    }

}

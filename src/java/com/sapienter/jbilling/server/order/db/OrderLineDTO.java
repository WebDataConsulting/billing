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


import java.io.Serializable;
import java.util.*;

import javax.persistence.*;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;

import java.math.BigDecimal;
import java.util.ArrayList;

@Entity
@org.hibernate.annotations.Entity(dynamicUpdate = true)
@TableGenerator(
        name="order_line_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="order_line",
        allocationSize = 100
        )
@Table(name="order_line")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderLineDTO extends CustomizedEntity implements Serializable, Comparable {

    private static final FormatLogger LOG =  new FormatLogger(OrderLineDTO.class);

    private int id;
    private OrderLineTypeDTO orderLineTypeDTO;
    private ItemDTO item;
    private OrderDTO orderDTO;
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal price;
    private Date createDatetime;
    private int deleted;
    private Boolean useItem = true;
    private String description;
    private Integer versionNum;
    private Boolean editable = null;
    private Set<AssetDTO> assets = new HashSet<AssetDTO>(2);
	private Set<AssetAssignmentDTO> assetAssignments = new HashSet<AssetAssignmentDTO>(0);
    private OrderLineDTO parentLine;
    private Set<OrderLineDTO> childLines = new HashSet<OrderLineDTO>(0);

    private String sipUri;
    
    private Set<OrderChangeDTO> orderChanges = new HashSet<OrderChangeDTO>(0);

    private Date startDate;
    private Date endDate;

    // other fields, non-persistent
    private String priceStr = null;
    private Boolean totalReadOnly = null;

    private boolean isTouched = false;

    private boolean mediated = false;
    private BigDecimal mediatedQuantity;
    private boolean isPercentage =false;
    
    public OrderLineDTO() {
    }

    public OrderLineDTO(OrderLineDTO other) {
        this.id = other.getId();
        this.orderLineTypeDTO = other.getOrderLineType();
        this.item = other.getItem();
        this.amount = other.getAmount();
        this.quantity = other.getQuantity();
        this.price = other.getPrice();
        this.createDatetime = other.getCreateDatetime();
        this.deleted = other.getDeleted();
        this.useItem = other.getUseItem();
        this.description = other.getDescription();
        this.orderDTO = other.getPurchaseOrder();
        this.versionNum = other.getVersionNum();
        this.assets.addAll(other.getAssets());
        this.parentLine = other.getParentLine();
        this.childLines.addAll(other.getChildLines());
        this.startDate = other.getStartDate();
        this.endDate = other.getEndDate();
        this.isPercentage =other.isPercentage();
    }

    public OrderLineDTO(int id, BigDecimal amount, Date createDatetime, Integer deleted) {
        this.id = id;
        this.amount = amount;
        this.createDatetime = createDatetime;
        this.deleted = deleted != null ? deleted : 0;
    }
    
    public OrderLineDTO(int id, OrderLineTypeDTO orderLineTypeDTO, ItemDTO item, OrderDTO orderDTO, BigDecimal amount,
            BigDecimal quantity, BigDecimal price, Date createDatetime, Integer deleted,
            String description) {
       this.id = id;
       this.orderLineTypeDTO = orderLineTypeDTO;
       this.item = item;
       this.orderDTO = orderDTO;
       this.amount = amount;
       this.quantity = quantity;
       this.price = price;
       this.createDatetime = createDatetime;
       this.deleted = deleted;
       this.description = description;
    }
    
    @Id @GeneratedValue(strategy=GenerationType.TABLE, generator="order_line_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="type_id", nullable=false)
    public OrderLineTypeDTO getOrderLineType() {
        return this.orderLineTypeDTO;
    }
    
    public void setOrderLineType(OrderLineTypeDTO orderLineTypeDTO) {
        this.orderLineTypeDTO = orderLineTypeDTO;
    }

    @OneToMany(fetch=FetchType.LAZY, mappedBy="orderLine")
    @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE, org.hibernate.annotations.CascadeType.MERGE, org.hibernate.annotations.CascadeType.DETACH})
    public Set<AssetDTO> getAssets() {
        return assets;
    }

    public void setAssets(Set<AssetDTO> assets) {
        this.assets = assets;
    }

    public void addAssets(Set<AssetDTO> assets) {
        for(AssetDTO asset : assets) {
            addAsset(asset);
        }
    }

    public void addAsset(AssetDTO asset) {
      assets.add(asset);
      asset.setOrderLine(this);
    }

    public void removeAsset(AssetDTO asset) {
        assets.remove(asset);
        asset.setOrderLine(null);
    }

    public Integer[] convertAssetIds() {
        Integer[] ids = new Integer[assets.size()];
        int idx=0;
        for(AssetDTO asset : assets) {
            ids[idx++] = asset.getId();
        }
        return ids;
    }

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "orderLine")
	@Cascade({org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.REMOVE})
	public Set<AssetAssignmentDTO> getAssetAssignments() {
		return assetAssignments;
	}

	public void setAssetAssignments(Set<AssetAssignmentDTO> assetAssignments) {
		this.assetAssignments = assetAssignments;
	}

	@Transient
	public Integer[] getAssetAssignmentIds() {
		Integer[] ids = new Integer[assetAssignments.size()];
		int idx = 0;
		for (AssetAssignmentDTO assign : assetAssignments) {
			ids[idx++] = assign.getId();
		}
		return ids;
	}

    @OneToMany(fetch = FetchType.LAZY, cascade = javax.persistence.CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "order_line_meta_field_map",
            joinColumns = @JoinColumn(name = "order_line_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    @Override
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @Override
    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.ORDER_LINE };
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_line_id")
    public OrderLineDTO getParentLine() {
        return parentLine;
    }

    public void setParentLine(OrderLineDTO parentLine) {
        this.parentLine = parentLine;
    }

    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "parentLine")
    public Set<OrderLineDTO> getChildLines() {
        return childLines;
    }

    public void setChildLines(Set<OrderLineDTO> childLines) {
        this.childLines = childLines;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="item_id")
    public ItemDTO getItem() {
        return this.item;
    }
    
    public void setItem(ItemDTO item) {
        this.item = item;
    }
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_id")
    public OrderDTO getPurchaseOrder() {
        return this.orderDTO;
    }
    
    public void setPurchaseOrder(OrderDTO orderDTO) {
        this.orderDTO = orderDTO;
    }

    /**
     * Returns the total amount for this line. Usually this would be
     * the {@code price * quantity}
     *
     * @return amount
     */
    @Column(name="amount", nullable=false, precision=17, scale=17)
    public BigDecimal getAmount() {
        return this.amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    @Column(name="quantity", precision=17, scale=17)
    public BigDecimal getQuantity() {
        return this.quantity;
    }
    
    @Transient
    public void setQuantity(Integer quantity) {
        setQuantity(new BigDecimal(quantity));
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @Transient
    public void setQuantity(Double quantity) {
        setQuantity(new BigDecimal(quantity).setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
    }

    /**
     * Returns the price of a single unit of this item.
     *
     * @return unit price
     */    
    @Column(name="price", precision=17, scale=17)
    public BigDecimal getPrice() {
        return this.price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Column(name="create_datetime", nullable=false, length=29)
    public Date getCreateDatetime() {
        return this.createDatetime;
    }
    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }
    
    @Column(name="deleted", nullable=false)
    public int getDeleted() {
        return this.deleted;
    }
    
    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @Column(name = "use_item", nullable = false)
    public Boolean getUseItem() {
        return useItem;
    }

    public void setUseItem(Boolean useItem) {
        this.useItem = useItem;
    }

    @Column(name="description", length=1000)
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        if (description != null && description.length() > 1000) {
            description = description.substring(0, 1000);
            LOG.warn("Truncated an order line description to " + description);
        }

        this.description = description;
    }

    @Version
    @Column(name="OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }
    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }
        
    /*
     * Conveniant methods to ease migration from entity beans
     */
    @Transient
    public Integer getItemId() {
        return (getItem() == null) ? null : getItem().getId();
    }

    public void setItemId(Integer itemId) {
        ItemDAS das = new ItemDAS();
        setItem(das.find(itemId));
    }

    @Transient
    public boolean hasItem() {
    	return getItem() != null;
    }

    @Transient
    public Boolean getEditable() {
        if (editable == null) {
            editable = getOrderLineType().getEditable() == 1;
        }
        return editable;
    }
    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    @Transient
    public String getPriceStr() {
        return priceStr;
    }

    public void setPriceStr(String priceStr) {
        this.priceStr = priceStr;
    }
    
    @Transient
    public Boolean getTotalReadOnly() {
        if (totalReadOnly == null) {
            setTotalReadOnly(false);
        }
        return totalReadOnly;
    }

    public void setTotalReadOnly(Boolean totalReadOnly) {
        this.totalReadOnly = totalReadOnly;
    }

    @Transient
    public Integer getTypeId() {
        return getOrderLineType() == null ? null : getOrderLineType().getId();
    }

    public void setTypeId(Integer typeId) {
        OrderLineTypeDAS das = new OrderLineTypeDAS();
        setOrderLineType(das.find(typeId));
    }
    
    @Transient
    public Integer getQuantityInt() {
        if (quantity == null) return null;
        return this.quantity.intValue();
    }

    @Column(name = "sip_uri", nullable = true)
    public String getSipUri () {
        return sipUri;
    }

    public void setSipUri (String sipUri) {
        this.sipUri = sipUri;
    }

    @Transient
    public boolean isMediated() {
    	return mediated;
    }

    public void setMediated(boolean mediated) {
    	this.mediated = mediated;
    }
    
    @Transient
	public BigDecimal getMediatedQuantity() {
		return mediatedQuantity;
	}

	public void setMediatedQuantity(BigDecimal mediatedQuantity) {
		this.mediatedQuantity = mediatedQuantity;
	}

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "orderLine")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<OrderChangeDTO> getOrderChanges() {
        return orderChanges;
    }

    public void setOrderChanges(Set<OrderChangeDTO> orderChanges) {
        this.orderChanges = orderChanges;
    }

    public void addOrderChange(OrderChangeDTO orderChange) {
        orderChanges.add(orderChange);
        orderChange.setOrderLine(this);
    }

    @Column(name = "start_date", nullable = true)
    @Temporal(TemporalType.DATE)
    public Date getStartDate () {
        return startDate;
    }

    public void setStartDate (Date startDate) {
        this.startDate = startDate;
    }

    @Column(name = "end_date", nullable = true)
    @Temporal(TemporalType.DATE)
    public Date getEndDate () {
        return endDate;
    }

    public void setEndDate (Date endDate) {
        this.endDate = endDate;
    }


    public void touch() {
        // touch entity with possible cycle dependencies only once
        if (isTouched) return;
        isTouched = true;

        getCreateDatetime();
        if (getItem() != null) {
            getItem().getInternalNumber();
        }
        getEditable();

        for(AssetDTO asset: assets) {
            asset.touch();
        }
        if (getParentLine() != null) {
            getParentLine().touch();
        }
        for (OrderLineDTO childLine : getChildLines()) {
            childLine.touch();
        }

        for(MetaFieldValue value: getMetaFields()) {
            value.touch();
        }
        
    }

    /**
     * Returns trye if the OrderLine is linked to asset with {@code id}
     * @param id
     * @return
     */
    public boolean containsAsset(int id) {
        for(AssetDTO assetDTO : assets) {
            if(assetDTO.getId() == id) return true;
        }
        return false;
    }

    @Transient
    public List<OrderChangeDTO> getOrderChangesSortedByStartDate () {
        List<OrderChangeDTO> sortedchanges = new ArrayList<OrderChangeDTO>(getOrderChanges());
        Collections.sort(sortedchanges, OrderLineChangeDTOStartDateComparator);
        return sortedchanges;
    }
    public final static Comparator<OrderChangeDTO> OrderLineChangeDTOStartDateComparator;

    static {
        OrderLineChangeDTOStartDateComparator = (change1, change2) -> {
                Date charge1Start = change1.getStartDate();
                Date charge2Start = change2.getStartDate();

                // ascending order
                int result = charge1Start.compareTo(charge2Start);
                if (result != 0) {
                    return result;
                }
                // same start date case
                return change1.getCreateDatetime().compareTo(change2.getCreateDatetime());
            };
    }

    @Transient
    public List<OrderChangeDTO> getOrderChangesSortedByCreateDateTime () {
        List<OrderChangeDTO> sortedchanges = new ArrayList<>(getOrderChanges());
        Collections.sort(sortedchanges, OrderLineChangeDTOCreateDateTimeComparator);
        return sortedchanges;
    }
    public final static Comparator<OrderChangeDTO> OrderLineChangeDTOCreateDateTimeComparator;

    static {
        OrderLineChangeDTOCreateDateTimeComparator = (change1, change2) -> change1.getCreateDatetime().compareTo(change2.getCreateDatetime());
    }

    @Transient
    public void moveToOtherOrder (OrderDTO otherOrder) {
        this.getPurchaseOrder().getLines().remove(this);
        this.setPurchaseOrder(otherOrder);
        this.getPurchaseOrder().getLines().add(this);

        for (OrderChangeDTO change : orderChanges) {
            change.setOrder(this.getPurchaseOrder());
        }
    }

    @Transient
    public void setDefaults() {
        if (getCreateDatetime() == null) {
            setCreateDatetime(Calendar.getInstance().getTime());
        }
    }

    // this helps to add lines to the treeSet
    public int compareTo(Object o) {
        OrderLineDTO other = (OrderLineDTO) o;
        if (other.getItem() == null || this.getItem() == null) {
            return -1;
        }
        return new Integer(this.getItem().getId()).compareTo(other.getItem().getId());
    }

    @Column(name = "is_Percentage", nullable = false, updatable = true)
	public boolean isPercentage() {
		return isPercentage;
	}

	public void setPercentage(boolean isPercentage) {
		this.isPercentage = isPercentage;
	}
    
    @Override
    public String toString() {
        return "OrderLine:[id=" + id +
        " orderLineType=" + ((orderLineTypeDTO == null) ? "null" : orderLineTypeDTO.getId()) +
        " item=" +  ((item==null) ? "null" : item.getId()) +
        " order id=" + ((orderDTO == null) ? "null" : orderDTO.getId()) +
        " amount=" +  amount +
        " quantity=" +  quantity +
        " price=" +  price +
        " isPercentage=" +  isPercentage +
        " createDatetime=" +  createDatetime +
        " deleted=" + deleted  +
        " useItem=" + useItem +
        " description=" + description + 
        " versionNum=" + versionNum  +
        " parentLineId=" + (parentLine != null ? parentLine.getId() : "null")  +
        " metaFields=" + getMetaFieldsList() +
        " editable=" + editable + "]";
    }

}

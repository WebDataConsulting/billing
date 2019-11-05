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
package com.sapienter.jbilling.server.item.db;


import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import javax.persistence.*;

import java.io.Serializable;
import java.util.*;

@Entity
@TableGenerator(
        name = "item_type_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "item_type",
        allocationSize = 100
)
@Table(name = "item_type")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ItemTypeDTO extends AbstractDescription implements MetaContent, Serializable {

    private int id;
    private CompanyDTO entity;
    private Set<CompanyDTO> entities = new HashSet<CompanyDTO>(0);
    private String description;
    private int orderLineTypeId;
    private boolean internal;
    private boolean global = false;
    private Set<ItemDTO> items = new HashSet<ItemDTO>(0);
    private Set<ItemDTO> excludedItems = new HashSet<ItemDTO>();
    private Set<AssetStatusDTO> assetStatuses = new HashSet<AssetStatusDTO>(0);
    private Set<MetaField> assetMetaFields = new HashSet<MetaField>(0);
    private Set<MetaFieldValue> metaFields = new TreeSet<>();
    private Integer allowAssetManagement;
    private String assetIdentifierLabel;
    private int versionNum;
    private ItemTypeDTO parent;
    
    private boolean onePerOrder = false;
    private boolean onePerCustomer = false;

    public ItemTypeDTO() {
    }

    public ItemTypeDTO(int id) {
        this.id = id;
    }

    public ItemTypeDTO(int id, CompanyDTO entity, int orderLineTypeId) {
        this.id = id;
        this.entity = entity;
        this.orderLineTypeId = orderLineTypeId;
    }

    public ItemTypeDTO(int id, CompanyDTO entity, String description, int orderLineTypeId, Set<ItemDTO> items) {
        this.id = id;
        this.entity = entity;
        this.description = description;
        this.orderLineTypeId = orderLineTypeId;
        this.items = items;
    }

    @Transient
    protected String getTable() {
        return ServerConstants.TABLE_ITEM_TYPE;
    }

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "item_type_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "item_type_entity_map", 
    			joinColumns = {@JoinColumn(name = "item_type_id", updatable = true) }, 
    			inverseJoinColumns = {@JoinColumn(name = "entity_id", updatable = true) })
    public Set<CompanyDTO> getEntities() {
        return this.entities;
    }

    public void setEntities(Set<CompanyDTO> entities) {
        this.entities = entities;
    }

    public void addEntity (CompanyDTO entity){this.entities.add(entity);}

    public void removeEntity (CompanyDTO entity){this.entities.remove(entity);}

    @Column(name = "allow_asset_management")
    public Integer getAllowAssetManagement() {
        return allowAssetManagement;
    }

    public void setAllowAssetManagement(Integer allowAssetManagement) {
        this.allowAssetManagement = allowAssetManagement;
    }

    @Column(name = "asset_identifier_label")
    public String getAssetIdentifierLabel() {
        return assetIdentifierLabel;
    }

    public void setAssetIdentifierLabel(String assetIdentifierLabel) {
        this.assetIdentifierLabel = assetIdentifierLabel;
    }

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="item_type_meta_field_def_map",
            joinColumns={@JoinColumn(name="item_type_id", referencedColumnName="id")},
            inverseJoinColumns={@JoinColumn(name="meta_field_id", referencedColumnName="id", unique = true)})
    @OrderBy("displayOrder")
    public Set<MetaField> getAssetMetaFields() {
        return assetMetaFields;
    }

    public void setAssetMetaFields(Set<MetaField> assetMetaFields) {
        this.assetMetaFields = assetMetaFields;
    }

    public void addAssetMetaField (MetaField assetMetaField){this.assetMetaFields.add(assetMetaField);}

    public void removeAssetMetaField (MetaField assetMetaField){this.assetMetaFields.remove(assetMetaField);}

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "item_type_meta_field_map",
            joinColumns = @JoinColumn(name = "item_type_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public Set<MetaFieldValue> getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(Set<MetaFieldValue> metaFields) {
        this.metaFields = metaFields;
    }

    public void addMetaField (MetaFieldValue metaField){this.metaFields.add(metaField);}

    public void removeMetaField (MetaFieldValue metaField){this.metaFields.remove(metaField);}


    @Column(name = "description", length = 100)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "order_line_type_id", nullable = false)
    public int getOrderLineTypeId() {
        return this.orderLineTypeId;
    }

    public void setOrderLineTypeId(int orderLineTypeId) {
        this.orderLineTypeId = orderLineTypeId;
    }

    @Column(name = "internal", nullable = false)
    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    @Column(name = "global", nullable = false, updatable = true)
    public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "item_type_map",
               joinColumns = {@JoinColumn(name = "type_id", updatable = false)},
               inverseJoinColumns = {@JoinColumn(name = "item_id", updatable = false)}
    )
    public Set<ItemDTO> getItems() {
        return this.items;
    }

    public void setItems(Set<ItemDTO> items) {
        this.items = items;
    }

    public void addItem (ItemDTO item){this.items.add(item);}

    public void removeItem (ItemDTO item){this.items.remove(item);}

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "item_type_exclude_map",
               joinColumns = {@JoinColumn(name = "type_id", updatable = false)},
               inverseJoinColumns = {@JoinColumn(name = "item_id", updatable = false)}
    )
    public Set<ItemDTO> getExcludedItems() {
        return excludedItems;
    }

    public void setExcludedItems(Set<ItemDTO> excludedItems) {
        this.excludedItems = excludedItems;
    }

    public void addExcludedItem (ItemDTO excludedItem){this.excludedItems.add(excludedItem);}

    public void removeExcludedItem (ItemDTO excludedItem){this.excludedItems.remove(excludedItem);}


    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(int versionNum) {
        this.versionNum = versionNum;
    }

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY, mappedBy = "itemType")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    public Set<AssetStatusDTO> getAssetStatuses() {
        return this.assetStatuses;
    }

    public void setAssetStatuses(Set<AssetStatusDTO> assetStatuses) {
        this.assetStatuses = assetStatuses;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public ItemTypeDTO getParent(){
        return this.parent;
    }

    public void setParent(ItemTypeDTO parent){
        this.parent = parent;
    }
    
    @Column(name = "one_per_order", nullable = false)
    public boolean isOnePerOrder() {
		return onePerOrder;
	}

	public void setOnePerOrder(boolean onePerOrder) {
		this.onePerOrder = onePerOrder;
		
		if(onePerOrder) {
			this.onePerCustomer = false;
		}
	}

	@Column(name = "one_per_customer", nullable = false)
	public boolean isOnePerCustomer() {
		return onePerCustomer;
	}

	public void setOnePerCustomer(boolean onePerCustomer) {
		this.onePerCustomer = onePerCustomer;
		
		if(onePerCustomer) {
			this.onePerOrder = false;
		}
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemTypeDTO that = (ItemTypeDTO) o;

        if (id != that.id) return false;
        if (orderLineTypeId != that.orderLineTypeId) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        //if (entity != null ? !entity.equals(that.entity) : that.entity != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        //result = 31 * result + (entity != null ? entity.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + orderLineTypeId;
        return result;
    }

    @Override
    public String toString() {
        return "ItemTypeDTO{"
               + "id=" + id
               + ", orderLineTypeId=" + orderLineTypeId
               + ", description='" + description + '\''
               + ", parent=" + (null != parent ? parent.getId() : null)
               + '}';
    }

    /**
     * Load some dependent object so object can be used disconnected from session.
     * Note that items and excluded items are not loaded.
     */
    public void touch() {
        assetMetaFields.size();
        assetStatuses.size();
        entities.size();
    }

    public void addAssetStatuses(Collection<AssetStatusDTO> assetStatusDTOs) {
        for(AssetStatusDTO status: assetStatusDTOs) {
            addAssetStatus(status);
        }
    }

    public void addAssetStatus(AssetStatusDTO statusDTO) {
        statusDTO.setItemType(this);
        assetStatuses.add(statusDTO);
    }

    public void removeAssetStatus(AssetStatusDTO statusDTO) {
        assetStatuses.remove(statusDTO);
    }

    public AssetStatusDTO findDefaultAssetStatus() {
        for( AssetStatusDTO assetStatusDTO : assetStatuses) {
            if(assetStatusDTO.getDeleted() == 0 && assetStatusDTO.getIsDefault() == 1) return assetStatusDTO;
        }
        return null;
    }

    public AssetStatusDTO findOrderSavedStatus() {
        for( AssetStatusDTO assetStatusDTO : assetStatuses) {
            if(assetStatusDTO.getDeleted() == 0 && assetStatusDTO.getIsOrderSaved() == 1) return assetStatusDTO;
        }
        return null;
    }


    /**
     * Find the assetMetaField with the specified name
     *
     * @param name
     * @return
     */
    public MetaField findMetaField(String name) {
        for(MetaField metaField : assetMetaFields) {
            if(metaField.getName().equals(name)) {
                return metaField;
            }
        }
        return null;
    }

    public void removeMetaField(MetaField metaField) {
        assetMetaFields.remove(metaField);
    }

    @Transient
    public Integer getEntityId() {
        return getEntity() != null ? getEntity().getId() : null;
    }

    @Transient
    public MetaFieldValue getMetaField(String name) {
        return MetaFieldHelper.getMetaField(this, name);
    }

    @Transient
    public MetaFieldValue getMetaField(String name, Integer groupId) {
        return MetaFieldHelper.getMetaField(this, name, groupId);
    }

    @Transient
    public MetaFieldValue getMetaField(Integer metaFieldNameId) {
        return MetaFieldHelper.getMetaField(this, metaFieldNameId);
    }

    @Transient
    public void setMetaField(MetaFieldValue field, Integer groupId) {
        MetaFieldHelper.setMetaField(this, field, groupId);
    }

    @Transient
    public void setMetaField(Integer entitId, Integer groupId, String name, Object value) throws IllegalArgumentException {
        MetaFieldHelper.setMetaField(entitId, groupId, this, name, value);
    }

    @Transient
    public void updateMetaFieldsWithValidation(Integer entitId, Integer accountTypeId, MetaContent dto) {
        MetaFieldHelper.updateMetaFieldsWithValidation(entitId, accountTypeId, this, dto, this.isGlobal());
    }

    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.PRODUCT_CATEGORY };
    }
}



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
package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;

import java.io.Serializable;
import java.util.*;

/**
 * @author Brian Cowdery
 * @since 07-10-2009
 */
public class ItemTypeWS implements Serializable {

    private Integer id;
    
    @Size (min=1,max=100, message="validation.error.size,1,100")    
    private String description;
    
    @Min(value = 1, message="validation.error.min,1")
    private Integer orderLineTypeId;

    private Integer parentItemTypeId;
    
    private boolean global;
    private boolean internal;

    private Integer entityId;
    private Set<Integer> entities = new HashSet<Integer>(0);

    private Integer allowAssetManagement = new Integer(0);
    private String assetIdentifierLabel;
    @Valid
    private Set<AssetStatusDTOEx> assetStatuses = new HashSet<AssetStatusDTOEx>(0);

    @Valid
    private Set<MetaFieldWS> assetMetaFields = new HashSet<MetaFieldWS>(0);

    @Valid
    private MetaFieldValueWS[] metaFields;
    private SortedMap<Integer, MetaFieldValueWS[]> metaFieldsMap = new TreeMap<Integer, MetaFieldValueWS[]>();

    private boolean onePerCustomer = false;
    private boolean onePerOrder = false;
    
    public ItemTypeWS() {
    }

    public ItemTypeWS(Integer id, String description, Integer orderLineTypeId, Integer allowAssetManagement) {
        this.id = id;
        this.description = description;
        this.orderLineTypeId = orderLineTypeId;
        this.allowAssetManagement = allowAssetManagement;
    }

    public String getAssetIdentifierLabel() {
        return assetIdentifierLabel;
    }

    public void setAssetIdentifierLabel(String assetIdentifierLabel) {
        this.assetIdentifierLabel = assetIdentifierLabel;
    }

    public Integer getAllowAssetManagement() {
        return allowAssetManagement;
    }

    public void setAllowAssetManagement(Integer allowAssetManagement) {
        this.allowAssetManagement = allowAssetManagement;
    }

    public Set<AssetStatusDTOEx> getAssetStatuses() {
        return assetStatuses;
    }

    public void setAssetStatuses(Set<AssetStatusDTOEx> assetStatuses) {
        this.assetStatuses = assetStatuses;
    }

    public Set<MetaFieldWS> getAssetMetaFields() {
        return assetMetaFields;
    }

    public void setAssetMetaFields(Set<MetaFieldWS> assetMetaFields) {
        this.assetMetaFields = assetMetaFields;
    }

    public boolean isOnePerCustomer() {
		return onePerCustomer;
	}

	public void setOnePerCustomer(boolean onePerCustomer) {
		this.onePerCustomer = onePerCustomer;
	}

	public boolean isOnePerOrder() {
		return onePerOrder;
	}

	public void setOnePerOrder(boolean onePerOrder) {
		this.onePerOrder = onePerOrder;
	}

	public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOrderLineTypeId() {
        return orderLineTypeId;
    }

    public void setOrderLineTypeId(Integer orderLineTypeId) {
        this.orderLineTypeId = orderLineTypeId;
    }

    public Integer getParentItemTypeId() {
        return parentItemTypeId;
    }

    public void setParentItemTypeId(Integer parentItemTypeId) {
        this.parentItemTypeId = parentItemTypeId;
    }

    public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public Integer getEntityId() {
        return this.entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

	public Set<Integer> getEntities() {
		return entities;
	}

	public void setEntities(Set<Integer> entities) {
		this.entities = entities;
	}

    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    public SortedMap<Integer, MetaFieldValueWS[]> getMetaFieldsMap() {
        return metaFieldsMap;
    }

    public void setMetaFieldsMap(SortedMap<Integer, MetaFieldValueWS[]> metaFieldsMap) {
        this.metaFieldsMap = metaFieldsMap;
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;
        
        ItemTypeWS that = (ItemTypeWS) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (!id.equals(that.id)) return false;
        if (!orderLineTypeId.equals(that.orderLineTypeId)) return false;
        if (parentItemTypeId != null ? !parentItemTypeId.equals(that.parentItemTypeId) : that.parentItemTypeId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + orderLineTypeId.hashCode();
        result = 31 * result + (parentItemTypeId != null ? parentItemTypeId.hashCode() : 0);
        return result;
    }
}

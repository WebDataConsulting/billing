package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.security.WSSecured;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.*;

/**
 * Created by aristokrates on 2/17/14.
 */
public class OrderChangeTypeWS implements WSSecured, Serializable {

    private Integer id;

    private Integer entityId;

    @NotNull(message = "validation.error.notnull")
    @Size(min=1,max=255, message="validation.error.size,1,255")
    private String name;

    private boolean defaultType;

    private boolean allowOrderStatusChange;

    private List<Integer> itemTypes = new ArrayList<Integer>(0);

    @Valid
    private Set<MetaFieldWS> orderChangeTypeMetaFields = new HashSet<MetaFieldWS>(0);

    public OrderChangeTypeWS() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefaultType() {
        return defaultType;
    }

    public void setDefaultType(boolean defaultType) {
        this.defaultType = defaultType;
    }

    public boolean isAllowOrderStatusChange() {
        return allowOrderStatusChange;
    }

    public void setAllowOrderStatusChange(boolean allowOrderStatusChange) {
        this.allowOrderStatusChange = allowOrderStatusChange;
    }

    public List<Integer> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(List<Integer> itemTypes) {
        this.itemTypes = itemTypes;
    }

    public Set<MetaFieldWS> getOrderChangeTypeMetaFields() {
        return orderChangeTypeMetaFields;
    }

    public void setOrderChangeTypeMetaFields(Set<MetaFieldWS> orderChangeTypeMetaFields) {
        this.orderChangeTypeMetaFields = orderChangeTypeMetaFields;
    }

    @Override
    public Integer getOwningEntityId() {
        return entityId;
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }
}

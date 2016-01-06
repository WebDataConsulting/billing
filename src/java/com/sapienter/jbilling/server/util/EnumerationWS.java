package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.server.security.WSSecured;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vojislav on 14.1.15.
 */
public class EnumerationWS implements Serializable, WSSecured {

    private Integer id;

    @Min(value = 1, message = "enumeration.entityId.negative")
    private Integer entityId;

    @NotNull(message="validation.error.notnull")
    @Size(min = 1, max = 50, message = "validation.error.size,1,50")
    private String name;

    @Valid
    private List<EnumerationValueWS> values;

    public EnumerationWS(){
        this(null);
    }

    public EnumerationWS(String name) {
        this(null, null, name);
    }

    public EnumerationWS(Integer id, Integer entityId, String name) {
        this(id, entityId, name, new ArrayList<EnumerationValueWS>());
    }

    public EnumerationWS(Integer id, Integer entityId, String name, List<EnumerationValueWS> values) {
        setId(id);
        setEntityId(entityId);
        setName(name);
        setValues(values);
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

    public List<EnumerationValueWS> getValues() {
        return values;
    }

    public void setValues(List<EnumerationValueWS> values) {
        this.values = values;
    }

    public boolean addValue(EnumerationValueWS valueWS){
        if(null == values){
            values = new ArrayList<EnumerationValueWS>();
        }
        return values.add(valueWS);
    }

    public boolean addValue(String value){
        if(null == value){
            return false;
        }
        if (null == values){
            values = new ArrayList<EnumerationValueWS>();
        }
        return values.add(new EnumerationValueWS(value));
    }

    @Override
    public Integer getOwningEntityId() {
        return this.entityId;
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }

    @Override
    public String toString() {
        return "EnumerationWS{" +
                "id=" + id +
                ", entityId=" + entityId +
                ", name='" + name + '\'' +
                ", values=" + values +
                '}';
    }
}

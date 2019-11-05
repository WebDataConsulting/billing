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

package com.sapienter.jbilling.server.metafields.db;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import java.io.Serializable;
import java.util.Set;

/**
 * A meta-field name that is associated with a particular entity type. The field names define
 * the allowed values and data-types of the values that can be attached to an entity.
 *
 * @author Brian Cowdery
 * @since 03-Oct-2011
 */
@Entity
@Table(name = "meta_field_name")
@TableGenerator(
        name = "meta_field_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "meta_field_name",
        allocationSize = 10
)
public class MetaField  extends AbstractDescription  implements Serializable {

    private static final long serialVersionUID = 8802138744319569281L;
    private int id;
    private CompanyDTO entity;
    private String name;
    private EntityType entityType;
    private DataType dataType;

    private boolean disabled = false;
    private boolean mandatory = false;

    private Integer displayOrder = 1;
    private MetaFieldValue defaultValue = null;

    private ValidationRule validationRule;
    
    //indicate whether the metafield is a primary field and can be used for creation of metafield groups and for providing
    //    a meta-fields to be populated for the entity type they belong to
    //Metafields created from the Configuration - MetaField menu will be considered as primary metafields by default. 
    //All other dynamic metafields created on the fly in the system (example: Account Information Type, Product Category) will not be considered as primary
    private Boolean primary;

    private MetaFieldType fieldUsage;

    private Set<MetaFieldGroup> metaFieldGroups;

    private Integer versionNum;

    private String filename;
    // indicate that metafield is unique or not for a EntityType
    private boolean unique = false;
    
    public MetaField() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "meta_field_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 25)
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 25)
    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @Column(name = "is_disabled", nullable = true)
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Column(name = "is_mandatory", nullable = true)
    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Column(name = "display_order", nullable = true)
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "default_value_id", nullable = true)
    public MetaFieldValue getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(MetaFieldValue defaultValue) {
        if (defaultValue != null)
            defaultValue.setField(this);

        this.defaultValue = defaultValue;
    }

    public MetaFieldValue createValue() {

        return MetaFieldBL.createValueFromDataType(this, null,getDataType());
    }

    @Version
    @Column(name="OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }
    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    public String toString() {
        return "MetaField{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", entityType=" + entityType +
                ", dataType=" + dataType +
                '}';
    }

    @Transient
    protected String getTable() {
        return ServerConstants.TABLE_METAFIELD;
    }

    @Column(name = "is_primary")
    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Enumerated(EnumType.STRING)
    @Column (name="field_usage",nullable=true)
    public MetaFieldType getFieldUsage() {
        return fieldUsage;

    }

    public void setFieldUsage(MetaFieldType fieldUsage) {
        this.fieldUsage = fieldUsage;
    }

    @ManyToMany( fetch = FetchType.LAZY, mappedBy = "metaFields",
            targetEntity = MetaFieldGroup.class)
    public Set<MetaFieldGroup> getMetaFieldGroups() {
        return metaFieldGroups;
    }

    public void setMetaFieldGroups(Set<MetaFieldGroup> metaFieldGroups) {
        this.metaFieldGroups = metaFieldGroups;
    }
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "validation_rule_id", nullable = true)
    public ValidationRule getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(ValidationRule validationRule) {
        this.validationRule = validationRule;
    }

    @Column(name = "filename")
    public String getFilename() {
        return filename;
    }
    
    @Column(name = "is_unique")
    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    @Override
    public boolean equals(Object obj) {
    	return this.name.equals(((MetaField)obj).getName());
    }

}

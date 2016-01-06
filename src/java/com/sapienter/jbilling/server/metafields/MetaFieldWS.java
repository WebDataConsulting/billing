package com.sapienter.jbilling.server.metafields;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

public class MetaFieldWS implements Serializable {


    private static final long serialVersionUID = -1889507849327464287L;

    private int id;
    private Integer entityId;

    @NotNull(message="validation.error.notnull")
    @Size(min = 1, max = 100, message = "validation.error.size,1,100")
    private String name;
    private EntityType entityType;
    private DataType dataType;

    private boolean disabled = false;
    private boolean mandatory = false;

    private Integer displayOrder = 1;

    @Valid
    private MetaFieldValueWS defaultValue = null;

    @Valid
    private ValidationRuleWS validationRule;

    //indicate whether the metafield is a primary field and can be used for creation of metafield groups and for providing
    //    a meta-fields to be populated for the entity type they belong to
    //Metafields created from the Configuration - MetaField menu will be considered as primary metafields by default. 
    //All other dynamic metafields created on the fly in the system (example: Account Information Type, Product Category) will not be considered as primary
    private boolean primary;
    private MetaFieldType fieldUsage;
    
    @Size(min = 0, max = 100, message = "validation.error.size,0,100")
    private String filename;
    
	public MetaFieldWS() {
	}


    public int getId() {
        return id;
    }


    public void setId(int id) {
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


    public EntityType getEntityType() {
        return entityType;
    }


    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }


    public DataType getDataType() {
        return dataType;
    }


    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }


    public boolean isDisabled() {
        return disabled;
    }


    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }


    public boolean isMandatory() {
        return mandatory;
    }


    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }


    public Integer getDisplayOrder() {
        return displayOrder;
    }


    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }


    public MetaFieldValueWS getDefaultValue() {
        return defaultValue;
    }


    public void setDefaultValue(MetaFieldValueWS defaultValue) {
        this.defaultValue = defaultValue;
    }


    public boolean isPrimary() {
        return primary;
    }


    public void setPrimary(boolean primary) {
        this.primary = primary;
    }


    public ValidationRuleWS getValidationRule() {
        return validationRule;
    }


    public void setValidationRule(ValidationRuleWS validationRule) {
        this.validationRule = validationRule;
    }

    public MetaFieldType getFieldUsage() {
        return fieldUsage;
    }

    public void setFieldUsage(MetaFieldType fieldUsage) {
        this.fieldUsage = fieldUsage;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

}

package com.sapienter.jbilling.server.metafield.builder;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.test.ApiTestCase;

/**
 * @author Vladimir Carevski
 * @since 12-FEB-2015
 */
public class MetaFieldBuilder {

	private Integer id;
	private Integer entityId;
	private String name;
	private EntityType entityType;
	private DataType dataType;

	private Boolean disabled;
	private Boolean mandatory;

	private Integer displayOrder;
	private MetaFieldValueWS defaultValue;

	private ValidationRuleWS validationRule;

	private Boolean primary;
	private MetaFieldType fieldUsage;

	public MetaFieldBuilder() {
	}

	public MetaFieldBuilder id(Integer id) {
		this.id = id;
		return this;
	}

	public MetaFieldBuilder entityId(Integer entityId) {
		this.entityId = entityId;
		return this;
	}

	public MetaFieldBuilder name(String name) {
		this.name = name;
		return this;
	}

	public MetaFieldBuilder entityType(EntityType entityType) {
		this.entityType = entityType;
		return this;
	}

	public MetaFieldBuilder dataType(DataType dataType) {
		this.dataType = dataType;
		return this;
	}

	public MetaFieldBuilder disabled(boolean disabled) {
		this.disabled = Boolean.valueOf(disabled);
		return this;
	}

	public MetaFieldBuilder mandatory(boolean mandatory) {
		this.mandatory = Boolean.valueOf(mandatory);
		return this;
	}

	public MetaFieldBuilder displayOrder(int displayOrder) {
		this.displayOrder = Integer.valueOf(displayOrder);
		return this;
	}

	public MetaFieldBuilder defaultValue(MetaFieldValueWS defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	public MetaFieldBuilder validationRule(ValidationRuleWS validationRule) {
		this.validationRule = validationRule;
		return this;
	}

	public MetaFieldBuilder primary(boolean primary) {
		this.primary = Boolean.valueOf(primary);
		return this;
	}

	public MetaFieldBuilder fieldUsage(MetaFieldType fieldUsage) {
		this.fieldUsage = fieldUsage;
		return this;
	}

	public MetaFieldWS build() {
		MetaFieldWS metaField = new MetaFieldWS();

		metaField.setId(null != id ? id.intValue() : 0);
		metaField.setEntityId(null != entityId ? entityId : ApiTestCase.TEST_ENTITY_ID);

		metaField.setName(null != name ? name : null);
		metaField.setEntityType(null != entityType ? entityType : null);
		metaField.setDataType(null != dataType ? dataType : DataType.STRING);
		metaField.setDisabled(null != disabled ? disabled.booleanValue() : false);
		metaField.setMandatory(null != mandatory ? mandatory.booleanValue() : false);

		metaField.setDisplayOrder(null != displayOrder ? displayOrder : Integer.valueOf(1));
		metaField.setDefaultValue(null != defaultValue ? defaultValue : null);

		metaField.setValidationRule(null != validationRule ? validationRule : null);
		metaField.setPrimary(null != primary ? primary.booleanValue() : false);
		metaField.setFieldUsage(null != fieldUsage ? fieldUsage : null);
		return metaField;
	}

}

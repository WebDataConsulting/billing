package com.sapienter.jbilling.server.metafields.validation;

import java.util.List;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;

/**
 *  Defines models for validation rules;
 *  Performs validation on entities
 *
 *  @author Panche Isajeski
 */
public interface ValidationRuleModel<T> {

    public List<AttributeDefinition> getAttributeDefinitions();

    public ValidationReport doValidation(MetaContent source, T object, ValidationRule validationRule, Integer languageId);

}

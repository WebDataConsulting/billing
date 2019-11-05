package com.sapienter.jbilling.server.metafields.validation;

import java.math.BigDecimal;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;

/**
 *  Range validation model
 *  </p>
 *  Validates that a big decimal value is between a range of values provided as attributes
 *
 *  @author Panche Isajeski
 *
 */
public class RangeValidationRuleModel extends AbstractValidationRuleModel<Object> {

    public static String VALIDATION_MIN_RANGE_FIELD = "minRange";
    public static String VALIDATION_MAX_RANGE_FIELD = "maxRange";

    public RangeValidationRuleModel() {

        setAttributeDefinitions(
                new AttributeDefinition(VALIDATION_MIN_RANGE_FIELD, Type.DECIMAL, false),
                new AttributeDefinition(VALIDATION_MAX_RANGE_FIELD, Type.DECIMAL, false)
        );
    }

    @Override
    public ValidationReport doValidation(MetaContent source, Object object, ValidationRule validationRule, Integer languageId) {

        if (!verifyValidationParameters(object, validationRule, languageId)) {
            return null;
        }

        String errorMessage = validationRule.getErrorMessage(languageId);
        ValidationReport report = new ValidationReport();

        BigDecimal minValue = getValidationMinRangeField(validationRule);
        BigDecimal maxValue = getValidationMaxRangeField(validationRule);

        boolean minCondition = true;
        boolean maxCondition = true;

        if (minValue != null) {
            minCondition = minValue.compareTo(new BigDecimal(object.toString())) == -1;
        }

        if (maxValue != null) {
            maxCondition = maxValue.compareTo(new BigDecimal(object.toString())) == 1;
        }

        // if min condition or max condition didn't pass report an error
        if (!minCondition || !maxCondition) {
            report.addError("MetaFieldValue,value," + errorMessage);
        }

        return report;
    }

    public BigDecimal getValidationMinRangeField(ValidationRule validationRule) {
        return AttributeUtils.getDecimal(validationRule.getRuleAttributes(), VALIDATION_MIN_RANGE_FIELD);
    }

    public BigDecimal getValidationMaxRangeField(ValidationRule validationRule) {
        return AttributeUtils.getDecimal(validationRule.getRuleAttributes(), VALIDATION_MAX_RANGE_FIELD);
    }

    public boolean verifyValidationParameters(Object object, ValidationRule validationRule, Integer languageId) {

        boolean verifyAttributes = super.verifyValidationParameters(object, validationRule, languageId);
        if (!verifyAttributes) {
            return false;
        }

        BigDecimal objectDecimal = null;
        // try convert object to BigDecimal
        // if the conversion fails the rule is not applicable
        try {
            objectDecimal = new BigDecimal(object.toString());
        } catch (NumberFormatException e) {
            // swallow the exception
        }

        return objectDecimal != null;
    }
}

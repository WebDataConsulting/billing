package com.sapienter.jbilling.server.metafield.builder;

import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.test.ApiTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Vladimir Carevski
 * @since 13-FEB-2014
 */
public class ValidationRuleBuilder {

	private Integer id;
	private ValidationRuleType ruleType;
	private SortedMap<String, String> ruleAttributes = new TreeMap();
	private List<InternationalDescriptionWS> errorMessages = new ArrayList<InternationalDescriptionWS>();

	public ValidationRuleBuilder() {
	}

	public ValidationRuleBuilder id(Integer id) {
		this.id = id;
		return this;
	}

	public ValidationRuleBuilder ruleType(ValidationRuleType ruleType) {
		this.ruleType = ruleType;
		return this;
	}

	public ValidationRuleBuilder addRuleAttribute(String key, String value) {
		ruleAttributes.put(key, value);
		return this;
	}

	public ValidationRuleBuilder addErrorMessage(String errorMessage) {
		addErrorMessage(errorMessage, ApiTestCase.TEST_LANGUAGE_ID);
		return this;
	}

	public ValidationRuleBuilder addErrorMessage(String errorMessage, Integer languageId) {
		InternationalDescriptionWS description = new InternationalDescriptionWS(languageId, errorMessage);
		return addErrorMessage(description);
	}

	public ValidationRuleBuilder addErrorMessage(InternationalDescriptionWS description) {
		errorMessages.add(description);
		return this;
	}

	public ValidationRuleWS build() {
		ValidationRuleWS rule = new ValidationRuleWS();
		rule.setId(null != id ? id : 0);
		rule.setRuleType(null != ruleType ? ruleType.name() : null);
		rule.setRuleAttributes(null != ruleAttributes ? ruleAttributes : null);
		rule.setErrorMessages(null != errorMessages ? errorMessages : null);
		return rule;
	}

}

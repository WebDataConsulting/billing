package com.sapienter.jbilling.server.metafields.validation;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import com.sapienter.jbilling.server.util.db.LanguageDTO;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * WS class from ValidationRule
 *
 *  @author Panche Isajeski
 */
public class ValidationRuleWS implements Serializable {
	
	public static final String ERROR_MSG_LABEL= "errorMessage";
    private int id;
    @NotNull(message = "validation.error.null.rule.type")
    private String ruleType;
    private SortedMap<String, String> ruleAttributes = new TreeMap<String, String>();

    @NotEmpty(message = "validation.error.empty.error.message")
    private List<InternationalDescriptionWS> errorMessages = ListUtils.lazyList(new ArrayList<InternationalDescriptionWS>(),
            FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));
    private boolean enabled = true;

    public ValidationRuleWS() {
    }

   
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public SortedMap<String, String> getRuleAttributes() {
        return ruleAttributes;
    }

    public void setRuleAttributes(SortedMap<String, String> ruleAttributes) {
        this.ruleAttributes = ruleAttributes;
    }

    public void addRuleAttribute(String name, String value) {
        this.ruleAttributes.put(name, value);
    }

    public List<InternationalDescriptionWS> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<InternationalDescriptionWS> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage(int langId, String errorMessage) {
        InternationalDescriptionWS errorMessageWS=new InternationalDescriptionWS(ERROR_MSG_LABEL, langId, errorMessage);
        this.errorMessages.add(errorMessageWS);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "ValidationRuleWS{" +
                "id=" + id +
                ", ruleType=" + ruleType +
                ", ruleAttributes=" + ruleAttributes +
                ", errorMessages=" + errorMessages +
                ", enabled=" + enabled +
                '}';
    }
}

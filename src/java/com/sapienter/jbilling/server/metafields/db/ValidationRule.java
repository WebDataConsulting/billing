package com.sapienter.jbilling.server.metafields.db;

import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *  Validation rule entity
 *  </p>
 *  Consist of error message and validation rule type
 *  along with rules attributes used in validation
 *
 *  @author Panche Isajeski
 */
@Entity
@Table(name = "validation_rule")
@TableGenerator(
        name = "validation_rule_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "validation_rule",
        allocationSize = 10
)
public class ValidationRule extends AbstractDescription implements Serializable {

    public static final String ERROR_MSG_LABEL= "errorMessage";

    private int id;

    private ValidationRuleType ruleType;
    private SortedMap<String, String> ruleAttributes = new TreeMap<String, String>();
    private boolean enabled = true;

    // transient, used for errors international description
    private Map<Integer, String> errors = new HashMap<Integer, String>();

    private Integer versionNum;

    public ValidationRule() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "validation_rule_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 25)
    public ValidationRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(ValidationRuleType ruleType) {
        this.ruleType = ruleType;
    }

    @CollectionOfElements(fetch = FetchType.LAZY)
    @JoinTable(name = "validation_rule_attributes", joinColumns = @JoinColumn(name = "validation_rule_id"))
    @org.hibernate.annotations.MapKey(columns = @Column(name = "attribute_name", nullable = true, length = 255))
    @Column(name = "attribute_value", nullable = true, length = 255)
    @Sort(type = SortType.NATURAL)
    @Fetch(FetchMode.SELECT)
    public SortedMap<String, String> getRuleAttributes() {
        return ruleAttributes;
    }

    public void setRuleAttributes(SortedMap<String, String> ruleAttributes) {
        this.ruleAttributes = ruleAttributes;
    }

    @Column(name = "enabled", nullable = false)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Version
    @Column(name="OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }
    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Transient
    public String getErrorMessage(int langId) {
        return getDescription(langId, ERROR_MSG_LABEL);
    }

    @Transient
    public void setErrorMessage(Integer languageId, String error) {
        setDescription(ERROR_MSG_LABEL, languageId, error);
    }

    // temp methods
    @Transient
    public Map<Integer, String> getErrors() {
        return errors;
    }

    @Transient
    public void setErrors(Map<Integer, String> errors) {
        this.errors = errors;
    }

    @Transient
    public void addError(Integer languageId, String error) {
        getErrors().put(languageId, error);
    }

    @Transient
    protected String getTable() {
        return Constants.TABLE_VALIDATION_RULE;
    }
}

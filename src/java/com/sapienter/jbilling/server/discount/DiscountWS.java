package com.sapienter.jbilling.server.discount;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class DiscountWS implements WSSecured, Serializable {

    public static final String ATTRIBUTE_WILDCARD = "*";

    private int id;
    private Integer entityId;
    @NotEmpty(message = "validation.error.notnull")
    @Size(min = 0, max = 20, message = "validation.error.size,1,20")
    private String code;
    private String type;
    @NotNull(message = "validation.error.notnull")
    @Digits(integer = 12, fraction = 4, message = "validation.error.invalid.number.or.fraction")
    private String rate;
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
    private Date startDate;
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
    private Date endDate;
    private SortedMap<String, String> attributes = new TreeMap<String, String>();
    private String description = null;
    private MetaFieldValueWS[] metaFields;
    
    @NotNull(message = "validation.error.notnull")
    @NotEmpty(message = "validation.error.notempty")
    private List<InternationalDescriptionWS> descriptions = ListUtils.lazyList(new ArrayList<InternationalDescriptionWS>(), FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));

    public DiscountWS() {

    }

    @Override
    public Integer getOwningEntityId() {
        return getEntityId();
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate != null ? rate.toString() : null;
    }

    public BigDecimal getRateAsDecimal() {
        return Util.string2decimal(rate);
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public SortedMap<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(SortedMap<String, String> attributes) {
        this.attributes = attributes;
    }

    public MetaFieldValueWS[] getMetaFields() {
		return metaFields;
	}

	public void setMetaFields(MetaFieldValueWS[] metaFields) {
		this.metaFields = metaFields;
	}
    
    /**
     * Returns an english description.
     *
     * @return String
     */
    public String getDescription() {
        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() == ServerConstants.LANGUAGE_ENGLISH_ID.intValue()) {
                return description.getContent();
            }
        }
        return "";
    }

    /**
     * Sets the a description in english.
     *
     * @param newDescription The description to set
     */
    public void setDescription(String newDescription) {
        description = newDescription;

        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() == ServerConstants.LANGUAGE_ENGLISH_ID.intValue()) {
                description.setContent(newDescription);
                return;
            }
        }
        InternationalDescriptionWS newDescriptionWS = new InternationalDescriptionWS();
        newDescriptionWS.setContent(newDescription);
        newDescriptionWS.setPsudoColumn("description");
        newDescriptionWS.setLanguageId(ServerConstants.LANGUAGE_ENGLISH_ID);
        descriptions.add(newDescriptionWS);
    }

    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    public boolean isPeriodBased() {
        return this.getType().equals(DiscountStrategyType.RECURRING_PERIODBASED.name());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String
                .format("DiscountWS [id=%s, entityId=%s, code=%s, type=%s, rate=%s, startDate=%s, endDate=%s, attributes=%s, description=%s, descriptions=%s]",
                        id, entityId, code, type, rate, startDate, endDate,
                        attributes, description, descriptions);
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }


}

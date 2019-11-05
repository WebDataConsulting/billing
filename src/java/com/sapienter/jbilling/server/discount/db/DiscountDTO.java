package com.sapienter.jbilling.server.discount.db;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.hibernate.annotations.MapKey;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.*;

@Entity
@TableGenerator(
        name = "discount_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "discount",
        allocationSize = 100
)
@Table(name = "discount")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class DiscountDTO extends AbstractDescription implements Exportable, MetaContent {

    private int id;
    private CompanyDTO entity;
    private String code;
    private DiscountStrategyType type;
    private BigDecimal rate;
    private Date startDate;
    private Date endDate;
    private SortedMap<String, String> attributes = new TreeMap<String, String>();
    private Set<MetaFieldValue> metaFields = new TreeSet<>();

    private Date lastUpdateDateTime;

    public DiscountDTO() {
        super();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "discount_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(name = "discount_meta_field_map",
            joinColumns = @JoinColumn(name = "discount_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id"))
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public Set<MetaFieldValue> getMetaFields() {
        return metaFields;
    }

    public void addMetaField(MetaFieldValue metaField) {
        this.metaFields.add(metaField);
    }

    public void removeMetaField(MetaFieldValue metaField) {
        this.metaFields.remove(metaField);
    }

    @Transient
    public void setMetaFields(Set<MetaFieldValue> fields) {
        this.metaFields = fields;
    }

    @Column(name = "code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 25)
    public DiscountStrategyType getType() {
        return type;
    }

    public void setType(DiscountStrategyType type) {
        this.type = type;
    }

    @Column(name = "rate", precision = 17, scale = 17)
    public BigDecimal getRate() {
        return this.rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    @Column(name = "start_date")
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column(name = "end_date")
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @CollectionOfElements(fetch = FetchType.EAGER)
    @JoinTable(name = "discount_attribute", joinColumns = @JoinColumn(name = "discount_id"))
    @MapKey(columns = @Column(name = "attribute_name", nullable = true, length = 255))
    @Column(name = "attribute_value", nullable = true, length = 255)
    @Sort(type = SortType.NATURAL)
    @Cascade(value = {org.hibernate.annotations.CascadeType.DELETE_ORPHAN, org.hibernate.annotations.CascadeType.SAVE_UPDATE})
    @Fetch(FetchMode.SELECT)
    public SortedMap<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(SortedMap<String, String> attributes) {
        this.attributes = attributes;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }

    public void setEntity(CompanyDTO entity) {
        this.entity = entity;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update_datetime", nullable = true)
    public Date getLastUpdateDateTime() {
        return lastUpdateDateTime;
    }

    public void setLastUpdateDateTime(Date lastUpdateDateTime) {
        this.lastUpdateDateTime = lastUpdateDateTime;
    }

    @Transient
    public Integer getEntityId() {
        return getEntity() != null ? getEntity().getId() : null;
    }

    @Transient
    protected String getDiscountCodeAndDescription() {
        return getCode() + " - " + getDescription();
    }

    @Transient
    protected String getTable() {
        return ServerConstants.TABLE_DISCOUNT;
    }

    @Transient
    public boolean isPeriodBased() {
        return this.getType().equals(DiscountStrategyType.RECURRING_PERIODBASED);
    }

    @Transient
    public boolean isAmountBased() {
        return this.getType().equals(DiscountStrategyType.ONE_TIME_AMOUNT);
    }

    @Transient
    public boolean isPercentageBased() {
        return this.getType().equals(DiscountStrategyType.ONE_TIME_PERCENTAGE);
    }

    @Transient
    public Integer getPeriodUnit() {
        if (isPeriodBased()) {
            return AttributeUtils.getInteger(getAttributes(), "periodUnit");
        }

        return null;
    }

    @Transient
    public Integer getPeriodValue() {
        if (isPeriodBased()) {
            return AttributeUtils.getInteger(getAttributes(), "periodValue");
        }

        return null;
    }

    @Transient
    public String getPeriodValueAsString() {
        if (isPeriodBased()) {
            String periodValue = getAttributes().get("periodValue");
            return periodValue != null && !periodValue.isEmpty() ? periodValue : "";
        }

        return "";
    }

    @Transient
    public boolean hasPeriodValue() {
        return !getPeriodValueAsString().isEmpty();
    }

    @Transient
    public Boolean isPercentageRate() {
        if (isPeriodBased()) {
            Integer isPercentage = AttributeUtils.getInteger(getAttributes(), "isPercentage", true);
            if (isPercentage != null) {
                return isPercentage == 1 ? Boolean.TRUE : Boolean.FALSE;
            }
        }

        return Boolean.FALSE;
    }

    /**
     * MetaContent Methods
     */
    @Transient
    public MetaFieldValue getMetaField(String name) {
        return MetaFieldHelper.getMetaField(this, name);
    }

    @Transient
    public MetaFieldValue getMetaField(String name, Integer groupId) {
        return MetaFieldHelper.getMetaField(this, name, groupId);
    }

    @Transient
    public MetaFieldValue getMetaField(Integer metaFieldNameId) {
        return MetaFieldHelper.getMetaField(this, metaFieldNameId);
    }

    @Transient
    public void setMetaField(MetaFieldValue field, Integer groupId) {
        MetaFieldHelper.setMetaField(this, field, groupId);
    }

    @Transient
    public void setMetaField(Integer entitId, Integer groupId, String name, Object value) throws IllegalArgumentException {
        MetaFieldHelper.setMetaField(entitId, groupId, this, name, value);
    }

    @Transient
    public void updateMetaFieldsWithValidation(Integer entitId, Integer accountTypeId, MetaContent dto) {
        MetaFieldHelper.updateMetaFieldsWithValidation(entitId, accountTypeId, this, dto);
    }

    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[]{EntityType.DISCOUNT};
    }

    /**
     * New Discounts : Based on the type of discount (percentage or amount), either returns the
     * calculated discount amount as per given percentage or returns direct rate as flat discount.
     * For example, item price: $50.00, amount type discount rate=$5, discount amount=$5
     * But if percentage type discount, then discount rate=$5 means 5% discount on $50, so discount amount=$2.5
     *
     * @param applicableAmount
     * @return Discount Amount
     */
    @Transient
    public BigDecimal getDiscountAmount(BigDecimal applicableAmount) {

        BigDecimal discountAmount = BigDecimal.ZERO;

        switch (this.type) {

            case ONE_TIME_PERCENTAGE:
                discountAmount = applicableAmount.multiply(rate).divide(new BigDecimal(100));
                break;
            case RECURRING_PERIODBASED:
                if (Boolean.TRUE.equals(isPercentageRate())) {
                    discountAmount = applicableAmount.multiply(rate).divide(new BigDecimal(100));
                } else {
                    discountAmount = rate;
                }
                break;
            case ONE_TIME_AMOUNT:
                discountAmount = rate;
                break;
            default:
                // no default case
                break;

        }

        return (null == discountAmount) ? BigDecimal.ZERO : discountAmount.setScale(CommonConstants.BIGDECIMAL_SCALE_STR, CommonConstants.BIGDECIMAL_ROUND);
    }

    @Transient
    public String[] getFieldNames() {
        String headers[] = new String[]{
                "id",
                "code",
                "description",
                "rate",
                "startDate",
                "endDate",
                "type"
        };

        List<String> list = new ArrayList<String>(Arrays.asList(headers));
        for (String field : metaFieldsNames) {
            list.add(field);
        }

        return list.toArray(new String[list.size()]);
    }

    @Transient
    public Object[][] getFieldValues() {

        List<Object[]> values = new ArrayList<Object[]>();

        // main invoice row
        values.add(
                new Object[]{
                        id,
                        code,
                        getDescription(),
                        rate,
                        startDate,
                        endDate,
                        type
                }
        );

        Object obj[] = new Object[metaFieldsNames.size()];
        int index = 0;
        for (String name : metaFieldsNames) {
            MetaFieldValue value = getMetaField(name);
            if (value != null) {
                obj[index++] = value.getValue();
            } else {
                obj[index++] = null;
            }
        }

        values.add(obj);
        return values.toArray(new Object[values.size()][]);
    }

    @Override
    public String toString() {
        return "DiscountDTO{" +
                "id=" + id +
                ", code=" + code +
                ", description='" + getDescription() +
                '}';
    }
}

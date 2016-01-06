package com.sapienter.jbilling.server.discount.db;

import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.MapKey;

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
public class DiscountDTO extends AbstractDescription implements Exportable {

    private int id;
    private CompanyDTO entity;
    private String code;
    private DiscountStrategyType type;
    private BigDecimal rate;
    private Date startDate;
    private Date endDate;
    private SortedMap<String, String> attributes = new TreeMap<String, String>();

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
    @Cascade(value = {CascadeType.DELETE_ORPHAN, CascadeType.SAVE_UPDATE})
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
        return Constants.TABLE_DISCOUNT;
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

        return (null == discountAmount) ? BigDecimal.ZERO : discountAmount.setScale(Constants.BIGDECIMAL_SCALE_STR, Constants.BIGDECIMAL_ROUND);
    }

    @Transient
    public String[] getFieldNames() {
        return new String[]{
                "id",
                "code",
                "description",
                "rate",
                "startDate",
                "endDate",
                "type"
        };
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

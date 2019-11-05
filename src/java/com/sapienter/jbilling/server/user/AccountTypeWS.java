package com.sapienter.jbilling.server.user;


import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class AccountTypeWS implements Serializable {

    private Integer id;
    private Integer entityId;
    private String invoiceDesign;
    private Date dateCreated;

    @Digits(integer = 12, fraction = 10, message = "validation.error.not.a.number.12.decimal")
    @Min(value = 0, message = "validation.error.not.a.positive.number")
    private String creditNotificationLimit1;
    @Digits(integer = 12, fraction = 10, message = "validation.error.not.a.number.12.decimal")
    @Min(value = 0, message = "validation.error.not.a.positive.number")
    private String creditNotificationLimit2;
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number.12.decimal")
    @Min(value = 0, message = "validation.error.not.a.positive.number")
    private String creditLimit = null;

    @NotNull
    private Integer invoiceDeliveryMethodId;
    @NotNull
    private Integer currencyId;
    @NotNull
    private Integer languageId;

    @NotNull(message = "validation.error.is.required")
    @Size(min = 1, message = "validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>(1);

    @NotNull(message = "validation.error.is.required")
    @Valid
    private MainSubscriptionWS mainSubscription;

    // optional list of account information type ids
    private Integer[] informationTypeIds = null;

    private Integer[] paymentMethodTypeIds = null;
    
    private Integer preferredNotificationAitId;
    
	public AccountTypeWS() {

    }

    public AccountTypeWS(Integer id, String creditLimit) {
        this.id = id;
        this.creditLimit = creditLimit;
    }

    public void setName(String name,Integer languageId) {
        InternationalDescriptionWS description = new InternationalDescriptionWS(languageId, name);
        addDescription(description);
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public void setLanguageId(Integer languageId) {
        this.languageId = languageId;
    }
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getInvoiceDesign() {
        return invoiceDesign;
    }

    public void setInvoiceDesign(String invoiceDesign) {
        this.invoiceDesign = invoiceDesign;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getCreditNotificationLimit1() {
        return creditNotificationLimit1;
    }

    public BigDecimal getCreditNotificationLimit1AsDecimal() {
        return Util.string2decimal(creditNotificationLimit1);
    }

    public void setCreditNotificationLimit1AsDecimal(BigDecimal creditNotificationLimit1) {
        setCreditNotificationLimit1(creditNotificationLimit1);
    }

    public void setCreditNotificationLimit1(String creditNotificationLimit1) {
        this.creditNotificationLimit1 = creditNotificationLimit1;
    }

    public void setCreditNotificationLimit1(BigDecimal creditNotificationLimit1) {
        this.creditNotificationLimit1 = (creditNotificationLimit1 != null ? creditNotificationLimit1.toString() : null);
    }
    public String getCreditNotificationLimit2() {
        return creditNotificationLimit2;
    }

    public BigDecimal getCreditNotificationLimit2AsDecimal() {
        return Util.string2decimal(creditNotificationLimit2);
    }

    public void setCreditNotificationLimit2AsDecimal(BigDecimal creditNotificationLimit2) {
        setCreditNotificationLimit2(creditNotificationLimit2);
    }

    public void setCreditNotificationLimit2(String creditNotificationLimit2) {
        this.creditNotificationLimit2 = creditNotificationLimit2;
    }

    public void setCreditNotificationLimit2(BigDecimal creditNotificationLimit2) {
        this.creditNotificationLimit2 = (creditNotificationLimit2 != null ? creditNotificationLimit2.toString() : null);
    }
    public Integer getInvoiceDeliveryMethodId() {
        return invoiceDeliveryMethodId;
    }

    public void setInvoiceDeliveryMethodId(Integer invoiceDeliveryMethodId) {
        this.invoiceDeliveryMethodId = invoiceDeliveryMethodId;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public MainSubscriptionWS getMainSubscription() {
        return mainSubscription;
    }

    public void setMainSubscription(MainSubscriptionWS mainSubscription) {
        this.mainSubscription = mainSubscription;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getCreditLimitAsDecimal() {
        return Util.string2decimal(creditLimit);
    }

    public void setCreditLimitAsDecimal(BigDecimal creditLimit) {
        setCreditLimit(creditLimit);
    }

    public void setCreditLimit(String creditLimit) {
        this.creditLimit = creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = (creditLimit != null ? creditLimit.toString() : null);
    }

    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    public void addDescription(InternationalDescriptionWS description) {
        this.descriptions.add(description);
    }

    public InternationalDescriptionWS getDescription(Integer languageId) {
        for (InternationalDescriptionWS description : descriptions)
            if (description.getLanguageId().equals(languageId))
                return description;
        return null;
    }

    public Integer[] getInformationTypeIds() {
        return informationTypeIds;
    }

    public void setInformationTypeIds(Integer[] informationTypeIds) {
        this.informationTypeIds = informationTypeIds;
    }

    public Integer[] getPaymentMethodTypeIds() {
    	return paymentMethodTypeIds;
    }
    
    public void setPaymentMethodTypeIds(Integer[] paymentMethodTypeIds) {
    	this.paymentMethodTypeIds = paymentMethodTypeIds;
    }
    
	public Integer getpreferredNotificationAitId() {
		return preferredNotificationAitId;
	}

	public void setpreferredNotificationAitId(Integer preferredNotificationAitId) {
		this.preferredNotificationAitId = preferredNotificationAitId;
	}
    
    @Override
    public String toString() {
        return "AccountTypeWS{" +
                "id=" + id +
                ", entityId=" + entityId +
                ", invoiceDesign='" + invoiceDesign + '\'' +
                ", dateCreated=" + dateCreated +
                ", preferredNotificationAitId=" + preferredNotificationAitId +
                ", creditNotificationLimit1='" + creditNotificationLimit1 + '\'' +
                ", creditNotificationLimit2='" + creditNotificationLimit2 + '\'' +
                ", creditLimit='" + creditLimit + '\'' +
                ", invoiceDeliveryMethodId=" + invoiceDeliveryMethodId +
                ", currencyId=" + currencyId +
                ", languageId=" + languageId +
                ", descriptions=" + descriptions +
                ", mainSubscription=" + mainSubscription +
                '}';
    }
}

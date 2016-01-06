package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.LanguageDTO;

import javax.persistence.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@TableGenerator(
        name = "account_type_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "account_type",
        allocationSize = 100
)
// No cache, mutable and critical
@Table(name = "account_type")
public class AccountTypeDTO extends AbstractDescription implements java.io.Serializable {

    private int id;
    private CompanyDTO company;
    private BigDecimal creditLimit;
    private String invoiceDesign;
    private MainSubscriptionDTO billingCycle;
    private Date dateCreated;
    private BigDecimal creditNotificationLimit1;
    private BigDecimal creditNotificationLimit2;
    private InvoiceDeliveryMethodDTO invoiceDeliveryMethod;
    private LanguageDTO language;
    private CurrencyDTO currency;
    private Integer versionNum;
    private Set<AccountInformationTypeDTO> informationTypes = new HashSet<AccountInformationTypeDTO>();
    private Set<CustomerDTO> customers = new HashSet<CustomerDTO>(0);
    private Set<PaymentMethodTypeDTO> paymentMethodTypes = new HashSet<PaymentMethodTypeDTO>(0);
    
    private Integer preferredNotificationAitId;
    

	public AccountTypeDTO() {
    }

    public AccountTypeDTO(int id) {
        this.id = id;
    }

    public AccountTypeDTO(int id, BigDecimal creditLimit, String invoiceDesign, MainSubscriptionDTO billingCycle,
                          Date dateCreated, BigDecimal creditNotificationLimit1, BigDecimal creditNotificationLimit2,
                          InvoiceDeliveryMethodDTO invoiceDeliveryMethod, LanguageDTO language, CurrencyDTO currency, Integer preferredNotificationAitId) {
        this.id = id;
        this.creditLimit = creditLimit;
        this.invoiceDesign = invoiceDesign;
        this.billingCycle = billingCycle;
        this.dateCreated = dateCreated;
        this.creditNotificationLimit1 = creditNotificationLimit1;
        this.creditNotificationLimit2 = creditNotificationLimit2;
        this.invoiceDeliveryMethod = invoiceDeliveryMethod;
        this.language = language;
        this.currency = currency;
        this.preferredNotificationAitId = preferredNotificationAitId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "account_type_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getCompany() {
        return this.company;
    }

    public void setCompany(CompanyDTO entity) {
        this.company = entity;
    }

    @Embedded
    public MainSubscriptionDTO getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(MainSubscriptionDTO billingCycle) {
        this.billingCycle = billingCycle;
    }

    @Transient
    public Integer getCurrencyId() {
        return null != getCurrency() ? getCurrency().getId() : null;
    }

    @Transient
    public Integer getLanguageId() {
        return null != getLanguage() ? getLanguage().getId() : null;
    }

    @Column(name = "invoice_design")
    public String getInvoiceDesign() {
        return invoiceDesign;
    }

    public void setInvoiceDesign(String invoiceDesign) {
        this.invoiceDesign = invoiceDesign;
    }

    @Column(name = "date_created", nullable = true, length = 29)
    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Column(name = "credit_notification_limit1")
    public BigDecimal getCreditNotificationLimit1() {
        return creditNotificationLimit1;
    }

    public void setCreditNotificationLimit1(BigDecimal creditNotificationLimit1) {
        this.creditNotificationLimit1 = creditNotificationLimit1;
    }

    @Column(name = "credit_notification_limit2")
    public BigDecimal getCreditNotificationLimit2() {
        return creditNotificationLimit2;
    }

    public void setCreditNotificationLimit2(BigDecimal creditNotificationLimit2) {
        this.creditNotificationLimit2 = creditNotificationLimit2;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_delivery_method_id", nullable = true)
    public InvoiceDeliveryMethodDTO getInvoiceDeliveryMethod() {
        return invoiceDeliveryMethod;
    }

    public void setInvoiceDeliveryMethod(InvoiceDeliveryMethodDTO invoiceDeliveryMethod) {
        this.invoiceDeliveryMethod = invoiceDeliveryMethod;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = true)
    public LanguageDTO getLanguage() {
        return language;
    }

    public void setLanguage(LanguageDTO language) {
        this.language = language;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = true)
    public CurrencyDTO getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyDTO currency) {
        this.currency = currency;
    }

    @Column(name = "credit_limit")
    public BigDecimal getCreditLimit() {
        if (creditLimit == null) {
            return BigDecimal.ZERO;
        }
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }
    
    @OneToMany(mappedBy = "accountType", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	public Set<AccountInformationTypeDTO> getInformationTypes() {
		return informationTypes;
	}

	public void setInformationTypes(Set<AccountInformationTypeDTO> informationTypes) {
		this.informationTypes = informationTypes;
	}

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "accountType")
    public Set<CustomerDTO> getCustomers() {
        return this.customers;
    }

    public void setCustomers(Set<CustomerDTO> customers) {
        this.customers = customers;
    }
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="payment_method_account_type_map",
            joinColumns = @JoinColumn( name="account_type_id"),
            inverseJoinColumns = @JoinColumn( name="payment_method_id")
    )
    public Set<PaymentMethodTypeDTO> getPaymentMethodTypes() {
    	return paymentMethodTypes;
    }
    
    public void setPaymentMethodTypes(Set<PaymentMethodTypeDTO> paymentMethodTypes) {
    	this.paymentMethodTypes = paymentMethodTypes;
    }

    @Version
    @Column(name = "OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    
	@Column(name = "notification_ait_id")
	public Integer getPreferredNotificationAitId() {
		return preferredNotificationAitId;
	}

	public void setPreferredNotificationAitId(Integer preferredNotificationAitId) {
		this.preferredNotificationAitId = preferredNotificationAitId;
	}
	
	
    @Transient
    protected String getTable() {
        return Constants.TABLE_ACCOUNT_TYPE;
    }

    @Override
    public String toString() {
        return "AccountTypeDTO{" +
                "id=" + id +
                ", creditLimit=" + creditLimit +
                ", invoiceDesign='" + invoiceDesign + '\'' +
                ", billingCycle=" + billingCycle +
                ", preferredNotificationAitId=" + preferredNotificationAitId +
                ", dateCreated=" + dateCreated +
                ", creditNotificationLimit1=" + creditNotificationLimit1 +
                ", creditNotificationLimit2=" + creditNotificationLimit2 +
                ", invoiceDeliveryMethod=" + invoiceDeliveryMethod +
                ", language=" + language +
                ", currency=" + currency +
                '}';
    }

}

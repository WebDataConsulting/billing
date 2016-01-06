package com.sapienter.jbilling.server.payment.db;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author khobab
 *
 */
@Entity
@TableGenerator(
        name="payment_method_type_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="payment_method_type",
        allocationSize = 10
        )
@Table(name = "payment_method_type")
public class PaymentMethodTypeDTO implements Serializable {
	
	private static final long serialVersionUID = 9217076040737980698L;
	
	private int id;
	private String methodName;
	private boolean isRecurring;
    private Boolean allAccountType;
    private CompanyDTO entity;
	private PaymentMethodTemplateDTO paymentMethodTemplate;
	private Set<MetaField> metaFields;
	private Set<AccountTypeDTO> accountTypes = new HashSet<AccountTypeDTO>(0);
	
	private int version;
	
	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "payment_method_type_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }
	
	public void setId(int id) {
		this.id = id;
	}

    @Column(name = "method_name", length = 50)
    public String getMethodName() {
        return this.methodName;
    }
    
    public void setMethodName(String methodName) {
    	this.methodName = methodName;
    }

    @Column(name = "is_recurring")
    public boolean getIsRecurring() {
        return this.isRecurring;
    }
    
    public void setIsRecurring(boolean isRecurring) {
    	this.isRecurring = isRecurring;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    public CompanyDTO getEntity() {
        return this.entity;
    }
    
    public void setEntity(CompanyDTO entity) {
    	this.entity = entity;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    public PaymentMethodTemplateDTO getPaymentMethodTemplate() {
        return this.paymentMethodTemplate;
    }
    
    public void setPaymentMethodTemplate(PaymentMethodTemplateDTO paymentMethodTemplate) {
    	this.paymentMethodTemplate = paymentMethodTemplate;
    }
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "payment_method_meta_fields_map",
            joinColumns = { @JoinColumn(name = "payment_method_id", updatable = false) },
            inverseJoinColumns = { @JoinColumn(name = "meta_field_id", updatable = false)}
    )
    @OrderBy("displayOrder")
    public Set<MetaField> getMetaFields() {
        return metaFields;
    }
    
    public void setMetaFields(Set<MetaField> metaFields) {
    	this.metaFields = metaFields;
    }

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "payment_method_account_type_map", 
			joinColumns = @JoinColumn(name = "payment_method_id"), 
			inverseJoinColumns = @JoinColumn(name = "account_type_id"))
	public Set<AccountTypeDTO> getAccountTypes() {
		return accountTypes;
	}

	public void setAccountTypes(Set<AccountTypeDTO> accountTypes) {
		this.accountTypes = accountTypes;
	}
    
    
    @Version
    @Column(name = "OPTLOCK")
    public int getVersion() {
    	return version;
    }
    
    public void setVersion(int version){
    	this.version = version;
    }
    @Column(name = "all_account_type")
    public Boolean isAllAccountType() {
        return allAccountType;
    }

    public void setAllAccountType(Boolean allAccountType) {
        this.allAccountType = allAccountType;
    }
}
	
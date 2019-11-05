package com.sapienter.jbilling.server.user.db;

import javax.persistence.*;

import org.hibernate.annotations.Cascade;

import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;

import java.util.Date;

@Entity
@Table(name = "customer_account_info_type_timeline")
@TableGenerator(
        name="customer_account_info_type_timeline_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="customer_account_info_type_timeline",
        allocationSize = 100
)
public class CustomerAccountInfoTypeMetaField implements java.io.Serializable{
	
	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "customer_account_info_type_timeline_GEN")
    @Column(name = "id", unique = true, nullable = false)
	private int id;
	
	@ManyToOne
	@JoinColumn(name = "customer_id", nullable = false)
	private CustomerDTO customer;
	
	@ManyToOne
	@JoinColumn(name = "account_info_type_id", nullable = false)
    private AccountInformationTypeDTO accountInfoType;

	@ManyToOne(cascade = CascadeType.ALL)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@JoinColumn(name = "meta_field_value_id", nullable = false)
    private MetaFieldValue metaFieldValue;
	
	@Temporal(TemporalType.DATE)
	@Column(name = "effective_date", nullable = false, length = 10)
	private Date effectiveDate;
   
	public CustomerAccountInfoTypeMetaField() {}
	
	public CustomerAccountInfoTypeMetaField(CustomerDTO customer, AccountInformationTypeDTO accountInfoType, MetaFieldValue value, Date effectiveDate) {
    	this.customer = customer;
    	this.accountInfoType =accountInfoType;
		this.metaFieldValue = value;
    	this.effectiveDate = effectiveDate;
    }
	
	public AccountInformationTypeDTO getAccountInfoType() {
		return accountInfoType;
	}
	
	public MetaFieldValue getMetaFieldValue() {
		return metaFieldValue;
	}
	
	public void setMetaFieldValue(MetaFieldValue value) {
		this.metaFieldValue = value;
	}
	
	public Date getEffectiveDate() {
		return effectiveDate;
	}
}

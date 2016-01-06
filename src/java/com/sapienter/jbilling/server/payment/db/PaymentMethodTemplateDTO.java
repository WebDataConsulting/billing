package com.sapienter.jbilling.server.payment.db;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import com.sapienter.jbilling.server.metafields.db.MetaField;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author khobab
 *
 */
@Entity
@TableGenerator(
        name="payment_method_template_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="payment_method_template",
        allocationSize = 10
        )
@Table(name = "payment_method_template")
public class PaymentMethodTemplateDTO implements Serializable{

	private static final long serialVersionUID = 6278737291727940386L;
	
	private int id;
	private String templateName;
	
	private Set<MetaField> paymentTemplateMetaFields = new HashSet<MetaField>();
	
	private int version;
	
	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "payment_method_template_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }
	
	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "template_name", length = 50)
    public String getTemplateName() {
        return this.templateName;
    }
	
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
    		name = "payment_method_template_meta_fields_map",
            joinColumns = @JoinColumn(name = "method_template_id", updatable = false),
            inverseJoinColumns = @JoinColumn(name = "meta_field_id", updatable = false)
    )
	@OrderBy("displayOrder")
    public Set<MetaField> getPaymentTemplateMetaFields() {
        return paymentTemplateMetaFields;
	}
	
	public void setPaymentTemplateMetaFields(Set<MetaField> paymentTemplateMetaFields) {
		this.paymentTemplateMetaFields = paymentTemplateMetaFields;
	}
	
	@Version
    @Column(name = "OPTLOCK")
    public int getVersion() {
        return version;
    }
	
	public void setVersion(int version) {
		this.version = version;
	}

}

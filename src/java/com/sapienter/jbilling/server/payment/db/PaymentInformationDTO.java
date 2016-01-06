package com.sapienter.jbilling.server.payment.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import lombok.ToString;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.GroupCustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * 
 * @author khobab
 *
 */
@Entity
@TableGenerator(
        name="payment_information_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="payment_information",
        allocationSize = 10
        )
@Table(name = "payment_information")
public class PaymentInformationDTO extends GroupCustomizedEntity implements Serializable{	
	private Integer id;
	private Integer processingOrder;
	private Integer deleted = 0;
	
	private UserDTO user;
	private PaymentMethodTypeDTO paymentMethodType;
	private List<MetaFieldValue> metaFields = new ArrayList<MetaFieldValue>(0);
	
	private int versionNum;
	
	private PaymentMethodDTO paymentMethod;
	
	// transient fields
	boolean blacklisted = false;
	
	public PaymentInformationDTO() {
		// default constructor
	}
	
	public PaymentInformationDTO(Integer processingOrder, UserDTO user, PaymentMethodTypeDTO paymentMethodTye) {
		this.processingOrder = processingOrder;
		this.user = user;
		this.paymentMethodType = paymentMethodTye;
	}
	
	public PaymentInformationDTO(PaymentInformationWS ws, Integer entityId) {
		if(ws.getId() != null) {
			setId(ws.getId());
		}
		
		setProcessingOrder(ws.getProcessingOrder());
		setPaymentMethodType(new PaymentMethodTypeDAS().find(ws.getPaymentMethodTypeId()));
		setPaymentMethod(new PaymentMethodDAS().find(ws.getPaymentMethodId()));
		
		if(ws.getPaymentMethodId() != null) {
			setPaymentMethod(new PaymentMethodDTO(ws.getPaymentMethodId()));
		}
		
		MetaFieldBL.fillMetaFieldsFromWS(entityId, this, ws.getMetaFields());
	}
	
	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "payment_information_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return this.id;
    }

	public void setId(Integer id) {
		this.id = id;
	}
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return this.user;
    }

    public void setUser(UserDTO user) {
    	this.user = user;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    public PaymentMethodTypeDTO getPaymentMethodType() {
        return this.paymentMethodType;
    }

    public void setPaymentMethodType(PaymentMethodTypeDTO paymentMethodType){
    	this.paymentMethodType = paymentMethodType;
    }
    
    @Transient
    public PaymentMethodDTO getPaymentMethod() {
    	return this.paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethodDTO paymentMethod) {
    	this.paymentMethod = paymentMethod;
    }
    
    @Column(name = "processing_order")
    public Integer getProcessingOrder() {
    	return processingOrder;
    }
    
    public void setProcessingOrder(Integer processingOrder) {
    	this.processingOrder = processingOrder;
    }
    
    @Column(name = "deleted")
    public Integer getDeleted() {
    	return this.deleted;
    }
    
    public void setDeleted(Integer deleted) {
    	this.deleted = deleted;
    }
    
    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum() {
        return versionNum;
    }
    
    public void setVersionNum(int versionNum){
    	this.versionNum = versionNum;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "payment_information_meta_fields_map",
            joinColumns = @JoinColumn(name = "payment_information_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

	@Transient
	public EntityType[] getCustomizedEntityType() {
		return new EntityType[] { EntityType.PAYMENT_METHOD_TYPE };
	}
	
	/**
     * Useful method for updating payment method meta fields with validation before entity saving
     *
     * @param dto dto with new data
     */
    @Transient
    public void updatePaymentMethodMetaFieldsWithValidation(Integer entityId, MetaContent dto) {
        MetaFieldHelper.updatePaymentMethodMetaFieldsWithValidation(entityId, getPaymentMethodType().getId(), this, dto);
    }
	
    @Transient
    public boolean isBlacklisted() {
		return blacklisted;
	}

	public void setBlacklisted(boolean blacklisted) {
		this.blacklisted = blacklisted;
	}

	@Transient
    public PaymentInformationDTO getDTO() {
    	PaymentInformationDTO paymentInformation = new PaymentInformationDTO();
		paymentInformation.setId(this.id);
		paymentInformation.setPaymentMethod(this.paymentMethod);
		paymentInformation.setPaymentMethodType(this.paymentMethodType);
		paymentInformation.setProcessingOrder(this.processingOrder);
		paymentInformation.setUser(this.user);

		for(MetaFieldValue metaField : getMetaFields()) {
			MetaFieldValue value = metaField.getField().createValue();
			value.setId(metaField.getId());
			value.setValue(metaField.getValue());
			
			paymentInformation.getMetaFields().add(value);
		}
		
		return paymentInformation;
    }
    
	@Transient
    public boolean isNumberObsucred(String ccNumber) {
        return ccNumber != null && ccNumber.contains("*");
    }
	
    @Transient
    public PaymentInformationDTO getSaveableDTO() {
    	PaymentInformationDTO paymentInformation = new PaymentInformationDTO();
		paymentInformation.setPaymentMethod(this.paymentMethod);
		paymentInformation.setPaymentMethodType(this.paymentMethodType);
		paymentInformation.setProcessingOrder(this.processingOrder);
		//paymentInformation.setPayments(this.payments);
		//paymentInformation.setUser(this.user);
		
		for(MetaFieldValue metaField : this.getMetaFields()) {
			MetaFieldValue value = metaField.getField().createValue();
			//value.setId(null);
			value.setValue(metaField.getValue());
			
			paymentInformation.getMetaFields().add(value);
		}
		
		return paymentInformation;
    }
}

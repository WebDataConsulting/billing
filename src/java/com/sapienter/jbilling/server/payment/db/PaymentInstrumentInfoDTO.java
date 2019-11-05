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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import lombok.ToString;

/**
 * 
 * @author khobab
 *
 */
@Entity
@TableGenerator(
        name="payment_instrument_info_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="payment_instrument_info",
        allocationSize = 10
        )
@Table(name = "payment_instrument_info")
@ToString
public class PaymentInstrumentInfoDTO implements Serializable{
	
	private Integer id;
	
	private PaymentDTO payment;
	private PaymentResultDTO result;
	private PaymentMethodDTO paymentMethod;
	private PaymentInformationDTO paymentInformation;
	
	public PaymentInstrumentInfoDTO() {
	}
	
	public PaymentInstrumentInfoDTO(PaymentDTO payment, PaymentResultDTO result, PaymentMethodDTO paymentMethod, PaymentInformationDTO paymentInformation) {
    	this.payment = payment;
    	this.result = result;
    	this.paymentMethod = paymentMethod;
    	this.paymentInformation = paymentInformation;
	}
	
	@Id @GeneratedValue(generator="payment_instrument_info_GEN", strategy=GenerationType.TABLE)
	@Column(name="id", unique=true, nullable=false)
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id")
	public PaymentDTO getPayment() {
		return payment;
	}

	public void setPayment(PaymentDTO payment) {
		this.payment = payment;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "result_id")
	public PaymentResultDTO getResult() {
		return result;
	}

	public void setResult(PaymentResultDTO result) {
		this.result = result;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "method_id")
	public PaymentMethodDTO getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethodDTO paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "instrument_id")
	public PaymentInformationDTO getPaymentInformation() {
		return paymentInformation;
	}

	public void setPaymentInformation(PaymentInformationDTO paymentInformation) {
		this.paymentInformation = paymentInformation;
	}
}

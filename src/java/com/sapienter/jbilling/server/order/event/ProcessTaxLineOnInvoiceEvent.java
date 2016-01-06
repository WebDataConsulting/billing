package com.sapienter.jbilling.server.order.event;

import java.math.BigDecimal;

import com.sapienter.jbilling.server.system.event.Event;

public class ProcessTaxLineOnInvoiceEvent implements Event {

	private Integer entityId;
	private Integer userId;
	private BigDecimal taxLineAmount;

	public ProcessTaxLineOnInvoiceEvent(Integer entityId, Integer userId,
			BigDecimal taxLineAmount) {
		super();
		this.entityId = entityId;
		this.userId = userId;
		this.taxLineAmount = taxLineAmount;
	}

	@Override
	public String getName() {
		return "Invoice Line Type Tax for Entity " + entityId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public BigDecimal getTaxLineAmount() {
		return taxLineAmount;
	}

	public void setTaxLineAmount(BigDecimal taxLineAmount) {
		this.taxLineAmount = taxLineAmount;
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}

	@Override
	public String toString() {
		return "ProcessTaxLineOnInvoiceEvent [entityId=" + entityId
				+ ", userId=" + userId + ", taxLineAmount=" + taxLineAmount
				+ "]";
	}
	
}

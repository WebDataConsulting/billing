/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
You may download the latest source from webdataconsulting.github.io.

 */
package com.sapienter.jbilling.server.user.db;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;

/**
 * 
 * @author Panche.Isajeski
 * 
 * 	Embedable class for customer main subscription parameters
 *
 */
@Embeddable
public class MainSubscriptionDTO implements Serializable {
	
	private OrderPeriodDTO subscriptionPeriod;
	private Integer nextInvoiceDayOfPeriod;

	public MainSubscriptionDTO() {
		super();
	}
	
	public MainSubscriptionDTO(OrderPeriodDTO subscriptionPeriod, Integer nextInvoiceDayOfPeriod) {
		super();
		this.subscriptionPeriod = subscriptionPeriod;
		this.nextInvoiceDayOfPeriod = nextInvoiceDayOfPeriod;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "main_subscript_order_period_id")
	public OrderPeriodDTO getSubscriptionPeriod() {
		return subscriptionPeriod;
	}

	public void setSubscriptionPeriod(OrderPeriodDTO subscriptionPeriod) {
		this.subscriptionPeriod = subscriptionPeriod;
	}
	
	public void setSubsriptionPeriodFromPeriodId(Integer periodId) {
		setSubscriptionPeriod(new OrderPeriodDAS().find(periodId));
	}

	@Column(name = "next_invoice_day_of_period")
	public Integer getNextInvoiceDayOfPeriod() {
		return nextInvoiceDayOfPeriod;
	}

	public void setNextInvoiceDayOfPeriod(Integer nextInvoiceDayOfMonth) {
		this.nextInvoiceDayOfPeriod = nextInvoiceDayOfMonth;
	}
	
	public static MainSubscriptionDTO createDefaultMainSubscription(Integer entityId) {
		return new MainSubscriptionDTO(new OrderPeriodDAS().findOrderPeriod(
				entityId, 1, 1), 1);
	}

	@Override
	public String toString() {
		return "MainSubscriptionDTO [subscriptionPeriod="
				+ subscriptionPeriod + ", nextInvoiceDayOfPeriod="
				+ nextInvoiceDayOfPeriod + "]";
	}

}

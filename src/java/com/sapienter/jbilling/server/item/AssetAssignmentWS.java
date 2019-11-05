/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.item;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * This entities (WSs) are meant to be created by the system and only
 * updated by the system and not from outside. This WS object is only
 * to provide the client with information about the asset assignment.
 *
 * @author Vladimir Carevski
 * @since 30-OCT-2014
 */
public class AssetAssignmentWS implements Serializable {

	private Integer id;
	@NotNull(message = "validation.error.notnull")
	private Integer assetId;
	@NotNull(message = "validation.error.notnull")
	private Integer orderId;
	@NotNull(message = "validation.error.notnull")
	private Integer orderLineId;
	private Date startDatetime;
	private Date endDatetime;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getAssetId() {
		return assetId;
	}

	public void setAssetId(Integer assetId) {
		this.assetId = assetId;
	}

	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	public Integer getOrderLineId() {
		return orderLineId;
	}

	public void setOrderLineId(Integer orderLineId) {
		this.orderLineId = orderLineId;
	}

	public Date getStartDatetime() {
		return startDatetime;
	}

	public void setStartDatetime(Date startDatetime) {
		this.startDatetime = startDatetime;
	}

	public Date getEndDatetime() {
		return endDatetime;
	}

	public void setEndDatetime(Date endDatetime) {
		this.endDatetime = endDatetime;
	}

	@Override
	public String toString() {
		return "Asset Assign ID: " + id +
				", Asset ID: " + assetId +
				", Order Line ID: " + orderLineId +
				", Start Date: " + startDatetime +
				", End Date: " + endDatetime;
	}
}
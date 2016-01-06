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
package com.sapienter.jbilling.server.item.db;

import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Vladimir Carevski
 * @since 30-OCT-2014
 */
@Entity
@Table(name = "asset_assignment")
@TableGenerator(
		name = "asset_assignment_GEN",
		table = "jbilling_seqs",
		pkColumnName = "name",
		valueColumnName = "next_id",
		pkColumnValue = "asset_assignment",
		allocationSize = 1
)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class AssetAssignmentDTO implements Serializable{

	private int id;
	private AssetDTO asset;
	private OrderLineDTO orderLine;
	private Date startDatetime;
	private Date endDatetime;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "asset_assignment_GEN")
	@Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "asset_id")
	public AssetDTO getAsset() {
		return this.asset;
	}

	public void setAsset(AssetDTO asset) {
		this.asset = asset;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_line_id")
	public OrderLineDTO getOrderLine() {
		return orderLine;
	}

	public void setOrderLine(OrderLineDTO orderLine) {
		this.orderLine = orderLine;
	}

	@Column(name = "start_datetime", nullable = false, length = 29)
	public Date getStartDatetime() {
		return startDatetime;
	}

	public void setStartDatetime(Date startDatetime) {
		this.startDatetime = startDatetime;
	}

	@Column(name = "end_datetime", nullable = true, length = 29)
	public Date getEndDatetime() {
		return endDatetime;
	}

	public void setEndDatetime(Date endDatetime) {
		this.endDatetime = endDatetime;
	}

	@Transient
	public Integer getOrderId(){
		return null != orderLine && null != orderLine.getPurchaseOrder() ?
				orderLine.getPurchaseOrder().getId() : null;
	}
}
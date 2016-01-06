package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Vladimir Carevski
 * @since 30-OCT-2014
 */
public class AssetAssignmentBL {

	private AssetAssignmentDAS das;

	public AssetAssignmentBL() {
		init();
	}

	private void init() {
		das = new AssetAssignmentDAS();
	}

	public List<OrderDTO> findOrdersForAssetAndDateRange(final Integer assetId, final Date startDate, final Date endDate) {
		List<OrderLineDTO> lines = das.findOrderLinesForAssetAndDateRange(assetId, startDate, endDate);
		List<OrderDTO> orders = new ArrayList<OrderDTO>();
		for (OrderLineDTO line : lines) {
			orders.add(line.getPurchaseOrder());
		}
		return orders;
	}

	public OrderDTO findOrderForAsset(final Integer assetId, final Date date) {
		OrderLineDTO orderLine = null;
		if (null != date) {
			orderLine = das.findOrderLineForAssetAndDate(assetId, date);
		} else {
			orderLine = das.findCurrentOrderLineForAssetId(assetId);
		}
		return null != orderLine ? orderLine.getPurchaseOrder() : null;
	}

	public static final AssetAssignmentWS toWS(final AssetAssignmentDTO dto) {
		AssetAssignmentWS ws = new AssetAssignmentWS();
		ws.setId(dto.getId());
		ws.setAssetId(dto.getAsset().getId());
		ws.setOrderLineId(dto.getOrderLine().getId());
		ws.setOrderId(dto.getOrderLine().getPurchaseOrder().getId());
		ws.setStartDatetime(dto.getStartDatetime());
		ws.setEndDatetime(dto.getEndDatetime());
		return ws;
	}

	public static final AssetAssignmentWS[] toWS(final Collection<AssetAssignmentDTO> assignments) {
		AssetAssignmentWS[] assignWss = new AssetAssignmentWS[assignments.size()];
		int index = 0;
		for (AssetAssignmentDTO dto : assignments) {
			assignWss[index++] = toWS(dto);
		}
		return assignWss;
	}
}
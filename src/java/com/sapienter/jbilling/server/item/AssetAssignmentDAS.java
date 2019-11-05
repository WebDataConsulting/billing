package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;

import java.util.Date;
import java.util.List;

/**
 * @author Vladimir Carevski
 * @since 30-OCT-2014
 */
public class AssetAssignmentDAS extends AbstractDAS<AssetAssignmentDTO> {

	private final static String FIND_ORDER_LINES_FOR_ASSET =
			"from AssetAssignmentDTO aa where " +
					"     aa.asset.id = :asset_id " +
					" order by aa.startDatetime desc";

	public List<AssetAssignmentDTO> getAssignmentsForAsset(Integer assetId) {
		Query query = getSession().createQuery(FIND_ORDER_LINES_FOR_ASSET);
		query.setParameter("asset_id", assetId);
		return query.list();
	}

	private final static String FIND_ASSETS_FOR_ORDER =
			"select aa from AssetAssignmentDTO aa inner join aa.orderLine ol where " +
					" ol.purchaseOrder.id = :order_id " +
					" order by aa.startDatetime desc";

	public List<AssetAssignmentDTO> getAssignmentsForOrder(Integer orderId) {
		Query query = getSession().createQuery(FIND_ASSETS_FOR_ORDER);
		query.setParameter("order_id", orderId);
		return query.list();
	}

	private final static String FIND_CURRENT_ORDER_LINE_FOR_ASSET =
			"from AssetAssignmentDTO aa where " +
					"     aa.asset.id = :asset_id " +
					" and aa.endDatetime is null " +
					" order by aa.startDatetime desc";

	public OrderLineDTO findCurrentOrderLineForAssetId(Integer assetId) {
		Query query = getSession().createQuery(FIND_CURRENT_ORDER_LINE_FOR_ASSET);
		query.setParameter("asset_id", assetId);
		List list = query.list();

		if (null != list && list.size() > 0) {
			AssetAssignmentDTO assignmentDTO = (AssetAssignmentDTO) list.get(0);
			return assignmentDTO.getOrderLine();
		} else {
			return null;
		}
	}

	private final static String FIND_ORDER_LINE_FOR_ASSET_DATE =
			"from AssetAssignmentDTO aa where " +
					"     aa.asset.id = :asset_id " +
					" and aa.startDatetime <= :date " +
					" and (aa.endDatetime is null or aa.endDatetime > :date)";


	public OrderLineDTO findOrderLineForAssetAndDate(Integer assetId, Date date) {
		Query query = getSession().createQuery(FIND_ORDER_LINE_FOR_ASSET_DATE);
		query.setParameter("asset_id", assetId);
		query.setParameter("date", date);

		//for a given date we should only have at most one
		//asset assignment i.e. an asset can be assigned to
		//only one order at a time. Or, time ranges of asset
		//assignment should NOT overlap. Therefore, we expect
		//only one result from the above query, if this explodes
		//then that smells like data corruption
		AssetAssignmentDTO assign = (AssetAssignmentDTO) query.uniqueResult();
		return null != assign ? assign.getOrderLine() : null;
	}

	private final static String FIND_ORDER_LINES_ASSET_DATE_RANGE =
			"select ol from OrderLineDTO ol inner join ol.assetAssignments assign " +
					" where " +
					"   assign.asset.id = :asset_id and" +
					"   assign.startDatetime <= :end_date and" +
					"   (assign.endDatetime is null or assign.endDatetime >= :start_date)";

	public List<OrderLineDTO> findOrderLinesForAssetAndDateRange(Integer assetId, Date startDate, Date endDate) {
		Query query = getSession().createQuery(FIND_ORDER_LINES_ASSET_DATE_RANGE);
		query.setParameter("asset_id", assetId);
		query.setParameter("start_date", startDate);
		query.setParameter("end_date", endDate);
		return query.list();
	}

}
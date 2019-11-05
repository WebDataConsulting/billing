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


import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

/**
 * Created by Fernando G. Morales on 10/24/14.
 */
public class AssetReservationDAS extends AbstractDAS<AssetReservationDTO> {

    private static final FormatLogger LOG = new FormatLogger(AssetReservationDAS.class);

    public AssetReservationDTO findActiveReservationByAsset(Integer assetId) {
        Query query = getSession().getNamedQuery("AssetReservationDTO.findActiveReservationByAsset");
        query.setInteger("assetId", assetId);
        return (AssetReservationDTO) query.uniqueResult();
    }

	public AssetReservationDTO findReservationByAssetNoFlush(Integer assetId) {
		Session session = getSession();
		FlushMode prevFlushMode = session.getFlushMode();
		session.setFlushMode(FlushMode.COMMIT);
		Query query = session.getNamedQuery("AssetReservationDTO.findActiveReservationByAsset");
		query.setInteger("assetId", assetId);
		AssetReservationDTO reservation = (AssetReservationDTO) query.uniqueResult();
		session.setFlushMode(prevFlushMode);
		return reservation;
	}

}

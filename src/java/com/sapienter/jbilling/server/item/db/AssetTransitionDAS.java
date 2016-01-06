/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;

import java.util.List;

/**
 * @author Gerhard
 * @since 15/04/13
 */
public class AssetTransitionDAS extends AbstractDAS<AssetTransitionDTO> {

    /**
     * Return all transitions linked to the asset, ordered by date.
     *
     * @param assetId
     * @return
     */
    public List<AssetTransitionDTO> getTransitions(int assetId) {
        Query query = getSession().getNamedQuery("AssetTransitionDTO.findForAsset");
        query.setParameter("asset_id", assetId);

        return query.list();
    }
}

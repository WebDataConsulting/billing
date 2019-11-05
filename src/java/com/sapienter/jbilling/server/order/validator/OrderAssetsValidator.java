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
package com.sapienter.jbilling.server.order.validator;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetReservationDAS;
import com.sapienter.jbilling.server.item.db.AssetReservationDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexander Aksenov
 * @since 14.07.13
 */
public class OrderAssetsValidator {

    public final static String ERROR_ASSET_MANAGEMENT_DISABLED = "OrderLineWS,assetIds,validation.assets.but.no.assetmanagement";
    public final static String ERROR_QUANTITY_NOT_MATCH = "OrderLineWS,assetIds,validation.assets.unequal.to.quantity";
    public final static String ERROR_ASSET_LINKED_TO_DIFFERENT_PRODUCT = "OrderLineWS,assetIds,validation.asset.linked.to.different.product";
    public final static String ERROR_ASSET_ALREADY_LINKED = "OrderLineWS,assetIds,validation.asset.already.linked";
    public final static String ERROR_ASSET_STATUS_UNAVAILABLE = "OrderLineWS,assetIds,validation.asset.status.unavailable";
    public final static String ERROR_ASSET_STATUS_RESERVED = "OrderLineWS,assetIds,validation.asset.status.reserved";

    private static final FormatLogger LOG = new FormatLogger(OrderAssetsValidator.class);

    /**
     * Validate assets in given order line
     * @param line Order line for validation
     * @param unlinkedAssets Assets unlinked from another lines
     * @return Error code if validation fails, null otherwise
     */
    public static String validateAssets(OrderLineDTO line, Map<Integer, AssetDTO> unlinkedAssets) {
        return validateAssets(line, unlinkedAssets, true);
    }

    public static String validateAssetsForOrderChangesApply(OrderLineDTO line) {
        return validateAssets(line, new HashMap<Integer, AssetDTO>(), false);
    }

    /**
     * Checks
     * - item allows asset mangement
     * - line quantity match the number of assets
     * - asset is not linked to another order line
     * - if asset is linked for the first time check that the old status is available
     *
     * @param line to validate
     * @param unlinkedAssets assets unlinked from other lines
     * @param validateStatus flag to indicate is status validation always needed, or should be skipped for unlinked lines
     * @return error code or null if validation passed
     */
    private static String validateAssets(OrderLineDTO line, Map<Integer, AssetDTO> unlinkedAssets, boolean validateStatus) {

        if ( null != line.getPurchaseOrder().getResellerOrder() ) {
            LOG.debug("We do not validate Assets for the orders in the parent entity for the Reseller Customer.");
            return null;
        }

        if (line.getDeleted() > 0) {
            return null;
        }
        //if asset management is not done
        if (line.getItem()== null || line.getItem().getAssetManagementEnabled() == 0) {
            if (line.getAssets().size() > 0) {
                return ERROR_ASSET_MANAGEMENT_DISABLED;
            } else {
                return null;
            }
        }

        if (line.getQuantityInt() != line.getAssets().size()) {
            return ERROR_QUANTITY_NOT_MATCH;
        }

        ItemDTO itemDto = line.getItem();

        for (AssetDTO asset : line.getAssets()) {
            //check if this asset was removed from another line
            if (unlinkedAssets.containsKey(asset.getId())) {
                asset = unlinkedAssets.get(asset.getId());
            }
            if (asset.getItem().getId() != itemDto.getId())
                return ERROR_ASSET_LINKED_TO_DIFFERENT_PRODUCT;

            if (asset.getPrevOrderLine() != null) {
                if (asset.getPrevOrderLine().getId() != line.getId()) {
                    return ERROR_ASSET_ALREADY_LINKED;
                }
            } else {
                //this is a new asset to link
                if (validateStatus || !asset.isUnlinkedFromLine()) {
                    /* an asset, that has been added to an order, should not be reserved by any other customer */
                    AssetReservationDTO activeReservation = new AssetReservationDAS().findReservationByAssetNoFlush(asset.getId());
                    if(asset.getAssetStatus().getIsAvailable() == 0){
                        return ERROR_ASSET_STATUS_UNAVAILABLE;
                    } else if ((activeReservation!=null && activeReservation.getUser().getId()!=line.getPurchaseOrder().getUser().getId())){
                        return ERROR_ASSET_STATUS_RESERVED;
                    }
                }
            }
        }
        return null;
    }
}

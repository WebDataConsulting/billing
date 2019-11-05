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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetReservationDAS;
import com.sapienter.jbilling.server.item.db.AssetReservationDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by Fernando G. Morales on 10/24/14.
 */
public class AssetReservationBL {

    private static final FormatLogger LOG = new FormatLogger(AssetReservationBL.class);
    public final static int MINIMUM_ASSET_RESERVATION_TIME_IN_MS = 0;

    private AssetReservationDTO assetReservationDTO;
    private AssetReservationDAS assetReservationDAS;

    public AssetReservationBL() {
        this.assetReservationDAS = new AssetReservationDAS();
    }

    public AssetReservationBL(AssetReservationDTO asset) {
        this.assetReservationDTO = asset;
        this.assetReservationDAS = new AssetReservationDAS();
    }

    /**
     * This method will create an Asset Reservation for the user and asset passed as parameters.
     * The duration will be taken from the asset’s item’s category NEW field.
     * If no duration was set, the return value will be the one defined in CommonConstants.DEFAULT_ASSET_RESERVATION_TIME_IN_MS.
     * The returned value is expresed in minutes.
     * @param userId
     * @param assetId
     * @return
     */
    public Integer reserveAsset(Integer assetId, Integer creatorId, Integer userId) throws SessionInternalError {

        UserDTO userDTO = new UserDAS().find(userId);
        if (userDTO == null) {
            String[] errors = new String[]{"AssetReservationWS,userId,bean.AssetReservationWS.validation.error.userId"};
            LOG.error("Cannot find a user with id: %d", userId);
            throw new SessionInternalError("Cannot find a user with id: " + userId, errors);
        }

        UserDTO creatorDTO = new UserDAS().find(creatorId);
        if (creatorDTO == null) {
            String[] errors = new String[]{"AssetReservationWS,creatorId,bean.AssetReservationWS.validation.error.creatorId"};
            LOG.error("Cannot find a creator with id: %d", creatorId);
            throw new SessionInternalError("Cannot find a creator with id: " + creatorId, errors);
        }

        AssetDTO assetDTO = new AssetDAS().findNow(assetId);
        if (assetDTO == null) {
            LOG.error("Cannot find an asset with id: %d", assetId);
            throw new SessionInternalError("Cannot find an asset with id: " + assetId);
        }

        // Check if asset is already used in any order then can not be reserved
        if (assetDTO.getAssetStatus().getIsOrderSaved() == AssetStatusBL.ASSET_STATUS_TRUE) {
            String[] errors = new String[]{"AssetReservationWS,assetId,bean.AssetReservationWS.validation.error.assigned.to.another.order"};
            LOG.error("This asset is already assigned to another order: %s", assetDTO);
            throw new SessionInternalError("This asset is already assigned to another order: " + assetDTO);
        }

        // Check if there is any active reservation for that asset
        AssetReservationDTO activeReservation = assetReservationDAS.findActiveReservationByAsset(assetId);

        if (activeReservation != null) {
            LOG.error("Active asset reservation is exist so there will be no transaction with asset_reservation db");
            if (activeReservation.getUser().getId() == userId) {
                LOG.error("Asset is already reserved for this user Id : %d", userId);
                return null;
            } else {
                LOG.error("Asset reservation for asset# %d it's not expired for another user", assetId);
                String[] errors = new String[]{"AssetReservationWS,assetId,bean.AssetReservationWS.validation.error.assetID.reserved," + assetDTO.getIdentifier()};
                throw new SessionInternalError("Asset reservation for asset#" + assetId + " it's not expired for another user", errors);
            }
        } else {
            // create a new  asset reservation because there is no active reservation for this asset
            assetReservationDTO = new AssetReservationDTO();
            assetReservationDTO.setAsset(assetDTO);
            assetReservationDTO.setUser(userDTO);
            assetReservationDTO.setCreator(creatorDTO);
            assetReservationDTO.setStartDate(DateTime.now().toDate());

            // fetch the duration from asset's product and calculate the end date for reservation
            Integer duration = assetDTO.getItem().getReservationDuration();
            duration = duration > MINIMUM_ASSET_RESERVATION_TIME_IN_MS ? duration : CommonConstants.DEFAULT_ASSET_RESERVATION_TIME_IN_MS;
            assetReservationDTO.setEndDate(
                    DateTime.now().plusMinutes(Util.convertFromMsToMinutes(duration)).toDate());
            assetReservationDTO = assetReservationDAS.save(assetReservationDTO);
        }
        return assetReservationDTO.getId();
    }

    /**
     * Release asset reservation given its assetId or reservationId
     * @param assetId
     * @param creatorId: if this param is set to null, then the asset reservation will be set to completed==true, otherwise it will be deleted.
     * @param userId
     */
    public void releaseAsset(Integer assetId, Integer creatorId, Integer userId) throws SessionInternalError {

        AssetReservationDTO activeReservation = assetReservationDAS.findReservationByAssetNoFlush(assetId);
        if (activeReservation == null) {
            // Order created through API calls or asset reservation is not used
            LOG.error("There is no active reservation for Asset # %d to release", assetId);
            return;
        }

        boolean isValidToRelease = false;
        UserDTO creatorDTO = new UserDAS().find(creatorId);
        if (creatorId != null && creatorDTO != null) {

            if (creatorId == activeReservation.getCreator().getId()) {
                isValidToRelease = true;
            } else {
                // Throw error that asset can not be released by this user : creatorId
                LOG.error("Asset can not be released by this user : %d", creatorId);
                String[] errors = new String[]{"AssetReservationWS,creatorId,bean.AssetReservationWS.validation.error.creatorId.unauthenticated," + creatorId};
                throw new SessionInternalError("Asset can not be released by this user : " + creatorId, errors);
            }
        } else if (creatorId == null) {
            // if asset is utilised in order
            isValidToRelease = true;
        } else {
            String[] errors = new String[]{"AssetReservationWS,creatorId,bean.AssetReservationWS.validation.error.creatorId"};
            LOG.error("Cannot find a creator with id: %d", creatorId);
            throw new SessionInternalError("Cannot find a creator with id: " + creatorId, errors);
        }

        UserDTO user = new UserDAS().find(userId);
        if (userId != null && user != null) {

            if (user.getId() ==  activeReservation.getUser().getId()) {
                isValidToRelease = true;
            } else {
                // Throw error that asset is not reserved for that customer : userId
                LOG.error("Asset is not reserved for that customer : %d", userId);
                String[] errors = new String[]{"AssetReservationWS,userId,bean.AssetReservationWS.validation.error.userId.unauthenticated," + userId};
                throw new SessionInternalError("Asset is not reserved for that customer : " + userId, errors);
            }
        } else if (userId == null) {
            // Its a case when asset is manually released.
            // if asset is utilised in order
            isValidToRelease = true;
        } else {
            String[] errors = new String[]{"AssetReservationWS,userId,bean.AssetReservationWS.validation.error.userId"};
            LOG.error("Cannot find a user with id: %d", userId);
            throw new SessionInternalError("Cannot find a user with id: " + userId, errors);
        }

        if (isValidToRelease) {
            activeReservation.setEndDate(new Date());
            try {
                assetReservationDAS.reattach(activeReservation);
            } catch (Exception e) {
                String[] errors = new String[]{"AssetReservationWS,assetId,bean.AssetReservationWS.validation.error.release.fail," + assetId};
                LOG.error("Error occurred while releasing the asset: %d", assetId);
                throw new SessionInternalError("Error occurred while releasing the asset: " + assetId, errors);
            }
        }
    }
}

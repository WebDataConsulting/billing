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

package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.AssetTransitionDAS;
import com.sapienter.jbilling.server.item.db.AssetTransitionDTO;


import java.util.Collection;
import java.util.List;

/**
 * @author Gerhard Maree
 * @since 15/04/13
 */
public class AssetTransitionBL {

    private static final FormatLogger LOG = new FormatLogger(ItemBL.class);

    private AssetTransitionDTO transition;
    private AssetTransitionDAS das;

    public AssetTransitionBL() {
        init();
    }

    public AssetTransitionBL(int id) {
        init();
        set(id);
    }

    /**
     * Convert the dto to AssetTransitionDTOEx for the web.
     *
     * @param dto
     * @return
     */
    public static final AssetTransitionDTOEx getWS(AssetTransitionDTO dto) {
        AssetTransitionDTOEx ex = new AssetTransitionDTOEx();
        ex.setCreateDatetime(dto.getCreateDatetime());
        ex.setNewStatusId(dto.getNewStatus().getId());

        if(dto.getUser() != null) {
            ex.setUserId(dto.getUser().getId());
        }

        if(dto.getAssignedTo() != null) {
            ex.setAssignedToId(dto.getAssignedTo().getId());
        }

        if(dto.getPreviousStatus() != null) {
            ex.setPreviousStatusId(dto.getPreviousStatus().getId());
        }
        return ex;
    }

    /**
     * Convert a collection of transitions into AssetTransitionDTOEx which are safe for external communication.
     *
     * @param transitions   AssetTransitionDTO objects to convert
     * @return
     */
    public static AssetTransitionDTOEx[] getWS(Collection<AssetTransitionDTO> transitions) {
        AssetTransitionDTOEx[] dtoExes = new AssetTransitionDTOEx[transitions.size()];
        int idx = 0;
        for(AssetTransitionDTO dto : transitions) {
            dtoExes[idx++] = getWS(dto);
        }
        return dtoExes;
    }

    public AssetTransitionBL set(int id) {
        transition = das.find(id);
        return this;
    }

    public AssetTransitionDTO getEntity() {
        return transition;
    }

    private void init() {
        das = new AssetTransitionDAS();
    }

    /**
     * Return all transitions linked to the asset, ordered by date descending.
     *
     * @param assetId
     * @return
     */
    public List<AssetTransitionDTO> getTransitions(int assetId) {
        return das.getTransitions(assetId);
    }

    public AssetTransitionDTO create(AssetTransitionDTO dto) {
        return das.save(dto);
    }
}

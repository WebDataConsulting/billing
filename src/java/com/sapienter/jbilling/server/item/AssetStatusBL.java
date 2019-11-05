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
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetStatusDAS;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gerhard Maree
 * @since 15/04/13
 */
public class AssetStatusBL {

    private static final FormatLogger LOG = new FormatLogger(ItemBL.class);

    public final static int ASSET_STATUS_TRUE = 1;
    public final static int ASSET_STATUS_FALSE = 0;

    private AssetStatusDTO status;
    private AssetStatusDAS das;

    public AssetStatusBL() {
        init();
    }

    public AssetStatusBL(int id) {
        init();
        set(id);
    }

    public AssetStatusBL set(int id) {
        status = das.find(id);
        return this;
    }

    public AssetStatusDTO getEntity() {
        return status;
    }

    private void init() {
        das = new AssetStatusDAS();
    }

    public AssetStatusDTO save(AssetStatusDTO statusDTO) {
        return das.save(statusDTO);
    }

    public void delete(AssetStatusDTO src) {
        das.delete(src);
    }

    /**
     * Convert a collection of AssetStatusDTOEx into AssetStatusDTO
     *
     * @param dtoExes
     * @return
     * @throws SessionInternalError
     */
    public Set convertAssetStatusDTOExes(Collection<AssetStatusDTOEx> dtoExes) throws SessionInternalError {
        AssetStatusBL statusBL = new AssetStatusBL();

        HashSet<AssetStatusDTO> dtos = new HashSet<AssetStatusDTO>((int)(dtoExes.size() * 1.5));
        for(AssetStatusDTOEx dtoEx : dtoExes) {
            AssetStatusDTO dto = new AssetStatusDTO(dtoEx);
            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Merge non-entity properties from source into destination.
     *
     * @param destination   Values will be set on destination
     * @param source        Source of new value
     * @return
     */
    public AssetStatusDTO mergeBasicProperties(AssetStatusDTO destination, AssetStatusDTO source) {
        //we do not merge internal statuses
        if(destination.getIsInternal() == 1 || source.getIsInternal() == 1) return destination;

        destination.setIsAvailable(source.getIsAvailable());
        destination.setIsDefault(source.getIsDefault());
        destination.setIsOrderSaved(source.getIsOrderSaved());
        destination.setDescription(source.getDescription());
        return destination;
    }

    /**
     * Lists all non deleted statuses for the given category.
     *
     * @param categoryId    ItemTypeDTO id
     * @param includeInternal Must internal statuses be included in the results.
     * @return
     */
    public List<AssetStatusDTO> getStatuses(int categoryId, boolean includeInternal) {
        return das.getStatuses(categoryId, includeInternal);
    }

    /**
     * Convert a collection of AssetStatusDTO objects into AssetStatusDTOEx objects.
     *
     * @param dtos  AssetStatusDTO objects to convert
     * @return      Set of AssetStatusDTOEx
     */
    public static final Set convertAssetStatusDTOs(Collection<AssetStatusDTO> dtos) {
        HashSet<AssetStatusDTOEx> dtoExs = new HashSet<AssetStatusDTOEx>((int)(dtos.size() * 1.5));
        for(AssetStatusDTO dto : dtos) {
            dtoExs.add(getWS(dto));
        }
        return dtoExs;
    }

    /**
     * Convert an AssetStatusDTO into AssetStatusDTOEx
     *
     * @param dto
     * @return
     */
    public static final AssetStatusDTOEx getWS(AssetStatusDTO dto) {
        return new AssetStatusDTOEx(dto.getId(), dto.getDescription(), dto.getIsDefault(), dto.getIsAvailable(), dto.getIsOrderSaved(), dto.getIsInternal());
    }

    /**
     * Find the default status for the ItemTypeDTO (which allows asset management) linked to the item.
     *
     * @param id    ItemDTO id
     * @return
     */
    public AssetStatusDTO findDefaultStatusForItem(int id) {
        return das.findDefaultStatusForItem(id);
    }

    /**
     * Return the internal status 'Member of Group'
     *
     * @return
     */
    public AssetStatusDTO getMemberOfGroupStatus() {
        return das.find(ServerConstants.ASSET_STATUS_MEMBER_OF_GROUP);
    }

    /**
     * Find the available status for the ItemTypeDTO (which allows asset management) linked to the item.
     * @param id    ItemDTO id
     * @return
     */
    public AssetStatusDTO findAvailableStatusForItem(int id) {
        return das.findAvailableStatusForItem(id);
    }

}

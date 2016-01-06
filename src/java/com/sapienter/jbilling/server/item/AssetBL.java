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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sapienter.jbilling.server.item.event.AbstractAssetEvent;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.event.AssetCreatedEvent;
import com.sapienter.jbilling.server.item.event.AssetDeletedEvent;
import com.sapienter.jbilling.server.item.event.AssetUpdatedEvent;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.search.SearchCriteria;

/**
 *
 * @author Gerhard Maree
 * @since 15/04/13
 */
public class AssetBL {

    private static final FormatLogger LOG = new FormatLogger(AssetBL.class);
    AssetDAS das = null;
    AssetDTO asset = null;
    private EventLogger eLogger = null;

    public AssetBL(Integer assetId)
            throws SessionInternalError {
        try {
            init();
            set(assetId);
        } catch (Exception e) {
            throw new SessionInternalError("Setting asset", AssetDTO.class, e);
        }
    }

    public AssetBL() {
        init();
    }

    public AssetBL(AssetDTO asset) {
        this.asset = asset;
        init();
    }

    /**
     * Convert AssetDTO into AssetWS which is safe for external communication.
     *
     * @param dto
     * @return
     */
    public static final AssetWS getWS(AssetDTO dto) {
        AssetWS ws = new AssetWS();
        ws.setId(dto.getId());
        //ws.setDeleted(dto.getDeleted());
        ws.setIdentifier(dto.getIdentifier());
        ws.setCreateDatetime(dto.getCreateDatetime());
        ws.setNotes(dto.getNotes());
        ws.setAssetStatusId(dto.getAssetStatus().getId());
        ws.setEntityId(dto.getEntity()!= null? dto.getEntity().getId() :null);
        ws.setItemId(dto.getItem().getId());
        ws.setMetaFields( MetaFieldBL.convertMetaFieldsToWS(dto.getItem().findItemTypeWithAssetManagement().getAssetMetaFields(), dto));

        if(dto.getOrderLine() != null) {
            ws.setOrderLineId(dto.getOrderLine().getId());
        }

        Integer[] containedAssetIds = new Integer[dto.getContainedAssets().size()];
        int idx = 0;
        for(AssetDTO containedAsset: dto.getContainedAssets()) {
            containedAssetIds[idx++] = containedAsset.getId();
        }
        ws.setContainedAssetIds(containedAssetIds);

        if(dto.getGroup() != null) {
            ws.setGroupId(dto.getGroup().getId());
        }
        ws.setAssignments(AssetAssignmentBL.toWS(dto.getAssignments()));
        return ws;
    }

    /**
     * Convert a collection of AssetDTO objects into AssetWS objects for external communication.
     *
     * @param dtoCollection
     * @return
     */
    public static final AssetWS[] getWS(Collection<AssetDTO> dtoCollection) {
        AssetWS[] assetWSs = new AssetWS[dtoCollection.size()];
        int idx = 0;
        for(AssetDTO dto: dtoCollection) {
            assetWSs[idx++] = getWS(dto);
        }
        return assetWSs;
    }

    public AssetBL set(Integer itemId) {
        asset = das.find(itemId);
        return this;
    }

    private void init() {
        das = new AssetDAS();
        eLogger = EventLogger.getInstance();
    }

    public AssetDTO getEntity() {
        return asset;
    }

    /**
     * Create the asset and fire the AssetCreatedEvent event.
     *
     * @param dto
     * @param userId    person who performed the operation
     * @return
     */
    public int create(AssetDTO dto, Integer userId) {
        dto.setCreateDatetime(new Date());
        dto.setDeleted(0);

        //merge the contained assets
        Set<AssetDTO> assetDTOSet = dto.getContainedAssets();
        dto.setContainedAssets(new HashSet<AssetDTO>(assetDTOSet.size()*2));
        List<Event> events = mergeContainedAssets(dto, assetDTOSet, userId);
        
        asset = das.save(dto);
        das.flush();

        //create and fire the AssetCreatedEvent
        UserDTO assignedTo = (dto.getOrderLine() != null
                && dto.getOrderLine().getPurchaseOrder() != null) ? dto.getOrderLine().getPurchaseOrder().getUser() : null;

        Integer entityId= (null != asset.getEntity()) ? asset.getEntity().getId() : null;

        AssetCreatedEvent event = new AssetCreatedEvent(entityId, asset, assignedTo, userId);
        EventManager.process(event);

        //fire the events
        for(Event ev: events) {
            EventManager.process(ev);
        }
        
        // triggering process for child companies
        for(Integer id : dto.getChildEntitiesIds()) {
	        if (!id.equals(entityId)) { //this was once triggered for the owning company
		        EventManager.process(new AssetCreatedEvent(id, asset, assignedTo, userId));
	        }
    	}
        return asset.getId();
    }

    /**
     * Update the asset with the values from {@code dto}.
     * Only the contained assets of a group can be set, not the parent group.
     * The OrderLine is also not updated.
     *
     * Fires AssetUpdatedEvent
     *
     * @param dto       Contains new values
     * @param userId    User performing the operation
     */
    public void update(AssetDTO dto, Integer userId) {
        AssetDTO persistentDto = das.find(dto.getId());
        AssetStatusDTO oldStatus = persistentDto.getAssetStatus();

        if (persistentDto != dto) {
            for (MetaFieldValue metaFieldValue : dto.getMetaFields()) {
                persistentDto.setMetaField(metaFieldValue.getField(), metaFieldValue.getValue());
            }
        }

        persistentDto.setAssetStatus(dto.getAssetStatus());
        persistentDto.setIdentifier(dto.getIdentifier());
        persistentDto.setNotes(dto.getNotes());
        persistentDto.setEntity(dto.getEntity());
        persistentDto.setGlobal(dto.isGlobal());
        persistentDto.setEntities(dto.getEntities());
        if(dto.getEntity() != null) {
        	persistentDto.setEntity(dto.getEntity());
        }

        //merge the contained assets
        List<Event> events = mergeContainedAssets(persistentDto, dto.getContainedAssets(), userId);
        
//        persistentDto.getMetaFields().clear();        This line caused to clear meta field's saved value.
        
        asset = das.save(persistentDto);
        das.flush();
        //create and fire the AssetUpdatedEvent event
        UserDTO assignedTo = (dto.getOrderLine() != null
                && dto.getOrderLine().getPurchaseOrder() != null) ? dto.getOrderLine().getPurchaseOrder().getUser() : null;

	    Integer entityId = persistentDto.getEntityId();
        events.add(new AssetUpdatedEvent(entityId, persistentDto, oldStatus, assignedTo, userId));

        //fire the events
        for(Event event: events) {
            EventManager.process(event);
        }

	    // triggering process for child companies
	    for (Integer id : dto.getChildEntitiesIds()) {
		    if (!id.equals(entityId)) { //this was once triggered for the owning company
			    EventManager.process(new AssetCreatedEvent(id, asset, assignedTo, userId));
		    }
	    }
    }

    public AssetDTO find(Integer assetId) {
        return das.find(assetId);
    }

    /**
     * Delete the asset and fires AssetDeletedEvent event.
     *
     * @param assetId   AssetDTO id
     * @param userId    user performing the operation
     */
    public void delete(Integer assetId, Integer userId) {
        AssetDTO dto = das.find(assetId);
        //if the DTO is already deleted return
        if(dto.getDeleted() == 1) {
            return;
        }

        //remove all the contained assets
        List<Event> events = mergeContainedAssets(dto, new ArrayList<AssetDTO>(0), userId);

        dto.setDeleted(1);
        das.save(dto);
        das.flush();

        //get the objects for the event and fire it
        UserDTO assignedTo = (dto.getOrderLine() != null
                && dto.getOrderLine().getPurchaseOrder() != null) ? dto.getOrderLine().getPurchaseOrder().getUser() : null;
        AssetDeletedEvent event = new AssetDeletedEvent(dto.getEntity() != null ? dto.getEntity().getId() : null, dto,
                assignedTo, userId);
        EventManager.process(event);

        //fire the asset change events
        for(Event assetEvent: events) {
            EventManager.process(assetEvent);
        }
    }

    /**
     * Convert the AssetWS object into AssetDTO.
     * The status and item are loaded.
     *
     * @param ws
     * @return
     */
    public AssetDTO getDTO(AssetWS ws) {
        AssetDTO dto = new AssetDTO();

        dto.setAssetStatus(new AssetStatusBL(ws.getAssetStatusId()).getEntity());
        dto.setItem(new ItemBL(ws.getItemId()).getEntity() );
        if ( null != ws.getEntityId() ) {
        	dto.setEntity(new CompanyDTO(ws.getEntityId()));
        }

        dto.setIdentifier(ws.getIdentifier());
        dto.setNotes(ws.getNotes());

        if(ws.getId() != null) {
            dto.setId(ws.getId());
        }

        if(ws.getOrderLineId() != null) {
            dto.setOrderLine(new OrderLineDTO(ws.getOrderLineId(), null, null, null));
        }

        if(ws.getMetaFields() != null) {
            ItemTypeDTO assetType = dto.getItem().findItemTypeWithAssetManagement();
            if(null != assetType){
                MetaFieldHelper.fillMetaFieldsFromWS(assetType.getAssetMetaFields(), dto, ws.getMetaFields());
            }
        }

        dto.setGlobal(ws.isGlobal());
        dto.setEntities(convertToCompanyDTO(ws.getEntities()));
        
        if(ws.getContainedAssetIds() != null) {
            for(Integer assetId :ws.getContainedAssetIds()){
                dto.getContainedAssets().add(das.find(assetId));
            }
        }
        return dto;
    }

    /**
     * List the asset ids linked to category for the given asset identifier.
     *
     * @param itemTypeId
     * @param assetIdentifier
     * @return
     */
    public List<AssetDTO> getAssetForItemTypeAndIdentifier(int itemTypeId, String assetIdentifier) {
        return das.getAssetForItemTypeAndIdentifier(itemTypeId, assetIdentifier);
    }

    /**
     * Count the number of non deleted assets linked to the item.
     *
     * @param itemId
     * @return
     */
    public int countAssetsForItem(int itemId) {
        return das.countAssetsForItem(itemId);
    }

    /**
     * Return ids for all non deleted assets linked to the category.
     *
     * @param categoryId ItemTypeDTO id
     * @return
     */
    public Integer[] getAssetsForCategory(Integer categoryId) {
        List<Integer> ids = das.getAssetsForCategory(categoryId);
        return ids.toArray(new Integer[ids.size()]);
    }

    /**
     * Return ids for all non deleted assets linked to the item.
     *
     * @param itemId  ItemDTO id
     * @return
     */
    public Integer[] getAssetsForItem(Integer itemId) {
        List<Integer> ids =  das.getAssetsForItem(itemId);
        return ids.toArray(new Integer[ids.size()]);
    }


    /**
     * Unlink all assets associated with the order and change their status back to the default status.
     * Fire AssetUpdatedEvents for all assets changed.
     *
     * @param orderId       PurchaseOrderDTO id
     * @param executorId    UserDTO id of person making the change
     */
    public void unlinkAssets(Integer orderId, Integer executorId) {
        /*
        We are loading the order and looping through the lines in order to avoid having duplicate instances of
        the assets in the session - which may happen if we search for them
         */
        //load the order (probably get instance from the session)
        OrderDTO order = new OrderBL(orderId).getDTO();

        AssetStatusBL assetStatusBL = new AssetStatusBL();

        //events we need to fire if we change the status
        List<AbstractAssetEvent> events = new ArrayList<AbstractAssetEvent>();

	    OrderBL orderBL = new OrderBL();
        for(OrderLineDTO orderLineDTO : order.getLines()) {
            for(AssetDTO asset : orderLineDTO.getAssets()) {
                //find the default status for the item
                AssetStatusDTO defaultStatus = assetStatusBL.findDefaultStatusForItem(asset.getItem().getId());
	            orderBL.removeAssetFromOrderLine(orderLineDTO, executorId, defaultStatus, asset, events, null);
            }
        }

        //process all the events
        for(AbstractAssetEvent event: events) {
            EventManager.process(event);
        }
    }

    /**
     * Change the asset status. The method returns the event that must be fired for this change.
     */
    public AssetUpdatedEvent changeAssetStatus(AssetDTO dto, AssetStatusDTO newStatus, UserDTO assignedTo, Integer executorId) {
        AssetUpdatedEvent event = new AssetUpdatedEvent(dto.getItem().getEntityId(), dto, dto.getAssetStatus(), assignedTo, executorId);
        dto.setAssetStatus(newStatus);
        return event;
    }


    /**
     * Assets with same identifier may not exist for the same category
     * @param dto
     * @throws SessionInternalError if we found another object with the same identifier
     */
	public void checkForDuplicateIdentifier(AssetDTO dto)
			throws SessionInternalError {
		ItemTypeDTO itemType = dto.getItem().findItemTypeWithAssetManagement();
		List<AssetDTO> assets = getAssetForItemTypeAndIdentifier(itemType.getId(), dto.getIdentifier());

		assets= assets.stream().filter( it -> it.getId() != dto.getId()).collect(Collectors.toList());
		
		if ( assets.size() > 0 ) {
			throw new SessionInternalError(
					"An asset with the same identifier already exists",
					new String[] { "AssetWS,identifier,asset.validation.duplicate.identifier" });
		}
	}

    /**
     * Search assets based on search criteria.
     *
     * @see AssetDAS#findAssets(int, com.sapienter.jbilling.server.util.search.SearchCriteria)
     *
     * @param productId
     * @param criteria
     * @return
     */
    public AssetSearchResult findAssets(int productId, SearchCriteria criteria) {
        return das.findAssets(productId, criteria);
    }

    /**
     * Check if any of the contained assets already belong to a group or are linked to an order.
     *
     * @param containedAssets
     * @throws SessionInternalError
     */
    public void checkContainedAssets(Collection<AssetDTO> containedAssets, int assetGroupId) throws SessionInternalError {
        for(AssetDTO containedAsset : containedAssets) {
            if(containedAsset.getOrderLine() != null) {
                throw new SessionInternalError("The asset ["+containedAsset.getIdentifier()+"] is already linked to an order",
                        new String[] {"AssetWS,containedAssets,asset.validation.order.linked,"+containedAsset.getIdentifier()});
            }

            if(containedAsset.getGroup() != null && containedAsset.getGroup().getId() != assetGroupId) {
                throw new SessionInternalError("The asset ["+containedAsset.getIdentifier()+"] is already part of an asset group.",
                        new String[] {"AssetWS,containedAssets,asset.validation.group.linked,"+containedAsset.getIdentifier()});
            }
        }
    }

    /**
     * Merge the collection of new {@code containedAssets} into the dto's set of containted assets.
     *
     * @param dto
     * @param containedAssets
     */
    public List<Event> mergeContainedAssets(AssetDTO dto, Collection<AssetDTO> containedAssets, Integer executorId) {
        AssetStatusBL assetStatusBL = new AssetStatusBL();

        Set<AssetDTO> currentAssets =  dto.getContainedAssets();
        Map currentAssetMap = new HashMap(currentAssets.size() * 2);

        //events we need to fire if we change the status
        List<Event> events = new ArrayList<Event>();

        //collect the current assets
        for(AssetDTO currentAsset : currentAssets) {
            currentAssetMap.put(currentAsset.getId(), currentAsset);
        }

        AssetStatusDTO memberOfGroup = assetStatusBL.getMemberOfGroupStatus();

        //loop through the new assets
        for(AssetDTO assetDTO : containedAssets) {
            if(currentAssetMap.remove(assetDTO.getId()) == null) {
                dto.addContainedAsset(assetDTO);
                //change status and create the event
                events.add(changeAssetStatus(assetDTO, memberOfGroup, null, executorId));
            }
        }

        //now unlink removed assets
        for(AssetDTO assetDTO : (Collection<AssetDTO>)currentAssetMap.values()) {
            //find the default status for the item
            AssetStatusDTO defaultStatus = assetStatusBL.findDefaultStatusForItem(assetDTO.getItem().getId());

            //change status and create the event
            events.add(changeAssetStatus(assetDTO, defaultStatus, null, executorId));

            dto.removeContainedAsset(assetDTO);
        }

        return events;
    }

    /**
     * Find the asset for the given identifier belonging to the product.
     *
     * @param assetId
     * @param itemId
     * @return
     */
    public AssetDTO getForItemAndIdentifier(String assetId, int itemId) {
        return das.getForItemAndIdentifier(assetId, itemId);
    }

    public AssetWS[] findAssetsByProductCode(String productCode, Integer companyId){
        List<AssetDTO> assetList=das.findAssetByProductCode(productCode, companyId);
        List<AssetWS> assetWSList=new ArrayList<AssetWS>();
        for (AssetDTO assetDTO:assetList){
            assetWSList.add(getWS(assetDTO));
        }
        return assetWSList.toArray(new AssetWS[assetWSList.size()]);

    }

    public AssetWS[] findAssetsByProductCode(String productCode, Integer assetStatusId, Integer companyId){
        List<AssetDTO> assetList=das.findAssetByProductCodeAndAssetStatusId(productCode, assetStatusId, companyId);
        List<AssetWS> assetWSList=new ArrayList<AssetWS>();
        for (AssetDTO assetDTO:assetList){
            assetWSList.add(getWS(assetDTO));
        }
        return assetWSList.toArray(new AssetWS[assetWSList.size()]);
    }

    public AssetStatusDTOEx[] findAssetStatuses(String identifier){
        AssetDTO assetDTO=das.getAssetByIdentifier(identifier);
        List<AssetStatusDTOEx> assetStatusWSList=new ArrayList<AssetStatusDTOEx>();
        if (assetDTO!=null){
            Set<ItemTypeDTO> itemTypeDTOs=assetDTO.getItem().getItemTypes();
            for (ItemTypeDTO itemTypeDTO : itemTypeDTOs){
                if (itemTypeDTO.getAllowAssetManagement()==1){
                    Set<AssetStatusDTO> assetStatusDTOs=itemTypeDTO.getAssetStatuses();
                    for (AssetStatusDTO assetStatusDTO:assetStatusDTOs){
                        assetStatusWSList.add(AssetStatusBL.getWS(assetStatusDTO));
                    }
                    break;
                }
            }

        }
        return assetStatusWSList.toArray(new AssetStatusDTOEx[assetStatusWSList.size()]);
    }

    public AssetWS findAssetByProductCodeAndIdentifier(String productCode, String identifier, Integer companyId){
        AssetDTO assetDTO=das.findAssetByProductCodeAndIdentifier(productCode, identifier, companyId);
        if (assetDTO!=null){
            return getWS(assetDTO);
        }
        return null;
    } 
    
    public static Set<CompanyDTO> convertToCompanyDTO(List<Integer> entities) {
    	Set<CompanyDTO> childsEntites = new HashSet<CompanyDTO>(0);
    	CompanyDAS das = new CompanyDAS();
    	for(Integer entity : entities) {
    		childsEntites.add(das.find(entity));
    	}
		return childsEntites;
    }

}

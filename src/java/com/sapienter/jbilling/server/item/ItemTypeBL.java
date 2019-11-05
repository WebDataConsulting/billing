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

package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.DescriptionBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;

import java.util.*;

public class ItemTypeBL {
    private static final FormatLogger LOG = new FormatLogger(ItemTypeBL.class);

    private ItemTypeDAS itemTypeDas = null;
    private ItemTypeDTO itemType = null;
    private Integer callerCompanyId = null;
    private EventLogger eLogger = null;
    
    public ItemTypeBL(Integer itemTypeId, Integer callerCompanyId)  {
        init();
        set(itemTypeId);
        this.callerCompanyId = callerCompanyId;
    }

    public ItemTypeBL(Integer itemTypeId)  {
        init();
        set(itemTypeId);
    }

    public ItemTypeBL() {
        init();
    }
    
    private void init() {
        eLogger = EventLogger.getInstance();        
        itemTypeDas = new ItemTypeDAS();
    }

    public ItemTypeDTO getEntity() {
        return itemType;
    }
    
    public void set(Integer id) {
        itemType = itemTypeDas.find(id);
    }

    public void setCallerCompanyId(Integer callerCompanyId) {
        this.callerCompanyId = callerCompanyId;
    }

    public void create(ItemTypeDTO dto) {
        itemType = new ItemTypeDTO();
        itemType.setEntity(dto.getEntity());
        itemType.setEntities(dto.getEntities());
        itemType.setOrderLineTypeId(dto.getOrderLineTypeId());
        itemType.setDescription(dto.getDescription());
        itemType.setAllowAssetManagement(dto.getAllowAssetManagement());
        itemType.setAssetIdentifierLabel(dto.getAssetIdentifierLabel());
        itemType.setGlobal(dto.isGlobal());
        
        itemType.setParent(dto.getParent());
        
        itemType.setOnePerOrder(dto.isOnePerOrder());
        itemType.setOnePerCustomer(dto.isOnePerCustomer());
        
        itemType = itemTypeDas.save(itemType);

        itemType.addAssetStatuses(dto.getAssetStatuses());

        //add the meta fields
        if(dto.getAssetMetaFields().size() > 0) {
            MetaFieldBL metaFieldBL = new MetaFieldBL();
            for(MetaField metaField : dto.getAssetMetaFields()) {
                itemType.addAssetMetaField(metaFieldBL.create(metaField));
            }
        }

        updateMetaFieldsWithValidation(dto);

        itemTypeDas.flush();
    }
    
    public void update(Integer executorId, ItemTypeDTO dto) 
            throws SessionInternalError {
        eLogger.audit(executorId, null, ServerConstants.TABLE_ITEM_TYPE,
                itemType.getId(), EventLogger.MODULE_ITEM_TYPE_MAINTENANCE, 
                EventLogger.ROW_UPDATED, null,  
                itemType.getDescription(), null);

        itemType.setDescription(dto.getDescription());
        itemType.setOrderLineTypeId(dto.getOrderLineTypeId());
        itemType.setParent(dto.getParent());
        itemType.setAllowAssetManagement(dto.getAllowAssetManagement());
        itemType.setAssetIdentifierLabel(dto.getAssetIdentifierLabel());
        itemType.setEntities(dto.getEntities());
        itemType.setGlobal(dto.isGlobal());
        
        itemType.setOnePerCustomer(dto.isOnePerCustomer());
        itemType.setOnePerOrder(dto.isOnePerOrder());
        
        // Set or clear entity field depending upon whether this field was set or not
        itemType.setEntity(dto.getEntity());
        
        //merge the asset statuses
        mergeStatuses(itemType, dto.getAssetStatuses());
        //merge the meta fields
        Collection<MetaField> metaFields = mergeAssetMetaFields(itemType, dto.getAssetMetaFields());

        updateMetaFieldsWithValidation(dto);

        itemTypeDas.flush();

        //delete all the metafields not linked to the item type anymore
        MetaFieldBL metaFieldBL = new MetaFieldBL();
        for(MetaField mf: metaFields) {
            metaFieldBL.delete(mf.getId());
        }

    }
    
    public void delete(Integer executorId) {
        if (isInUse()) {
            throw new SessionInternalError("Cannot delete a non-empty item type, remove items before deleting.");
        }

        LOG.debug("Deleting item type: %s", itemType.getId());

        for(AssetStatusDTO status : itemType.getAssetStatuses()) {
            status.setItemType(null);
        }

        Integer itemTypeId = itemType.getId();
        itemTypeDas.delete(itemType);
        itemTypeDas.flush();
        itemTypeDas.clear();

        // now remove all the descriptions 
        DescriptionBL desc = new DescriptionBL();
        desc.delete(ServerConstants.TABLE_ITEM_TYPE, itemTypeId);

        eLogger.audit(executorId, null, ServerConstants.TABLE_ITEM_TYPE, itemTypeId,
                EventLogger.MODULE_ITEM_TYPE_MAINTENANCE, 
                EventLogger.ROW_DELETED, null, null,null);

    }   

    public boolean isInUse() {
        return itemTypeDas.isInUse(itemType.getId());
    }

    /**
     * Gets the internal category for plan subscription items. If the category does not
     * exist, it will be created.
     *
     * @param entityId entity id
     * @return plan category
     */
    public ItemTypeDTO getInternalPlansType(Integer entityId) {
        return itemTypeDas.getCreateInternalPlansType(entityId);
    }
    
    /**
     * Returns all item types by entity Id, or an empty array if none found.
     *
     * @return array of item types, empty if none found.
     */
    public ItemTypeWS[] getAllItemTypesByEntity(Integer entityId) {
        return toArray(new ItemTypeDAS().findByEntityId(entityId));
    }

    /**
     * Checks to see a category with the same description already exists.
     * @param description Description to use to find an existent category. The search is done case insensitive, for example "calls"
     * and "Calls" are considered the same.
     * @return <b>true</b> if another category exists. <b>false</b> if no category with the same description exists.
     */
    public boolean exists(List<Integer> entities, String description) {
        if (description == null) {
            LOG.error("exists is being call with a null description");
            return true;
        }
        ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
    
        for (Integer entity : entities) {
        	if (itemTypeDAS.findByDescription(entity, description) != null) {
                return true;
            } 
        }
        return false;
    }

    /**
     * Method added to fix #7890 - Duplicate Global categories are possible.
     * Checks if a global category with the same description already exists.
     * @param description Description to use to find an existent category.
     * @return <b>true</b> if another category exists. <b>false</b> if no category with the same description exists.
     */
    public boolean existsGlobal(Integer entity, String description) {
        if (description == null) {
            LOG.error("exists is being called with a null description");
            return true;
        }
        ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
    
        if (itemTypeDAS.findByGlobalDescription(entity, description) != null) {
        	return true;
        } 
                
        return false;
    }
    
    /**
     * Returns immediate children categories for given parent category.
     *
     * @param itemTypeId - parent category id
     * @return returns all the children categories for the given parent category
     */
    public ItemTypeWS[] getChildItemCategories(Integer itemTypeId) {
        return toArray(new ItemTypeDAS().getChildItemCategories(itemTypeId));
    }

    public ItemTypeWS[] getItemCategoriesByPartner(String partner, boolean parentCategoriesOnly) {
        return toArray(new ItemTypeDAS().getItemCategoriesByPartner(partner, parentCategoriesOnly));
    }

    private ItemTypeWS[] toArray(List<ItemTypeDTO> results) {
        if (null == results) return null;
        ItemTypeWS[] types = new ItemTypeWS[results.size()];

        int index = 0;
        for (ItemTypeDTO type : results) {
            types[index++] = toWS(type);
        }

        return types;
    }

    /**
     * Merge the collection of AssetStatusDTO into the ItemTypeDTO. Change the properties of the statuses
     * linked to {@code itemType}.
     * Add new statuses in ${@code statusDTOs} and delete statuses not in {@code statusDTOs}.
     * After the merge {@code itemTypeDTO} will have the statuses as they are provided in {@code statusDTOs}.
     * Deletes are only logical deletes, and status ids are preserved.
     *
     * @param itemTypeDTO   Type containing old statuses.
     * @param statusDTOs    New status objects.
     */
    public void mergeStatuses(ItemTypeDTO itemTypeDTO, Collection<AssetStatusDTO> statusDTOs) {
        AssetStatusBL assetStatusBL = new AssetStatusBL();
        Set<AssetStatusDTO> currentStatuses =  itemTypeDTO.getAssetStatuses();
        Map currentStatusMap = new HashMap(currentStatuses.size() * 2);

        //collect the current statuses
        for(AssetStatusDTO dto : currentStatuses) {
            currentStatusMap.put(dto.getId(), dto);
        }

        //loop through the new statuses
        for(AssetStatusDTO statusDTO : statusDTOs) {
            //We do not update internal statuses
            if(statusDTO.getIsInternal() == 1) continue;

            //if it is a saved status update the current object
            if(statusDTO.getId() > 0) {
                assetStatusBL.mergeBasicProperties((AssetStatusDTO) currentStatusMap.remove(statusDTO.getId()), statusDTO);
            //else it is a new status and we must create it
            } else {
                statusDTO.setDeleted(0);
                itemTypeDTO.addAssetStatus(statusDTO);
            }
        }

        //now delete statuses not linked to the type anymore
        for(AssetStatusDTO statusDTO : (Collection<AssetStatusDTO>)currentStatusMap.values()) {
            statusDTO.setDeleted(1);
        }
    }

    /**
     * Find the ItemType which has asset management enabled for the given itemId.
     *
     * @param itemId
     * @return
     */
    public ItemTypeDTO findItemTypeWithAssetManagementForItem(int itemId) {
        return itemTypeDas.findItemTypeWithAssetManagementForItem(itemId);
    }

    /**
     * Merge the collection of MetaField into the ItemTypeDTO. Change the properties of the metafields
     * linked to {@code itemTypeDTO}.
     * Add new metafields in ${@code metaFields} and delete metafields not in {@code metaFields}.
     * After the merge {@code itemTypeDTO} will have the metafields as they are provided in {@code metaFields}.
     *
     * @param itemTypeDTO   Type containing old statuses.
     * @param metaFields    New assetMetaField objects.
     * @return list of MetaField ids that was removed. They must be deleted by calling MetaFieldBL.delete
     */
    public Collection<MetaField> mergeAssetMetaFields(ItemTypeDTO itemTypeDTO, Collection<MetaField> metaFields) {
        MetaFieldBL metaFieldBL = new MetaFieldBL();
        Set<MetaField> currentMetaFields =  itemTypeDTO.getAssetMetaFields();
        Map currentMetaFieldMap = new HashMap(currentMetaFields.size() * 2);

        //collect the current meta fields
        for(MetaField dto : currentMetaFields) {
            currentMetaFieldMap.put(dto.getId(), dto);
        }

        //loop through the new metaFields
        for(MetaField metaField : metaFields) {
            //if it is a saved status update the current object
            if(metaField.getId() > 0) {
                MetaField mergedField = (MetaField) currentMetaFieldMap.remove(metaField.getId());
                mergeBasicProperties(mergedField, metaField);
                metaFieldBL.update(mergedField);
            } else {
                //else it is a new meta field and we must create it
                itemTypeDTO.addAssetMetaField(metaFieldBL.create(metaField));
            }
        }

        //now delete statuses not linked to the type anymore
        for(MetaField dto : (Collection<MetaField>)currentMetaFieldMap.values()) {
            itemTypeDTO.removeMetaField(dto);
        }
        return currentMetaFieldMap.values();
    }

    /**
     * Copy the properties from source to destination which are not links to other persistent objects.
     *
     * @param destination
     * @param source
     */
    private void mergeBasicProperties(MetaField destination, MetaField source) {
        destination.setName(source.getName());
        destination.setPrimary(source.getPrimary());
        destination.setValidationRule(source.getValidationRule());
        destination.setDataType(source.getDataType());
        destination.setDefaultValue(source.getDefaultValue());
        destination.setDisabled(source.isDisabled());
        destination.setMandatory(source.isMandatory());
        destination.setDisplayOrder(source.getDisplayOrder());
        destination.setFieldUsage(source.getFieldUsage());
    }


    /**
     * Find all ItemTypes linked to products which are linked to the ItemType identified by id
     *
     * @param id  ItemType id
     * @return list of ItemType ids
     */
    public List<Integer> findAllTypesLinkedThroughProduct(Integer id) {
        return itemTypeDas.findAllTypesLinkedThroughProduct(id);
    }
    
    /**
     * Get all item categories for the given entity and its child entity if corresponding.
     */
    public ItemTypeWS[] getItemCategoriesByEntity(Integer entityId) {
        CompanyDAS das = new CompanyDAS();

        boolean isRoot = das.isRoot(entityId);
        List<Integer> allCompanies = das.getChildEntitiesIds(entityId);
        allCompanies.add(entityId);

        List<ItemTypeDTO> itemTypes = itemTypeDas.findItemCategories(entityId, allCompanies, isRoot);

        List<ItemTypeWS> list = new ArrayList<ItemTypeWS>();
        for (ItemTypeDTO item : itemTypes) {
            list.add(toWS(item));
        }

        return list.toArray(new ItemTypeWS[list.size()]);
    }

    public void updateMetaFieldsWithValidation(Integer accountTypeId, ItemTypeDTO dto) {

        if(itemType.isGlobal()){
            if(itemType.getEntity() != null) {
                List<Integer> allEntities = new CompanyDAS().getChildEntitiesIds(dto.getEntityId());
                allEntities.add(dto.getEntityId());
                for(Integer id: allEntities){
                    itemType.updateMetaFieldsWithValidation(id, accountTypeId, dto);
                }
            }
        } else if (new CompanyDAS().isRoot(callerCompanyId)){
            // only root company should be able to set meta-field values for other companies
            for(CompanyDTO companyDTO : dto.getEntities()) {
                itemType.updateMetaFieldsWithValidation(companyDTO.getId(), accountTypeId, dto);
            }
        }else {
            // if not global category and caller company is not root then set only meta-fields visible to the caller company
            itemType.updateMetaFieldsWithValidation(callerCompanyId, accountTypeId, dto);
        }
    }

    public void updateMetaFieldsWithValidation(ItemTypeDTO dto){
        updateMetaFieldsWithValidation(null, dto);
    }

    public static void fillMetaFieldsFromWS(ItemTypeDTO dto, ItemTypeWS itemType){

        if(dto.isGlobal()){
            if(dto.getEntity() != null) {

                List<Integer> allEntities = new CompanyDAS().getChildEntitiesIds(dto.getEntityId());
                allEntities.add(dto.getEntityId());
                for(Integer id: allEntities){
                    MetaFieldBL.fillMetaFieldsFromWS(id, dto, itemType.getMetaFieldsMap().get(id));
                }
            }
        } else {
            for(Integer id : itemType.getEntities()) {
                MetaFieldBL.fillMetaFieldsFromWS(id, dto, itemType.getMetaFieldsMap().get(id));
            }
        }
    }

    public static boolean isChildMetaFieldPresent(ItemTypeWS itemType, Integer callerCompanyId){
        for(Integer companyId : itemType.getMetaFieldsMap().keySet()){
            if(!companyId.equals(callerCompanyId) && (itemType.getMetaFieldsMap().get(companyId).length > 0)){
                return true;
            }
        }
        return false;
    }

	public ItemTypeDTO getById(Integer itemTypeId, Integer entityId, boolean checkParentCompany) {

		if (null == itemTypeId || null == entityId) {
			throw new IllegalArgumentException("Arguments itemTypeId and entityId can not be null");
		}

		Integer parentId = null;
		if (checkParentCompany) {
			CompanyDAS companyDAS = new CompanyDAS();
			CompanyDTO parent = companyDAS.find(entityId).getParent();
			parentId = null != parent ? parent.getId() : null;
		}

		ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
		return itemTypeDAS.getById(itemTypeId, entityId, parentId);
	}

	public static ItemTypeWS toWS(ItemTypeDTO dto){
		ItemTypeWS ws = new ItemTypeWS();
		ws.setId(dto.getId());

		if (null != dto.getEntity()) {
			ws.setEntityId(dto.getEntity().getId());
		}

		ws.setInternal(dto.isInternal());
		ws.setDescription(dto.getDescription());
		ws.setOrderLineTypeId(dto.getOrderLineTypeId());
		ws.setParentItemTypeId((null != dto.getParent()) ? dto.getParent().getId() : null);
		ws.setAllowAssetManagement(dto.getAllowAssetManagement());
		ws.setAssetIdentifierLabel(dto.getAssetIdentifierLabel());

		for(AssetStatusDTO statusDTO : dto.getAssetStatuses()) {
			if(statusDTO.getDeleted() == 0) {
				ws.getAssetStatuses().add(new AssetStatusDTOEx(
						statusDTO.getId(),
						statusDTO.getDescription(),
						statusDTO.getIsDefault(),
						statusDTO.getIsAvailable(),
						statusDTO.getIsOrderSaved(),
						statusDTO.getIsInternal()));
			}
		}

		for(MetaField metaField : dto.getAssetMetaFields()) {
			ws.getAssetMetaFields().add(MetaFieldBL.getWS(metaField));
		}

		return ws;
	}

}

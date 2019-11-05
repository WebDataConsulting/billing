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
package com.sapienter.jbilling.server.metafields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroupDAS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.DescriptionBL;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.audit.EventLogger;

/**
 * Business Logic for meta-field groups.
 *
 * @author Oleg Baskakov
 * @since 18-Apr-2013
 */
public class MetaFieldGroupBL {
	
    private static final FormatLogger LOG = new FormatLogger(MetaFieldGroupBL.class);
    private EventLogger eLogger = null;

    
    private MetaFieldGroup group;
    private MetaFieldGroupDAS mfGroupDas; 
    
    public MetaFieldGroupBL() {
        init();
    }

    public MetaFieldGroupBL(MetaFieldGroup group) {
        this.group = group;
        init();
    }

    public void set(Integer groupId) {
    	group = mfGroupDas.find(groupId);
    }

    private void init() {
        eLogger = EventLogger.getInstance();
        mfGroupDas = new MetaFieldGroupDAS();
    }

    public  MetaFieldGroup getEntity() {
        return group;
    }
    
   

	public Integer save() {
		
		try {
			MetaFieldGroup groupWithSameName = mfGroupDas.getGroupByName(group.getEntity().getId(), group.getEntityType(), group.getDescription());
			
			if(groupWithSameName!=null){
	            throw new SessionInternalError("Exception saving metafield group to database.MetaFieldGroup" +
                        " with same name ["+group.getDescription()+"] is existed in db",
                        new String[] { "MetaFieldGroupWS,mfGroup,cannot.save.metafieldgroup.db.error" });
				
			}
			
			group=mfGroupDas.save(group);
			//group.setDescription(group.getDescription(), CommonConstants.LANGUAGE_ENGLISH_ID);
			
			return group.getId();
		} catch (Exception e) {

            throw new SessionInternalError("Exception saving metafield group to database",
                    e, new String[] { "MetaFieldGroupWS,metafieldgroup,metafieldgroup.db.error.cannot.save" });
		}
	}

	public void update(MetaFieldGroup metaFieldGroup) {
		try {
            MetaFieldGroupDAS das = new MetaFieldGroupDAS();
            MetaFieldGroup group = das.findForUpdate(metaFieldGroup.getId());

            group.setDateUpdated(new Date());
            group.setDisplayOrder(metaFieldGroup.getDisplayOrder());
            group.setDescription(metaFieldGroup.getDescription());

            group.setMetaFields(metaFieldGroup.getMetaFields());
            das.save(group);
		} catch (Exception e) {
            throw new SessionInternalError("Exception when updating metafield group to database",
                    e, new String[] { "MetaFieldGroupWS,mfGroup,cannot.update.metafieldgroup.db.error" });
		}
	}

	public void delete() {
		try {
			mfGroupDas.delete(group);
		} catch (Exception e) {
            throw new SessionInternalError("Exception  deleting metafield group",
                    e, new String[] { "MetaFieldGroupWS,mfGroup,cannot.delete.metafieldgroup.db.error" });
		}
		
	}

    /**
     * Load the list of available meta field groups for the entity and EntityType.
     *
     * @param entityId
     * @param entityType
     * @return
     */
    public List<MetaFieldGroup> getAvailableFieldGroups(Integer entityId, EntityType entityType) {
        return mfGroupDas.getAvailableFieldGroups(entityId, entityType);
    }

    /**
     * Convert a collection of MetaFieldGroup objects to MetaFieldGroupWS[]
     *
     * @param metaFieldGroups
     * @return
     */
    public static MetaFieldGroupWS[] convertMetaFieldGroupsToWS(Collection<MetaFieldGroup> metaFieldGroups) {
        MetaFieldGroupWS[] metaFieldGroupArray = new MetaFieldGroupWS[metaFieldGroups.size()];
        int idx = 0;
        for(MetaFieldGroup metaFieldGroup : metaFieldGroups) {
            metaFieldGroupArray[idx++] = getWS(metaFieldGroup);
        }
        return metaFieldGroupArray;
    }
    public static final AccountInformationTypeWS getAccountInformationTypeWS(MetaFieldGroup dto){
    	AccountInformationTypeWS ws = new AccountInformationTypeWS();
    	if(null != dto){
    		
    		ws.setId(dto.getId());
    		
    		ws.setDateCreated(dto.getDateCreated());
    		ws.setDateUpdated(dto.getDateUpdated());
    		ws.setDisplayOrder(dto.getDisplayOrder());
            ws.setEntityId(dto.getEntity().getId());
    		ws.setEntityType(dto.getEntityType());
    		
    		if(dto.getMetaFields()!=null && dto.getMetaFields().size()>0){
    			Set<MetaFieldWS> tmpMetaFields=new HashSet<MetaFieldWS>();
        		for(MetaField metafield:dto.getMetaFields()){
        			tmpMetaFields.add(MetaFieldBL.getWS(metafield));
        		}
        		ws.setMetaFields(tmpMetaFields.toArray(new MetaFieldWS[tmpMetaFields.size()]));
    		}
    		if(dto.getDescription(ServerConstants.LANGUAGE_ENGLISH_ID)!=null){
	    		List<InternationalDescriptionWS> tmpDescriptions=new ArrayList<InternationalDescriptionWS>(1);
	    		tmpDescriptions.add(DescriptionBL.getInternationalDescriptionWS(dto.getDescriptionDTO(ServerConstants.LANGUAGE_ENGLISH_ID)));
				ws.setDescriptions(tmpDescriptions);
    		}
    		
    	}
    	return ws;
    }
    
    public static final  MetaFieldGroupWS getWS(MetaFieldGroup groupDTO){
    	MetaFieldGroupWS ws = new MetaFieldGroupWS();
    	if(groupDTO!=null){
    		
    		
    		ws.setId(groupDTO.getId());
    		ws.setDateCreated(groupDTO.getDateCreated());
    		ws.setDateUpdated(groupDTO.getDateUpdated());
    		ws.setDisplayOrder(groupDTO.getDisplayOrder());
            ws.setEntityId(groupDTO.getEntity().getId());
    		ws.setEntityType(groupDTO.getEntityType());
    		
    		if(groupDTO.getMetaFields()!=null && groupDTO.getMetaFields().size()>0){
    			Set<MetaFieldWS> tmpMetaFields=new HashSet<MetaFieldWS>();
        		for(MetaField metafield:groupDTO.getMetaFields()){
        			tmpMetaFields.add(MetaFieldBL.getWS(metafield));
        		}
        		ws.setMetaFields(tmpMetaFields.toArray(new MetaFieldWS[tmpMetaFields.size()]));
    		}
    		if(groupDTO.getDescription(ServerConstants.LANGUAGE_ENGLISH_ID)!=null){
	    		List<InternationalDescriptionWS> tmpDescriptions=new ArrayList<InternationalDescriptionWS>(1);
	    		tmpDescriptions.add(DescriptionBL.getInternationalDescriptionWS(groupDTO.getDescriptionDTO(ServerConstants.LANGUAGE_ENGLISH_ID)));
				ws.setDescriptions(tmpDescriptions);
    		}
    		
    	}
    	return ws;
    }
    
    public static final MetaFieldGroup getDTO(MetaFieldGroupWS ws) {

		MetaFieldGroup mfGroup = new MetaFieldGroup();

		mfGroup.setDisplayOrder(ws.getDisplayOrder());
		mfGroup.setEntityType(ws.getEntityType());
		mfGroup.setId(ws.getId());
		try {

            if (ws.getMetaFields() != null) {
                MetaField metaField;
                Set<MetaField> metafieldsDTO = new HashSet<MetaField>();
                for (MetaFieldWS metafieldWS : ws.getMetaFields()) {
                    metaField = new MetaFieldDAS().find(metafieldWS.getId());
                    if (metaField != null) {
                        metafieldsDTO.add(metaField);
                    }
                }
                mfGroup.setMetaFields(metafieldsDTO);
            }
			
			if (ws.getId() > 0) {
				List<InternationalDescriptionWS> descriptions = ws.getDescriptions();
				for (InternationalDescriptionWS description : descriptions) {
					if (description.getLanguageId() != null
							&& description.getContent() != null) {
						if (description.isDeleted()) {
							mfGroup.deleteDescription(description
									.getLanguageId());
						} else {
							mfGroup.setDescription(description.getContent(),
									description.getLanguageId());
						}
					}
				}
			}
		} catch (Exception e) {
		
            throw new SessionInternalError("Exception converting MetaFieldGroupWS to DTO object", e, new String[] {
                    "MetaFieldGroupWS,metafieldGroups,cannot.convert.metafieldgroupws.error" });
            
		}
		return mfGroup;
	}

    
}

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

package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;

import java.util.Collection;
import java.util.Set;

/**
 * RoleBL
 *
 * @author Brian Cowdery
 * @since 03/06/11
 */
public class RoleBL {
    private static final FormatLogger LOG = new FormatLogger(RoleBL.class);

    private RoleDAS roleDas;
    private RoleDTO role;

    public RoleBL() {
        _init();
    }

    public RoleBL(RoleDTO role) {
        _init();
        this.role = role;
    }

    public RoleBL(Integer roleId) {
        _init();
        set(roleId);
    }

    private void _init() {
        this.roleDas = new RoleDAS();
    }

    public void set(Integer roleId) {
        this.role = roleDas.find(roleId);
    }

    public RoleDTO getEntity() {
        return role;
    }

    /**
     * Saves a new role to and sets the BL entity to the newly created role. This method does not
     * save the description or title of a permission. Use {@link #setDescription(Integer, String)} and
     * {@link #setTitle(Integer, String)} to set the international descriptions.
     *
     * @param role role to save
     * @return id of the new role
     */
    public Integer create(RoleDTO role) {
        if (role != null) {
            this.role = roleDas.save(role);
            roleDas.flush();
            return this.role.getId();
        }

        LOG.error("Cannot save a null RoleDTO!");
        return null;
    }
    
    public void validateDuplicateRoleName(String roleName, Integer languageId, Integer companyId) {
        	
    	if (roleName != null && !roleName.trim().isEmpty()) {

    		InternationalDescriptionDAS internationalDescriptionDAS = InternationalDescriptionDAS.getInstance();
	        
    		//check if the description already exists in international_description
    		Collection<InternationalDescriptionDTO> list = 
	        		internationalDescriptionDAS.roleExists(ServerConstants.TABLE_ROLE,
	        										   ServerConstants.PSUDO_COLUMN_TITLE,
	        										   roleName.trim(), 
	        										   languageId,
                                                       companyId);

            if (list != null && !list.isEmpty()){
	        	throw new SessionInternalError("The role already exists with name " + roleName,
	                    new String[]{"RoleDTO,title,validation.error.roleName.already.exists," + roleName});
	        	
	        }
    	}
    	
    }

    /**
     * Deletes this role.
     *
     * Any users that use this role as their primary will be left without a role. It's best to move user
     * out of this role before deleting to ensure that the user doesn't experience an interruption in
     * service (by having no role).
     */
    public void delete() {
        if (role != null) {
            role.getBaseUsers().clear();

            roleDas.delete(role);
            roleDas.flush();

        } else {
            LOG.error("Cannot delete, RoleDTO not found or not set!");
        }
    }
    
    public void updateRoleType(int roleTypeId) {
    	
    	if (role != null) {
    		role.setRoleTypeId(roleTypeId);
    		roleDas.save(role);
    		 roleDas.flush();
    	} else {
    		LOG.error("Cannot delete, RoleDTO not found or not set!");
    	}
    }

    public void setDescription(Integer languageId, String description) {
        this.role.setDescription("description", languageId, description);
    }

    public void setTitle(Integer languageId, String title) {
        this.role.setDescription("title", languageId, title);
    }
    
    public void deleteDescription(Integer languageId) {
    	this.role.deleteDescription(languageId);
    }
    
    public void deleteTitle(Integer languageId) {
    	this.role.deleteDescription(ServerConstants.PSUDO_COLUMN_TITLE, languageId);
    }
}

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
import com.sapienter.jbilling.server.item.db.*;

import java.util.*;

public class ItemDependencyBL {
    private static final FormatLogger LOG = new FormatLogger(ItemDependencyBL.class);

    private ItemDependencyDAS das = null;
    private ItemDependencyDTO entity = null;

    public ItemDependencyBL(Integer itemTypeId)  {
        init();
        set(itemTypeId);
    }

    public ItemDependencyBL() {
        init();
    }
    
    private void init() {
        das = new ItemDependencyDAS();
    }

    public ItemDependencyDTO getEntity() {
        return entity;
    }
    
    public void set(Integer id) {
        entity = das.find(id);
    }

    /**
     * Convert ItemDependencyDTOEx[] to Set of ItemDependencyDTO
     * @param dependencies
     * @return
     */
    public static Set<ItemDependencyDTO> toDto(ItemDependencyDTOEx[] dependencies, ItemDTO item) {
        if(dependencies == null) {
            return new HashSet<ItemDependencyDTO>(0);
        }
        Set<ItemDependencyDTO> dependencyDTOs = new HashSet<ItemDependencyDTO>(dependencies.length * 2);
        for(ItemDependencyDTOEx dtoEx : dependencies) {
            ItemDependencyDTO dto = toDto(dtoEx);
            dto.setItem(item);
            dependencyDTOs.add(dto);
        }
        return dependencyDTOs;
    }

    /**
     * Convert a single ItemDependencyDTOEx to ItemDependencyDTO
     * @param dtoEx
     * @return
     */
    public static ItemDependencyDTO toDto(ItemDependencyDTOEx dtoEx) {
        ItemDependencyDTO dto = create(dtoEx.getType());
        if(dtoEx.getId() != null) dto.setId(dtoEx.getId());
        if(dtoEx.getItemId() != null) dto.setItem(new ItemDTO(dtoEx.getItemId()));
        dto.setMinimum(dtoEx.getMinimum());
        dto.setMaximum(dtoEx.getMaximum());
        dto.setDependentObject(findDependent(dtoEx.getType(), dtoEx.getDependentId()));

        return dto;
    }

    /**
     * Create a new subclass of ItemDependencyDTO for the type.
     *
     * @param type
     * @return
     */
    private static ItemDependencyDTO create(ItemDependencyType type) {
        switch (type) {
            case ITEM: return new ItemDependencyOnItemDTO();
            case ITEM_TYPE: return new ItemDependencyOnItemTypeDTO();
            default: throw new SessionInternalError("ItemDependencyDTOEx.Type is "+type);
        }
    }

    /**
     * Load the instance of the dependent object specified by the id for the given type.
     *
     * @param type
     * @param id
     * @return
     */
    private static Object findDependent(ItemDependencyType type, int id) {
        switch (type) {
            case ITEM: return new ItemBL(id).getEntity();
            case ITEM_TYPE: return new ItemTypeBL(id).getEntity();
            default: throw new SessionInternalError("ItemDependencyDTOEx.Type is "+type);
        }
    }

    /**
     * Convert a set of ItemDependencyDTO to ItemDependencyDTOEx[]
     *
     * @param dependencies
     * @return
     */
    public static ItemDependencyDTOEx[] toWs(Set<ItemDependencyDTO> dependencies) {
        ItemDependencyDTOEx[] ws = new ItemDependencyDTOEx[dependencies.size()];
        int idx=0;

        for(ItemDependencyDTO dependencyDTO : dependencies) {
            ws[idx++] = toWs(dependencyDTO);
        }
        return ws;
    }

    /**
     * Convert a ItemDependencyDTO to ItemDependencyDTOEx
     * @param dep
     * @return
     */
    public static ItemDependencyDTOEx toWs(ItemDependencyDTO dep) {
        ItemDependencyDTOEx ws = new ItemDependencyDTOEx();
        ws.setDependentId(dep.getDependentObjectId());
        ws.setId(dep.getId());
        ws.setItemId(dep.getItem().getId());
        ws.setDependentDescription(dep.getDependentDescription());
        ws.setMaximum(dep.getMaximum());
        ws.setMinimum(dep.getMinimum());
        ws.setType(dep.getType());

        return ws;
    }
}

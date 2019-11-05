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
package com.sapienter.jbilling.server.item.db;

import com.sapienter.jbilling.server.item.ItemDependencyType;

import javax.persistence.*;

/**
 * Class represents a dependency of an ItemDTO on an ItemTypeDTO.
 *
 */
@Entity
@DiscriminatorValue("item_type")
public class ItemDependencyOnItemTypeDTO extends ItemDependencyDTO {

    private ItemTypeDTO dependent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_item_type_id")
    public ItemTypeDTO getDependent() {
        return dependent;
    }

    public void setDependent(ItemTypeDTO dependent) {
        this.dependent = dependent;
    }

    @Override
    public void setDependentObject(Object dependent) {
        setDependent((ItemTypeDTO) dependent);
    }

    @Override
    @Transient
    public Integer getDependentObjectId() {
        return dependent.getId();
    }

    @Override
    @Transient
    public ItemDependencyType getType() {
        return ItemDependencyType.ITEM_TYPE;
    }

    @Transient
    @Override
    public String getDependentDescription() {
        return dependent.getDescription();
    }
}



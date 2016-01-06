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
 * Class represents a dependency of an ItemDTO on another ItemDTO.
 *
 */
@Entity
@DiscriminatorValue("item")
public class ItemDependencyOnItemDTO extends ItemDependencyDTO {

    private ItemDTO dependent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_item_id")
    public ItemDTO getDependent() {
        return dependent;
    }

    public void setDependent(ItemDTO dependent) {
        this.dependent = dependent;
    }

    @Override
    public void setDependentObject(Object dependent) {
        setDependent((ItemDTO) dependent);
    }

    @Transient
    @Override
    public Integer getDependentObjectId() {
        return dependent.getId();
    }

    @Transient
    @Override
    public String getDependentDescription() {
        return dependent.getDescription();
    }

    @Override
    @Transient
    public ItemDependencyType getType() {
        return ItemDependencyType.ITEM;
    }
}



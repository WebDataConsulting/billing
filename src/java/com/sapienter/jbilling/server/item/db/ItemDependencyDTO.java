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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Class represents a dependency of an ItemDTO on other products.
 * It specifies a minimum and maximum quantity of the dependent product which must be in the same order hierarchy.
 *
 */
@Entity
@TableGenerator(
        name = "item_dependency_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "item_dependency",
        allocationSize = 100
)

@NamedQueries({
        @NamedQuery(name = "ItemDependencyOnItemDTO.countForDependItem",
                query = "select count(a.id) from ItemDependencyOnItemDTO a where a.dependent.id = :item_id ")
})

@Table(name = "item_dependency")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
public abstract class ItemDependencyDTO implements Serializable {

    private int id;
    private ItemDTO item;
    private Integer minimum;
    private Integer maximum;

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "item_dependency_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public ItemDTO getItem() {
        return item;
    }

    public void setItem(ItemDTO item) {
        this.item = item;
    }

    @Column (name = "min")
    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    @Column (name = "max")
    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public abstract void setDependentObject(Object dependent);

    @Transient
    public abstract Object getDependent();

    @Transient
    public abstract Integer getDependentObjectId();

    @Transient
    public abstract String getDependentDescription();

    @Transient
    public abstract ItemDependencyType getType();
}



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

import com.sapienter.jbilling.server.user.db.CompanyDTO;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import java.io.Serializable;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

@Entity
@TableGenerator(
        name = "entity_item_price_map_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "entity_item_price_map",
        allocationSize = 100
)
@Table(name = "entity_item_price_map")
public class EntityItemPrice implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -649387332758012893L;

	@Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "entity_item_price_map_GEN")
    @Column(name = "id", unique = true, nullable = false)
	private int id;
	
	@ManyToOne
	@JoinColumn(name = "entity_id", nullable = true, updatable = false)
	private CompanyDTO entity;
	
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private ItemDTO item;
	
	public EntityItemPrice() {}
	
	public EntityItemPrice (ItemDTO item, CompanyDTO entity) {
		this.item = item;
		this.entity = entity;
	}
	
	public ItemDTO getItem () {
		return this.item;
	}
	
	public CompanyDTO getEntity () {
		return this.entity;
	}
	
}



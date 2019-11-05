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
package com.sapienter.jbilling.server.metafields.db;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.db.AbstractDescription;

/**
 * A meta-field group that is associated with a particular entity type.
 * 
 * @author Pance Isajeski, Oleg Baskakov
 * @since 18-Apr-2013
 */
@Entity
@Table(name = "meta_field_group")
@TableGenerator(
	    name = "metafield_group_GEN",
	    table = "jbilling_seqs",
	    pkColumnName = "name",
	    valueColumnName = "next_id",
	    pkColumnValue = "meta_field_group",
	    allocationSize = 10
	)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("METAFIELD_GROUP")
public class MetaFieldGroup extends AbstractDescription implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "metafield_group_GEN")
    @Column(name = "id", unique = true, nullable = false)
	private int id;
	
	@Generated(GenerationTime.INSERT)
	@Column(name = "date_created")
	private Date dateCreated;
	
	@Generated(GenerationTime.ALWAYS)
	@Column(name = "date_updated", nullable=true)
	private Date dateUpdated;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private CompanyDTO entity;
	
	@Column(name = "display_order")
    private Integer displayOrder;

	
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 25)
	private EntityType entityType;
	
    @Version
    @Column(name="optlock")
	private Integer versionNum;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "metafield_group_meta_field_map",
            joinColumns = @JoinColumn(name = "metafield_group_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
	private Set<MetaField> metaFields = new HashSet<MetaField>(0);

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public Date getDateUpdated() {
		return dateUpdated;
	}

	public void setDateUpdated(Date dateUpdated) {
		this.dateUpdated = dateUpdated;
	}


	public EntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	public Set<MetaField> getMetaFields() {
		return metaFields;
	}

	public void setMetaFields(Set<MetaField> metaFields) {
		this.metaFields = metaFields;
	}
	
    @Transient
    protected String getTable() {
        return ServerConstants.TABLE_METAFIELD_GROUP;
    }

    public String toString() {
        return "MetaField Group{" +
               "id=" + id +
//               ", name='" + entityType + '\'' +
               ", entityType=" + entityType +

               '}';
    }

	public CompanyDTO getEntity() {
		return entity;
	}

	public void setEntity(CompanyDTO entity) {
		this.entity = entity;
	}

	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	public Integer getVersionNum() {
		return versionNum;
	}

	public void setVersionNum(Integer versionNum) {
		this.versionNum = versionNum;
	}
}

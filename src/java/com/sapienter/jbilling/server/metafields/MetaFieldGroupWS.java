package com.sapienter.jbilling.server.metafields;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

public class MetaFieldGroupWS implements Serializable {

	private int id;
	private Date dateCreated;
	private Date dateUpdated;
    private Integer entityId;
	private EntityType entityType;
	private Integer displayOrder;
	
    @NotEmpty(message = "validation.error.notnull")
    @Valid
	private MetaFieldWS[] metaFields;
	
    @NotEmpty(message = "validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = ListUtils.lazyList(
            new ArrayList<InternationalDescriptionWS>(), FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));

    public MetaFieldGroupWS(){
    }

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

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public EntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

    public MetaFieldWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldWS[] metaFields) {
        this.metaFields = metaFields;
    }

	

	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	public List<InternationalDescriptionWS> getDescriptions() {
		return descriptions;
	}

	public String getDescription() {
		//currently there is only default language is supported for description
		return descriptions.size()>0?descriptions.get(0).getContent():"";
	}

	public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
		this.descriptions = descriptions;
	}

	public void setName(String name){
		
		InternationalDescriptionWS nameDesc=new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, name);
		descriptions.add(nameDesc);
	}
}

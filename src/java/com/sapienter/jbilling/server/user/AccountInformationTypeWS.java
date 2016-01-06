package com.sapienter.jbilling.server.user;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

public class AccountInformationTypeWS extends MetaFieldGroupWS implements Serializable {
    
	@NotNull
	private String name;

    @NotNull
	private Integer accountTypeId;

    private boolean useForNotifications;

	public AccountInformationTypeWS() {
		super();
        setEntityType(EntityType.ACCOUNT_TYPE);
		
	}

	public AccountInformationTypeWS(MetaFieldGroupWS groupWS) {
		super();
		this.setDateCreated(groupWS.getDateCreated());
		this.setDateUpdated(groupWS.getDateUpdated());
		this.setDescriptions(groupWS.getDescriptions());
		this.setDisplayOrder(groupWS.getDisplayOrder());
		this.setEntityId(groupWS.getEntityId());
		this.setEntityType(groupWS.getEntityType());
		this.setMetaFields(groupWS.getMetaFields());
	}

	public String getName() {
		return name;
	}

    public boolean isUseForNotifications() {
        return useForNotifications;
    }
	public void setName(String name) {
		super.setName(name);
		this.name = name;
	}
    public void setUseForNotifications(boolean useForNotifications) {
        this.useForNotifications = useForNotifications;
    }

    public Integer getAccountTypeId() {
        return accountTypeId;
    }

    public void setAccountTypeId(Integer accountTypeId) {
        this.accountTypeId = accountTypeId;
    }

	@Override
	public String toString() {
		return "AccountInformationTypeWS [name=" + name + ", accountType="
				+ accountTypeId + ", getId()=" + getId() + "]";
	}

}

package com.sapienter.jbilling.server.accountType.builder;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Vladimir Carevski
 * @since 12-FEB-2015
 */
public class AccountInformationTypeBuilder {

	private final JbillingAPI api;

	//account information type specific
	private Integer id;
	private String name;
	private Integer accountTypeId;

	//meta field group
	private Date dateCreated;
	private Date dateUpdated;
	private Integer entityId;
	private EntityType entityType;
	private Integer displayOrder;

	private List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>();
	private List<MetaFieldWS> metaFields = new ArrayList<MetaFieldWS>();

	public AccountInformationTypeBuilder(AccountTypeWS accountType) {
		this(null != accountType ? accountType.getId() : null);
	}

	public AccountInformationTypeBuilder(Integer accountTypeId) {
		this.accountTypeId = accountTypeId;
		this.api = null;
	}

	public AccountInformationTypeBuilder id(Integer id) {
		this.id = id;
		return this;
	}

	public AccountInformationTypeBuilder name(String name) {
		this.name = name;
		if (null != name) {
			addDescription(name, ApiTestCase.TEST_LANGUAGE_ID);
		} else {
			noDescriptions();
		}
		return this;
	}

	public AccountInformationTypeBuilder dateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
		return this;
	}

	public AccountInformationTypeBuilder dateUpdated(Date dateUpdated) {
		this.dateUpdated = dateUpdated;
		return this;
	}

	public AccountInformationTypeBuilder entityId(Integer entityId) {
		this.entityId = entityId;
		return this;
	}

	public AccountInformationTypeBuilder entityType(EntityType entityType) {
		this.entityType = entityType;
		return this;
	}

	public AccountInformationTypeBuilder displayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
		return this;
	}

	public AccountInformationTypeBuilder addDescription(String name, Integer languageId) {
		InternationalDescriptionWS description = new InternationalDescriptionWS(languageId, name);
		return addDescription(description);
	}

	public AccountInformationTypeBuilder addDescription(InternationalDescriptionWS description) {
		descriptions.add(description);
		return this;
	}

	public AccountInformationTypeBuilder noDescriptions() {
		this.descriptions = null;
		return this;
	}

	public AccountInformationTypeBuilder addMetaField(MetaFieldWS metaField) {
		metaFields.add(metaField);
		return this;
	}

	public AccountInformationTypeWS build() {
		AccountInformationTypeWS ait = new AccountInformationTypeWS();

		ait.setId(null != id ? id.intValue() : 0);
		if (descriptions.isEmpty()) {
			name("AIT Test:" + System.currentTimeMillis());
		}
		ait.setName(name);
		ait.setAccountTypeId(null != accountTypeId ? accountTypeId : null);

		ait.setDateCreated(null != dateCreated ? dateCreated : null);
		ait.setDateUpdated(null != dateUpdated ? dateUpdated : null);
		ait.setEntityId(null != entityId ? entityId : ApiTestCase.TEST_ENTITY_ID);
		ait.setEntityType(null != entityType ? entityType : EntityType.ACCOUNT_TYPE);
		ait.setDisplayOrder(null != displayOrder ? displayOrder : Integer.valueOf(1));
		ait.setDescriptions(descriptions);
		ait.setMetaFields(null != metaFields ? metaFields.toArray(new MetaFieldWS[metaFields.size()]) : null);

		return ait;
	}
}

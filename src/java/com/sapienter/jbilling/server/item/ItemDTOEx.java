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

import com.sapienter.jbilling.server.item.validator.ItemPrices;
import com.sapienter.jbilling.server.item.validator.ItemTypes;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.validator.DateRange;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.Util;
import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static com.sapienter.jbilling.server.util.ServerConstants.LANGUAGE_ENGLISH_ID;

@DateRange(start = "activeSince", end = "activeUntil", message = "validation.activeUntil.before.activeSince")
public class ItemDTOEx implements WSSecured, Serializable {

    private static final long serialVersionUID = 20130704L;
    private final static int MAXIMUM_MINUTES_ALLOWED = 600000;
    private final static int MINIMUM_MINUTES_ALLOWED = 0;

    // ItemDTO
    private Integer id;
    @NotNull(message = "validation.error.notnull")
   	@Size(min=1,max=50, message="validation.error.size,1,50")
    private String number;
    @Size (min=0,max=50, message="validation.error.size,1,50")
    private String glCode;
    private Integer[] excludedTypes = null;
    private Integer priceManual;
    private Integer hasDecimals;
    private Integer deleted;
    private Integer assetManagementEnabled = 0;
    private Integer entityId;
    
    @Size(min=1,max=50, message="validation.error.size,1,50")

    private String description = null;
    @ItemTypes
    private Integer[] types = null;
    private String promoCode = null;
    private Integer currencyId = null;
    @Digits(integer=12, fraction=10, message="validation.error.not.a.number")
    private String price = null;
    private Integer orderLineTypeId = null;
    @ItemPrices @Valid
    private List<ItemPriceDTOEx> prices = null;
    @NotEmpty(message = "validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = ListUtils.lazyList(new ArrayList<InternationalDescriptionWS>(), FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));
    @Valid
    private MetaFieldValueWS[] metaFields;
    @Valid
    private MetaFieldWS[] orderLineMetaFields;
    private SortedMap <Integer, MetaFieldValueWS[]> metaFieldsMap = new TreeMap<Integer, MetaFieldValueWS[]>();
    @Valid
    private ItemDependencyDTOEx[] dependencies;

    private boolean standardAvailability = true;
    private boolean global = false;
    private Integer[] accountTypes = null;

    private Set<Integer> entities = new HashSet<Integer>(0);
    private Integer priceModelCompanyId = null;
    @Range(min = 0, max = 100, message = "validation.error.invalid.percentage.value")
    private String standardPartnerPercentage;
    @Range(min = 0, max = 100, message = "validation.error.invalid.percentage.value")
    private String masterPartnerPercentage;
    
    private Date activeSince;
    private Date activeUntil;
    private boolean isPlan=false;

    @Range(min = MINIMUM_MINUTES_ALLOWED, max = MAXIMUM_MINUTES_ALLOWED, message = "reserve.duration.validation.message,{min},{max}")
    private Integer reservationDuration;


    public ItemDTOEx() {
    }

    public ItemDTOEx(Integer id,String number, String glCode, Integer entity, String description, Integer priceManual,
                     Integer deleted, Integer currencyId, BigDecimal price,
                     Integer orderLineTypeId, Integer hasDecimals, Integer assetManagementEnabled) {

        this(id, number, glCode, hasDecimals, priceManual, deleted, entity, assetManagementEnabled);
        setDescription(description);
        setCurrencyId(currencyId);
        setPrice(price);
        setOrderLineTypeId(orderLineTypeId);
    }

    public ItemDTOEx(Integer id, String number, String glCode, Integer priceManual, Integer hasDecimals,
                     Integer deleted, Integer entityId, Integer assetManagementEnabled) {
        this.id = id;
        this.number = number;
        this.glCode= glCode;
        this.priceManual = priceManual;
        this.hasDecimals = hasDecimals;
        this.deleted = deleted;
        this.entityId = entityId;
        this.assetManagementEnabled = assetManagementEnabled;
    }

    public ItemDTOEx(ItemDTOEx otherValue) {
        this.id = otherValue.id;
        this.number = otherValue.number;
        this.glCode = otherValue.glCode;
        this.priceManual = otherValue.priceManual;
        this.hasDecimals = otherValue.hasDecimals;
        this.deleted = otherValue.deleted;
        this.entityId = otherValue.entityId;
        this.metaFields = otherValue.getMetaFields();
        this.assetManagementEnabled = otherValue.assetManagementEnabled;
        this.orderLineMetaFields = otherValue.getOrderLineMetaFields();
        this.standardPartnerPercentage = otherValue.standardPartnerPercentage;
        this.masterPartnerPercentage = otherValue.masterPartnerPercentage;
    }
    
   /* public ItemDTOEx(ItemDTO otherValue) {
        this.id = otherValue.getId();
        this.number = otherValue.getNumber();
        this.glCode = otherValue.getGlCode();
        this.hasDecimals = otherValue.getHasDecimals();
        this.deleted = otherValue.getDeleted();
        this.entityId = otherValue.getEntityId();
        if (null != otherValue.getMetaFields()) {
	        this.metaFields = new MetaFieldValueWS[otherValue.getMetaFields().size()];
	        int index = 0;
	        for (MetaFieldValue metaFieldValue : otherValue.getMetaFields()) {
	        	this.metaFields[index++] = MetaFieldBL.getWS(metaFieldValue);
	        }
        }
        this.assetManagementEnabled = otherValue.getAssetManagementEnabled();
        if (null != otherValue.getOrderLineMetaFields()) {
			this.orderLineMetaFields = new MetaFieldWS[otherValue.getOrderLineMetaFields().size()];
			int index=0;
			for (MetaField metaValue : otherValue.getOrderLineMetaFields()) {
				this.orderLineMetaFields[index++] = MetaFieldBL.getWS(metaValue);
			}
		}
        
        this.standardPartnerPercentage = null != otherValue.getStandardPartnerPercentage() ? otherValue.getStandardPartnerPercentage().toString() : "";
        this.masterPartnerPercentage = null != otherValue.getMasterPartnerPercentage() ? otherValue.getMasterPartnerPercentage().toString() : "";
        this.reservationDuration = otherValue.getReservationDuration();
    }*/

    public Integer getAssetManagementEnabled() {
        return assetManagementEnabled;
    }

    public void setAssetManagementEnabled(Integer assetManagementEnabled) {
        this.assetManagementEnabled = assetManagementEnabled;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getGlCode() {
        return glCode;
    }

    public void setGlCode(String glCode) {
        this.glCode = glCode;
    }


    public String getMasterPartnerPercentage () {
        return this.masterPartnerPercentage;
    }

    public BigDecimal getMasterPartnerPercentageAsDecimal () {
        return masterPartnerPercentage != null ? new BigDecimal(masterPartnerPercentage) : null;
    }

    public void setMasterPartnerPercentageAsDecimal (BigDecimal percentage) {
        setMasterPartnerPercentage(percentage);
    }

    public void setMasterPartnerPercentage (String percentage) {
        this.masterPartnerPercentage = percentage;
    }

    public void setMasterPartnerPercentage (BigDecimal percentage) {
        this.masterPartnerPercentage = (percentage != null ? percentage.toString() : null);
    }

    public String getStandardPartnerPercentage () {
        return this.standardPartnerPercentage;
    }

    public BigDecimal getStandardPartnerPercentageAsDecimal () {
        return standardPartnerPercentage != null ? new BigDecimal(standardPartnerPercentage) : null;
    }

    public void setStandardPartnerPercentageAsDecimal (BigDecimal percentage) {
        setStandardPartnerPercentage(percentage);
    }

    public void setStandardPartnerPercentage (String percentage) {
        this.standardPartnerPercentage = percentage;
    }

    public void setStandardPartnerPercentage (BigDecimal percentage) {
        this.standardPartnerPercentage = (percentage != null ? percentage.toString() : null);
    }

    public Integer[] getExcludedTypes() {
        return excludedTypes;
    }

    public void setExcludedTypes(Integer[] excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

    public boolean isStandardAvailability() {
        return standardAvailability;
    }

    public void setStandardAvailability(boolean standardAvailability) {
        this.standardAvailability = standardAvailability;
    }

    public Integer[] getAccountTypes() {
        return accountTypes;
    }

    public void setAccountTypes(Integer[] accountTypes) {
        this.accountTypes = accountTypes;
    }

    public Integer getPriceManual() {
	    return this.priceManual;
	}
	
	public void setPriceManual(Integer priceManual) {
	    this.priceManual = priceManual;
	}
    	
    public Integer getHasDecimals() {
        return this.hasDecimals;
    }

    public void setHasDecimals(Integer hasDecimals) {
        this.hasDecimals = hasDecimals;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getEntityId() {
        return this.entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    /**
     * Returns an english description.
     * 
     * @return String
     */
    public String getDescription() {
        return getDescriptionForMultiLangBeans(descriptions);
    }

    /**
     * Sets the a description in english.
     * 
     * @param newDescription
     *            The description to set
     */
    public void setDescription(String newDescription) {
        description = newDescription;
        	setDescriptionForMultiLangBeans(descriptions, newDescription);
    }

    public Integer[] getTypes() {
        return types;
    }

    /*
     * Rules only work on collections of strings (operator contains)
     */
    public Collection<String> getStrTypes() {
        List<String> retValue = new ArrayList<String>();
        for (Integer i: types) {
            retValue.add(i.toString());
        }
        return retValue;
    }

    public void setTypes(Integer[] vector) {
        types = vector;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String string) {
        promoCode = string;
    }

    public Integer getOrderLineTypeId() {
        return orderLineTypeId;
    }

    public void setOrderLineTypeId(Integer typeId) {
        orderLineTypeId = typeId;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }
    
    public String getPrice() {
        return price;
    }

    public BigDecimal getPriceAsDecimal() {
        return Util.string2decimal(price);
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setPrice(BigDecimal price) {
        setPrice((price != null ? price.toString() : null));
    }

	public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    public MetaFieldWS[] getOrderLineMetaFields() {
        return orderLineMetaFields;
    }

    public void setOrderLineMetaFields(MetaFieldWS[] orderLineMetaFields) {
        this.orderLineMetaFields = orderLineMetaFields;
    }
    
    public List<ItemPriceDTOEx> getPrices() {
	    return prices;
	}
	
	public void setPrices(List<ItemPriceDTOEx> prices) {
	    this.prices = prices;
	}

    public SortedMap <Integer, MetaFieldValueWS[]> getMetaFieldsMap() {
		return metaFieldsMap;
	}

	public void setMetaFieldsMap(SortedMap <Integer, MetaFieldValueWS[]> metaFieldsMap) {
		this.metaFieldsMap = metaFieldsMap;
	}

	public Integer getOwningEntityId() {
        return getEntityId();
    }

	public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    public ItemDependencyDTOEx[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(ItemDependencyDTOEx[] dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * From the dependencies extract those of type {@code type }
     *
     * @param type Type of dependecies to extract
     * @return
     */
    public ItemDependencyDTOEx[] getDependenciesOfType(ItemDependencyType type) {
        ArrayList<ItemDependencyDTOEx> result = new ArrayList<ItemDependencyDTOEx>();
        if(dependencies != null) {
            for(ItemDependencyDTOEx dependency : dependencies) {
                if(dependency.getType().equals(type)) {
                    result.add(dependency);
                }
            }
        }
        return result.toArray(new ItemDependencyDTOEx[result.size()]);
    }

    /**
     * From the dependencies extract the ids of those of type {@code type }
     *
     * @param type Type of dependecies to extract
     * @return
     */
    public Integer[] getDependencyIdsOfType(ItemDependencyType type) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if(dependencies != null) {
            for(ItemDependencyDTOEx dependency : dependencies) {
                if(dependency.getType().equals(type)) {
                    result.add(dependency.getDependentId());
                }
            }
        }
        return result.toArray(new Integer[result.size()]);
    }

    /**
     * From the dependencies extract the ids of those of type {@code type }
     * which has a minimum required qty of 1
     *
     * @param type Type of dependecies to extract
     * @return
     */
    public Integer[] getMandatoryDependencyIdsOfType(ItemDependencyType type) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if(dependencies != null) {
            for(ItemDependencyDTOEx dependency : dependencies) {
                if(dependency.getType().equals(type) && dependency.getMinimum() > 0) {
                    result.add(dependency.getDependentId());
                }
            }
        }
        return result.toArray(new Integer[result.size()]);
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningEntityId()}
     * @return null
     */
    public Integer getOwningUserId() {
        return null;
    }

	public Set<Integer> getEntities() {
		return entities;
	}

	public void setEntities(Set<Integer> entities) {
		this.entities = entities;
	}

	public Integer getPriceModelCompanyId() {
		return priceModelCompanyId;
	}

	public void setPriceModelCompanyId(Integer priceModelCompanyId) {
		this.priceModelCompanyId = priceModelCompanyId;
	}

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public Date getActiveSince() {
		return activeSince;
	}

	public void setActiveSince(Date activeSince) {
		this.activeSince = activeSince;
	}

	public Date getActiveUntil() {
		return activeUntil;
	}

	public void setActiveUntil(Date activeUntil) {
		this.activeUntil = activeUntil;
	}

    public Integer getReservationDuration() {
        return reservationDuration;
    }

    public void setReservationDuration(Integer reservationDuration) {
        this.reservationDuration = reservationDuration;
    }

    public boolean getIsPlan() {
        return isPlan;
    }

    public void setIsPlan(boolean isPlan) {
        this.isPlan = isPlan;
    }

    public boolean isIdentical(Object other) {
        if (other instanceof ItemDTOEx) {
            ItemDTOEx that = (ItemDTOEx) other;
            boolean lEquals = true;
            if (this.number == null) {
                lEquals = lEquals && (that.number == null);
            } else {
                lEquals = lEquals && this.number.equals(that.number);
            }
            if (this.glCode == null) {
                lEquals = lEquals && (that.glCode == null);
            } else {
                lEquals = lEquals && this.glCode.equals(that.glCode);
            }
            if (this.priceManual == null) {
        	    lEquals = lEquals && (that.priceManual == null);
        	} else {
        	    lEquals = lEquals && this.priceManual.equals(that.priceManual);
        	}
            if (this.hasDecimals == null) {
                lEquals = lEquals && (that.hasDecimals == null);
            } else {
                lEquals = lEquals && this.hasDecimals.equals(that.hasDecimals);
            }
            if (this.deleted == null) {
                lEquals = lEquals && (that.deleted == null);
            } else {
                lEquals = lEquals && this.deleted.equals(that.deleted);
            }
            if (this.entityId == null) {
                lEquals = lEquals && (that.entityId == null);
            } else {
                lEquals = lEquals && this.entityId.equals(that.entityId);
            }
            if (this.masterPartnerPercentage == null) {
                lEquals = lEquals && (that.masterPartnerPercentage == null);
            } else {
                lEquals = lEquals && this.masterPartnerPercentage.equals(that.masterPartnerPercentage);
            }
            if (this.standardPartnerPercentage == null) {
                lEquals = lEquals && (that.standardPartnerPercentage == null);
            } else {
                lEquals = lEquals && this.standardPartnerPercentage.equals(that.standardPartnerPercentage);
            }

            return lEquals;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof ItemDTOEx))
            return false;

        ItemDTOEx that = (ItemDTOEx) other;
        boolean lEquals = true;
        if( this.id == null ) {
            lEquals = lEquals && ( that.id == null );
        } else {
            lEquals = lEquals && this.id.equals( that.id );
        }

        lEquals = lEquals && isIdentical(that);
        return lEquals;
    }

    @Override
    public int hashCode(){
        int result = 17;
        result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);
        result = 37*result + ((this.number != null) ? this.number.hashCode() : 0);
        result = 37*result + ((this.glCode != null) ? this.glCode.hashCode() : 0);
        result = 37*result + ((this.priceManual != null) ? this.priceManual.hashCode() : 0);
        result = 37*result + ((this.hasDecimals != null) ? this.hasDecimals.hashCode() : 0);
        result = 37*result + ((this.deleted != null) ? this.deleted.hashCode() : 0);
        result = 37*result + ((this.entityId != null) ? this.entityId.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemDTOEx [currencyId=");
        builder.append(currencyId);
        builder.append(", deleted=");
        builder.append(deleted);
        builder.append(", description=");
        builder.append(description);
        builder.append(", entityId=");
        builder.append(entityId);
        builder.append(", entities=");
        builder.append(entities);
        builder.append(", hasDecimals=");
        builder.append(hasDecimals);
        builder.append(", id=");
        builder.append(id);
        builder.append(", number=");
        builder.append(number);
        builder.append(", glCode=");
        builder.append(glCode);
        builder.append(", orderLineTypeId=");
        builder.append(orderLineTypeId);
        builder.append(", price=");
        builder.append(price);
        builder.append(", promoCode=");
        builder.append(promoCode);
        builder.append(", types=");
        builder.append(Arrays.toString(types));
        builder.append(", excludedTypes=");
        builder.append(Arrays.toString(excludedTypes));
        builder.append(", dependencies=");
        builder.append(Arrays.toString(dependencies));
        builder.append(", masterAgentPercentage=");
        builder.append(masterPartnerPercentage);
        builder.append(", standardAgentPercentage=");
        builder.append(standardPartnerPercentage);
        builder.append(']');
        return builder.toString();
    }
    
    public  String getDescriptionForMultiLangBeans(List<InternationalDescriptionWS> descriptions) {
        for (InternationalDescriptionWS description : descriptions) {
            if (description != null &&
            		LANGUAGE_ENGLISH_ID.intValue() == description.getLanguageId()) {
                return description.getContent();
            }
        }
        return "";
    }
    
	public  void setDescriptionForMultiLangBeans(
			List<InternationalDescriptionWS> beanDescriptions,
			String newDescription) {
		for (InternationalDescriptionWS description : beanDescriptions) {
			if (description != null
					&& LANGUAGE_ENGLISH_ID.intValue() == description
							.getLanguageId()) {
				description.setContent(newDescription);
				return;
			}
		}
		beanDescriptions.add(new InternationalDescriptionWS("description",
				LANGUAGE_ENGLISH_ID, newDescription));
	}

}

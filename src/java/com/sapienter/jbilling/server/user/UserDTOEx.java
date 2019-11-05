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

package com.sapienter.jbilling.server.user;

import java.util.Date;
import java.util.Locale;
import java.util.List;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.LanguageDAS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author emilc
 */
public final class UserDTOEx extends UserDTO {

    // constants
    
    // user status in synch with db table user_status
    public static final Integer STATUS_ACTIVE = new Integer(1); // this HAS to be the very first status
    
    // subscriber status in synch with db table subscriber_status
    public static final Integer SUBSCRIBER_ACTIVE = new Integer(1); 
    public static final Integer SUBSCRIBER_PENDING_UNSUBSCRIPTION = new Integer(2);
    public static final Integer SUBSCRIBER_UNSUBSCRIBED = new Integer(3);
    public static final Integer SUBSCRIBER_PENDING_EXPIRATION= new Integer(4);
    public static final Integer SUBSCRIBER_EXPIRED = new Integer(5);
    public static final Integer SUBSCRIBER_NONSUBSCRIBED = new Integer(6);
    public static final Integer SUBSCRIBER_DISCONTINUED = new Integer(7);

    private List<Integer> roles = null;
    private Integer mainRoleId = null;
    private String mainRoleStr = null;
    private String languageStr = null;
    private Integer statusId = null;
    private String statusStr = null;
    private Integer subscriptionStatusId = null;
    private String subscriptionStatusStr = null;
    private Integer lastInvoiceId = null;
    private String currencySymbol = null;
    private String currencyName = null;
    private Locale locale = null;
    private List<String> blacklistMatches = null;
    private Boolean userIdBlacklisted = null;
    private BigDecimal balance = null; // calculated in real-time. Not a DB field

    private Map<Integer, ArrayList<Date>> timelineDatesMap = null;
    private Map<Integer, Date> effectiveDateMap = null;
    private Map<Integer, ArrayList<Date>> removedDatesMap = null;
    private boolean createCredentials = false;

    /**
     * Constructor for UserDTOEx.
     * @param userId
     * @param entityId
     * @param userName
     * @param password
     * @param deleted
     */
    public UserDTOEx(Integer userId, Integer entityId, String userName,
            String password, Integer deleted, Integer language, Integer roleId,
            Integer currencyId, Date creation, Date modified, Date lLogin, 
            Integer failedAttempts) {
        // set the base dto fields
        setId((userId == null) ? 0 : userId);
        setUserName(userName);
        setPassword(password);
        setDeleted((deleted == null) ? 0 : deleted);
        setLanguage(new LanguageDAS().find(language));
        setCurrency(new CurrencyDAS().find(currencyId));
        setCreateDatetime(creation);
        setLastStatusChange(modified);
        setLastLoginDate(lLogin);
        setFailedLoginAttempts((failedAttempts == null) ? 0 : failedAttempts);
        // the entity id
        setEntityId(entityId);
        roles = new ArrayList<Integer>();
        if (roleId != null) {
            // we ask for at least one role for this user
            roles.add(roleId);
            mainRoleId = roleId;
        }
    }
    
    public UserDTOEx(UserWS dto, Integer entityId) {
        setId(dto.getUserId());
        setPassword(dto.getPassword());
        setDeleted(dto.getDeleted());
        setCreateDatetime(dto.getCreateDatetime());
        setLastStatusChange(dto.getLastStatusChange());
        setLastLoginDate(dto.getLastLoginDate());
        setUserName(dto.getUserName());
        setFailedLoginAttempts(dto.getFailedAttempts());
        setCurrency(dto.getCurrencyId() == null ? null : new CurrencyDTO(dto.getCurrencyId()));
        mainRoleStr = dto.getRole();
        mainRoleId = dto.getMainRoleId();
        languageStr = dto.getLanguage();
        setLanguage(dto.getLanguageId() == null ? null : 
                new LanguageDAS().find(dto.getLanguageId()));
        statusStr = dto.getStatus();
        statusId = dto.getStatusId();
        subscriptionStatusId = dto.getSubscriberStatusId();
        setEntityId(entityId);

        if(Boolean.TRUE.equals( dto.isAccountLocked() ) ){
            setAccountLockedTime(new Date());
        } else {
            setAccountLockedTime(null);
        }
        setAccountExpired(dto.isAccountExpired());
        setAccountDisabledDate(dto.isAccountExpired() ? new Date() : null);
        
        roles = new ArrayList<Integer>();
        roles.add(mainRoleId);
        
        if (mainRoleId.equals(CommonConstants.TYPE_CUSTOMER)) {
            CustomerDTO customer = new CustomerDTO(entityId, dto);
            setCustomer(customer);
        }
        
        // timelines dates map and effective date map
        setTimelineDatesMap(dto.getTimelineDatesMap());
        setEffectiveDateMap(dto.getEffectiveDateMap());
        setRemovedDatesMap(dto.getRemovedDatesMap());
        
        if(dto.getPaymentInstruments() != null) {
        	List<PaymentInformationDTO> paymentInstruments = new ArrayList<PaymentInformationDTO>(0);
            if(dto.getPaymentInstruments().size() > 0) {
        	for(PaymentInformationWS paymentInformation : dto.getPaymentInstruments()) {
                paymentInstruments.add(new PaymentInformationDTO(paymentInformation, entityId));
        	}
            }
            setPaymentInstruments(paymentInstruments);
        }
        setCreateCredentials(dto.isCreateCredentials());
    }
    
    public UserDTOEx() {
        super();
    }
    
    public UserDTOEx(UserDTO user) {
       super(user); 
    }

    private boolean paymentInstrumentEntered(List<PaymentInformationDTO> paymentInstruments) {
    	if(paymentInstruments.size() < 2) {
    		for(MetaFieldValue value : paymentInstruments.iterator().next().getMetaFields()) {
    			if(value.getValue() != null &&  !value.getValue().toString().isEmpty()) {
    				return true;
    			}
    		}
    		return false;
    	}
    	return true;
    }

    /**
     * Returns the entityId.
     * @return Integer
     */
    public Integer getEntityId() {
        return getCompany().getId();
    }

    /**
     * Sets the entityId.
     * @param entityId The entityId to set
     */
    public void setEntityId(Integer entityId) {
        setCompany(new CompanyDAS().find(entityId));
    }

    /**
     * @return
     */
    public Integer getMainRoleId() {
        return mainRoleId;
    }

    /**
     * @return
     */
    public String getMainRoleStr() {
        return mainRoleStr;
    }

    /**
     * @param integer
     */
    public void setMainRoleId(Integer integer) {
        mainRoleId = integer;
        if (roles == null) {
            roles = new ArrayList<Integer>();
        }
        if (!roles.contains(integer)) {
            roles.add(integer);
        }
    }

    /**
     * @param string
     */
    public void setMainRoleStr(String string) {
        mainRoleStr = string;
    }

    /**
     * @return
     */
    public String getLanguageStr() {
        return languageStr;
    }

    /**
     * @param string
     */
    public void setLanguageStr(String string) {
        languageStr = string;
    }

    /**
     * @return
     */
    public Integer getStatusId() {
        return statusId;
    }

    /**
     * @return
     */
    public String getStatusStr() {
        return statusStr;
    }

    /**
     * @param integer
     */
    public void setStatusId(Integer integer) {
        statusId = integer;
    }

    /**
     * @param string
     */
    public void setStatusStr(String string) {
        statusStr = string;
    }

    /**
     * @return
     */
    public Integer getLastInvoiceId() {
        return lastInvoiceId;
    }

    /**
     * @param lastInvoiceId
     */
    public void setLastInvoiceId(Integer lastInvoiceId) {
        this.lastInvoiceId = lastInvoiceId;
    }

    /**
     * @return
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * @param currencySymbol
     */
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    /**
     * @return
     */
    public String getCurrencyName() {
        return currencyName;
    }

    /**
     * @param currencyName
     */
    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }
    
    public Locale getLocale() {
        return locale;
    }
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Integer getSubscriptionStatusId() {
        return subscriptionStatusId;
    }

    public void setSubscriptionStatusId(Integer subscriptionStatusId) {
        this.subscriptionStatusId = subscriptionStatusId;
    }

    public String getSubscriptionStatusStr() {
        return subscriptionStatusStr;
    }

    public void setSubscriptionStatusStr(String subscriptionStatusStr) {
        this.subscriptionStatusStr = subscriptionStatusStr;
    }
    
    public Integer getLanguageId() {
        if (getLanguage() != null) {
            return getLanguage().getId();
        }
        return null;
    }
    
    public void setUserId(Integer id) {
        setId(id);
    }
    
    public Integer getUserId() {
        return getId();
    }

    public List<String> getBlacklistMatches() {
        return blacklistMatches;
    }

    public void setBlacklistMatches(List<String> blacklistMatches) {
        this.blacklistMatches = blacklistMatches;
    }

    public Boolean getUserIdBlacklisted() {
        return userIdBlacklisted;
    }

    public void setUserIdBlacklisted(Boolean userIdBlacklisted) {
        this.userIdBlacklisted = userIdBlacklisted;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setTimelineDatesMap(Map<Integer, ArrayList<Date>> timelineDatesMap) {
    	this.timelineDatesMap =  timelineDatesMap;
    }
    
    public Map<Integer, ArrayList<Date>> getTimelineDatesMap() {
    	return timelineDatesMap;
    }
    
    public void setEffectiveDateMap(Map<Integer, Date> effectiveDateMap) {
    	this.effectiveDateMap =  effectiveDateMap;
    }
    
    public Map<Integer, Date> getEffectiveDateMap() {
    	return effectiveDateMap;
    }
    
    public void setRemovedDatesMap(Map<Integer, ArrayList<Date>> removedDatesMap) {
    	this.removedDatesMap =  removedDatesMap;
    }
    
    public Map<Integer, ArrayList<Date>> getRemovedDatesMap() {
    	return removedDatesMap;
    }

    public boolean isCreateCredentials() {
        return createCredentials;
    }

    public void setCreateCredentials(boolean createCredentials) {
        this.createCredentials = createCredentials;
    }
}

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

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.payment.blacklist.BlacklistBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import com.sapienter.jbilling.server.util.db.LanguageDTO;

import javax.naming.NamingException;
import java.util.Locale;

/**
 * 
 * This is the class to provide initialization of DTOs with the entities values.
 * It can't be instantiated, all the methods are static
 * 
 * @author Emil
 */

/*
 * This code can't be testest through a JUnit test case because it will start
 * using local interfaces ... from a remote client ;)
 */
public class DTOFactory {

    private static final FormatLogger LOG = new FormatLogger(DTOFactory.class);

    /**
     * The constructor is private, do it doesn't get instantiated. All the
     * methods then are static.
     */
    private DTOFactory() {
    }

    /**
     * Get's an entity bean of the user, using the username, and then creates a
     * DTO with that data. It is not setting permissions or menu
     * 
     * @param username
     * @return UserDTO
     * @throws NamingException
     */
    public static UserDTOEx getUserDTO(String username, Integer entityId)
            throws NamingException, SessionInternalError {

        LOG.debug("getting the user %s", username);

        UserDTO user = new UserDAS().findByUserName(username, entityId);
        if (user == null)
            return null;
        return getUserDTOEx(user);
    }

    public static UserDTOEx getUserDTOEx(Integer userId)
            throws NamingException, SessionInternalError {

        LOG.debug("getting the user %s", userId);

        UserDTO user = new UserDAS().find(userId);
        return getUserDTOEx(user);
    }

    public static UserDTOEx getUserDTOEx(UserDTO user) throws SessionInternalError {
        UserDTOEx dto = new UserDTOEx(user);

        // get the status
        dto.setStatusId(user.getStatus().getId());
        dto.setStatusStr(user.getStatus().getDescription(
                user.getLanguageIdField()));
        // the subscriber status
        dto.setSubscriptionStatusId(user.getSubscriberStatus().getId());
        dto.setSubscriptionStatusStr(user.getSubscriberStatus().getDescription(
                user.getLanguageIdField()));

        // add the roles
        Integer mainRole = new Integer(1000);
        String roleStr = null;
        dto.getRoles().addAll(user.getRoles());
        for (RoleDTO role : user.getRoles()) {
            // the main role is the smallest of them, so they have to be ordered
            // in the
            // db in ascending order (small = important);
            if (role.getRoleTypeId() < mainRole) {
                mainRole = role.getRoleTypeId();
                roleStr = role.getTitle(user.getLanguageIdField());
            }
        }
        dto.setMainRoleId(mainRole);
        dto.setMainRoleStr(roleStr);

        // now get the language
        LanguageDTO language = new LanguageDAS()
                .find(user.getLanguageIdField());
        dto.setLanguageStr(language.getDescription());

        // add the last invoice id
        InvoiceBL invoiceBL = new InvoiceBL();
        try {
            dto.setLastInvoiceId(invoiceBL.getLastByUser(user.getUserId()));
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        // make sure the currency is set
        if (dto.getCurrencyId() == null) {
            // defaults to the one from the entity
            dto.setCurrency(user.getEntity().getCurrency());
        }
        CurrencyBL currency = new CurrencyBL(dto.getCurrencyId());
        dto.setCurrencySymbol(currency.getEntity().getSymbol());
        dto.setCurrencyName(currency.getEntity().getDescription(
                user.getLanguageIdField()));

        // add a credit card if available
        if (!user.getPaymentInstruments().isEmpty()) {
            dto.setPaymentInstruments(user.getPaymentInstruments());
        }

        // if this is a customer, add its dto
        if (user.getCustomer() != null) {

            dto.setCustomer(user.getCustomer());
        }

        // if this is a partner, add its dto
        if (user.getPartner() != null) {
            PartnerBL partner = new PartnerBL(user.getPartner());
            dto.setPartner(partner.getDTO());
        }

        // the locale will be handy
        try {
            UserBL bl = new UserBL(user);
            dto.setLocale(bl.getLocale());
        } catch (Exception e) {
            dto.setLocale(new Locale("en"));
        }
        
        dto.setCompany(user.getCompany());

        // if the blacklist plug-in enabled, add the list of blacklist
        // entries that match this user and set whether their id is blacklisted
        if (BlacklistBL.isBlacklistEnabled(user.getCompany().getId())) {
            dto.setBlacklistMatches(BlacklistBL.getBlacklistMatches(user
                    .getId()));
            dto.setUserIdBlacklisted(BlacklistBL.isUserIdBlacklisted(user
                    .getId()));
        }

        // set the balance
        dto.setBalance(UserBL.getBalance(dto.getId()));
        dto.setPaymentInstruments(user.getPaymentInstruments());
        dto.setAccountLockedTime(user.getAccountLockedTime());

        // User Account Expired value population
        try {
            UserBL userBL = new UserBL(user);
            if(userBL.validateAccountExpired(user.getAccountDisabledDate())) {
                dto.setAccountExpired(true);
                dto.setAccountDisabledDate(user.getAccountDisabledDate());
            }
        } catch (Exception e) {
            LOG.error("Exception occurred %s", e.getMessage());
        }

        return dto;
    }

}

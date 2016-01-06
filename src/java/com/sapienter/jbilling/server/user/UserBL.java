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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.sql.rowset.CachedRowSet;

import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.util.credentials.PasswordService;

import org.apache.commons.collections.CollectionUtils;

import com.sapienter.jbilling.server.process.AgeingDTOEx;
import com.sapienter.jbilling.server.metafields.MetaContent;
import org.joda.time.DateTime;
import org.springframework.dao.EmptyResultDataAccessException;

import com.sapienter.jbilling.CustomerNoteDAS;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.security.JBCrypto;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDAS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.process.AgeingBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.user.db.SubscriberStatusDAS;
import com.sapienter.jbilling.server.user.db.UserCodeCustomerLinkDTO;
import com.sapienter.jbilling.server.user.db.UserCodeDAS;
import com.sapienter.jbilling.server.user.db.UserCodeDTO;
import com.sapienter.jbilling.server.user.db.UserCodeLinkDAS;
import com.sapienter.jbilling.server.user.db.UserCodeLinkDTO;
import com.sapienter.jbilling.server.user.db.UserCodeObjectType;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserPasswordDAS;
import com.sapienter.jbilling.server.user.db.UserPasswordDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.user.tasks.IValidatePurchaseTask;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.DTOFactory;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import com.sapienter.jbilling.server.user.db.*;

import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;


public class UserBL extends ResultList implements UserSQL {

    private final static FormatLogger LOG                   = new FormatLogger(UserBL.class);

    private UserDTO                   user                  = null;
    private EventLogger               eLogger               = null;
    private Integer                   mainRole              = null;
    private UserDAS                   das                   = null;
    private PaymentInformationDAS     paymentInformationDAS = null;
    private PasswordService passwordService = null;

    public UserBL (Integer userId) {
        init();
        set(userId);
    }

    public UserBL () {
        init();
    }

    public UserBL (UserDTO entity) {
        user = entity;
        init();
    }

    public UserBL (String username, Integer entityId) {
        init();
        user = das.findByUserName(username, entityId);
    }

    public static UserDTO getUserEntity(Integer userId) {
        return new UserDAS().find(userId);
    }

    private static Integer selectMainRole(Collection allRoleIds){
        // the main role is the smallest of them, so they have to be ordered in the
        // db in ascending order (small = important);
        Integer result = null;
        for (Iterator roleIds = allRoleIds.iterator(); roleIds.hasNext();){
            Integer nextId = (Integer)roleIds.next();
            if (result == null || nextId.compareTo(result) < 0) {
                result = nextId;
            }
        }
        return result;
    }

    /**
     * Get a locale for the given user based on their selected language and set country.
     *
     * This method assumes that the user is part of the current persistence context, and that
     * the LanguageDTO association can safely be lazy-loaded.
     *
     * @param user user
     * @return users locale
     */
    public static Locale getLocale(UserDTO user) {
        String languageCode = user.getLanguage().getCode();

        ContactDTO contact = new ContactDAS().findContact(user.getId());

        String countryCode = null;
        if (contact != null)
            countryCode = contact.getCountryCode();

        return countryCode != null ? new Locale(languageCode, countryCode) : new Locale(languageCode);
    }

    public static boolean validate(UserWS userWS) {
        return validate(new UserDTOEx(userWS, null));
    }

    /**
     * Validates the user info and the credit card if present
     * @param dto
     * @return
     */
    public static boolean validate(UserDTOEx dto) {
        boolean retValue = true;

        if (dto == null || dto.getUserName() == null ||
                dto.getPassword() == null || dto.getLanguageId() == null ||
                dto.getMainRoleId() == null || dto.getStatusId() == null) {
            retValue = false;
            LOG.debug("Invalid %s", dto);
        } else if (dto.getPaymentInstruments() != null) {
            //retValue = can validate paymentInstruments here
        }

        return retValue;
    }

    public void set(Integer userId) {
        user = das.find(userId);
    }

    public void set (String userName, Integer entityId) {
        user = das.findByUserName(userName, entityId);
    }

    public void set (UserDTO user) {
        this.user = user;
    }

    public void setRoot (String userName) {
        user = das.findRoot(userName);
    }

    private void init () {
        eLogger = EventLogger.getInstance();
        das = new UserDAS();
        paymentInformationDAS = new PaymentInformationDAS();
        passwordService = (PasswordService)Context.getBean(Context.Name.PASSWORD_SERVICE);
    }

    /**
     * @param executorId
     *            This is the user that has ordered the update
     * @param dto
     *            This is the user that will be updated
     */
    public void update (Integer executorId, UserDTOEx dto) throws SessionInternalError {
        LOG.debug("Updating User");

        // password is the only one that might've not been set
        String changedPassword = dto.getPassword();
        Date date = new DateTime().toDateMidnight().toDate(); 
        if (changedPassword != null){
            //encrypt it based on the user role
	        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(getMainRole());
            String encPassword = user.getPassword();
	        boolean matches = JBCrypto.passwordsMatch(passwordEncoderId, encPassword, changedPassword);

	        if (!matches) {
                eLogger.audit(executorId, dto.getId(), Constants.TABLE_BASE_USER,
                        user.getUserId(), EventLogger.MODULE_USER_MAINTENANCE,
                        EventLogger.PASSWORD_CHANGE, null, user.getPassword(), null);

                user.setChangePasswordDate(date);
                user.setPassword(JBCrypto.encodePassword(passwordEncoderId, changedPassword));
                user.setEncryptionScheme(passwordEncoderId);
                savePasswordHistory();
            }
        }
        if (dto.getUserName() != null && !user.getUserName().equals(dto.getUserName())) {
            user.setUserName(dto.getUserName());
        }
        if (dto.getLanguageId() != null && !user.getLanguageIdField().equals(dto.getLanguageId())) {

            user.setLanguage(new LanguageDAS().find(dto.getLanguageId()));
        }
        if (dto.getEntityId() != null && user.getEntity().getId() != dto.getEntityId()) {

            user.setCompany(new CompanyDAS().find(dto.getEntityId()));
        }
        if (dto.getStatusId() != null && user.getStatus().getId() != dto.getStatusId()) {
            AgeingBL age = new AgeingBL();
            age.setUserStatus(executorId, user.getUserId(), dto.getStatusId(), Calendar.getInstance().getTime());
        }
        updateSubscriptionStatus(dto.getSubscriptionStatusId(), executorId);
        if (dto.getCurrencyId() != null && !user.getCurrencyId().equals(dto.getCurrencyId())) {
            user.setCurrency(new CurrencyDAS().find(dto.getCurrencyId()));
        }
        setAccountExpired(validateAccountExpired(dto.getAccountDisabledDate()), dto.getAccountDisabledDate());

        user.setAccountLockedTime(dto.getAccountLockedTime());

        if (dto.getCustomer() != null && user.getCustomer() != null) {
            if (dto.getCustomer().getInvoiceDeliveryMethod() != null) {
                user.getCustomer().setInvoiceDeliveryMethod(dto.getCustomer().getInvoiceDeliveryMethod());
            }

            user.getCustomer().setDueDateUnitId(dto.getCustomer().getDueDateUnitId());
            user.getCustomer().setDueDateValue(dto.getCustomer().getDueDateValue());
            user.getCustomer().setDfFm(dto.getCustomer().getDfFm());

            try {
                if (dto.getCustomer().getPartner() != null
                        && dto.getCustomer().getPartner().getBaseUser().getEntity() != null) {
                    user.getCustomer().setPartner(dto.getCustomer().getPartner());
                } else {
                    user.getCustomer().setPartner(null);
                }
            } catch (Exception ex) {
                throw new SessionInternalError("It doesn't exist a partner with the supplied id.",
                        new String[] { "UserWS,partnerId,validation.error.partner.does.not.exist" });
            }

            user.getCustomer().setExcludeAging(dto.getCustomer().getExcludeAging());
            user.getCustomer().setCreditLimit(dto.getCustomer().getCreditLimit());
            user.getCustomer().setAutoRecharge(dto.getCustomer().getAutoRecharge());

            user.getCustomer().setRechargeThreshold(dto.getCustomer().getRechargeThreshold());
            user.getCustomer().setMonthlyLimit(dto.getCustomer().getMonthlyLimit());
            
            user.getCustomer().setAutoPaymentType(dto.getCustomer().getAutoPaymentType());

            // update the linked user code
            UserBL.updateAssociateUserCodesToLookLikeTarget(user.getCustomer(), dto.getCustomer().getUserCodeLinks(),
                    "CustomerWS,userCode");

            // set the sub-account fields
            user.getCustomer().setIsParent(dto.getCustomer().getIsParent());
            if (dto.getCustomer().getParent() != null) {
                // the API accepts the user ID of the parent instead of the customer ID
                try {
                    if (dto.getCustomer().getParent() != null) {
                        user.getCustomer().setParent(
                                new UserDAS().find(dto.getCustomer().getParent().getId()).getCustomer());
                    } else {
                        user.getCustomer().setParent(null);
                    }
                } catch (Exception ex) {
                    throw new SessionInternalError("There doesn't exist a parent with the supplied id.",
                            new String[] { "UserWS,parentId,validation.error.parent.does.not.exist" });
                }

                // use parent pricing flag
                user.getCustomer().setUseParentPricing(dto.getCustomer().useParentPricing());

                // log invoice if child changes
                Integer oldInvoiceIfChild = user.getCustomer().getInvoiceChild();
                user.getCustomer().setInvoiceChild(dto.getCustomer().getInvoiceChild());

                eLogger.audit(executorId,
                        user.getId(),
                        Constants.TABLE_CUSTOMER,
                        user.getCustomer().getId(),
                        EventLogger.MODULE_USER_MAINTENANCE,
                        EventLogger.INVOICE_IF_CHILD_CHANGE,
                        (oldInvoiceIfChild != null ? oldInvoiceIfChild : 0),
                        null, null);
            } else {

                user.getCustomer().setParent(null);
            }

            Integer periodUnitId = -1;
            int nextInvoiceDayOfPeriod = -1;

            if (null != dto.getCustomer().getMainSubscription()
                    && null != dto.getCustomer().getMainSubscription().getSubscriptionPeriod()
                    && null != dto.getCustomer().getMainSubscription().getSubscriptionPeriod().getPeriodUnit()) {
                periodUnitId = dto.getCustomer().getMainSubscription().getSubscriptionPeriod().getPeriodUnit().getId();
            }

            if (null != dto.getCustomer().getMainSubscription()
                    && null != dto.getCustomer().getMainSubscription().getNextInvoiceDayOfPeriod()) {
                nextInvoiceDayOfPeriod = dto.getCustomer().getMainSubscription().getNextInvoiceDayOfPeriod();
            }

            Calendar nextInvoiceCalendar = Calendar.getInstance();
            if(dto.getCustomer().getNextInvoiceDate()!=null)
            	nextInvoiceCalendar.setTime(dto.getCustomer().getNextInvoiceDate()); 

            if (dto.getCustomer().getNextInvoiceDate()!=null && user.getCustomer().getNextInvoiceDate()!=null &&
            				dto.getCustomer().getNextInvoiceDate().compareTo(user.getCustomer().getNextInvoiceDate()) != 0)  {
                if (periodUnitId.compareTo(Constants.PERIOD_UNIT_MONTH) == 0) {
                    if (nextInvoiceCalendar.get(Calendar.DAY_OF_MONTH) != nextInvoiceDayOfPeriod) {
                        throw new SessionInternalError("Billing cycle value should match with next invoice date",
                                new String[] { "CustomerWS,billingCycleValue,next.invoice.date.monthly.error" });
                    }
                } else if (periodUnitId.compareTo(Constants.PERIOD_UNIT_WEEK) == 0) {
                    if (nextInvoiceCalendar.get(Calendar.DAY_OF_WEEK) != (nextInvoiceDayOfPeriod)) {
                        throw new SessionInternalError("Billing cycle value should match with next invoice date",
                                new String[] { "CustomerWS,billingCycleValue,next.invoice.date.weekly.error" });
                    }

                } else if (periodUnitId.compareTo(Constants.PERIOD_UNIT_YEAR) == 0) {
                    if (nextInvoiceCalendar.get(Calendar.DAY_OF_YEAR) != nextInvoiceDayOfPeriod) {
                        throw new SessionInternalError("Billing cycle value should match with next invoice date",
                                new String[] { "CustomerWS,billingCycleValue,next.invoice.date.yearly.error" });
                    }

                } else if (periodUnitId.compareTo(Constants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
                    Integer otherPossibleNextInvoiceDay = CalendarUtils.getSemiMonthlyOtherPossibleNextInvoiceDay(
                            nextInvoiceCalendar.getTime(), nextInvoiceDayOfPeriod);
                    Integer nextInvoiceDay = nextInvoiceCalendar.get(Calendar.DAY_OF_MONTH);
                    if (nextInvoiceDay != nextInvoiceDayOfPeriod && nextInvoiceDay != otherPossibleNextInvoiceDay) {
                        throw new SessionInternalError("Billing cycle value should match with next invoice date",
                                new String[] { "CustomerWS,billingCycleValue,next.invoice.date.monthly.error" });
                    }
                }
            }

            int oldPeriodUnitId = -1;
            if (null != user.getCustomer().getMainSubscription()
                    && null != user.getCustomer().getMainSubscription().getSubscriptionPeriod()
                    && null != user.getCustomer().getMainSubscription().getSubscriptionPeriod().getPeriodUnit()) {
                oldPeriodUnitId = user
                        .getCustomer()
                        .getMainSubscription()
                        .getSubscriptionPeriod()
                        .getPeriodUnit()
                        .getId();
            }

            int oldNextInvoiceDayOfPeriod = -1;
            if (null != user.getCustomer().getMainSubscription()
                    && null != user.getCustomer().getMainSubscription().getNextInvoiceDayOfPeriod()) {
                oldNextInvoiceDayOfPeriod = user.getCustomer().getMainSubscription().getNextInvoiceDayOfPeriod();
            }

            // update the main subscription
            MainSubscriptionDTO newMainSubscription = dto.getCustomer().getMainSubscription();
            MainSubscriptionDTO oldMainSubscription = user.getCustomer().getMainSubscription();
            CustomerDTO parent = user.getCustomer().getParent();

            if (null != parent
                    && (user.getCustomer().getInvoiceChild() == null || user.getCustomer().getInvoiceChild().intValue() == 0)
                    && (newMainSubscription.getSubscriptionPeriod().getId() != parent
                            .getMainSubscription()
                            .getSubscriptionPeriod()
                            .getId()
                    || newMainSubscription.getNextInvoiceDayOfPeriod().compareTo(
                                    parent.getMainSubscription().getNextInvoiceDayOfPeriod()) != 0
                    || dto.getCustomer()
                            .getNextInvoiceDate()
                            .compareTo(parent.getNextInvoiceDate()) != 0)) {
                throw new SessionInternalError(
                        "Child Billing cycle values should match with Parent Billing cycle values",
                        new String[] { "CustomerWS,billingCycleValue,child.billing.cycle.is.diffrent.than.parent.billing.cycle" });
            }

            user.getCustomer().setMainSubscription(dto.getCustomer().getMainSubscription());
            Date oldNextInvoiceDate = user.getCustomer().getNextInvoiceDate();
            Date newNextInvoiceDate = dto.getCustomer().getNextInvoiceDate();
            boolean nextInvocieDateUpdateFlag = false;
            if (periodUnitId != oldPeriodUnitId || nextInvoiceDayOfPeriod != oldNextInvoiceDayOfPeriod) {
                setCustomerNextInvoiceDate(user);
                nextInvocieDateUpdateFlag = true;
            }

            // update the next invoice date
            if (dto.getCustomer().getNextInvoiceDate() != null && newNextInvoiceDate!= null && !newNextInvoiceDate.equals(oldNextInvoiceDate)){
                if (!nextInvocieDateUpdateFlag) {
                    user.getCustomer().setNextInvoiceDate(newNextInvoiceDate);
                } else {
                    setMonthAndYearFrom(newNextInvoiceDate, user);
                }
                if (null != executorId) {
                    eLogger.audit(executorId,
                            user.getId(),
                            Constants.TABLE_CUSTOMER, user.getId(),
                            EventLogger.MODULE_USER_MAINTENANCE,
                            EventLogger.NEXT_INVOICE_DATE_CHANGE,
                            null, null, oldNextInvoiceDate);
                } else {
                    eLogger.auditBySystem(user.getEntity().getId(),
                            user.getId(),
                            Constants.TABLE_CUSTOMER,
                            user.getId(),
                            EventLogger.MODULE_USER_MAINTENANCE,
                            EventLogger.NEXT_INVOICE_DATE_CHANGE,
                            null, null, oldNextInvoiceDate);
                }
            }
			 else {
				 if ( null == dto.getCustomer().getNextInvoiceDate())
						throw new SessionInternalError(
								"Billing cycle value should match with next invoice date",
								new String[] { "CustomerWS,billingCycleValue,next.invoice.date.not.null" });
				}

            // When parent billing cycle is updated then update the billing cycle, invoice generation day and
            // next invoice date of the parent customer as well as all its sub-accounts with 'Invoice if Child' flag
            // unchecked.
            if (oldNextInvoiceDate!= null && newNextInvoiceDate!=null  && newNextInvoiceDate.compareTo(oldNextInvoiceDate) != 0
                    || newMainSubscription.getSubscriptionPeriod().getId() != oldMainSubscription
                    .getSubscriptionPeriod()
                    .getId()
                    || newMainSubscription.getNextInvoiceDayOfPeriod().compareTo(
                            oldMainSubscription.getNextInvoiceDayOfPeriod()) != 0) {
                // update the Next invoice date and billing cycle of childs
                updateBillingCycleOfChildAsPerParent(user);
            }

            updateMetaFieldsWithValidation(dto.getEntityId(),
                    user.getCustomer().getAccountType().getId(),
                    dto.getCustomer());

            // delete removed meta fields
            if (dto.getRemovedDatesMap() != null) {
                for (Map.Entry<Integer, ArrayList<Date>> entry : dto.getRemovedDatesMap().entrySet()) {
                    Integer aitId = entry.getKey();
                    for (Date dateKey : entry.getValue()) {
                        user.getCustomer().removeCustomerAccountInfoTypeMetaFields(aitId, dateKey);
                    }
                }
            }

            // update/create ait meta fields with validation for given date
            user.getCustomer().updateAitMetaFieldsWithValidation(dto.getEntityId(),
                    user.getCustomer().getAccountType().getId(), dto.getCustomer());

            LOG.debug("Setting ait meta fields");
            AccountInformationTypeDAS accountInfoTypeDas = new AccountInformationTypeDAS();
            AccountInformationTypeDTO accountInfoType = null;
            for (Map.Entry<Integer, List<MetaFieldValue>> entry : user.getCustomer().getAitMetaFieldMap().entrySet()) {
                accountInfoType = accountInfoTypeDas.find(entry.getKey());
                for (MetaFieldValue value : entry.getValue()) {
                    Date effectiveDate = dto.getEffectiveDateMap().get(entry.getKey());
                    // if there is no effective date, set it to default date
                    if (effectiveDate == null) {
                        effectiveDate = CommonConstants.EPOCH_DATE;
                    }
                    user.getCustomer().addCustomerAccountInfoTypeMetaField(value, accountInfoType, effectiveDate);

                    if (dto.getTimelineDatesMap() != null) {
                        if (dto.getTimelineDatesMap().containsKey(entry.getKey())) {
                            for (Date dateKey : dto.getTimelineDatesMap().get(entry.getKey())) {
                                MetaFieldValue rigged = generateValue(value);
                                user.getCustomer().insertCustomerAccountInfoTypeMetaField(rigged, accountInfoType,
                                        dateKey);
                            }
                        }
                    }
                }
            }

            MetaFieldHelper.removeEmptyAitMetaFields(user.getCustomer());
            user.getCustomer().setInvoiceDesign(dto.getCustomer().getInvoiceDesign());
        }

        if(dto.getPaymentInstruments().size()==0 && getUserWS().getPaymentInstruments().size() !=0){
            PaymentInformationWS paymentInformationWS = getUserWS().getPaymentInstruments().remove(0);
            if(paymentInformationWS.getId() != null && paymentInformationWS.getId() != 0) {
                PaymentInformationDAS das = new PaymentInformationDAS();
                PaymentInformationDTO paymentInformationDTO = das.findNow(paymentInformationWS.getId());
                if(paymentInformationDTO != null) {
                    try {
                        das.delete(paymentInformationDTO);
                    } catch(Exception e) {
                        LOG.error("Could not delete payment instrument. Exception is: " + e);
                    }
                }
            }
        }

        if(dto.getPaymentInstruments().size()==0 && getUserWS().getPaymentInstruments().size() !=0){
            PaymentInformationWS paymentInformationWS = getUserWS().getPaymentInstruments().remove(0);
            if(paymentInformationWS.getId() != null && paymentInformationWS.getId() != 0) {
                PaymentInformationDAS das = new PaymentInformationDAS();
                PaymentInformationDTO paymentInformationDTO = das.findNow(paymentInformationWS.getId());
                if(paymentInformationDTO != null) {
                    try {
                        das.delete(paymentInformationDTO);
                    } catch(Exception e) {
                        LOG.error("Could not delete payment instrument. Exception is: " + e);
                    }
                }
            }
        }

        // payment instruments
        for (PaymentInformationDTO paymentInformation : dto.getPaymentInstruments()) {
            if (paymentInformation.getId() != null) {
                // update payment information, get index of payment instrument in saved user instruments list
                LOG.debug("Existing user instruments are: %s", user.getPaymentInstruments().size());
                Integer index = getPaymentInformationIndex(paymentInformation.getId(), user.getPaymentInstruments());

                removeCCPreAuthorization(paymentInformation, user.getId());

                // update saved payment information
                LOG.debug("Getting payment instrument to update at index: %s", index);
                PaymentInformationDTO saved = user.getPaymentInstruments().get(index);

                saved.setProcessingOrder(paymentInformation.getProcessingOrder());
                // if we have changed payment method type for a payment instrument then old fields
                // should be cleared
                if (saved.getPaymentMethodType().getId() != paymentInformation.getPaymentMethodType().getId()) {
                    saved.setPaymentMethodType(paymentInformation.getPaymentMethodType());
                    saved.getMetaFields().clear();
                }
                saved.updatePaymentMethodMetaFieldsWithValidation(dto.getEntityId(), paymentInformation);

                setCreditCardType(saved);
                paymentInformationDAS.save(saved);

                user.getPaymentInstruments().add(index, saved);

            } else {
                // create a new one
                paymentInformation.setUser(user);
                PaymentInformationDTO saved = paymentInformationDAS.create(paymentInformation, dto.getEntityId());
                setCreditCardType(paymentInformation);

                user.getPaymentInstruments().add(saved);
            }

        }

        eLogger.audit(executorId, user.getId(), Constants.TABLE_BASE_USER, user.getId(),
                EventLogger.MODULE_USER_MAINTENANCE, EventLogger.ROW_UPDATED, null, null, null);

        Set<RoleDTO> roles = new HashSet<RoleDTO>();
        roles.addAll(dto.getRoles());
        updateRoles(dto.getEntityId(), roles, dto.getMainRoleId());

    }

    private void setMonthAndYearFrom(Date newNextInvoiceDate, UserDTO user) {
        Calendar cal = new GregorianCalendar();
        Calendar newInvoiceCal = new GregorianCalendar();
        newInvoiceCal.setTime(newNextInvoiceDate);
        cal.setTime(user.getCustomer().getNextInvoiceDate());
        cal.set(Calendar.MONTH, newInvoiceCal.get(Calendar.MONTH));
        cal.set(Calendar.YEAR, newInvoiceCal.get(Calendar.YEAR));
        user.getCustomer().setNextInvoiceDate(cal.getTime());
    }

    private void setCreditCardType (PaymentInformationDTO saved) {
        PaymentInformationBL piBl = new PaymentInformationBL();
        // if its a credit card, sets is type
        MetaFieldValue value = piBl.getMetaField(saved, MetaFieldType.CC_TYPE);
        String creditCardNumber = piBl.getStringMetaFieldByType(saved, MetaFieldType.PAYMENT_CARD_NUMBER);
        if (value != null && value.getField() != null) {
            LOG.debug("Updating credit card type for instrument: " + saved.getId());
            saved.setMetaField(value.getField(), piBl.getPaymentMethod(creditCardNumber));
        }
    }

    private int countDiffDay (Calendar c1, Calendar c2) {
        int returnInt = 0;
        while (!c1.after(c2)) {
            c1.add(Calendar.DAY_OF_MONTH, 1);
            returnInt++;
        }

        /*
         * if (returnInt > 0) { returnInt = returnInt - 1; }
         */

        return (returnInt);
    }

    private void updateRoles (Integer entityId, Set<RoleDTO> theseRoles, Integer main) throws SessionInternalError {

        if (theseRoles == null || theseRoles.isEmpty()) {
            if (main != null) {
                if (theseRoles == null) {
                    theseRoles = new HashSet<RoleDTO>();
                }
                theseRoles.add(new RoleDTO(0, null, main, null));
            } else {
                return; // nothing to do
            }
        }

        user.getRoles().clear();
        for (RoleDTO aRole : theseRoles) {
            // make sure the role is in the session
            RoleDTO dbRole = new RoleDAS().findByRoleTypeIdAndCompanyId(aRole.getRoleTypeId(), entityId);
            // dbRole.getBaseUsers().add(user);
            user.getRoles().add(dbRole);
        }
    }

    public boolean exists (String userName, Integer entityId) {
        if (userName == null || entityId == null) {
            LOG.debug("User name and entity ID are required, cannot check user existence");
            return true; // just in case this prompts them to try and create a user.
        }

        return new UserDAS().findByUserName(userName, entityId) != null;
    }

    public boolean exists (Integer userId, Integer entityId) {
        if (userId == null || entityId == null) {
            LOG.debug("User ID and entity ID are required, cannot check user existence");
            return true; // just in case this prompts them to try and create a user.
        }

        return new UserDAS().exists(userId, entityId);
    }

    public Integer create (UserDTOEx dto, Integer executorUserId) throws SessionInternalError {

        Integer newId;
        LOG.debug("Creating user %s", dto);
        List<Integer> roles = new ArrayList<Integer>();
        if (dto.getRoles() == null || dto.getRoles().size() == 0) {
            if (dto.getMainRoleId() != null) {
                roles.add(dto.getMainRoleId());
            } else {
                LOG.warn("Creating user without any role...");
            }
        } else {
            for (RoleDTO role : dto.getRoles()) {
                roles.add(role.getRoleTypeId());
            }
        }

        LOG.debug("Roles set for user");
        Integer newUserRole = dto.getMainRoleId();
        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(newUserRole);
        dto.setEncryptionScheme(passwordEncoderId);

        // may be this is a partner
        if (dto.getPartner() != null) {
            newId = create(dto.getEntityId(), dto.getUserName(), dto.getPassword(), dto.getLanguageId(), roles,
                    dto.getCurrencyId(), dto.getStatusId(), dto.getSubscriptionStatusId(), executorUserId,
                    dto.getPaymentInstruments());
            PartnerBL partner = new PartnerBL();
            Integer partnerId = partner.create(dto.getPartner());
            partner.getEntity().setId(partnerId);
            user.setPartner(partner.getEntity());
            partner.getEntity().setBaseUser(user);
            user.getPartner().updateMetaFieldsWithValidation(dto.getEntityId(), null, dto.getPartner());

            createUserCode(user);
        } else if (dto.getCustomer() != null) {
            // link the partner
            PartnerBL partner = null;
            if (dto.getCustomer().getPartner() != null) {
                try {
                    partner = new PartnerBL(dto.getCustomer().getPartner().getId());
                    // see that this partner is valid
                    if (partner.getEntity().getUser().getEntity().getId() != dto.getEntityId()
                            || partner.getEntity().getUser().getDeleted() == 1) {
                        partner = null;
                    }
                } catch (Exception ex) {
                    throw new SessionInternalError("It doesn't exist a partner with the supplied id.",
                            new String[] { "UserWS,partnerId,validation.error.partner.does.not.exist" });
                }
            }
            newId = create(dto.getEntityId(), dto.getUserName(), dto.getPassword(), dto.getLanguageId(), roles,
                    dto.getCurrencyId(), dto.getStatusId(), dto.getSubscriptionStatusId(), executorUserId,
                    dto.getPaymentInstruments());

            user.setCustomer(new CustomerDAS().create());

            user.getCustomer().setBaseUser(user);
            user.getCustomer().setReferralFeePaid(dto.getCustomer().getReferralFeePaid());

            if (dto.getCustomer().getInvoiceDeliveryMethod() != null) {
                user.getCustomer().setInvoiceDeliveryMethod(dto.getCustomer().getInvoiceDeliveryMethod());
            }

            if (partner != null) {
                user.getCustomer().setPartner(partner.getEntity());
            }

            // set the sub-account fields
            user.getCustomer().setIsParent(dto.getCustomer().getIsParent());
            if (dto.getCustomer().getParent() != null) {
                // the API accepts the user ID of the parent instead of the customer ID
                user.getCustomer().setParent(new UserDAS().find(dto.getCustomer().getParent().getId()).getCustomer());
                user.getCustomer().setInvoiceChild(dto.getCustomer().getInvoiceChild());
                user.getCustomer().setUseParentPricing(dto.getCustomer().useParentPricing());
            }

            user.getCustomer().setDueDateUnitId(dto.getCustomer().getDueDateUnitId());
            user.getCustomer().setDueDateValue(dto.getCustomer().getDueDateValue());

            // set dynamic balance fields
            user.getCustomer().setCreditLimit(dto.getCustomer().getCreditLimit());
            user.getCustomer().setDynamicBalance(dto.getCustomer().getDynamicBalance());
            user.getCustomer().setAutoRecharge(dto.getCustomer().getAutoRecharge());
            
            user.getCustomer().setMonthlyLimit(dto.getCustomer().getMonthlyLimit());
            user.getCustomer().setRechargeThreshold(dto.getCustomer().getRechargeThreshold());
            
            AccountTypeDTO accountType = dto.getCustomer().getAccountType();

            // set credit notification limit 1 & 2
            if (accountType != null) {
	            user.getCustomer().setCreditNotificationLimit1(null != dto.getCustomer().getCreditNotificationLimit1() ?
			            dto.getCustomer().getCreditNotificationLimit1() : accountType.getCreditNotificationLimit1());
	            user.getCustomer().setCreditNotificationLimit2(null != dto.getCustomer().getCreditNotificationLimit2() ?
			            dto.getCustomer().getCreditNotificationLimit2() : accountType.getCreditNotificationLimit2());
            }

            // additional customer fields
            user.getCustomer().setMainSubscription(dto.getCustomer().getMainSubscription());

            // next Invoice Date
            setCustomerNextInvoiceDate(user);

            // Validation if The Billing cycle of sub-accounts should match with parent account billing cycle.
            MainSubscriptionDTO newMainSubscription = dto.getCustomer().getMainSubscription();
            CustomerDTO parent = user.getCustomer().getParent();
            if (null != parent
                    && (user.getCustomer().getInvoiceChild() == null || user.getCustomer().getInvoiceChild().intValue() == 0)
                    && (newMainSubscription.getSubscriptionPeriod().getId() != parent
                            .getMainSubscription()
                            .getSubscriptionPeriod()
                            .getId()
                        || newMainSubscription.getNextInvoiceDayOfPeriod().compareTo(
                            parent.getMainSubscription().getNextInvoiceDayOfPeriod()) != 0)) {
                throw new SessionInternalError(
                        "Child Billing cycle value should match with Parent Billing cycle value",
                        new String[] { "CustomerWS,billingCycleValue,child.billing.cycle.is.diffrent.than.parent.billing.cycle" });
            }

            user.getCustomer().setAutoPaymentType(dto.getCustomer().getAutoPaymentType());

            // save linked user codes
            UserBL.updateAssociateUserCodesToLookLikeTarget(user.getCustomer(), dto.getCustomer().getUserCodeLinks(),
                    "CustomerWS,userCode");

            // meta fields
            Integer accountTypeId = null != accountType ? accountType.getId() : null;
            updateMetaFieldsWithValidation(dto.getEntityId(), accountTypeId, dto.getCustomer());
            user.getCustomer().updateAitMetaFieldsWithValidation(dto.getEntityId(), accountTypeId, dto.getCustomer());

            // save ait meta field with given dates
            LOG.debug("Setting AIT meta fields for given dates in customer");
            AccountInformationTypeDAS accountInfoTypeDas = new AccountInformationTypeDAS();
            AccountInformationTypeDTO accountInfoType = null;
            List<Date> timelineDates = new ArrayList<Date>(0);

            for (Map.Entry<Integer, List<MetaFieldValue>> entry : user.getCustomer().getAitMetaFieldMap().entrySet()) {
                Integer aitId = entry.getKey();

                accountInfoType = accountInfoTypeDas.find(entry.getKey());
                timelineDates = dto.getTimelineDatesMap().get(aitId);

                // if no dates are provided for the given aitId then use default date
                if (timelineDates == null || timelineDates.size() < 1) {
                    timelineDates = new ArrayList<Date>(0);
                    timelineDates.add(CommonConstants.EPOCH_DATE);
                }

                for (MetaFieldValue value : entry.getValue()) {
                    LOG.debug("Setting meta field: " + value);
                    for (Date date : timelineDates) {
                        MetaFieldValue rigged = generateValue(value);
                        user.getCustomer().addCustomerAccountInfoTypeMetaField(rigged, accountInfoType, date);
                    }
                }
            }

            MetaFieldHelper.removeEmptyAitMetaFields(user.getCustomer());

            user.getCustomer().setInvoiceDesign(dto.getCustomer().getInvoiceDesign());
            user.getCustomer().setAccountType(accountType);

        } else { // all the rest
            newId = create(dto.getEntityId(), dto.getUserName(), dto.getPassword(), dto.getLanguageId(), roles,
                    dto.getCurrencyId(), dto.getStatusId(), dto.getSubscriptionStatusId(), executorUserId,
                    dto.getPaymentInstruments());

            createUserCode(user);
        }
        
        //lock or unlock user account
        user.setAccountLockedTime(null);
        setAccountExpired(validateAccountExpired(dto.getAccountDisabledDate()), dto.getAccountDisabledDate());

        LOG.debug("created user id %s", newId);
        das.save(user);
        das.flush();
        return newId;
    }


    private Integer create (Integer entityId, String userName, String password, Integer languageId,
            List<Integer> roles, Integer currencyId, Integer statusId, Integer subscriberStatusId,
            Integer executorUserId, List<PaymentInformationDTO> paymentInstruments) throws SessionInternalError {

        // Default the language and currency to that one of the entity
        if (languageId == null) {
            EntityBL entity = new EntityBL(entityId);
            languageId = entity.getEntity().getLanguageId();
        }
        if (currencyId == null) {
            EntityBL entity = new EntityBL(entityId);
            currencyId = entity.getEntity().getCurrencyId();
        }

        // default the statuses
        if (statusId == null) {
            statusId = UserDTOEx.STATUS_ACTIVE;
        }
        if (subscriberStatusId == null) {
            subscriberStatusId = UserDTOEx.SUBSCRIBER_NONSUBSCRIBED;
        }

        UserDTO newUser = new UserDTO();
        newUser.setCompany(new CompanyDAS().find(entityId));
        newUser.setUserName(userName);
        newUser.setPassword(password);
        newUser.setLanguage(new LanguageDAS().find(languageId));
        newUser.setCurrency(new CurrencyDAS().find(currencyId));
        newUser.setUserStatus(new UserStatusDAS().find(statusId));
        newUser.setSubscriberStatus(new SubscriberStatusDAS().find(subscriberStatusId));
        newUser.setDeleted(new Integer(0));
        newUser.setCreateDatetime(Calendar.getInstance().getTime());
        newUser.setFailedLoginAttempts(0);
        newUser.setEncryptionScheme(Integer.parseInt(Util.getSysProp(Constants.PASSWORD_ENCRYPTION_SCHEME)));

        user = das.save(newUser);
        LOG.debug("Changes flushed");

        // payment instruments
        for (PaymentInformationDTO paymentInformation : paymentInstruments) {
            paymentInformation.setUser(user);
            setCreditCardType(paymentInformation);
            PaymentInformationDTO saved = paymentInformationDAS.create(paymentInformation, entityId);

            user.getPaymentInstruments().add(saved);
        }

        HashSet<RoleDTO> rolesDTO = new HashSet<RoleDTO>();
        for (Integer roleId : roles) {
            rolesDTO.add(new RoleDAS().findByRoleTypeIdAndCompanyId(roleId, entityId));
        }
        updateRoles(entityId, rolesDTO, null);

        if (null != executorUserId) {
            eLogger.audit(executorUserId, user.getId(), Constants.TABLE_BASE_USER, user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE, EventLogger.ROW_CREATED, null, null, null);
        } else {
            eLogger.auditBySystem(entityId,
                    user.getId(),
                    Constants.TABLE_BASE_USER,
                    user.getId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.ROW_CREATED, null, null, null);
        }
        return user.getUserId();
    }

    /**
     * Create the default user code.
     *
     * @param user
     */
    private void createUserCode (UserDTO user) {
        UserCodeDTO userCode = new UserCodeDTO();
        userCode.setIdentifier(user.getUserName() + "00001");
        userCode.setValidFrom(new Date());
        userCode.setUser(user);
        new UserCodeDAS().save(userCode);
    }

    @Deprecated
    public boolean validateUserNamePassword (UserDTOEx loggingUser, UserDTOEx db) {

        // the user status is not part of this check, as a customer that
        // can't login to the entity's service still has to be able to
        // as a customer to submit a payment or update her credit card
        if (db.getDeleted() == 0 && loggingUser.getEntityId().equals(db.getEntityId())) {

        	System.out.println("validateUserNamePassword **************");
            String encodedPassword = db.getPassword();
            String plainPassword = loggingUser.getPassword();

            //using service specific for DB-user, loging one may not have its role set
            Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(db.getMainRoleId());

            if (JBCrypto.passwordsMatch(passwordEncoderId, encodedPassword, plainPassword)){
                user = getUserEntity(db.getUserId());
                return true;
            }
        }

        return false;
    }

    /**
     * Tries to authenticate username/password for web services call. The user must be an administrator and have
     * permission 120 set. Returns the user's UserDTO if successful, otherwise null.
     */
    @Deprecated
    public UserDTO webServicesAuthenticate(String username, String plainPassword) {
        // try to get root user for this username that has web
        // services permission
        user = das.findWebServicesRoot(username);
        if (user == null) {
            LOG.warn("Web services authentication: Username \"%s"
                    + "\" is either invalid, isn't an administrator or doesn't "
                    + "have web services permission granted (120).", username);
            return null;
        }

        // check password
        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(Constants.TYPE_ROOT);
        if (JBCrypto.passwordsMatch(passwordEncoderId, user.getPassword(), plainPassword)) {
            return user;
        }
        LOG.warn("Web services authentication: Invlid password for username \"%s\"", username);
        return null;
    }

    public UserDTO getEntity() {
        return user;
    }
     /**
      * sent the lost password to the user
      * @param entityId
      * @param userId
      * @param languageId
      * @throws SessionInternalError
      * @throws NotificationNotFoundException when no message row or message row is not activated for the specified entity
      */
     public void sendLostPassword(Integer entityId, Integer userId,
             Integer languageId, String link) throws SessionInternalError,
             NotificationNotFoundException {
         try{
             NotificationBL notif = new NotificationBL();
             MessageDTO message = notif.getForgetPasswordEmailMessage(entityId, userId, languageId, link);
             INotificationSessionBean notificationSess =
                     (INotificationSessionBean) Context.getBean(
                     Context.Name.NOTIFICATION_SESSION);
             notificationSess.notify(userId, message);
         } catch (NotificationNotFoundException e){
             LOG.error("Exception while sending notification : " + e.getMessage());
             throw new SessionInternalError("Notification not found for sending lost password");
         }
     }

    /**
     * Sends the initial credentials
     *
     * @param entityId
     * @param userId
     * @param languageId
     * @throws SessionInternalError
     * @throws NotificationNotFoundException when no message row or message row is not activated for the specified entity
     */
    public void sendCredentials(Integer entityId, Integer userId,
                                 Integer languageId, String link) throws SessionInternalError,
            NotificationNotFoundException {
        try{
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getInitialCredentialsEmailMessage(entityId, userId, languageId, link);
            INotificationSessionBean notificationSess =
                    (INotificationSessionBean) Context.getBean(
                            Context.Name.NOTIFICATION_SESSION);
            notificationSess.notify(userId, message);
        } catch (NotificationNotFoundException e){
            LOG.error("Exception while sending notification : " + e.getMessage());
            throw new SessionInternalError("Notification not found for sending lost password");
        }
    }

    public UserWS getUserWS () throws SessionInternalError {
        UserDTOEx dto = DTOFactory.getUserDTOEx(user);
        UserWS retValue = getWS(dto);

        //Check whether account should be locked or not & update user account lock time to null if it should be unlocked
        if(null != dto.getAccountLockedTime()){
           retValue.setIsAccountLocked(isAccountLocked());
        } else {
           retValue.setIsAccountLocked(false);
        }

        // the contact is not included in the Ex
        ContactBL bl = new ContactBL();

        bl.set(dto.getUserId());
        if (bl.getEntity() != null) { // this user has no contact ...
            retValue.setContact(ContactBL.getContactWS(bl.getDTO()));
        }

        List <CustomerNoteDTO> customerNoteDTOs=new CustomerNoteDAS().findByCustomer(retValue.getCustomerId(),dto.getEntityId());
        List <CustomerNoteWS> customerNoteWSList =new ArrayList<CustomerNoteWS>();
        for(CustomerNoteDTO customerNoteDTO:customerNoteDTOs)
        {
            customerNoteWSList.add(convertCustomerNoteToWS(customerNoteDTO));
        }
        retValue.setCustomerNotes(customerNoteWSList.toArray(new CustomerNoteWS[customerNoteWSList.size()]));

        return retValue;
    }

    public Integer getMainRole () {
        if (mainRole == null) {
            List roleIds = new LinkedList();
            for (RoleDTO nextRoleObject : user.getRoles()) {
                roleIds.add(nextRoleObject.getRoleTypeId());
            }
            mainRole = selectMainRole(roleIds);
        }
        return mainRole;
    }

    /**
     * Get the locale for this user.
     *
     * @return users locale
     */
    public Locale getLocale () {
        return getLocale(user);
    }

    public Integer getCurrencyId () {
        Integer retValue;

        if (user.getCurrencyId() == null) {
            retValue = user.getEntity().getCurrency().getId();
        } else {
            retValue = user.getCurrencyId();
        }

        return retValue;
    }

    /**
     * Will mark the user as deleted (deleted = 1), and do the same with all her orders, etc ... Not deleted for
     * reporting reasong: invoices, payments
     */
    public void delete (Integer executorId) throws SessionInternalError {

        List<Integer> childList = das.findChildList(user.getId());
        if (CollectionUtils.isNotEmpty(childList)) {
            LOG.debug("User Id " + user.getId() + " cannot be deleted. Child users exists.");
            String errorMessages[] = new String[1];
            errorMessages[0] = "UserWS,childIds,validation.error.parent.user.cannot.be.deleted," + childList;
            throw new SessionInternalError("Cannot delete Parent User. Child ID(s) " + childList +" exists.", errorMessages);
        }

        user.setDeleted(1);
        user.setLastStatusChange(Calendar.getInstance().getTime());
        user.setSubscriberStatus(new SubscriberStatusDAS().find(UserDTOEx.SUBSCRIBER_EXPIRED));

        // orders
        for (OrderDTO order : user.getOrders()) {
            order.setDeleted(1);
            order.setDeletedDate(new Date());
        }
        // roles
        user.getRoles().clear();

        if (executorId != null) {
            eLogger.audit(executorId, user.getId(), Constants.TABLE_BASE_USER, user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE, EventLogger.ROW_DELETED, null, null, null);
        }

        // Delete AIT meta fields, uncomment following lines if
        // you want to remove ait meta fileds from database
        // when a user is deleted
        /*
         * if(user.getOrders().size() < 1) { user.getCustomer().removeAitMetaFields(); }
         */
    }

    public UserDTO getDto () {
        return user;
    }

    /**
     * Verifies that both user belong to the same entity.
     *
     * @param rootUserName
     *            This has to be a root user
     * @param callerUserId
     * @return
     */
    public boolean validateUserBelongs (String rootUserName, Integer callerUserId) throws SessionInternalError {

        boolean retValue;
        user = das.find(callerUserId);
        set(rootUserName, user.getEntity().getId());
        if (user == null) {
            return false;
        }
        if (user.getDeleted() == 1) {
            throw new SessionInternalError("the caller is set as deleted");
        }
        if (!getMainRole().equals(Constants.TYPE_ROOT)) {
            throw new SessionInternalError("can't validate but root users");
        }
        retValue = true;

        return retValue;
    }

    public UserWS[] convertEntitiesToWS (Collection dtos) throws SessionInternalError {
        try {
            UserWS[] ret = new UserWS[dtos.size()];
            int index = 0;
            for (Iterator it = dtos.iterator(); it.hasNext();) {
                user = (UserDTO) it.next();
                ret[index] = entity2WS();
                index++;
            }

            return ret;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }
    
    public static CustomerNoteWS convertCustomerNoteToWS(CustomerNoteDTO customerNoteDTO) {
    	
        if (customerNoteDTO == null) {
            return null;
        }
        return new CustomerNoteWS(customerNoteDTO.getNoteId(),customerNoteDTO.getNoteTitle(),
                customerNoteDTO.getNoteContent(),customerNoteDTO.getCreationTime(),customerNoteDTO.getCompany().getId(),
                customerNoteDTO.getCustomer().getId(),customerNoteDTO.getUser().getId());
    }
    public static MainSubscriptionWS convertMainSubscriptionToWS(MainSubscriptionDTO mainSubscription) {

        if (mainSubscription == null) {
            return null;
        }

        return new MainSubscriptionWS(mainSubscription.getSubscriptionPeriod().getId(),
                mainSubscription.getNextInvoiceDayOfPeriod());
    }

    public static MainSubscriptionDTO convertMainSubscriptionFromWS (MainSubscriptionWS mainSubscriptionWS,
            Integer entityId) {

        if (mainSubscriptionWS == null) {
            return MainSubscriptionDTO.createDefaultMainSubscription(entityId);
        }

        MainSubscriptionDTO mainSub = new MainSubscriptionDTO();
        mainSub.setSubsriptionPeriodFromPeriodId(mainSubscriptionWS.getPeriodId());
        mainSub.setNextInvoiceDayOfPeriod(mainSubscriptionWS.getNextInvoiceDayOfPeriod());
        return mainSub;
    }

    /**
     * It calculates billing until date to which the billing process evaluates the customer
     * <p>
     * This date determines how far the billing process sees in future based on user's main subscription
     *
     * @param nextInvoiceDate
     * @param billingDate
     * @return billing untill date
     */
    public Date getBillingUntilDate (Date nextInvoiceDate, Date billingDate) {

        LOG.debug("Calculating billing until date based on the next invoice date %s and billing date %s",
                nextInvoiceDate, billingDate);

        MainSubscriptionDTO mainSubscription = getMainSubscription();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(nextInvoiceDate);

        if (!cal.getTime().after(billingDate)) {

            while (!cal.getTime().after(billingDate)) {
                if (CalendarUtils.isSemiMonthlyPeriod(mainSubscription.getSubscriptionPeriod().getPeriodUnit())) {
                    cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
                } else {
                    cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()),
                            mainSubscription.getSubscriptionPeriod().getValue());
                }
            }
        } else {
            if (CalendarUtils.isSemiMonthlyPeriod(mainSubscription.getSubscriptionPeriod().getPeriodUnit())) {
                cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
            } else {
                cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()),
                        mainSubscription.getSubscriptionPeriod().getValue());
            }
        }

        return cal.getTime();
    }

    /**
     * Checks if the user has to be included in the billing process
     *
     * @param startDate
     * @return true if the user is billable
     */
    public boolean isBillable (Date startDate) {

        startDate = new Date(startDate.getTime()); // to escape problem in java.sql.Timestamp compare to java.util.Date
        Date nextInvoiceDate = user.getCustomer().getNextInvoiceDate();
        if (nextInvoiceDate == null) {
            return true;
        }
        nextInvoiceDate = new Date(nextInvoiceDate.getTime()); // same as for startDate

        BillingProcessConfigurationDTO config = new ConfigurationBL(user.getEntity().getId()).getDTO();

        PeriodUnit billingPeriodUnit = PeriodUnit.valueOfPeriodUnit(
                new DateTime(startDate).getDayOfMonth(), config.getPeriodUnit().getId());
        Date endDate = DateConvertUtils.asUtilDate(billingPeriodUnit
                .addTo(DateConvertUtils.asLocalDate(startDate), 1)
                .minusDays(1));

        LOG.debug(Util.S("user[{}].nid: {}, interval: {} - {}", user.getId(), nextInvoiceDate, startDate, endDate));

        return !(nextInvoiceDate.before(startDate) || nextInvoiceDate.after(endDate));
    }

    private MainSubscriptionDTO getMainSubscription () {

        MainSubscriptionDTO mainSubscription = user.getCustomer().getMainSubscription();
        if (mainSubscription == null) {
            throw new SessionInternalError("Main Subscription is not set for customer: " + user);
        }

        return mainSubscription;
    }

    public UserWS entity2WS () {
        UserWS retValue = new UserWS();
        retValue.setCreateDatetime(user.getCreateDatetime());
        retValue.setCurrencyId(getCurrencyId());
        retValue.setDeleted(user.getDeleted());
        retValue.setLanguageId(user.getLanguageIdField());
        retValue.setLastLoginDate(user.getLastLoginDate());
        retValue.setLastStatusChange(user.getLastStatusChange());
        mainRole = null;
        retValue.setMainRoleId(getMainRole());
        if (user.getPartner() != null) {
            retValue.setPartnerId(user.getPartner().getId());
        }
        retValue.setPassword(user.getPassword());
        retValue.setStatusId(user.getStatus().getId());
        retValue.setUserId(user.getUserId());
        retValue.setUserName(user.getUserName());
        // now the contact
        ContactBL contact = new ContactBL();
        contact.set(retValue.getUserId());
        retValue.setContact(ContactBL.getContactWS(contact.getDTO()));

        return retValue;
    }

    public CachedRowSet findActiveWithOpenInvoices (Integer entityId) throws SQLException, NamingException {
        prepareStatement(UserSQL.findActiveWithOpenInvoices);
        cachedResults.setInt(1, entityId);

        execute();
        conn.close();
        return cachedResults;
    }

    public UserTransitionResponseWS[] getUserTransitionsById (Integer entityId, Integer last, Date to) {

        try {
            UserTransitionResponseWS[] result = null;
            java.sql.Date toDate = null;
            String query = UserSQL.findUserTransitions;
            if (last.intValue() > 0) {
                query += UserSQL.findUserTransitionsByIdSuffix;
            }
            if (to != null) {
                query += UserSQL.findUserTransitionsUpperDateSuffix;
                toDate = new java.sql.Date(to.getTime());
            }

            int pos = 2;
            LOG.info("Getting transaction list by Id. query --> %s", query);
            prepareStatement(query);
            cachedResults.setInt(1, entityId);

            if (last.intValue() > 0) {
                cachedResults.setInt(pos, last);
                pos++;
            }
            if (toDate != null) {
                cachedResults.setDate(pos, toDate);
            }

            execute();
            conn.close();

            if (cachedResults == null || !cachedResults.next()) {
                return null;
            }

            // Load the results into a linked list.
            List tempList = new LinkedList();
            UserTransitionResponseWS temp;
            do {
                temp = new UserTransitionResponseWS();
                temp.setId(cachedResults.getInt(1));
                temp.setToStatusId(Integer.parseInt(cachedResults.getString(2)));
                temp.setTransitionDate(new Date(cachedResults.getDate(3).getTime()));
                temp.setUserId(cachedResults.getInt(5));
                temp.setFromStatusId(cachedResults.getInt(4));
                tempList.add(temp);
            } while (cachedResults.next());

            // The list is now ready. Convert into an array and return.
            result = new UserTransitionResponseWS[tempList.size()];
            int count = 0;
            for (Iterator i = tempList.iterator(); i.hasNext();) {
                result[count] = (UserTransitionResponseWS) i.next();
                count++;
            }
            return result;
        } catch (SQLException e) {
            throw new SessionInternalError("Getting transitions", UserBL.class, e);
        }
    }

    public static BigDecimal getBalance (Integer userId) {
        return new InvoiceDAS()
                .findTotalAmountOwed(userId)
                .subtract(new PaymentDAS().findTotalBalanceByUser(userId))
                .setScale(CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND);
    }

    @Deprecated
    public BigDecimal getTotalOwed (Integer userId) {
        return new InvoiceDAS().findTotalAmountOwed(userId);
    }

    public UserTransitionResponseWS[] getUserTransitionsByDate (Integer entityId, Date from, Date to) {

        try {

            UserTransitionResponseWS[] result = null;
            java.sql.Date toDate = null;
            String query = UserSQL.findUserTransitions;
            query += UserSQL.findUserTransitionsByDateSuffix;

            if (to != null) {
                query += UserSQL.findUserTransitionsUpperDateSuffix;
                toDate = new java.sql.Date(to.getTime());
            }
            LOG.info("Getting transaction list by date. query --> %s", query);

            prepareStatement(query);
            cachedResults.setInt(1, entityId);
            cachedResults.setDate(2, new java.sql.Date(from.getTime()));
            if (toDate != null) {
                cachedResults.setDate(3, toDate);
            }
            execute();
            conn.close();

            if (cachedResults == null || !cachedResults.next()) {
                return null;
            }

            // Load the results into a linked list.
            List tempList = new LinkedList();
            UserTransitionResponseWS temp;
            do {
                temp = new UserTransitionResponseWS();
                temp.setId(cachedResults.getInt(1));
                temp.setToStatusId(Integer.parseInt(cachedResults.getString(2)));
                temp.setTransitionDate(new Date(cachedResults.getDate(3).getTime()));
                temp.setUserId(cachedResults.getInt(5));
                temp.setFromStatusId(cachedResults.getInt(4));
                tempList.add(temp);
            } while (cachedResults.next());

            // The list is now ready. Convert into an array and return.
            result = new UserTransitionResponseWS[tempList.size()];
            int count = 0;
            for (Iterator i = tempList.iterator(); i.hasNext();) {
                result[count] = (UserTransitionResponseWS) i.next();
                count++;
            }
            return result;
        } catch (SQLException e) {
            throw new SessionInternalError("Finding transitions", UserBL.class, e);
        }
    }

    public void updateSubscriptionStatus (Integer id, Integer executorId) {
        if (id == null || user.getSubscriberStatus().getId() == id) {
            // no update ... it's already there
            return;
        }
        if (null != executorId) {
            eLogger.audit(executorId, user.getId(), Constants.TABLE_BASE_USER, user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE, EventLogger.SUBSCRIPTION_STATUS_CHANGE, user
                            .getSubscriberStatus()
                            .getId(), id.toString(), null);
        } else {
            eLogger.auditBySystem(user.getEntity().getId(), user.getId(), Constants.TABLE_BASE_USER, user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE, EventLogger.SUBSCRIPTION_STATUS_CHANGE, user
                            .getSubscriberStatus()
                            .getId(), id.toString(), null);
        }

        try {
            user.setSubscriberStatus(new SubscriberStatusDAS().find(id));
        } catch (Exception e) {
            throw new SessionInternalError("Can't update a user subscription status", UserBL.class, e);
        }

        // make sure this is in synch with the ageing status of the user
        try {
            int preferenceLinkAgeingToSubscription = 0;
            try {
                preferenceLinkAgeingToSubscription = PreferenceBL.getPreferenceValueAsIntegerOrZero(user
                        .getEntity()
                        .getId(), Constants.PREFERENCE_LINK_AGEING_TO_SUBSCRIPTION);
            } catch (EmptyResultDataAccessException e) {
                // i'll use the default
            }
            if (preferenceLinkAgeingToSubscription == 1) {
                AgeingBL ageing = new AgeingBL();
                // todo:
                if (id.equals(UserDTOEx.SUBSCRIBER_ACTIVE)) {
                    // remove the user from the ageing
                    ageing.out(user, null);
                } else if (id.equals(UserDTOEx.SUBSCRIBER_EXPIRED) || id.equals(UserDTOEx.SUBSCRIBER_DISCONTINUED)) {
                    AgeingDTOEx[] ageingStatuses = ageing.getOrderedSteps(user.getEntity().getId());
                    if (ageingStatuses != null && ageingStatuses.length > 0) {
                        ageing.setUserStatus(null, user.getUserId(), ageingStatuses[0].getStatusId(), Calendar
                                .getInstance()
                                .getTime());
                    } else {
                        // do nothing
                        LOG
                                .warn("User should be suspended by subscription expire, but ageing is not configured for entity "
                                        + user.getEntity().getId());
                    }
                }
            }
        } catch (Exception e) {
            throw new SessionInternalError("Can't update a user status", UserBL.class, e);
        }

        LOG.debug("Subscription status updated to %s", id);
    }

    // todo: should be moved into a scheduled task that expires passwords and sets a flag on the user
    @Deprecated
    public boolean isPasswordExpired () {
        boolean retValue = false;
        try {
            int expirationDays = 0;
            try {
                expirationDays = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(),
                        Constants.PREFERENCE_PASSWORD_EXPIRATION);
            } catch (EmptyResultDataAccessException e) {
                // go with default
            }

            // zero means that this is not enforced
            if (expirationDays == 0) {
                return false;
            }

            Date lastChange =user.getChangePasswordDate();
            // no changes? then take when the user signed-up
            if (lastChange == null) {
                lastChange = user.getCreateDatetime();
            }

            long days = (Calendar.getInstance().getTimeInMillis() - lastChange.getTime()) / (1000 * 60 * 60 * 24);
            if (days >= expirationDays) {
                retValue = true;
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    /**
     * Call this method when the user has provided the wrong password
     *
     * @return True if the account is now locked (maximum retries) or false if it is not locked.
     */
    public boolean failedLoginAttempt () {
        boolean retValue = false;
        int allowedRetries = 0;
        try {
            allowedRetries = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(),
                    Constants.PREFERENCE_FAILED_LOGINS_LOCKOUT);
        } catch (EmptyResultDataAccessException e) {
            // go with default
        }

        eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                Constants.TABLE_BASE_USER, user.getUserId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.FAILED_USER_LOGIN, null,
                user.getUserName(), null);

        // zero means not to enforce this rule
        if (allowedRetries > 0) {
            int total = user.getFailedLoginAttempts();
            total++;
            user.setFailedLoginAttempts(total);

            //log failed attempts of user
            eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                    Constants.TABLE_BASE_USER, user.getUserId(),
                    EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.FAILED_LOGIN_ATTEMPTS, new Integer(total),
                    null, null);

            if (total >= allowedRetries) {
                retValue = true;

                // Lock out the user
                setAccountLocked(retValue);

                eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                        Constants.TABLE_BASE_USER, user.getUserId(),
                        EventLogger.MODULE_USER_MAINTENANCE,
                        EventLogger.ACCOUNT_LOCKED, new Integer(total),
                        null, null);
                LOG.debug("Locked account for user %s", user.getUserId());
            }
        }

        return retValue;
    }

	/**
	 * Checks if the user has been set the 'lockout_password'. Checking
	 * is always done by using the current hashing method configured for
	 * that user. If the user is configured with lockout_password then
	 * the account is considered as lockout out.
	 *
	 * @return true - if 'lockout_password' is set, false otherwise
	 */
	public boolean isLockoutPasswordSet() {
		String lockoutPassword = Util.getSysProp(Constants.PROPERTY_LOCKOUT_PASSWORD);
		Integer userEncodeMethodId = getEntity().getEncryptionScheme();
		return JBCrypto.passwordsMatch(userEncodeMethodId, user.getPassword(), lockoutPassword);
	}

    public void successLoginAttempt () {
        user.setLastLoginDate(Calendar.getInstance().getTime());
        user.setFailedLoginAttempts(0);
        user.setAccountLockedTime(null);
        eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                Constants.TABLE_BASE_USER, user.getUserId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.SUCESSFULL_USER_LOGIN,null,
                user.getUserName(), null);
    }

    public boolean canInvoice () {
        // can't be deleted and has to be a customer
        if (user.getDeleted() == 1
                || user.getCustomer() == null
                || !getMainRole().equals(Constants.TYPE_CUSTOMER)) {
            return false;
        }

        // child accounts only get invoiced if the explicit flag is on
        if (user.getCustomer().getParent() != null &&
                (user.getCustomer().getInvoiceChild() == null ||
                        user.getCustomer().getInvoiceChild().intValue() == 0)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the user has been invoiced for anything at the time given as a parameter.
     *
     * @return
     */
    public boolean isCurrentlySubscribed (Date forDate) {

        List<Integer> results = new InvoiceDAS().findIdsByUserAndPeriodDate(user.getUserId(), forDate);
        boolean retValue = !results.isEmpty();

        LOG.debug(" user %s is subscribed result %s", user.getUserId(), retValue);

        return retValue;
    }

    public CachedRowSet getByStatus (Integer entityId, Integer statusId, boolean in) {
        try {
            if (in) {
                prepareStatement(UserSQL.findInStatus);
            } else {
                prepareStatement(UserSQL.findNotInStatus);
            }
            cachedResults.setInt(1, statusId.intValue());
            cachedResults.setInt(2, entityId.intValue());
            execute();
            conn.close();
            return cachedResults;
        } catch (Exception e) {
            throw new SessionInternalError("Error getting user by status", UserBL.class, e);
        }
    }

    public CachedRowSet getByCCNumber (Integer entityId, String number) {
        try {

            prepareStatement(UserSQL.findByCreditCard);
            cachedResults.setString(1, number);
            cachedResults.setInt(2, entityId.intValue());
            execute();
            conn.close();

            return cachedResults;
        } catch (Exception e) {
            throw new SessionInternalError("Error getting user by cc", UserBL.class, e);
        }
    }

    public Integer getByEmail (String email) {
        try {
            Integer retValue = null;
            prepareStatement(UserSQL.findByEmail);
            // this is being use for paypal subscriptions. It only has an email
            // so there is not way to limit by entity_id
            cachedResults.setString(1, email);
            execute();
            if (cachedResults.next()) {
                retValue = cachedResults.getInt(1);
            }
            cachedResults.close();
            conn.close();
            return retValue;
        } catch (Exception e) {
            throw new SessionInternalError("Error getting user by cc", UserBL.class, e);
        }
    }

    /**
     * Only needed due to the locking of entity beans. Remove when using JPA
     *
     * @param userId
     * @return
     * @throws SQLException
     * @throws NamingException
     */
    public Integer getEntityId (Integer userId) {
        if (userId == null) {
            userId = user.getUserId();
        }
        UserDTO user = das.find(userId);
        return user.getCompany().getId();

    }

    /**
     * Adds/removes blacklist entries directly related to this user.
     */
    public void setUserBlacklisted (Integer executorId, Boolean isBlacklisted) {
        BlacklistDAS blacklistDAS = new BlacklistDAS();
        List<BlacklistDTO> blacklist = blacklistDAS.findByUserType(user.getId(), BlacklistDTO.TYPE_USER_ID);
        if (isBlacklisted) {
            if (blacklist.isEmpty()) {
                // add a new blacklist entry
                LOG.debug("Adding blacklist record for user id: %s", user.getId());

                BlacklistDTO entry = new BlacklistDTO();
                entry.setCompany(user.getCompany());
                entry.setCreateDate(new Date());
                entry.setType(BlacklistDTO.TYPE_USER_ID);
                entry.setSource(BlacklistDTO.SOURCE_CUSTOMER_SERVICE);
                entry.setUser(user);
                if(user.getPaymentInstruments().size()>=1)
                	entry.setCreditCard(user.getPaymentInstruments().get(0));
                entry = blacklistDAS.save(entry);

                eLogger.audit(executorId, user.getId(), Constants.TABLE_BLACKLIST, entry.getId(),
                        EventLogger.MODULE_BLACKLIST, EventLogger.BLACKLIST_USER_ID_ADDED, null, null, null);
            }
        } else {
            if (!blacklist.isEmpty()) {
                // remove any blacklist entries found
                LOG.debug("Removing blacklist records for user id: %s", user.getId());

                for (BlacklistDTO entry : blacklist) {
                    blacklistDAS.delete(entry);

                    eLogger.audit(executorId, user.getId(), Constants.TABLE_BLACKLIST, entry.getId(),
                            EventLogger.MODULE_BLACKLIST, EventLogger.BLACKLIST_USER_ID_REMOVED, null, null, null);
                }
            }
        }
    }


    public ValidatePurchaseWS validatePurchase(List<ItemDTO> items,
                                               List<BigDecimal> amounts,
                                               List<List<PricingField>> pricingFields) {

        if (user.getCustomer() == null) {
            return null;
        }

        LOG.debug("validating purchase items: %s amounts %s customer %s", Arrays.toString(items.toArray()), amounts,
                user.getCustomer());

        ValidatePurchaseWS result = new ValidatePurchaseWS();

        // call plug-ins
        try {
            PluggableTaskManager<IValidatePurchaseTask> taskManager = new PluggableTaskManager<IValidatePurchaseTask>(
                    user.getCompany().getId(), Constants.PLUGGABLE_TASK_VALIDATE_PURCHASE);
            IValidatePurchaseTask myTask = taskManager.getNextClass();

            while (myTask != null) {
                myTask.validate(user.getCustomer(), items, amounts, result, pricingFields);
                myTask = taskManager.getNextClass();
            }
        } catch (Exception e) {
            // log stacktrace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            LOG.error("Validate Purchase error: %s\n%s", e.getMessage(), sw.toString());

            result.setSuccess(false);
            result.setAuthorized(false);
            result.setQuantity(BigDecimal.ZERO);
            result.setMessage(new String[] { "Error: " + e.getMessage() });
        }

        return result;
    }

    public Integer getLanguage () {
        return user.getLanguageIdField();
    }

    /**
     * Checks if the string passed is less than 1000 characters
     *
     * @param notes
     * @return boolean
     */
    public static boolean ifValidNotes (String notes) {
        return notes == null || notes.length() <= 1000;
    }

    public boolean isEmailUsedByOthers (String email) {

        try {
            Integer retValue = null;
            prepareStatement(UserSQL.findOthersByEmail);

            cachedResults.setString(1, email.toLowerCase());
            cachedResults.setInt(2, user.getId());
            cachedResults.setInt(3, user.getEntity().getId());
            execute();
            if (cachedResults.next()) {
                retValue = cachedResults.getInt(1);
            }
            cachedResults.close();
            conn.close();

            boolean used = retValue != null;
            if (used)
                return used;// no further checks needed

            // check if the email is defined in meta field
            return getEmailMetaFieldValueIds(user.getEntity().getId(), email.toLowerCase()).size() > 0;
        } catch (Exception e) {

            throw new SessionInternalError("Error getting user by email, id, entity_id", UserBL.class, e);
        }
    }

    public List<UserDTO> findUsersByEmail (String email, Integer entity) {
        UserDAS userDAS = new UserDAS();

        List<UserDTO> users = new ArrayList<UserDTO>();
        users.addAll(userDAS.findByEmail(email, entity));

        List<UserDTO> metaFieldUsers = findByMetaFieldEmailValue(email, entity);
        if (null != metaFieldUsers) {
            users.addAll(metaFieldUsers);
        }

        return users;
    }

    public List<UserDTO> findByMetaFieldEmailValue (String email, Integer entityId) {
        List<Integer> valueIds = getEmailMetaFieldValueIds(entityId, email.toLowerCase());

        if (null != valueIds && valueIds.size() > 0) {
            UserDAS userDAS = new UserDAS();

            List<UserDTO> users = new ArrayList<UserDTO>(0);
            List<UserDTO> mfUsers = userDAS.findByMetaFieldValueIds(entityId, valueIds);
            List<UserDTO> aitMFUsers = userDAS.findByAitMetaFieldValueIds(entityId, valueIds);

            if (mfUsers != null) {
                users.addAll(mfUsers);
            }

            if (aitMFUsers != null) {
                users.addAll(aitMFUsers);
            }

            return users;
        }

        return new ArrayList<UserDTO>();
    }

    public List<Integer> getEmailMetaFieldValueIds (Integer entityId, String email) {
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

        List<Integer> emailMetaFieldIds = metaFieldDAS.getByFieldType(entityId,
                new MetaFieldType[] { MetaFieldType.EMAIL });

        if (null != emailMetaFieldIds && emailMetaFieldIds.size() > 0) {
            return metaFieldDAS.findByValueAndField(DataType.STRING, email.toLowerCase(), Boolean.FALSE,
                    emailMetaFieldIds);
        }

        return new ArrayList<Integer>();
    }

    public AccountTypeDTO getAccountType () {
        if (user.getCustomer() != null) {
            return user.getCustomer().getAccountType();
        }

        return null;
    }

    public static UserWS getWS (UserDTOEx dto) {

        UserWS userWS = new UserWS();
        userWS.setId(dto.getId());
        userWS.setCurrencyId(dto.getCurrencyId());
        userWS.setPassword(dto.getPassword());
        userWS.setDeleted(dto.getDeleted());
        userWS.setCreateDatetime(dto.getCreateDatetime());
        userWS.setLastStatusChange(dto.getLastStatusChange());
        userWS.setLastLoginDate(dto.getLastLoginDate());
        userWS.setUserName(dto.getUserName());
        userWS.setFailedAttempts(dto.getFailedLoginAttempts());
        userWS.setLanguageId(dto.getLanguageId());
        userWS.setRole(dto.getMainRoleStr());
        userWS.setMainRoleId(dto.getMainRoleId());
        userWS.setLanguage(dto.getLanguageStr());
        userWS.setStatus(dto.getStatusStr());
        userWS.setRole(dto.getMainRoleStr());
        userWS.setStatusId(dto.getStatusId());
        userWS.setSubscriberStatusId(dto.getSubscriptionStatusId());

	    //may be overwritten later on in case of a customer user
	    userWS.setEntityId(null != dto.getCompany() ? dto.getCompany().getId() : null);

        if(null != dto.getAccountDisabledDate()) {
            userWS.setAccountExpired(true);
            userWS.setAccountDisabledDate(dto.getAccountDisabledDate());
        }

        if (dto.getCustomer() != null) {
            userWS.setCustomerId(dto.getCustomer().getId());
            userWS.setPartnerId((dto.getCustomer().getPartner() == null) ? null : dto
                    .getCustomer()
                    .getPartner()
                    .getId());
            userWS.setParentId((dto.getCustomer().getParent() == null) ? null : dto
                    .getCustomer()
                    .getParent()
                    .getBaseUser()
                    .getId());
            userWS.setMainSubscription(convertMainSubscriptionToWS(dto.getCustomer().getMainSubscription()));
            userWS.setIsParent(dto.getCustomer().getIsParent() != null && dto.getCustomer().getIsParent().equals(1));
            userWS.setInvoiceChild(dto.getCustomer().getInvoiceChild() != null
                    && dto.getCustomer().getInvoiceChild().equals(1));
            userWS.setUseParentPricing(dto.getCustomer().useParentPricing());
            userWS.setExcludeAgeing(dto.getCustomer().getExcludeAging() == 1);
            userWS.setNextInvoiceDate(dto.getCustomer().getNextInvoiceDate());

            Integer[] childIds = new Integer[dto.getCustomer().getChildren().size()];
            int index = 0;
            for (CustomerDTO customer : dto.getCustomer().getChildren()) {
                childIds[index] = customer.getBaseUser().getId();
                index++;
            }
            userWS.setChildIds(childIds);

            userWS.setDynamicBalance(dto.getCustomer().getDynamicBalance());
            userWS.setCreditLimit(dto.getCustomer().getCreditLimit());
            userWS.setAutoRecharge(dto.getCustomer().getAutoRecharge());
            userWS.setRechargeThreshold(dto.getCustomer().getRechargeThreshold());
            userWS.setMonthlyLimit(dto.getCustomer().getMonthlyLimit());
            

            List <CustomerNoteDTO> customerNoteDTOs=new CustomerNoteDAS().findByCustomer(userWS.getCustomerId(),dto.getEntityId());
            List <CustomerNoteWS> customerNoteWSList =new ArrayList<CustomerNoteWS>();
            for(CustomerNoteDTO customerNoteDTO: customerNoteDTOs) {
                customerNoteWSList.add(convertCustomerNoteToWS(customerNoteDTO));
            }
            userWS.setCustomerNotes(customerNoteWSList.toArray(new CustomerNoteWS[customerNoteWSList.size()]));

            userWS.setAutomaticPaymentType(dto.getCustomer().getAutoPaymentType());

            userWS.setInvoiceDeliveryMethodId(dto.getCustomer().getInvoiceDeliveryMethod() == null ? null : dto
                    .getCustomer()
                    .getInvoiceDeliveryMethod()
                    .getId());
            userWS.setDueDateUnitId(dto.getCustomer().getDueDateUnitId());
            userWS.setDueDateValue(dto.getCustomer().getDueDateValue());

            if (!dto.getCustomer().getUserCodeLinks().isEmpty()) {
                Set<UserCodeCustomerLinkDTO> userCodeLinks = dto.getCustomer().getUserCodeLinks();

                userWS.setUserCodeLink(userCodeLinks.iterator().next().getUserCode().getIdentifier());
            }
            Integer entityId;
            if (null == dto.getCompany()) {
                entityId = new UserBL().getEntityId(dto.getCustomer().getId());
            } else {
                entityId = dto.getCompany().getId();
            }
            userWS.setEntityId(entityId);
            userWS.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(dto.getCustomer()));

            userWS.setAccountTypeId(dto.getCustomer().getAccountType() != null ? dto.getCustomer().getAccountType().getId() : null);
            userWS.setInvoiceDesign(dto.getCustomer().getInvoiceDesign());

            // convert to Map<ait,Map<date, values>> map and set it in UserWS
            Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> accountInfoTypeFieldsMap = new HashMap<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>>();
            for (CustomerAccountInfoTypeMetaField accountInfoTypeField : dto
                    .getCustomer()
                    .getCustomerAccountInfoTypeMetaFields()) {
                Integer groupId = accountInfoTypeField.getAccountInfoType().getId();
                if (accountInfoTypeFieldsMap.containsKey(accountInfoTypeField.getAccountInfoType().getId())) {
                    Map<Date, ArrayList<MetaFieldValueWS>> metaFieldMap = accountInfoTypeFieldsMap
                            .get(accountInfoTypeField.getAccountInfoType().getId());
                    ArrayList<MetaFieldValueWS> valueList;

                    if (metaFieldMap.containsKey(accountInfoTypeField.getEffectiveDate())) {
                        valueList = metaFieldMap.get(accountInfoTypeField.getEffectiveDate());
                        valueList.add(MetaFieldBL.getWS(accountInfoTypeField.getMetaFieldValue(), groupId));
                    } else {
                        valueList = new ArrayList<MetaFieldValueWS>();
                        valueList.add(MetaFieldBL.getWS(accountInfoTypeField.getMetaFieldValue(), groupId));
                    }

                    metaFieldMap.put(accountInfoTypeField.getEffectiveDate(), valueList);
                    accountInfoTypeFieldsMap.put(accountInfoTypeField.getAccountInfoType().getId(),
                            (HashMap<Date, ArrayList<MetaFieldValueWS>>) metaFieldMap);
                } else {
                    HashMap<Date, ArrayList<MetaFieldValueWS>> metaFieldMap = new HashMap<Date, ArrayList<MetaFieldValueWS>>();
                    List<MetaFieldValueWS> valueList = new ArrayList<MetaFieldValueWS>();

                    valueList.add(MetaFieldBL.getWS(accountInfoTypeField.getMetaFieldValue(), groupId));
                    metaFieldMap.put(accountInfoTypeField.getEffectiveDate(), (ArrayList<MetaFieldValueWS>) valueList);

                    accountInfoTypeFieldsMap.put(accountInfoTypeField.getAccountInfoType().getId(), metaFieldMap);
                }
            }
            userWS.setAccountInfoTypeFieldsMap(accountInfoTypeFieldsMap);

            // set timelines dates map and effective dates map
            Map<Integer, ArrayList<Date>> timelineDatesMap = new HashMap<Integer, ArrayList<Date>>();
            Map<Integer, Date> effectiveDatesMap = new HashMap<Integer, Date>();
            for (Map.Entry<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> entry : accountInfoTypeFieldsMap
                    .entrySet()) {
                List<Date> dates = new ArrayList<Date>(0);
                for (Map.Entry<Date, ArrayList<MetaFieldValueWS>> en : entry.getValue().entrySet()) {
                    dates.add(en.getKey());
                }
                Collections.sort(dates);
                timelineDatesMap.put(entry.getKey(), (ArrayList<Date>) dates);
                effectiveDatesMap.put(entry.getKey(), findEffectiveDate(dates));
            }
            userWS.setTimelineDatesMap(timelineDatesMap);
            userWS.setEffectiveDateMap(effectiveDatesMap);

            // merge ait latest meta fields with customer meta fields
            List<MetaFieldValueWS> aitMetaFields = new ArrayList<MetaFieldValueWS>();
            for (Map.Entry<Integer, Date> entry : effectiveDatesMap.entrySet()) {
                aitMetaFields.addAll(accountInfoTypeFieldsMap.get(entry.getKey()).get(entry.getValue()));
            }
            LOG.debug("Total ait meta fields found: " + aitMetaFields.size());
            MetaFieldValueWS[] aitMetaFieldsArray = aitMetaFields.toArray(new MetaFieldValueWS[aitMetaFields.size()]);
            MetaFieldValueWS[] combined = new MetaFieldValueWS[userWS.getMetaFields().length
                    + aitMetaFieldsArray.length];
            System.arraycopy(userWS.getMetaFields(), 0, combined, 0, userWS.getMetaFields().length);
            System.arraycopy(aitMetaFieldsArray, 0, combined, userWS.getMetaFields().length, aitMetaFieldsArray.length);
            userWS.setMetaFields(combined);
        }

        // set payment informations
        for (PaymentInformationDTO paymentInformation : dto.getPaymentInstruments()) {
            userWS.getPaymentInstruments().add(PaymentInformationBL.getWS(paymentInformation));
        }

        if (dto.getPartner() != null) {
            userWS.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(dto.getCompany().getId(), dto.getPartner()));
        }

        userWS.setBlacklistMatches(dto.getBlacklistMatches() != null ? dto.getBlacklistMatches().toArray(
                new String[dto.getBlacklistMatches().size()]) : null);
        userWS.setUserIdBlacklisted(dto.getUserIdBlacklisted());

        if (null != dto.getCompany()) {
            userWS.setCompanyName(dto.getCompany().getDescription());
        }

        userWS.setOwingBalance(dto.getBalance());
        return userWS;
    }

    /**
     * Get first credit card from user's instrument list, returns null if no credit card is found
     *
     * @return PaymentInrormationDTO
     */
    public PaymentInformationDTO getCreditCard () {
        if (this.user.getPaymentInstruments() != null && !this.user.getPaymentInstruments().isEmpty()) {
            return new PaymentInformationBL().findCreditCard(this.user.getPaymentInstruments());
        }
        return null;
    }

    /**
     * Get all credit cards from user's instrument list, returns null if no credit card is found
     *
     * @return List<PaymentInrormationDTO>
     */
    public List<PaymentInformationDTO> getAllCreditCards () {
        if (user.getPaymentInstruments() != null && !user.getPaymentInstruments().isEmpty()) {
            return new PaymentInformationBL().findAllCreditCards(user.getPaymentInstruments());
        }
        return null;
    }

    /**
     * Determine if the parentId is not already in currentUserId's hierarchy
     *
     * @param currentUserId
     * @param parentId
     * @return
     */
    public boolean okToAddAsParent (int currentUserId, int parentId) {

        List<Integer> childList = das.findChildList(currentUserId);

        if (childList.isEmpty())
            return true;

        for (Integer childId : childList) {
            if (childId.equals(parentId)) {
                return false;
            } else {
                return this.okToAddAsParent(childId, parentId);
            }
        }

        return true;
    }

    /**
     * Calculate the date that this customer can be expected to receive their next invoice. This method will return null
     * if the customer does not exist
     *
     * @param userDto
     */
    public void setCustomerNextInvoiceDate (UserDTO userDto) {

        MainSubscriptionDTO mainSubscription = userDto.getCustomer().getMainSubscription();
        Date createdDate = userDto.getCreateDatetime();
        GregorianCalendar cal = new GregorianCalendar();

        Date nextInvoiceDate = userDto.getCustomer().getNextInvoiceDate();

        createdDate = Util.truncateDate(createdDate != null ? createdDate : new Date());

        LOG.debug("Initial run date: %s. Next invoice date for user: %s retrieved from orders is: %s ", createdDate,
                user.getId(), nextInvoiceDate);

        Integer customerDayOfInvoice = mainSubscription.getNextInvoiceDayOfPeriod();
        Integer mainSubscriptionPeriodUnit = mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId();
        Integer mainSubscriptionPeriodValue = mainSubscription.getSubscriptionPeriod().getValue();

        cal.setTime(nextInvoiceDate == null ? createdDate : Util.truncateDate(nextInvoiceDate));

        // consider end of month case
        if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) <= customerDayOfInvoice
                && Constants.PERIOD_UNIT_MONTH.equals(mainSubscriptionPeriodUnit)) {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {

            if (mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_MONTH)) {
                cal.set(Calendar.DAY_OF_MONTH, customerDayOfInvoice);
            } else if (mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_WEEK)) {
                cal.set(Calendar.DAY_OF_WEEK, customerDayOfInvoice);
            } else if (mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_YEAR)) {
                cal.set(Calendar.DAY_OF_YEAR, customerDayOfInvoice);
            } else if (mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_SEMI_MONTHLY)) {
                cal.setTime(addSemiMonthlyPeriod(cal, customerDayOfInvoice));
            }
        }

        if (!mainSubscriptionPeriodUnit.equals(Constants.PERIOD_UNIT_SEMI_MONTHLY)) {
            // if next invoice date exists set the day to next invoice day of period
            // greater than the next invoice date
            nextInvoiceDate = CalendarUtils.getNextInvoiceDateWithEOMAdjustment(cal.getTime(),
                    (nextInvoiceDate == null ? createdDate : nextInvoiceDate),
                    customerDayOfInvoice,
                    mainSubscriptionPeriodUnit,
                    mainSubscriptionPeriodValue);
        } else {
            nextInvoiceDate = cal.getTime();
        }

        LOG.debug("Final next invoice date for user %s is: %s ", user.getId(), nextInvoiceDate);
        // user.getCustomer would always update parent customer and hence userDto.getCustomer is used.
        userDto.getCustomer().setNextInvoiceDate(nextInvoiceDate);
    }

    /**
     * Adds semi monthly period to given date, considering the day of invoice generation received from UI.
     *
     * @param cal
     * @param customerDayOfInvoice
     * @return
     */
    private Date addSemiMonthlyPeriod (GregorianCalendar cal, Integer customerDayOfInvoice) {
        Integer nextInvoiceDay = cal.get(Calendar.DAY_OF_MONTH);
        Integer sourceDay = cal.get(Calendar.DAY_OF_MONTH);

        if (sourceDay < customerDayOfInvoice) {
            nextInvoiceDay = customerDayOfInvoice;
        } else if (customerDayOfInvoice <= 14) {
            nextInvoiceDay = Math.min(customerDayOfInvoice + 15, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            if (sourceDay >= nextInvoiceDay) {
                // Lets say today is 30th and nextInvoiceDay is 29th after adding 15 days.
                // then next invoice date should be 14th of the next month
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            }
        } else if (customerDayOfInvoice == 15 && sourceDay >= customerDayOfInvoice) {
            DateTime sourceDatetime = new DateTime(cal.getTime());
            sourceDatetime = sourceDatetime.withDayOfMonth(sourceDatetime.dayOfMonth().getMaximumValue());
            nextInvoiceDay = sourceDatetime.getDayOfMonth();

            if (sourceDay == nextInvoiceDay) {
                // Lets say today is 31st and nextInvoiceDay is 30 after adding 15 days
                // then next invoice date should be 15th of next month
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            } else if (sourceDay > customerDayOfInvoice) {
                // source day is 30th but not month end
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            }
        }
        cal.set(Calendar.DAY_OF_MONTH, nextInvoiceDay);
        return cal.getTime();
    }

    /**
     * Update Sub-account next invoice date and billing cycle as same as Parent account.
     *
     * @param user
     */
    private void updateBillingCycleOfChildAsPerParent (UserDTO user) {

        if (user.getCustomer().getIsParent() != null) {

            UserBL parent = null;
            Iterator subAccountsIt = null;
            if (user.getCustomer().getIsParent() != null && user.getCustomer().getIsParent().intValue() == 1) {
                parent = new UserBL(user.getId());
                subAccountsIt = parent.getEntity().getCustomer().getChildren().iterator();
            }

            // see if there is any subaccounts to include in this invoice
            while (subAccountsIt != null) { // until there are no more subaccounts (subAccountsIt != null) {
                CustomerDTO customer = null;
                while (subAccountsIt.hasNext()) {
                    customer = (CustomerDTO) subAccountsIt.next();
                    if (customer.getInvoiceChild() == null || customer.getInvoiceChild().intValue() == 0) {
                        break;
                    } else {
                        LOG.debug("Subaccount not included in parent's invoice %s", customer.getId());
                        customer = null;
                    }
                }
                if (customer != null) {
                    customer.setMainSubscription(parent.getMainSubscription());
                    customer.setNextInvoiceDate(parent.getDto().getCustomer().getNextInvoiceDate());

                    if (customer.getIsParent() != null && customer.getIsParent().intValue() == 1) {
                        parent = new UserBL(customer.getBaseUser().getUserId());
                        if (parent != null && parent.getEntity() != null && parent.getEntity().getCustomer() != null
                                && checkIfUserhasAnychildren(parent)) {
                            subAccountsIt = parent.getEntity().getCustomer().getChildren().iterator();
                        }
                    }
                } else {
                    subAccountsIt = null;
                    LOG.debug("No more subaccounts to process");
                }
            }
        }
	}

    /**
     * Gets ait meta field values for currently effective date and adds to give list
     *
     * @param metaFields
     *            : Meta Fields list
     * @param aitTimelineMetaFieldsMap
     *            : MetaFields map
     */
    public void getCustomerEffectiveAitMetaFieldValues (List<MetaFieldValue> metaFields,
            Map<Integer, Map<Date, List<MetaFieldValue>>> aitTimelineMetaFieldsMap) {
        List<MetaFieldValue> aitFields = new ArrayList<MetaFieldValue>();

        for (Map.Entry<Integer, Map<Date, List<MetaFieldValue>>> entry : aitTimelineMetaFieldsMap.entrySet()) {
            List<Date> timelineDates = new ArrayList<Date>();
            Map<Date, List<MetaFieldValue>> timeline = entry.getValue();

            for (Map.Entry<Date, List<MetaFieldValue>> inner : timeline.entrySet()) {
                timelineDates.add(inner.getKey());
            }

            Collections.sort(timelineDates);
            metaFields.addAll(timeline.get(findEffectiveDate(timelineDates)));
        }
    }

    private MetaFieldValue generateValue (MetaFieldValue value) {
        MetaFieldValue generated = value.getField().createValue();
        generated.setField(value.getField());
        generated.setValue(value.getValue());
        return generated;
    }

    private static Date findEffectiveDate (List<Date> dates) {
        Date date = new Date();
        Date forDate = null;
        for (Date start : dates) {
            if (start != null && start.after(date))
                break;

            forDate = start;
        }
        return forDate;
    }

    public List<UserCodeDTO> getUserCodesForUser (int userId) {
        return new UserCodeDAS().findForUser(userId);
    }

    public static UserCodeWS convertUserCodeToWS (UserCodeDTO userCode) {
        UserCodeWS ws = new UserCodeWS();
        ws.setId(userCode.getId());
        ws.setIdentifier(userCode.getIdentifier());
        ws.setTypeDescription(userCode.getTypeDescription());
        ws.setType(userCode.getType());
        ws.setExternalReference(userCode.getExternalReference());
        ws.setUserId(userCode.getUser().getId());
        ws.setValidFrom(userCode.getValidFrom());
        ws.setValidTo(userCode.getValidTo());

        return ws;
    }

    public static UserCodeWS[] convertUserCodeToWS (List<UserCodeDTO> userCodes) {
        UserCodeWS[] result = new UserCodeWS[userCodes.size()];
        for (int i = 0; i < userCodes.size(); i++) {
            result[i] = convertUserCodeToWS(userCodes.get(0));
        }
        return result;
    }

    public int createUserCode (UserCodeWS userCode) {
        UserCodeDAS userCodeDAS = new UserCodeDAS();
        UserCodeDTO dto = converUserCodeToDTO(userCode);

        if (dto.getId() != 0) {
            throw new SessionInternalError("New User Code has an id" + dto,
                    new String[] { "UserCodeWS,identifier,userCode.validation.new.id.notnull" });
        }
        // This call will ensure that the usercode is unique in the company
        UserCodeDTO persistentDto = new UserCodeDAS().findForIdentifier(dto.getIdentifier(), dto
                .getUser()
                .getEntity()
                .getId());

        // check that the identifier is unique and belongs to this object
        if (persistentDto != null) {
            throw new SessionInternalError("User Code is in use: " + dto,
                    new String[] { "UserCodeWS,identifier,userCode.validation.duplicate.identifier" });
        }

        verifyUserCodeIdentifierFormat(dto.getUser(), dto.getIdentifier());
        verifyUserCode(dto);

        dto = userCodeDAS.save(dto);

        return dto.getId();
    }

    public void updateUserCode (UserCodeWS userCode) {
        UserCodeDAS userCodeDAS = new UserCodeDAS();
        UserCodeDTO dto = converUserCodeToDTO(userCode);
        // check that nothing has changed if the usercode is used

        UserCodeDTO persistentDto = new UserCodeDAS().find(dto.getId());

        if( (!dto.getIdentifier().equals(persistentDto.getIdentifier())) ||
                (dto.getType() != null && !dto.getType().equals(persistentDto.getType())) ||
                (!dto.getExternalReference().equals(persistentDto.getExternalReference())) ||
                (dto.getUser().getId() != persistentDto.getUser().getId()) ||
                (!dto.getValidFrom().equals(persistentDto.getValidFrom())) || (!dto.getTypeDescription().equals(persistentDto.getTypeDescription())) ) {


            if (!persistentDto.getUserCodeLinks().isEmpty()) {
                throw new SessionInternalError("Attempting to update a UserCodeDTO which is already linked: "
                        + persistentDto, new String[] { "userCode.validation.update.linked" });
            }

            if ((!dto.getIdentifier().equals(persistentDto.getIdentifier()))) {
                if (userCodeDAS.findForIdentifier(dto.getIdentifier(), dto.getUser().getEntity().getId()) != null) {
                    throw new SessionInternalError("User Code is in use: " + dto,
                            new String[] { "UserCodeWS,identifier,userCode.validation.duplicate.identifier" });
                }
            }

            persistentDto.setIdentifier(dto.getIdentifier());
            persistentDto.setType(dto.getType());
            persistentDto.setExternalReference(dto.getExternalReference());
            persistentDto.setValidFrom(dto.getValidFrom());
            persistentDto.setTypeDescription(dto.getTypeDescription());

            verifyUserCodeIdentifierFormat(persistentDto.getUser(), persistentDto.getIdentifier());
        }
        persistentDto.setValidTo(dto.getValidTo());

        verifyUserCode(persistentDto);

        dto = persistentDto;
        userCodeDAS.save(dto);
    }

    private void verifyUserCode (UserCodeDTO dto) {
        if (dto.getValidTo() != null && dto.getValidFrom() != null && dto.getValidTo().before(dto.getValidFrom())) {
            throw new SessionInternalError("User Code 'valid to' is before 'valid from'",
                    new String[] { "UserCodeWS,validTo,validation.validTo.before.validFrom" });
        }
    }

    public UserCodeDTO findUserCodeForIdentifier (String userCode, Integer companyId) {
        return new UserCodeDAS().findForIdentifier(userCode, companyId);
    }

    private void verifyUserCodeIdentifierFormat (UserDTO user, String identifier) {
        String regex = user.getUserName() + "\\d{5}";
        Pattern pattern = Pattern.compile(regex);
        if (identifier == null || !pattern.matcher(identifier).matches()) {
            throw new SessionInternalError("User Code identifier does not match pattern",
                    new String[] { "UserCodeWS,identifier,validation.identifier.pattern.fail" });
        }
    }

    public UserCodeDTO converUserCodeToDTO (UserCodeWS ws) {
        UserCodeDTO dto = new UserCodeDTO(ws.getId());
        dto.setIdentifier(ws.getIdentifier());
        dto.setTypeDescription(ws.getTypeDescription());
        dto.setType(ws.getType());
        dto.setExternalReference(ws.getExternalReference());
        dto.setUser(das.find(ws.getUserId()));
        dto.setValidFrom(ws.getValidFrom());
        dto.setValidTo(ws.getValidTo());
        return dto;
    }

    public List<Integer> getAssociatedObjectsByUserCodeAndType (String userCode, UserCodeObjectType objectType) {
        return new UserCodeLinkDAS().getAssociatedObjectsByUserCodeAndType(userCode, objectType);
    }

    public List<Integer> getAssociatedObjectsByUserAndType (int userId, UserCodeObjectType objectType) {
        return new UserCodeLinkDAS().getAssociatedObjectsByUserAndType(userId, objectType);
    }

    /**
     * Update the UserCodeAssociate to have the links defined in targetLinks
     *
     * @param associate
     * @param targetLinks
     */
    public static <T extends UserCodeLinkDTO> void updateAssociateUserCodesToLookLikeTarget (
            UserCodeAssociate<T> associate, Collection<T> targetLinks, String errorBeanAndProperty) {
        ConcurrentHashMap<String, T> userCodeLinkMap = new ConcurrentHashMap<String, T>();

        Set<T> currentLinks = associate.getUserCodeLinks();
        if (currentLinks != null) {
            for (T link : currentLinks) {
                userCodeLinkMap.put(link.getUserCode().getIdentifier(), link);
            }
        }
        if (targetLinks != null) {
            for (T link : targetLinks) {
                if (userCodeLinkMap.remove(link.getUserCode().getIdentifier()) == null) {
                    if (link.getUserCode().hasExpired()) {
                        throw new SessionInternalError("The user code has expired and can not be linked to an object "
                                + link.getUserCode(),
                                new String[] { errorBeanAndProperty == null ? "UserCodeWS,validTo"
                                        : errorBeanAndProperty + ",userCode.validation.expired,"
                                                + link.getUserCode().getIdentifier() });
                    }
                    associate.addUserCodeLink(link);
                }
            }
        }

        currentLinks.removeAll(userCodeLinkMap.values());
    }

    /**
     * Convert a Set of UserCodeLinkDTO to an array of strings containing the UserCodeDTO.identifier
     *
     * @param userCodeLinks
     * @return
     */
    public static String[] convertToUserCodeStringArray (Set<? extends UserCodeLinkDTO> userCodeLinks) {
        if (userCodeLinks == null) {
            return new String[0];
        }

        String[] userCodes = new String[userCodeLinks.size()];
        int idx = 0;
        for (UserCodeLinkDTO link : userCodeLinks) {
            userCodes[idx++] = link.getUserCode().getIdentifier();
        }
        return userCodes;
    }

    private Integer getPaymentInformationIndex (Integer paymentId, List<PaymentInformationDTO> paymentInstruments) {
        Integer count = 0;
        for (PaymentInformationDTO dto : paymentInstruments) {
            if (dto.getId().equals(paymentId)) {
                return count;
            }
            count++;
        }
        // not found
        return null;
    }

    /**
     * This method removes pre authorization of credit card in case credit card is updated
     * 
     * @param saved
     *            PaymentInformationDTO
     * @param userId
     *            id of the credit carduser
     */
    private void removeCCPreAuthorization (PaymentInformationDTO saved, Integer userId) {
        if (saved.getId() == null || saved.getId().intValue() < 1 ) {
            LOG.debug("Saving a new instrument does not require further action here.");
            return;
        }
        PaymentInformationBL piBl = new PaymentInformationBL(saved.getId());

        if ( null == piBl.get() ) {
            LOG.debug("Instrument id [%s] not found. This may be a new instrument. Aborting removeCCPreAuth for this instrument.", saved.getId());
            return;
        }

        // verify if new and the old one are credit card instruments
        if (!(piBl.isCreditCard(saved) &&  piBl.isCreditCard(piBl.get()))) {
            LOG.debug("Either former or new Instrument [%s] being updated is not a credit card.", saved.getId());
            return;
        }

        // verify that its value has changed
        if (piBl.isCCUpdated(saved)) {
            LOG.debug("Credit card [%s] has been updated. Removing pre authrization", saved.getId());
            if (userId != null) {
                PaymentBL paymentBl = new PaymentBL();
                for (PaymentDTO auth : (Collection<PaymentDTO>) paymentBl.getHome().findPreauth(userId)) {
                    paymentBl.set(auth);
                    paymentBl.delete();
                }
            }
            LOG.debug("Done removing pre-auths");
    	}
    }
    
    public void saveUserWithNewPasswordScheme(Integer userId, Integer entityId, String newPasswordEncoded, Integer newScheme) {

        eLogger.audit(userId, userId, Constants.TABLE_BASE_USER,
                user.getUserId(), EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.PASSWORD_CHANGE, null, user.getPassword(), null);
        Date date = new DateTime().toDateMidnight().toDate();
        user.setChangePasswordDate(date);
        user.setPassword( newPasswordEncoded );
        user.setEncryptionScheme(newScheme);
        savePasswordHistory();

    }
    
    public Boolean isEncryptionSchemeSame(Integer methodId){
    	return methodId.equals(user.getEncryptionScheme());
    }
    
    public Integer getEncryptionSchemeOfUser(String userName, Integer entityId){
    	return user.getEncryptionScheme();
    }

    public void setAccountLocked(boolean isAccountLocked){
        user.setAccountLocked(isAccountLocked);
        if( isAccountLocked ) {
            if(null == user.getAccountLockedTime()) {       //We only need to update the lock time if it is not already set
                user.setAccountLockedTime(new Date());
            }
        } else {
            user.setAccountLockedTime(null);
        }
    }

    /**
     * this method checks whether account is locked or not. It first checks
     * for preferences(39 & 54) being set and return false if not. Then it checks
     * whether user lockout time is greater than the lock out time set in preference
     * 39. if it is than it should not be locked else lock
     *
     * @return
     */
    public boolean isAccountLocked() {

        if ("".equals(user.getPassword())) {
            //If no credentials have been created the account is locked
            return true;
        }

        int allowedRetries = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(), Constants.PREFERENCE_FAILED_LOGINS_LOCKOUT);
        if (allowedRetries == 0) {
            return false;
        }

        int lockoutTime = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(), Constants.PREFERENCE_ACCOUNT_LOCKOUT_TIME);
        if (lockoutTime == 0) {
            return false;
        }

        if (null == user.getAccountLockedTime()) {
            return false;
        }

        //convert preference time to milliseconds
        long lockoutTimeInMilliSeconds = lockoutTime * 60 * 1000; //minutes to ms
        long accountLockedTimeInMilliSeconds = (Calendar.getInstance().getTimeInMillis() -
                user.getAccountLockedTime().getTime());
        if (lockoutTimeInMilliSeconds <= accountLockedTimeInMilliSeconds) {
            return false;
        }

        return true;
    }

    public void updateMetaFieldsWithValidation(Integer entityId, Integer accountTypeId, MetaContent dto) {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.updateMetaFieldsWithValidation(entityId, accountTypeId, dto);

        for(MetaFieldValue mfv : customerDTO.getMetaFields()){
            user.getCustomer().setMetaField(mfv.getField(),mfv.getValue());
        }
	}

    public boolean checkIfUserhasAnychildren (UserBL parent) {
        return (parent.getEntity().getCustomer().getChildren() != null && !parent
                .getEntity()
                .getCustomer()
                .getChildren()
                .isEmpty());
    }

    /***
     * If value of accountExpired is true , function will set the accountDisabledDate to current date and audit log the date
     * else set accountDisabledDate to null
     * @param accountExpired
     *
     */
    public void setAccountExpired(boolean accountExpired, Date accountDisabledDate) {
        user.setAccountExpired(accountExpired);
        if(accountExpired) {
            user.setAccountDisabledDate(null != accountDisabledDate ? accountDisabledDate : new Date());
            eLogger.audit(user.getId(), user.getId(), Constants.TABLE_BASE_USER,
                    user.getUserId(), EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.ACCOUNT_EXPIRED, null, user.getAccountDisabledDate().toString(),
                    null);
        }
        else {
            user.setAccountDisabledDate(null);
        }
        LOG.debug("Account Expired set for user " + user.getId() + " to expired = " + accountExpired);
    }

    /***
     *
     * This method checks Constants.PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS preference value.
     * if value is 0 that means the in-active feature is disabled, returns false
     * if the value is greater then 0 then checks if accountDisabledDate of user, if null return false else true
     *
     */
    public boolean validateAccountExpired(Date accountDisableDate) {
        PreferenceBL pref = new PreferenceBL();
        Integer daysToDeActivateAccount;

        try {
            pref.set(user.getCompany().getId(), Constants.PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS);
            daysToDeActivateAccount = pref.getInt();
        } catch (Exception e) {
            LOG.debug("Exception while reading preference");
            return false;
        }

        LOG.debug("Number of days Set for Inactive User Activity are " + daysToDeActivateAccount);

        // Account Expiry feature disabled
        if (daysToDeActivateAccount.equals(new Integer(0))) {
            // check previous state in-case of user update and account in-active feature is not enabled
            if(null != user && null != user.getAccountDisabledDate())
                return true;
            else
                return false;
        }

        // Account is currently in-active
        if (null != accountDisableDate) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Get parent company for user
     * @param userId
     * @return
     */
    public Integer getParentId(Integer entityId){
    	CompanyDAS companyDas = new CompanyDAS();
    	CompanyDTO companyDto = companyDas.find(entityId);
    	if(companyDto.getParent()!=null){
    		return companyDto.getParent().getId();
    	}
    	return -1;
    }
    
    public Integer getParentCompany(Integer entityId){
    	CompanyDAS companyDas = new CompanyDAS();
    	return companyDas.getParentCompanyId(entityId);
    }

    private void savePasswordHistory() {
      	 UserPasswordDTO userPasswordDTO = new UserPasswordDTO(user, user.getPassword());
      	 UserPasswordDAS userPasswordDAS = new UserPasswordDAS();
         userPasswordDAS.save(userPasswordDTO);
      }

    /**
     * It creates credentials for the current user in the BL using the parameters from the DTOEx
     * @param dto the UserDTOEx that comes from the API call
     */
    public void createCredentialsFromDTO(UserDTOEx dto) {
        boolean shouldCreateCredentialsByDefault = PreferenceBL.getPreferenceValueAsBoolean(user
                .getEntity()
                .getId(), Constants.PREFERENCE_CREATE_CREDENTIALS_BY_DEFAULT);

        if (shouldCreateCredentialsByDefault || dto.isCreateCredentials()) {
            LOG.debug("Credentials have been sent");
            passwordService.createPassword(user);
        }
    }
    
    public void logout() {
        //ToDo: Put any other pre-logout code here
        eLogger.auditBySystem(user.getEntity().getId(), user.getId(),
                Constants.TABLE_BASE_USER, user.getUserId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.USER_LOGOUT,null,
                user.getUserName(), null);
    }
}

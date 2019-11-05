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

package com.sapienter.jbilling.server.security;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.security.methods.SecuredMethodType;
import com.sapienter.jbilling.server.user.UserTransitionResponseWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.util.Context;

import grails.plugin.springsecurity.SpringSecurityService;

import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Security advice for web-service method calls to ensure that only data belonging to the
 * web-service caller is accessed.
 *
 * @author Brian Cowdery
 * @since 01-11-2010
 */
public class WSSecurityAdvice implements MethodBeforeAdvice {
    private static final FormatLogger LOG = new FormatLogger(WSSecurityAdvice.class);

    private SpringSecurityService springSecurityService;
    private TransactionTemplate transactionTemplate;

    public SpringSecurityService getSpringSecurityService() {
        if (springSecurityService == null)
            springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        return springSecurityService;
    }

    public void setSpringSecurityService(SpringSecurityService springSecurityService) {
        this.springSecurityService = springSecurityService;
    }

    public TransactionTemplate getTransactionTemplate() {
        if (transactionTemplate == null) {
            PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
            transactionTemplate = new TransactionTemplate(transactionManager);
        }
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public Integer getCallerCompanyId() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService().getPrincipal();
        return details.getCompanyId();
    }

    public String getCallerUserName() {
        CompanyUserDetails details = (CompanyUserDetails) getSpringSecurityService().getPrincipal();
        return details.getPlainUsername();
    }

    /**
     * Validates that method call arguments are accessible to the web-service caller company.
     *
     * @param method method to call
     * @param args method arguments to validate
     * @param target method call target, may be null
     * @throws Throwable throws a SecurityException if the calling user does not have access to the given data
     */
    public void before(Method method, Object[] args, Object target) throws Throwable {
        if (!getSpringSecurityService().isLoggedIn())
            throw new SecurityException("Web-service call has not been authenticated.");

        LOG.debug("Validating web-service method '%s()'", method.getName());

        // try validating the method call itself
        WSSecured securedMethod = getMappedSecuredWS(method, args);
        if (securedMethod != null)
            validate(securedMethod);

        // validate each method call argument
        for (Object o : args) {
            if (o != null) {
                if (o instanceof Collection) {
                    for (Object element : (Collection) o)
                        validate(element);

                } else if (o.getClass().isArray()) {
                    for (Object element : (Object[]) o)
                        validate(element);

                } else {
                    validate(o);
                }
            }
        }
    }

    /**
     * Attempt to map the method call as an instance of WSSecured so that it can be validated.
     *
     * @see com.sapienter.jbilling.server.security.WSSecurityMethodMapper
     *
     * @param method method to map
     * @param args method arguments
     * @return mapped method call, or null if method call is unknown
     */
    protected WSSecured getMappedSecuredWS(final Method method, final Object[] args) {
        return getTransactionTemplate().execute(new TransactionCallback<WSSecured>() {
            public WSSecured doInTransaction(TransactionStatus status) {
                return WSSecurityMethodMapper.getMappedSecuredWS(method, args);
            }
        });
    }

    /**
     * Attempt to map the given object as an instance of WSSecured so that it can be validated.
     *
     * @see com.sapienter.jbilling.server.security.WSSecurityEntityMapper
     *
     * @param o object to map
     * @return mapped object, or null if object is of an unknown type
     */
    protected WSSecured getMappedSecuredWS(final Object o) {
        LOG.debug("Non WSSecured object %s, attempting to map a secure class for validation.", o.getClass().getSimpleName());

        return getTransactionTemplate().execute(new TransactionCallback<WSSecured>() {
            public WSSecured doInTransaction(TransactionStatus status) {
                return WSSecurityEntityMapper.getMappedSecuredWS(o);
            }
        });
    }

    /**
     * Attempt to validate the given object.
     *
     * @param o object to validate
     * @throws SecurityException thrown if user is accessing data that does not belonging to them
     */
    protected void validate(Object o) throws SecurityException {
        if (o != null) {
            if (o instanceof WSSecured) {
                validateEntityChange((WSSecured) o);
            }

            final WSSecured secured = (o instanceof WSSecured)
                                      ? (WSSecured) o
                                      : getMappedSecuredWS(o);

            if (secured != null) {
                LOG.debug("Validating secure object %s", secured.getClass().getSimpleName());

                getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        if (secured.getOwningEntityId() != null)
                            validateEntity(secured.getOwningEntityId());

                        if (secured.getOwningUserId() != null)
                            validateUser(secured.getOwningUserId());
                    }
                });
            }
        }
    }

    /**
     * This method validate is entity (user) was changed in input object (for update usually)
     * Changing the entity (company) is not allowed for invoices, items, orders, etc.
     * So, we should compare persisted object, is it owned by entity for caller user, or not
     * Not persisted objects (without id) is not checked
     * @param inputObject WSSecured input object for check entity in persisted one
     */
    protected void validateEntityChange(final WSSecured inputObject) {
        Integer persistedId = null;
        SecuredMethodType type = null;
        if (inputObject instanceof AgeingWS) {
            // do nothing, entity can't be changed
        } else if (inputObject instanceof AssetWS && ((AssetWS) inputObject).getId() != null) {
            persistedId = ((AssetWS) inputObject).getId();
            type = SecuredMethodType.ASSET;
        } else if (inputObject instanceof BillingProcessWS) {
            // do nothing, entity can't be changed
        } else if (inputObject instanceof InvoiceWS && ((InvoiceWS) inputObject).getId() != null) {
            type = SecuredMethodType.INVOICE;
            persistedId = ((InvoiceWS) inputObject).getId();
        } else if (inputObject instanceof ItemDTOEx && ((ItemDTOEx) inputObject).getId() != null) {
            type = SecuredMethodType.ITEM;
            persistedId = ((ItemDTOEx) inputObject).getId();
        } else if (inputObject instanceof OrderWS && ((OrderWS) inputObject).getId() != null) {
            type = SecuredMethodType.ORDER;
            persistedId = ((OrderWS) inputObject).getId();
        } else if (inputObject instanceof PartnerWS && ((PartnerWS) inputObject).getId() != null) {
            type = SecuredMethodType.PARTNER;
            persistedId = ((PartnerWS) inputObject).getId();
        } else if (inputObject instanceof PaymentWS && ((PaymentWS) inputObject).getId() > 0) {
            type = SecuredMethodType.PAYMENT;
            persistedId = ((PaymentWS) inputObject).getId();
        } else if (inputObject instanceof PluggableTaskWS) {
            // do nothing, entity can't be changed
        } else if (inputObject instanceof UserTransitionResponseWS) {
           // do nothing, entity can't be changed
        } else if (inputObject instanceof UserWS) {
           // do nothing, entity can't be changed
        }

        if (type != null && persistedId != null) {
            // validate user and entity in persisted object - they should be the same as for caller
            final SecuredMethodType finalType = type;
            final Integer finalId = persistedId;
            getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    WSSecured persistedSecuredObject = finalType.getMappedSecuredWS(finalId);
                    if (persistedSecuredObject != null) {
                        if (persistedSecuredObject.getOwningEntityId() != null)
                            validateEntity(persistedSecuredObject.getOwningEntityId());

                        if (persistedSecuredObject.getOwningUserId() != null)
                            validateUser(persistedSecuredObject.getOwningUserId());
                    }
                }
            } );
        }
    }

    /**
     * Validates that the given owningUserId resides under the same entity as authenticated
     * user account making the web-service call.
     *
     * @param owningUserId user id owning the data being accessed
     * @throws SecurityException thrown if user is accessing data that does not belonging to them
     */
    protected void validateUser(Integer owningUserId) throws SecurityException {
        // validate only when the owning user ID has been persisted (not a transient user)
        UserDAS userDas = new UserDAS();
        if (userDas.isIdPersisted(owningUserId)) {

            UserDTO user = userDas.find(owningUserId);
            if (user != null && user.getCompany() != null) {
                // extract company and validate entity id against the caller
                validateEntity(user.getCompany().getId());

            } else {
                // impossible, a persisted user must belong to a company
                throw new SecurityException("User " + owningUserId + " does not belong to an entity.");
            }

        } else {
            LOG.warn("Data accessed via web-service call belongs to a transient user.");
        }
    }

    /**
     * Validates that the given owningEntityId matches the entity of the user account making
     * the web-service call.
     *
     * @param owningEntityId entity id owning the data being accessed
     * @throws SecurityException thrown if user is accessing data that does not belong to them
     */
    protected void validateEntity(Integer owningEntityId) throws SecurityException {

        List<Integer> allHierarchyEntitiesIds = new CompanyDAS().findAllCurrentAndChildEntities(getCallerCompanyId());

        if (!allHierarchyEntitiesIds.contains(owningEntityId))
            throw new SecurityException("Unauthorized access to entity " + owningEntityId
                    + " by caller '" + getCallerUserName() + "' (id " + getCallerCompanyId() + ")");
    }
}

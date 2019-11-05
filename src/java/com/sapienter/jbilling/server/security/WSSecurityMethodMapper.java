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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.ObjectNotFoundException;

import com.sapienter.jbilling.server.security.methods.SecuredMethodFactory;
import com.sapienter.jbilling.server.security.methods.SecuredMethodSignature;
import com.sapienter.jbilling.server.security.methods.SecuredMethodType;

/**
 * WSSecurityMethodMapper
 *
 * @author Brian Cowdery
 * @since 02-11-2010
 */
public class WSSecurityMethodMapper {

    static {
        SecuredMethodFactory.add("getUserWS", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("deleteUser", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserContactsWS", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("updateUserContact", 0, SecuredMethodType.USER);   // todo: should validate user and contact type ids
        SecuredMethodFactory.add("setAuthPaymentType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getAuthPaymentType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getPartner", 0, SecuredMethodType.PARTNER);

        SecuredMethodFactory.add("getItem", 0, SecuredMethodType.ITEM);             // todo: should validate item id and user id
        SecuredMethodFactory.add("deleteItem", 0, SecuredMethodType.ITEM);
        SecuredMethodFactory.add("deleteItemCategory", 0, SecuredMethodType.ITEM_CATEGORY);
        SecuredMethodFactory.add("getUserItemsByCategory", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("isUserSubscribedTo", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLatestInvoiceByItemType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastInvoicesByItemType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLatestOrderByItemType", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastOrdersByItemType", 0, SecuredMethodType.USER);

        SecuredMethodFactory.add("validatePurchase", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("validateMultiPurchase", 0, SecuredMethodType.USER);

        SecuredMethodFactory.add("getOrder", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("deleteOrder", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("getCurrentOrder", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("updateCurrentOrder", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getOrderLine", 0, SecuredMethodType.ORDER_LINE);
        SecuredMethodFactory.add("getOrderByPeriod", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLatestOrder", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastOrders", 0, SecuredMethodType.USER);

        SecuredMethodFactory.add("getInvoiceWS", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("createInvoice", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("createInvoiceFromOrder", 0, SecuredMethodType.ORDER);
        SecuredMethodFactory.add("deleteInvoice", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("getAllInvoices", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLatestInvoice", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastInvoices", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getUserInvoicesByDate", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getPaperInvoicePDF", 0, SecuredMethodType.INVOICE);

        SecuredMethodFactory.add("getPayment", 0, SecuredMethodType.PAYMENT);
        SecuredMethodFactory.add("getLatestPayment", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("getLastPayments", 0, SecuredMethodType.USER);
        SecuredMethodFactory.add("payInvoice", 0, SecuredMethodType.INVOICE);

        SecuredMethodFactory.add("getBillingProcess", 0, SecuredMethodType.BILLING_PROCESS);
        SecuredMethodFactory.add("getBillingProcessGeneratedInvoices", 0, SecuredMethodType.BILLING_PROCESS);

        SecuredMethodFactory.add("notifyInvoiceByEmail", 0, SecuredMethodType.INVOICE);
        SecuredMethodFactory.add("notifyPaymentByEmail", 0, SecuredMethodType.PAYMENT);
        SecuredMethodFactory.add("deletePlugin", 0, SecuredMethodType.PLUG_IN);

        SecuredMethodFactory.add("getAsset", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("deleteAsset", 0, SecuredMethodType.ASSET);
        SecuredMethodFactory.add("deleteDiscount", 0, SecuredMethodType.DISCOUNT);
    }

    /**
     * Return a WSSecured object mapped from the given method and method arguments for validation.
     * This produced a secure object for validation from web-service method calls that only accept and return
     * ID's instead of WS objects that can be individually validated.
     *
     * @param method method to map
     * @param args method arguments
     * @return instance of WSSecured mapped from the given entity, null if entity could not be mapped.
     */
    public static WSSecured getMappedSecuredWS(Method method, Object[] args) {
        if (method != null) {

            SecuredMethodSignature sig = SecuredMethodFactory.getSignature(method);
            if (sig != null && sig.getIdArgIndex() <= args.length) {
                try {
                    return sig.getType().getMappedSecuredWS((Serializable) args[sig.getIdArgIndex()]);
                } catch (ObjectNotFoundException e) {
                    // hibernate complains loudly... object does not exist, no reason to validate.
                    return null;
                }
            }
        }

        return null;
    }
}

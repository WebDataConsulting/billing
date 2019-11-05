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

package com.sapienter.jbilling.server.customer;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emil
 */
public final class CustomerBL extends ResultList implements CustomerSQL {

    private CustomerDTO customer = null;

    public CustomerBL() {
    }

    public CustomerBL(Integer id) {
        customer = new CustomerDAS().find(id);
    }

    public CustomerBL(CustomerDTO customer) {
        this.customer = customer;
    }

    public CustomerDTO getEntity() {
        return customer;
    }

    /**
     * Searches through parent customers (including this customer) looking for a
     * customer with "invoice if child" set to true. If no parent account is explicitly
     * invoiceable, the top/root parent will be returned.
     *
     * @return invoiceable customer account
     */
    public CustomerDTO getInvoicableParent() {
        CustomerDTO parent = customer;

        while (parent.getInvoiceChild() == null || !parent.getInvoiceChild().equals(new Integer(1))) {
            if (parent.getParent() == null) break;
            parent = parent.getParent();
        }

        return parent;
    }

    public CachedRowSet getList(int entityID, Integer userRole,
                                Integer userId)
            throws SQLException, Exception {

        if (userRole.equals(CommonConstants.TYPE_ROOT)) {
            prepareStatement(CustomerSQL.listRoot);
            cachedResults.setInt(1, entityID);
        } else if (userRole.equals(CommonConstants.TYPE_CLERK)) {
            prepareStatement(CustomerSQL.listClerk);
            cachedResults.setInt(1, entityID);
        } else if (userRole.equals(CommonConstants.TYPE_PARTNER)) {
            prepareStatement(CustomerSQL.listPartner);
            cachedResults.setInt(1, entityID);
            cachedResults.setInt(2, userId.intValue());
        } else {
            throw new Exception("The user list for the type " + userRole +
                    " is not supported");
        }

        execute();
        conn.close();
        return cachedResults;
    }

    // this is the list for the Customer menu option, where only
    // customers/partners are listed. Meant for the clients customer service
    public CachedRowSet getCustomerList(int entityID, Integer userRole,
                                        Integer userId)
            throws SQLException, Exception {

        if (userRole.equals(CommonConstants.TYPE_INTERNAL) ||
                userRole.equals(CommonConstants.TYPE_ROOT) ||
                userRole.equals(CommonConstants.TYPE_CLERK)) {
            prepareStatement(CustomerSQL.listCustomers);
            cachedResults.setInt(1, entityID);
        } else if (userRole.equals(CommonConstants.TYPE_PARTNER)) {
            prepareStatement(CustomerSQL.listPartner);
            cachedResults.setInt(1, entityID);
            cachedResults.setInt(2, userId.intValue());
        } else {
            throw new Exception("The user list for the type " + userRole +
                    " is not supported");
        }

        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet getSubAccountsList(Integer userId)
            throws SQLException, Exception {

        // find out the customer id of this user
        UserBL user = new UserBL(userId);

        prepareStatement(CustomerSQL.listSubaccounts);
        cachedResults.setInt(1, user.getEntity().getCustomer().getId());

        execute();
        conn.close();
        return cachedResults;
    }

    /**
     * Returns a list of userIds for the descendants of the customer given
     *
     * @param parent: top parent customer
     * @return
     */
    public List<Integer> getDescendants(CustomerDTO parent) {
        List<Integer> descendants = new ArrayList<Integer>();
        if (parent != null) {
            for (CustomerDTO customer : parent.getChildren()) {
                if (customer.getBaseUser().getDeleted() == 0) {
                    //add it as descendant
                    descendants.add(customer.getBaseUser().getId());
                    //call the same function in a recursive way to get all the descendants
                    descendants.addAll(getDescendants(customer));
                }
            }
        }
        return descendants;
    }

    public List<String> getCustomerEmails(Integer userId, Integer entityId) {
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        CustomerDAS customerDas = new CustomerDAS();
        List<String> emails = new ArrayList<String>();

        Integer customerId = customerDas.getCustomerId(userId);
        if (null != customerId) {

            List<Integer> emailMetaFieldIds = metaFieldDAS.getByFieldType(
                    entityId, new MetaFieldType[]{MetaFieldType.EMAIL});

            List<Integer> valueIds = metaFieldDAS.getValuesByCustomerAndFields(
                    customerId, emailMetaFieldIds);


            for (Integer valueId : valueIds) {
                MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(valueId);
                String email = null != value.getValue() ? (String) value.getValue() : null;
                if (null != email && !email.trim().isEmpty()) {
                    emails.add(email);
                }
            }
        }

        return emails;
    }

    /**
     * Returns the top parent of the hierarchy and all the descendants
     *
     * @param parent
     * @return
     */
    public List<Integer> getUsersOfSameTree(CustomerDTO parent) {
        List<Integer> usersInTree = new ArrayList<Integer>();

        CustomerDTO topParent = parent;
        while (topParent.getParent() != null) {
            topParent = topParent.getParent();
        }

        if (topParent.getBaseUser().getDeleted() == 0) {
            usersInTree.add(topParent.getBaseUser().getId());
        }

        usersInTree.addAll(getDescendants(topParent));

        return usersInTree;
    }

}

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

package com.sapienter.jbilling.server.invoice;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Set;

import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.PreferenceDAS;

import org.apache.commons.lang.StringUtils;
;

import javax.sql.rowset.CachedRowSet;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.process.event.BeforeInvoiceDeleteEvent;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;

import org.springframework.dao.EmptyResultDataAccessException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InvoiceBL extends ResultList implements Serializable, InvoiceSQL {

    private InvoiceDAS invoiceDas = null;
    private InvoiceDTO invoice = null;
    private static final FormatLogger LOG = new FormatLogger(InvoiceBL.class);
    private EventLogger eLogger = null;

    private static final Object NUMBER_LOCK = new Object();

    public InvoiceBL(Integer invoiceId) {
        init();
        set(invoiceId);
    }

    public InvoiceBL() {
        init();
    }

    public InvoiceBL(InvoiceDTO invoice) {
        init();
        set(invoice.getId());
    }

    private void init() {
        eLogger = EventLogger.getInstance();
        invoiceDas = new InvoiceDAS();
    }

    public InvoiceDTO getEntity() {
        return invoice;
    }

    public InvoiceDAS getHome() {
        return invoiceDas;
    }

    public void set(Integer id) {
        invoice = invoiceDas.find(id);
    }

    public void set(InvoiceDTO invoice) {
        this.invoice = invoice;
    }

    /**
     *
     * @param userId
     * @param newInvoice
     * @param process
     *            It can be null.
     */
    public void create(Integer userId, NewInvoiceContext newInvoice, BillingProcessDTO process, Integer executorUserId) {
        // find out the entity id
        UserBL user = null;
        Integer entityId;
        if (process != null) {
            entityId = process.getEntity().getId();
        } else {
            // this is a manual invoice, there's no billing process
            user = new UserBL(userId);
            entityId = user.getEntityId(userId);
        }

        // verify if this entity is using the 'continuous invoice date'
        // preference
        try {
            String preferenceContinuousDateValue = 
            	PreferenceBL.getPreferenceValue(entityId, Constants.PREFERENCE_CONTINUOUS_DATE);

            if (StringUtils.isNotBlank(preferenceContinuousDateValue)) {
                Date lastDate = com.sapienter.jbilling.common.Util.parseDate(preferenceContinuousDateValue);
                LOG.debug("Last date invoiced: %s", lastDate);

                if (lastDate.after(newInvoice.getBillingDate())) {
                    LOG.debug("Due date is before the last recorded date. Moving due date forward for continuous invoice dates.");
                    newInvoice.setBillingDate(lastDate);

                } else {
                    // update the lastest date only if this is not a review
                    if (newInvoice.getIsReview() == null || newInvoice.getIsReview() == 0) {
                        new PreferenceBL().createUpdateForEntity(entityId,
                                                   Constants.PREFERENCE_CONTINUOUS_DATE,
                                                   com.sapienter.jbilling.common.Util.parseDate(newInvoice.getBillingDate()));
                    }
                }
            }
        } catch (EmptyResultDataAccessException e) {
            // not interested, ignore
        }

        // in any case, ensure that the due date is => that invoice date
        if (newInvoice.getDueDate().before(newInvoice.getBillingDate())) {
            LOG.debug("Due date before billing date, moving date up to billing date.");
            newInvoice.setDueDate(newInvoice.getBillingDate());
        }

        // ensure that there are only so many decimals in the invoice
        Integer decimals = null;
        try {
        	decimals = PreferenceBL.getPreferenceValueAsInteger(
        			entityId, Constants.PREFERENCE_INVOICE_DECIMALS);
        	if (decimals == null) {
        		decimals = Constants.BIGDECIMAL_SCALE;
        	}
        } catch (EmptyResultDataAccessException e) {
            // not interested, ignore
        	decimals = Constants.BIGDECIMAL_SCALE;
        }

       	LOG.debug("Rounding %s to %d decimals.", newInvoice.getTotal(), decimals);
        if (newInvoice.getTotal() != null) {
            newInvoice.setTotal(newInvoice.getTotal().setScale(decimals.intValue(), Constants.BIGDECIMAL_ROUND));
        }
        if (newInvoice.getBalance() != null) {
            newInvoice.setBalance(newInvoice.getBalance().setScale(decimals.intValue(), Constants.BIGDECIMAL_ROUND));
        }

        // some API calls only accept ID's and do not pass meta-fields
        // update and validate meta-fields if they've been populated
        if (newInvoice.getMetaFields() != null && !newInvoice.getMetaFields().isEmpty()) {
            newInvoice.updateMetaFieldsWithValidation(entityId, null, newInvoice);
        }

        // create the invoice row
        invoice = invoiceDas.create(userId, newInvoice, process);

        // add delegated/included invoice links
        if (newInvoice.getIsReview() == 0) {
            for (InvoiceDTO dto : newInvoice.getInvoices()) {
                dto.setInvoice(invoice);
            }
        }


        invoice.setPublicNumber( String.valueOf( generateInvoiceNumber(newInvoice, entityId) ) );

        // set the invoice's contact info with the current user's contact
        ContactBL contactBL = new ContactBL();
        ContactDTOEx contact = ContactBL.buildFromMetaField(userId, newInvoice.getBillingDate());
        if ( null != contact)
        	contactBL.createForInvoice(contact, invoice.getId());

        // add a log row for convenience
        if ( null != executorUserId ) {
            eLogger.audit(executorUserId, userId, Constants.TABLE_INVOICE,
                    invoice.getId(), EventLogger.MODULE_INVOICE_MAINTENANCE,
                    EventLogger.ROW_CREATED, null, null, null);
        } else {
            eLogger.auditBySystem(entityId, userId, Constants.TABLE_INVOICE,
                    invoice.getId(), EventLogger.MODULE_INVOICE_MAINTENANCE,
                    EventLogger.ROW_CREATED, null, null, null);
        }

    }

    private String generateInvoiceNumber(NewInvoiceContext newInvoice, Integer entityId) {

        // calculate/compose the number
        String numberStr = "";
        if ( newInvoice.isReviewInvoice() ) {
            // invoices for review will be seen by the entity employees
            // so the entity locale will be used
            EntityBL entity = new EntityBL(entityId);
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "entityNotifications", entity.getLocale());
            numberStr = bundle.getString("invoice.review.number");
        } else if ( StringUtils.isEmpty(newInvoice.getPublicNumber()) ) {
            String prefix= "";
            try {
                prefix = PreferenceBL.getPreferenceValue(entityId, Constants.PREFERENCE_INVOICE_PREFIX);
                if (StringUtils.isEmpty(prefix)) {
                    prefix = "";
                }
            } catch (EmptyResultDataAccessException e) {
                //
            }

            //get and update number
            numberStr = prefix + getAndUpdateInvoiceNumberPreference(entityId);

        } else { // for upload of legacy invoices
            numberStr = newInvoice.getPublicNumber();
        }
        return numberStr;
    }

    private Integer getAndUpdateInvoiceNumberPreference(Integer entityId) {
        return new PreferenceDAS().getPreferenceAndIncrement(entityId, Constants.PREFERENCE_INVOICE_NUMBER);
    }

    public void createLines(NewInvoiceContext newInvoice) {
        Collection invoiceLines = invoice.getInvoiceLines();

        // Now create all the invoice lines, from the lines in the DTO
        // put there by the invoice composition pluggable tasks
        InvoiceLineDAS invoiceLineDas = new InvoiceLineDAS();

        // get the result DTO lines
        Iterator dueInvoiceLines = newInvoice.getResultLines().iterator();
        // go over the DTO lines, creating one invoice line for each

        while (dueInvoiceLines.hasNext()) {
            InvoiceLineDTO lineToAdd = (InvoiceLineDTO) dueInvoiceLines.next();
            
            // create the database row
            InvoiceLineDTO newLine = invoiceLineDas.create(lineToAdd.getDescription(), lineToAdd.getAmount(), lineToAdd.getQuantity(), lineToAdd.getPrice(),
                    lineToAdd.getTypeId(), lineToAdd.getItem(), lineToAdd.getSourceUserId(), lineToAdd.getIsPercentage());

            // update the invoice-lines relationship
            newLine.setInvoice(invoice);
            newLine.setOrder(lineToAdd.getOrder());
            invoiceLines.add(newLine);
        }
        getHome().save(invoice);
        EventManager.process(new NewInvoiceEvent(invoice));
    }

    /**
     * This will remove all the records (sql delete, not just flag them). It
     * will also update the related orders if applicable
     */
    public void delete(Integer executorId) throws SessionInternalError {
        if (invoice == null) {
            throw new SessionInternalError("An invoice has to be set before delete");
        }
        
        //delete the reseller invoices and orders of this invoice
        EventManager.process(new InvoiceDeletedEvent(invoice));
        
        //prevent a delegated Invoice from being deleted
        if (invoice.getDelegatedInvoiceId() != null && invoice.getDelegatedInvoiceId().intValue() > 0 ) {
            SessionInternalError sie= new SessionInternalError("A carried forward Invoice cannot be deleted");
            sie.setErrorMessages(new String[] {
                    "InvoiceDTO,invoice,invoice.error.fkconstraint," + invoice.getId()});
            throw sie;
        }
        // start by updating purchase_order.next_billable_day if applicatble
        // for each of the orders included in this invoice
        for (OrderProcessDTO orderProcess : (Collection<OrderProcessDTO>) invoice.getOrderProcesses()) {
            OrderDTO order = orderProcess.getPurchaseOrder();
            if (order.getNextBillableDay() == null) {
                // the next billable day doesn't need updating
                if (order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
                    OrderBL orderBL = new OrderBL(order);
                    orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, order.getUser().getCompany().getId()));//Constants.DEFAULT_ORDER_INVOICE_STATUS_ID
                }
                continue;
            }
            // only if this invoice is the responsible for the order's
            // next billable day
            if (order.getNextBillableDay().equals(orderProcess.getPeriodEnd())) {
                order.setNextBillableDay(orderProcess.getPeriodStart());

                for (OrderLineDTO line: order.getLines()) {
                    for (OrderChangeDTO change: line.getOrderChanges()) {
                        if ((change.getNextBillableDate() != null) && change.getNextBillableDate().equals(orderProcess.getPeriodEnd())) {
                            change.setNextBillableDate(orderProcess.getPeriodStart());
                        }
                    } 
                }

                if (order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
                    OrderBL orderBL = new OrderBL(order);
                    orderBL.setStatus(null, new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, order.getUser().getCompany().getId()));//Constants.DEFAULT_ORDER_INVOICE_STATUS_ID
                }
            }

        }

        // go over the order process records again just to delete them
        // we are done with this order, delete the process row
        for (OrderProcessDTO orderProcess : (Collection<OrderProcessDTO>) invoice.getOrderProcesses()) {
            OrderDTO order = orderProcess.getPurchaseOrder();
            OrderProcessDAS das = new OrderProcessDAS();
            order.getOrderProcesses().remove(orderProcess);
            das.delete(orderProcess);
        }
        invoice.getOrderProcesses().clear();

        // get rid of the contact associated with this invoice
        try {
            ContactBL contact = new ContactBL();
            if (contact.setInvoice(invoice.getId())) {
                contact.delete();
            }
        } catch (Exception e1) {
            LOG.error("Exception deleting the contact of an invoice", e1);
        }

        // remove the payment link/s
        PaymentBL payment = new PaymentBL();
        Iterator<PaymentInvoiceMapDTO> it = invoice.getPaymentMap().iterator();
        while (it.hasNext()) {
            PaymentInvoiceMapDTO map = it.next();
            payment.removeInvoiceLink(map.getId());
            invoice.getPaymentMap().remove(map);
            // needed because the collection has changed
            it = invoice.getPaymentMap().iterator();
        }

        // log that this was deleted, otherwise there will be no trace
        if (executorId != null) {
            eLogger.audit(executorId, invoice.getBaseUser().getId(),
                    Constants.TABLE_INVOICE, invoice.getId(),
                    EventLogger.MODULE_INVOICE_MAINTENANCE,
                    EventLogger.ROW_DELETED, null, null, null);
        }

        // before delete the invoice most delete the reference in table
        // PAYMENT_INVOICE
        new PaymentInvoiceMapDAS().deleteAllWithInvoice(invoice);

        Set<InvoiceDTO> invoices= invoice.getInvoices();
        if (invoices.size() > 0 ) {
            for (InvoiceDTO delegate: invoices) {
                //set status to unpaid as against carried
                delegate.setInvoiceStatus(new InvoiceStatusDAS().find(Constants.INVOICE_STATUS_UNPAID));
                //remove delegated invoice link
                delegate.setInvoice(null);
                getHome().save(delegate);
            }
        }

        // now delete the invoice itself
        EventManager.process(new BeforeInvoiceDeleteEvent(invoice));
        getHome().delete(invoice);
        getHome().flush();
        
    }

    public void update(Integer entityId, NewInvoiceContext addition) {
        // add the lines to the invoice first
        createLines(addition);
        // update the inoice record considering the new lines
        invoice.setTotal(calculateTotal()); // new total
        // adjust the balance
        addition.calculateTotal();
        BigDecimal balance = invoice.getBalance();
        balance = balance.add(addition.getTotal());
        invoice.setBalance(balance);

        //set to process = 0 only if balance is minimum balance to ignore
        if (!isInvoiceBalanceEnoughToAge(invoice, entityId)) {
            invoice.setToProcess(new Integer(0));
        }else {
			invoice.setToProcess(Integer.valueOf(1));
		}

        if (addition.getMetaFields() != null && !addition.getMetaFields().isEmpty()) {
            invoice.updateMetaFieldsWithValidation(entityId, null, addition);
        }
    }

    private BigDecimal calculateTotal() {
        BigDecimal total = new BigDecimal(0);
        for (Iterator it = invoice.getInvoiceLines().iterator(); it.hasNext();) {
            InvoiceLineDTO line = (InvoiceLineDTO) it.next();
            total = total.add(line.getAmount());
        }
        return total;
    }

    public CachedRowSet getPayableInvoicesByUser(Integer userId)
            throws SQLException, Exception {

        prepareStatement(InvoiceSQL.payableByUser);
        cachedResults.setInt(1, userId.intValue());

        execute();
        conn.close();
        return cachedResults;
    }

    public BigDecimal getTotalPaid() {
        BigDecimal retValue = new BigDecimal(0);
        for (Iterator<PaymentInvoiceMapDTO> it = invoice.getPaymentMap().iterator(); it.hasNext();) {
            PaymentInvoiceMapDTO paymentMap = it.next();
            retValue = retValue.add(paymentMap.getAmount());
        }
        return retValue;
    }

    public CachedRowSet getList(Integer orderId) throws SQLException, Exception {
        prepareStatement(InvoiceSQL.customerList);

        // find out the user from the order
        Integer userId;
        OrderBL order = new OrderBL(orderId);
        if (order.getDTO().getUser().getCustomer().getParent() == null) {
            userId = order.getDTO().getUser().getUserId();
        } else {
            userId = order.getDTO().getUser().getCustomer().getParent().getBaseUser().getUserId();
        }
        cachedResults.setInt(1, userId.intValue());
        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet getList(Integer entityId, Integer userRole,
            Integer userId) throws SQLException, Exception {

        if (userRole.equals(Constants.TYPE_INTERNAL)) {
            prepareStatement(InvoiceSQL.internalList);
        } else if (userRole.equals(Constants.TYPE_ROOT) || userRole.equals(Constants.TYPE_CLERK)) {
            prepareStatement(InvoiceSQL.rootClerkList);
            cachedResults.setInt(1, entityId.intValue());
        } else if (userRole.equals(Constants.TYPE_PARTNER)) {
            prepareStatement(InvoiceSQL.partnerList);
            cachedResults.setInt(1, entityId.intValue());
            cachedResults.setInt(2, userId.intValue());
        } else if (userRole.equals(Constants.TYPE_CUSTOMER)) {
            prepareStatement(InvoiceSQL.customerList);
            cachedResults.setInt(1, userId.intValue());
        } else {
            throw new Exception("The invoice list for the type " + userRole + " is not supported");
        }

        execute();
        conn.close();
        return cachedResults;
    }

    public List<InvoiceDTO> getListInvoicesPaged(Integer entityId, Integer userId, Integer limit, Integer offset) {

        List<InvoiceDTO> result = new InvoiceDAS().findInvoicesByUserPaged(userId, limit, offset);
        return result;
    }

    public CachedRowSet getInvoicesByProcessId(Integer processId)
            throws SQLException, Exception {

        prepareStatement(InvoiceSQL.processList);
        cachedResults.setInt(1, processId.intValue());

        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet getInvoicesToPrintByProcessId(Integer processId)
            throws SQLException, Exception {

        prepareStatement(InvoiceSQL.processPrintableList);
        cachedResults.setInt(1, processId.intValue());

        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet getInvoicesByUserId(Integer userId)
            throws SQLException, Exception {

        prepareStatement(InvoiceSQL.custList);
        cachedResults.setInt(1, userId.intValue());

        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet getInvoicesByIdRange(Integer from, Integer to,
            Integer entityId) throws SQLException, Exception {

        prepareStatement(InvoiceSQL.rangeList);
        cachedResults.setInt(1, from.intValue());
        cachedResults.setInt(2, to.intValue());
        cachedResults.setInt(3, entityId.intValue());

        execute();
        conn.close();
        return cachedResults;
    }

    public Integer[] getInvoicesByCreateDateArray(Integer entityId, Date since,
            Date until) throws SQLException, Exception {

        cachedResults = getInvoicesByCreateDate(entityId, since, until);

        // get ids for return
        List ids = new ArrayList();
        while (cachedResults.next()) {
            ids.add(new Integer(cachedResults.getInt(1)));
        }
        Integer[] retValue = new Integer[ids.size()];
        if (retValue.length > 0) {
            ids.toArray(retValue);
        }

        return retValue;
    }

    public CachedRowSet getInvoicesByCreateDate(Integer entityId, Date since,
            Date until) throws SQLException, Exception {

        prepareStatement(InvoiceSQL.getByDate);
        cachedResults.setInt(1, entityId.intValue());
        cachedResults.setDate(2, new java.sql.Date(since.getTime()));
        // add a day to include the until date
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(until);
        cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
        cachedResults.setDate(3, new java.sql.Date(cal.getTime().getTime()));

        execute();

        conn.close();
        return cachedResults;
    }

    public Integer convertNumberToID(Integer entityId, String number)
            throws SQLException, Exception {

        prepareStatement(InvoiceSQL.getIDfromNumber);
        cachedResults.setInt(1, entityId.intValue());
        cachedResults.setString(2, number);

        execute();

        conn.close();
        if (cachedResults.wasNull()) {
            return null;
        } else {
            cachedResults.next();
            return new Integer(cachedResults.getInt(1));
        }
    }

    public Integer getLastByUser(Integer userId) throws SQLException {

        Integer retValue = null;
        if (userId == null) {
            return null;
        }
        prepareStatement(InvoiceSQL.lastIdbyUser);
        cachedResults.setInt(1, userId.intValue());

        execute();
        if (cachedResults.next()) {
            int value = cachedResults.getInt(1);
            if (!cachedResults.wasNull()) {
                retValue = new Integer(value);
            }
        }
        conn.close();
        return retValue;
    }

    public Integer getLastByUserAndItemType(Integer userId, Integer itemTypeId)
            throws SQLException {

        Integer retValue = null;
        if (userId == null) {
            return null;
        }
        prepareStatement(InvoiceSQL.lastIdbyUserAndItemType);
        cachedResults.setInt(1, userId.intValue());
        cachedResults.setInt(2, itemTypeId.intValue());

        execute();
        if (cachedResults.next()) {
            int value = cachedResults.getInt(1);
            if (!cachedResults.wasNull()) {
                retValue = new Integer(value);
            }
        }
        cachedResults.close();
        conn.close();
        return retValue;
    }

    public Boolean isUserWithOverdueInvoices(Integer userId, Date today,
            Integer excludeInvoiceId) throws SQLException {

        Boolean retValue= Boolean.FALSE;
        prepareStatement(InvoiceSQL.getOverdueForAgeing);
        cachedResults.setDate(1, new java.sql.Date(today.getTime()));
        cachedResults.setInt(2, userId.intValue());
        if (excludeInvoiceId != null) {
            cachedResults.setInt(3, excludeInvoiceId.intValue());
        } else {
            // nothing to exclude, use an imposible ID (zero)
            cachedResults.setInt(3, 0);
        }

        execute();
        UserDTO user= new UserDAS().find(userId);
        InvoiceDAS invoiceDAS= new InvoiceDAS();
        while (cachedResults.next()) {
            int invoiceId = cachedResults.getInt(1);
            if (isInvoiceBalanceEnoughToAge(invoiceDAS.find(invoiceId), user.getEntity().getId())) {
            	retValue= Boolean.TRUE;
            	LOG.debug("user with invoice:" + cachedResults.getInt(1));
            	break;
            }
        }
        
        conn.close();
        LOG.debug("user with overdue: %s", retValue);
        return retValue;
    }

    public Integer[] getUsersOverdueInvoices(Integer userId, Date date) {
        List<Integer> result = new InvoiceDAS().findIdsOverdueForUser(userId, date);
        return result.toArray(new Integer[result.size()]);
    }

    public Integer[] getUserInvoicesByDate(Integer userId, Date since,
            Date until) {
        // add a day to include the until date
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(until);
        cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
        until = cal.getTime();

        List<Integer> result = new InvoiceDAS().findIdsByUserAndDate(
                userId, since, until);

        return result.toArray(new Integer[result.size()]);
    }

    public Integer[] getManyWS(Integer userId, Integer number)
            throws SessionInternalError {
        List<Integer> result = new InvoiceDAS().findIdsByUserLatestFirst(
                userId, number);
        return result.toArray(new Integer[result.size()]);
    }

    public Integer[] getManyByItemTypeWS(Integer userId, Integer itemTypeId, Integer number)
            throws SessionInternalError {
        List<Integer> result = new InvoiceDAS().findIdsByUserAndItemTypeLatestFirst(userId, itemTypeId, number);
        return result.toArray(new Integer[result.size()]);
    }


    public InvoiceWS[] DTOtoWS(List dtos) {
        InvoiceWS retValue[] = new InvoiceWS[dtos.size()];
        for (int f = 0; f < retValue.length; f++) {
            retValue[f] = InvoiceBL.getWS((InvoiceDTO) dtos.get(f));
        }
        LOG.debug("converstion %d", retValue.length);

        return retValue;
    }



    public void sendReminders(Date today) throws SQLException,
            SessionInternalError {
        GregorianCalendar cal = new GregorianCalendar();

        for (Iterator it = new CompanyDAS().findEntities().iterator(); it.hasNext();) {
            CompanyDTO thisEntity = (CompanyDTO) it.next();
            Integer entityId = thisEntity.getId();
            int preferenceUseInvoiceReminders = 0;
            try {
                preferenceUseInvoiceReminders = 
                	PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_USE_INVOICE_REMINDERS);
            } catch (EmptyResultDataAccessException e1) {
            // let it use the defaults
            }
            if (preferenceUseInvoiceReminders == 1) {
                prepareStatement(InvoiceSQL.toRemind);

                cachedResults.setDate(1, new java.sql.Date(today.getTime()));
                cal.setTime(today);
                int preferenceFirstReminder = 
                	PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_FIRST_REMINDER);
                cal.add(GregorianCalendar.DAY_OF_MONTH, (preferenceFirstReminder != 0 ? -preferenceFirstReminder : preferenceFirstReminder));
                cachedResults.setDate(2, new java.sql.Date(cal.getTimeInMillis()));
                cal.setTime(today);
                int preferenceNextReminder = 
                	PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_NEXT_REMINDER);
                cal.add(GregorianCalendar.DAY_OF_MONTH, (preferenceNextReminder != 0 ? -preferenceNextReminder : preferenceNextReminder));
                cachedResults.setDate(3, new java.sql.Date(cal.getTimeInMillis()));

                cachedResults.setInt(4, entityId.intValue());

                execute();
                while (cachedResults.next()) {
                    int invoiceId = cachedResults.getInt(1);
                    set(new Integer(invoiceId));
                    NotificationBL notif = new NotificationBL();
                    long mils = invoice.getDueDate().getTime() - today.getTime();
                    int days = Math.round(mils / 1000 / 60 / 60 / 24);

                    try {
                        MessageDTO message = notif.getInvoiceReminderMessage(
                                entityId, invoice.getBaseUser().getUserId(),
                                new Integer(days), invoice.getDueDate(),
                                invoice.getPublicNumber(), invoice.getTotal(),
                                invoice.getCreateDatetime(), invoice.getCurrency().getId());

                        INotificationSessionBean notificationSess =
                                (INotificationSessionBean) Context.getBean(
                                Context.Name.NOTIFICATION_SESSION);

                        notificationSess.notify(invoice.getBaseUser(), message);

                        invoice.setLastReminder(today);
                    } catch (NotificationNotFoundException e) {
                        LOG.warn("There are invoice to send reminders, but the notification message is missing for entity %d", entityId);
                    }
                }
            }
        }

        if (conn != null) { // only if somthing run
            conn.close();
        }

    }

    public InvoiceWS getWS() {
        return getWS(invoice);
    }

    public static InvoiceWS getWS(InvoiceDTO i) {
        if (i == null) {
            return null;
        }
        InvoiceWS retValue = new InvoiceWS();
        retValue.setId(i.getId());
        retValue.setCreateDateTime(i.getCreateDatetime());
        retValue.setCreateTimeStamp(i.getCreateTimestamp());
        retValue.setLastReminder(i.getLastReminder());
        retValue.setDueDate(i.getDueDate());
        retValue.setTotal(i.getTotal());
        retValue.setToProcess(i.getToProcess());
        retValue.setStatusId(i.getInvoiceStatus().getId());
        retValue.setBalance(i.getBalance());
        retValue.setCarriedBalance(i.getCarriedBalance());
        retValue.setInProcessPayment(i.getInProcessPayment());
        retValue.setDeleted(i.getDeleted());
        retValue.setPaymentAttempts(i.getPaymentAttempts());
        retValue.setIsReview(i.getIsReview());
        retValue.setCurrencyId(i.getCurrency().getId());
        retValue.setCustomerNotes(i.getCustomerNotes());
        retValue.setNumber(i.getPublicNumber());
        retValue.setOverdueStep(i.getOverdueStep());
        retValue.setUserId(i.getBaseUser().getId());

        Integer delegatedInvoiceId = i.getInvoice() == null ? null : i.getInvoice().getId();
        Integer userId = i.getBaseUser().getId();
        Integer payments[] = new Integer[i.getPaymentMap().size()];
        com.sapienter.jbilling.server.entity.InvoiceLineDTO invoiceLines[] =
                new com.sapienter.jbilling.server.entity.InvoiceLineDTO[i.getInvoiceLines().size()];
        Integer orders[] = new Integer[i.getOrderProcesses().size()];

        int f;
        f = 0;
        for (PaymentInvoiceMapDTO p : i.getPaymentMap()) {
            payments[f++] = p.getPayment().getId();
        }
        f = 0;
        for (OrderProcessDTO orderP : i.getOrderProcesses()) {
            orders[f++] = orderP.getPurchaseOrder().getId();
        }
        f = 0;
        List<InvoiceLineDTO> ordInvoiceLines = new ArrayList<InvoiceLineDTO>(i.getInvoiceLines());
        Collections.sort(ordInvoiceLines, new InvoiceLineComparator());
        for (InvoiceLineDTO line : ordInvoiceLines) {
            invoiceLines[f++] = new com.sapienter.jbilling.server.entity.InvoiceLineDTO(line.getId(),
                    line.getDescription(), line.getAmount(), line.getPrice(), line.getQuantity(),
                    line.getDeleted(), line.getItem() == null ? null : line.getItem().getId(),
                    line.getSourceUserId(), line.getIsPercentage());
        }

        retValue.setDelegatedInvoiceId(delegatedInvoiceId);
        retValue.setUserId(userId);
        retValue.setPayments(payments);
        retValue.setInvoiceLines(invoiceLines);
        retValue.setOrders(orders);

        retValue.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(
                new UserBL().getEntityId(userId), i));

        retValue.setBillingProcess(i.getBillingProcess() != null ? BillingProcessBL.getWS(i.getBillingProcess()) : null);

        return retValue;
    }

    public InvoiceDTO getDTOEx(Integer languageId, boolean forDisplay) {

        if (!forDisplay) {
            return invoice;
        }

        InvoiceDTO invoiceDTO = new InvoiceDTO(invoice);
        // make sure that the lines are properly ordered
        List<InvoiceLineDTO> orderdLines = new ArrayList<InvoiceLineDTO>(invoiceDTO.getInvoiceLines());
        Collections.sort(orderdLines, new InvoiceLineComparator());
        invoiceDTO.setInvoiceLines(orderdLines);

        UserBL userBl = new UserBL(invoice.getBaseUser());
        Locale locale = userBl.getLocale();
        ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);

        // now add headers and footers if this invoices has sub-account
        // lines
        if (invoiceDTO.hasSubAccounts()) {
            addHeadersFooters(orderdLines, bundle, invoice.getBaseUser().getCustomer().getId());
        }
        // add a grand total final line
        InvoiceLineDTO total = new InvoiceLineDTO();
        total.setDescription(bundle.getString("invoice.line.total"));
        total.setAmount(invoice.getTotal());
        total.setIsPercentage(0);
        invoiceDTO.getInvoiceLines().add(total);

        // add some currency info for the human
        CurrencyBL currency = new CurrencyBL(invoice.getCurrency().getId());
        if (languageId != null) {
            invoiceDTO.setCurrencyName(currency.getEntity().getDescription(
                    languageId));
        }
        invoiceDTO.setCurrencySymbol(currency.getEntity().getSymbol());

        return invoiceDTO;

    }

    /**
     * Will add lines with headers and footers to make an invoice with
     * sub-accounts more readable. The lines have to be already sorted.
     *
     * @param lines
     * @param parentId
     * @return
     */
    private void addHeadersFooters(List<InvoiceLineDTO> lines, ResourceBundle bundle, Integer parentId) {
        Integer nowProcessing = new Integer(-1);
        BigDecimal total = null;
        int totalLines = lines.size();
        int subaccountNumber = 0;
        CustomerDTO subAccountCustomer;
        LOG.debug("adding headers & footers.%d", totalLines);

        for (int idx = 0; idx < totalLines; idx++) {
            InvoiceLineDTO line = (InvoiceLineDTO) lines.get(idx);

            if ( null != line.getSourceUserId() ) {
            	// to check an invoiceLine belongs to a sub-account user
                // compare invoiceLine.customer.parent with invoice.customer
                subAccountCustomer = UserBL.getUserEntity(line.getSourceUserId()).getCustomer();
                if ( null != subAccountCustomer.getParent() && (subAccountCustomer.getParent().getId() == parentId) && (!line.getSourceUserId().equals(nowProcessing))){
                    // line break
                    nowProcessing = line.getSourceUserId();
                    subaccountNumber++;
                    // put the total first
                    if (total != null) { // it could be the first sub-account
                        InvoiceLineDTO totalLine = new InvoiceLineDTO();
                        totalLine.setDescription(bundle.getString("invoice.line.subAccount.footer"));
                        totalLine.setAmount(total);
                        lines.add(idx, totalLine);
                        idx++;
                        totalLines++;               
                    }
                    total = BigDecimal.ZERO;

                    // now the header announcing a new sub-accout
                    InvoiceLineDTO headerLine = new InvoiceLineDTO();
                    try {
                        ContactBL contact = new ContactBL();
                        contact.set(nowProcessing);
                        StringBuffer text = new StringBuffer();
                        text.append(subaccountNumber + " - ");
                        text.append(bundle.getString("invoice.line.subAccount.header1"));
                        text.append(" " + bundle.getString("invoice.line.subAccount.header2") + " " + nowProcessing);
                        if ( null != contact.getEntity() ) {
    	                    if (contact.getEntity().getFirstName() != null) {
    	                        text.append(" " + contact.getEntity().getFirstName());
    	                    }
    	                    if (contact.getEntity().getLastName() != null) {
    	                        text.append(" " + contact.getEntity().getLastName());
    	                    }
                        }
                        headerLine.setDescription(text.toString());
                        lines.add(idx, headerLine);
                        idx++;
                        totalLines++;
                    } catch (Exception e) {
                        LOG.error("Exception", e);
                        return;
                    }
                }

                // update the total
                if (total != null) {
                    // there had been at least one sub-account processed
                    if (null != subAccountCustomer.getParent() && (subAccountCustomer.getParent().getId()==parentId)) {
                        total = total.add(line.getAmount());
                    } else {
                        // this is the last total to display, from now on the
                        // lines are not of sub-accounts
                        InvoiceLineDTO totalLine = new InvoiceLineDTO();
                        totalLine.setDescription(bundle.getString("invoice.line.subAccount.footer"));
                        totalLine.setAmount(total);
                        lines.add(idx, totalLine);
                        total = null; // to avoid repeating
                    }
                }
            }
        }
        // if there are no lines after the last sub-account, we need
        // a total for it
        if (total != null) { // only if it wasn't added before
            InvoiceLineDTO totalLine = new InvoiceLineDTO();
            totalLine.setDescription(bundle.getString("invoice.line.subAccount.footer"));
            totalLine.setAmount(total);
            lines.add(totalLine);
        }

        LOG.debug("done %d", lines.size());
    }

    /***
     * Will return InvoiceDTO with headers
     * if invoice for sub-account users
     *
     * @return
     */
    public InvoiceDTO getInvoiceDTOWithHeaderLines() {
        InvoiceDTO invoiceDTO = new InvoiceDTO(invoice);
        List<InvoiceLineDTO> invoiceLines = new ArrayList<InvoiceLineDTO>(invoiceDTO.getInvoiceLines());

        // now add headers if this invoices has sub-account lines
        if (invoiceDTO.hasSubAccounts()) {
            invoiceDTO.setInvoiceLines(addHeaders(invoiceLines, invoice.getBaseUser().getId()));
        } else {
            //if there are no sub account than just sort the lines
            Collections.sort(invoiceLines, new InvoiceLineComparator());
            invoiceDTO.setInvoiceLines(invoiceLines);
        }

        return invoiceDTO;
    }

    /**
     * Will add lines with headers to make an invoice with
     * subaccounts more readable.
     *
     * @param lines
     * @param parentUserId
     * @return
     */
    public static List<InvoiceLineDTO> addHeaders(List<InvoiceLineDTO> lines, Integer parentUserId) {
        Integer nowProcessing = Integer.valueOf(-1);
        Integer parentCustomerId = UserBL.getUserEntity(parentUserId).getCustomer().getId();

        LOG.debug("adding headers for sub-account users, total invoice lines : %d", lines.size());

        Map<Integer, List<InvoiceLineDTO>> accountLineGroups = new HashMap<Integer, List<InvoiceLineDTO>>();
        Map<Integer, InvoiceLineDTO> accountLineHeaders = new HashMap<Integer, InvoiceLineDTO>();

        for (int idx = 0; idx < lines.size(); idx++) {
            InvoiceLineDTO line = lines.get(idx);

            // to check an invoiceLine belongs to a sub-account user
            // compare invoiceLine.customer.parent with invoice.customer
            if ( null != line.getSourceUserId() ) {

                nowProcessing = line.getSourceUserId();

            	UserDTO subAccount = UserBL.getUserEntity( nowProcessing );
            	if (subAccount != null){
	            	CustomerDTO subAccountCustomer = subAccount.getCustomer();
		            if (null != subAccountCustomer.getParent() &&
		                    subAccountCustomer.getParent().getId() == parentCustomerId &&
		                    !accountLineHeaders.containsKey(nowProcessing)) {
		                InvoiceLineDTO headerLine = createHeaderLine(nowProcessing);
		                if(null == headerLine){
		                    LOG.debug("Could not create a header line for invoice line %d source user id $d", line.getId(), nowProcessing);
		                } else {
		                    accountLineHeaders.put(nowProcessing, headerLine);
		                }
		            }
            	}

                //groups the lines based on a account
                List<InvoiceLineDTO> group = accountLineGroups.get(nowProcessing);
                if(null == group){
                    group = new ArrayList<InvoiceLineDTO>();
                    accountLineGroups.put(nowProcessing, group);
                }
                group.add(line);
            }
        }

        //first add the parent invoice lines
        List<InvoiceLineDTO> result = new ArrayList<InvoiceLineDTO>();
        List<InvoiceLineDTO> parentLines = accountLineGroups.get(parentUserId);
        if(null != parentLines && !parentLines.isEmpty()){
            accountLineGroups.remove(parentUserId);
            Collections.sort(parentLines, new InvoiceLineComparator());
            result.addAll(parentLines);
        }

        //now add subaccount invoice lines
        for(Map.Entry<Integer, List<InvoiceLineDTO>> entry : accountLineGroups.entrySet()){
            Integer userId = entry.getKey();
            InvoiceLineDTO headerLine = accountLineHeaders.get(userId);
            if(null != headerLine){
                result.add(headerLine);
            }

            List<InvoiceLineDTO> subAccountLines = entry.getValue();
            if(null != subAccountLines && !subAccountLines.isEmpty()){
                Collections.sort(subAccountLines, new InvoiceLineComparator());
                result.addAll(subAccountLines);
            }
        }


        LOG.debug("Now, total line size : %d", result.size());
        return result;
    }

    private static InvoiceLineDTO createHeaderLine(Integer sourceUserId) {
        // now the header announcing a new sub-account
        InvoiceLineDTO headerLine = new InvoiceLineDTO();
        try {
            ContactDTOEx contact = ContactBL.buildFromMetaField(sourceUserId, new Date());
            //get user's name
            StringBuilder name = new StringBuilder("");
            if ( null != contact ) {
	            if (!StringUtils.isEmpty(contact.getFirstName()) || !StringUtils.isEmpty(contact.getLastName())) {
	                if (contact.getFirstName() != null) {
	                    name.append(contact.getFirstName());
	                }
	                if (contact.getLastName() != null) {
	                    name.append(" " + contact.getLastName());
	                }
                } else if(!StringUtils.isEmpty(contact.getOrganizationName())){
	                name.append(contact.getOrganizationName());
                }
            }
            if (name.toString().equals("")) {
                name.append(new UserDAS().find(sourceUserId).getUserName());
            }
            headerLine.setDescription(name.toString());
            headerLine.setSourceUserId(sourceUserId);
        } catch (Exception e) {
            LOG.error("Exception", e);
            return null;
        }
        return headerLine;
    }
    
    public InvoiceDTO getDTO() {
        return invoice;

    }

    // given the current invoice, it will 'rewind' to the previous one
    public void setPrevious() throws SQLException,
            EmptyResultDataAccessException {

        prepareStatement(InvoiceSQL.previous);
        cachedResults.setInt(1, invoice.getBaseUser().getUserId().intValue());
        cachedResults.setInt(2, invoice.getId());
        boolean found = false;

        execute();
        if (cachedResults.next()) {
            int value = cachedResults.getInt(1);
            if (!cachedResults.wasNull()) {
                set(new Integer(value));
                found = true;
            }
        }
        conn.close();

        if (!found) {
            throw new EmptyResultDataAccessException("No previous invoice found", 1);
        }
    }
    
    public static boolean isInvoiceBalanceEnoughToAge (InvoiceDTO invoice, Integer entityId) {
        //check if balance below minimum balance to ignore ageing
        BigDecimal minBalanceToIgnore = BigDecimal.ZERO;
        try {
        	if (null == entityId && null != invoice.getBaseUser()) {
        		entityId= invoice.getBaseUser().getEntity().getId();
        	}
        	
            minBalanceToIgnore = 
            	PreferenceBL.getPreferenceValueAsDecimalOrZero(entityId, Constants.PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING);
            
            LOG.debug("Mininmum balance to ignore ageing preference set to " + minBalanceToIgnore);
        } catch (EmptyResultDataAccessException e) {
            LOG.debug("Preference minimum balance to ignore ageing not set.");
        }
    
        LOG.debug("Checking balance " + invoice.getBalance()
                + " against " + minBalanceToIgnore+ " for invoiceId : " + invoice.getId());
        
        //Return 'true' if balance above min to ignore preference value or zeor if preference not set 
        return (invoice.getBalance().compareTo(minBalanceToIgnore) > 0);
    }

    /*
    public static InvoiceWS getWS(InvoiceDTO dto) {
        InvoiceWS ret = new InvoiceWS();
        ret.setBalance(dto.getBalance());
        ret.setCarriedBalance(dto.getCarriedBalance());
        ret.setCreateDateTime(dto.getCreateDatetime());
        ret.setCreateTimeStamp(dto.getCreateTimestamp());
        ret.setCurrencyId(dto.getCurrency().getId());
        ret.setCustomerNotes(dto.getCustomerNotes());
        ret.setDelegatedInvoiceId(dto.getDelegatedInvoiceId());
        ret.setDeleted(dto.getDeleted());
        ret.setDueDate(dto.getDueDate());
        ret.setInProcessPayment(dto.getInProcessPayment());
        ret.setUserId(dto.getUserId());
        ret.setIsReview(dto.getIsReview());
        ret.setLastReminder(dto.getLastReminder());
        ret.setNumber(dto.getPublicNumber());
        ret.setOverdueStep(dto.getOverdueStep());
        ret.setPaymentAttempts(dto.getOverdueStep());
        ret.setToProcess(dto.getToProcess());
        ret.setTotal(dto.getTotal());
        ret.setUserId(dto.getUserId());

        Integer payments[] = new Integer[dto.getPaymentMap().size()];
        Integer orders[] = new Integer[dto.getOrders().size()];

        int f;
        for (f = 0; f < dto.getPaymentMap().size(); f++) {
            PaymentInvoiceMapDTOEx map = (PaymentInvoiceMapDTOEx) dto.getPaymentMap().get(f);
            payments[f] = map.getPaymentId();
        }
        ret.setPayments(payments);
        for (f = 0; f < dto.getOrders().size(); f++) {
            OrderDTO order = (OrderDTO) dto.getOrders().get(f);
            orders[f] = order.getId();
        }
        ret.setOrders(orders);

        com.sapienter.jbilling.server.entity.InvoiceLineDTO lines[] =
                new com.sapienter.jbilling.server.entity.InvoiceLineDTO[dto.getInvoiceLines().size()];

        f=0;
        for (InvoiceLineDTO line : dto.getInvoiceLines()) {
            lines[f++] = new com.sapienter.jbilling.server.entity.InvoiceLineDTO(line.getId(),
                    line.getDescription(), line.getAmount(), line.getPrice(), line.getQuantity(),
                    line.getDeleted(), line.getItem() == null ? null : line.getItem().getId(),
                    line.getSourceUserId(), line.getIsPercentage());
        }

        return ret;
    }
     * */
}

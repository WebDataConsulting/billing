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

package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.PreferenceBL;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This task will copy all the lines on the orders and invoices to the new invoice, considering the periods involved for
 * each order, but not the fractions of perios. It will not copy the lines that are taxes. The quantity and total of
 * each line will be multiplied by the amount of periods.
 * 
 * @author Emil Created on 27-Apr-2003
 */
public class BasicCompositionTask extends PluggableTask implements InvoiceCompositionTask {

    private static final FormatLogger LOG                       = new FormatLogger(
                                                                        BasicCompositionTask.class);

    private String                    DATE_FORMAT;
    private String                    INVOICE_LINE_TO;
    private String                    INVOICE_LINE_PERIOD;
    private String                    INVOICE_LINE_ORDER_NUMBER;
    private String                    INVOICE_LINE_DELEGATED;
    private String                    INVOICE_LINE_DELEGATED_DUE;
    private DateTimeFormatter         dateFormatter;
    private Locale                    locale;

    private boolean                   resourceBundleInitialized = false;

    private BigDecimal                ETALON_ZERO               = new BigDecimal(0).setScale(
                                                                        ServerConstants.BIGDECIMAL_SCALE,
                                                                        ServerConstants.BIGDECIMAL_ROUND);

    public void apply (NewInvoiceContext invoiceCtx, Integer userId) throws TaskException {
        // initialize resource bundle once, if not initialized
        if (!resourceBundleInitialized) {
            initializeResourceBundleProperties(userId);
        }
        /*
         * Process each order being included in this invoice
         */
        for (NewInvoiceContext.OrderContext orderCtx : invoiceCtx.getOrders()) {

            OrderDTO order = orderCtx.order;

            if (Integer.valueOf(1).equals(order.getNotesInInvoice())) {
                invoiceCtx.appendCustomerNote(order.getNotes());
            }
            // Add order lines - excluding taxes
            for (OrderLineDTO orderLine : order.getLines()) {

                if (orderLine.getDeleted() == 1) {
                    continue;
                }
                List<NewInvoiceContext.OrderLineCtx> contexts = NewInvoiceContext.calcOrderLineChanges(orderLine);

                for (PeriodOfTime period : orderCtx.periods) {
                    for (NewInvoiceContext.OrderLineCtx orderLineCtx : contexts) {
                        PeriodOfTime adjustedPeriod = getAdjustedPeriod(orderLineCtx, period);
                        LOG.debug("%s adjusted period for %s is %s", orderLineCtx.toString(), period, adjustedPeriod);
                        if (adjustedPeriod.getDaysInPeriod() != 0
                            || adjustedPeriod == PeriodOfTime.OneTimeOrderPeriodOfTime) {
                            BigDecimal periodAmount = composeInvoiceLineForPeriod(invoiceCtx, userId, orderLineCtx,
                                    adjustedPeriod);
                            LOG.debug("Total Contribution %s, before adding Period Amount %s", orderCtx.totalContribution, periodAmount);
                            orderCtx.totalContribution = orderCtx.totalContribution.add(periodAmount);
                        }
                    }
                }
            }
        }

        /*
         * add delegated invoices
         */
        for (InvoiceDTO invoice : invoiceCtx.getInvoices()) {
            // the whole invoice will be added as a single line
            // The text of this line has to be i18n
            String delegatedLine = new StringBuilder(100)
                    .append(INVOICE_LINE_DELEGATED)
                    .append(' ')
                    .append(invoice.getPublicNumber())
                    .append(' ')
                    .append(INVOICE_LINE_DELEGATED_DUE)
                    .append(' ')
                    .append(dateFormatter.print(invoice.getDueDate().getTime()))
                    .toString();

            invoiceCtx.addResultLine(new InvoiceLineDTO.Builder()
                    .description(delegatedLine)
                    .amount(invoice.getBalance())
                    .type(ServerConstants.INVOICE_LINE_TYPE_DUE_INVOICE)
                    .build());
        }
    }

    private PeriodOfTime getAdjustedPeriod (NewInvoiceContext.OrderLineCtx orderLineCtx, PeriodOfTime period) {
        if (period == PeriodOfTime.OneTimeOrderPeriodOfTime) {
            return period;
        }
        Date start = period.getStart();
        if ((orderLineCtx.getStartDate() != null) && orderLineCtx.getStartDate().after(start)) {
            start = orderLineCtx.getStartDate();
        }
        if ((orderLineCtx.getNextBillableDate() != null) && orderLineCtx.getNextBillableDate().after(start)) {
            start = orderLineCtx.getNextBillableDate();
        }
        Date end = period.getEnd();
        if (orderLineCtx.getEndDate() != null && orderLineCtx.getEndDate().before(end)) {
            end = orderLineCtx.getEndDate();
        }

        PeriodOfTime result = new PeriodOfTime(start, end, period.getDaysInCycle());
        if (! orderLineCtx.getPurchaseOrder().getProrateFlag() && result.getDaysInPeriod() != 0) {
            // reset to full period for non prorated orders to get correct description lines.
            result = period;
        }
        return result;
    }

    /**
     * for each order line tier value, based on order line type:
     * 
     * 1. determine invoice line type (specific for ITEM, DISCOUNT)
     * 
     * 2. compose description (specific for ITEM, DISCOUNT)
     * 
     * 3. calculate amount for period (specific for ITEM, DISCOUNT)
     * 
     * 4. apply amount to invoice line (specific to TAX)
     * 
     * 5. fill other invoice line fields
     * 
     * 6. apply amount to order totals
     * 
     * @param invoiceCtx
     * @param userId
     * @param orderLine
     * @param period
     * 
     * @return calclulated amount this line is contributed to invoice
     */
    private BigDecimal composeInvoiceLineForPeriod (NewInvoiceContext invoiceCtx, Integer userId,
            OrderLineDTO orderLine, PeriodOfTime period) {

        OrderDTO order = orderLine.getPurchaseOrder();
        /*
         * { Set<OrderChangeDTO> lineChanges = orderLine.getOrderChanges(); // exclude post-dated changes // find and
         * apply unappplied back-dated changes // for each line change create orderLineCtx to calc }
         */
        LOG.debug("Adding order line from %s, quantity %s, price %s, typeid %s. Period: %s", order.getId(),
                orderLine.getQuantity(), orderLine.getPrice(), orderLine.getTypeId(), period);

        BigDecimal periodAmount = calculateAmountForPeriod(orderLine, period);

        if (ETALON_ZERO.equals(periodAmount)) {
            return BigDecimal.ZERO;
        }

        InvoiceLineDTO.Builder newLine = new InvoiceLineDTO.Builder()
                .description(orderLine.getDescription())
                .sourceUserId(order.getUser().getId())
                .itemId(orderLine.getItemId())
                .amount(periodAmount)
                .price(orderLine.getPrice());
        newLine.isPercentage(orderLine.isPercentage());
        switch (orderLine.getTypeId()) {

        case ServerConstants.ORDER_LINE_TYPE_ITEM:
        case ServerConstants.ORDER_LINE_TYPE_DISCOUNT:
        case ServerConstants.ORDER_LINE_TYPE_SUBSCRIPTION:

            newLine.type(determineInvoiceLineType(userId, order));
            newLine.description(composeDescription(orderLine, period));

            newLine.quantity(orderLine.getQuantity());
            // link invoice line to the order that originally held the charge
            newLine.order(order);
            break;

        // tax items
        case ServerConstants.ORDER_LINE_TYPE_TAX:

            newLine.type(ServerConstants.INVOICE_LINE_TYPE_TAX);

            InvoiceLineDTO taxLine = findTaxLine(invoiceCtx.getResultLines(), orderLine.getDescription());
            if (taxLine != null) {
                // tax already exists, add the total
                taxLine.setAmount(taxLine.getAmount().add(periodAmount));
                // not need to add a new invoice line
                newLine = null;
            }
            break;

        // penalty items
        case ServerConstants.ORDER_LINE_TYPE_PENALTY:

            newLine.type(ServerConstants.INVOICE_LINE_TYPE_PENALTY);
            break;

        default:
            LOG.debug("Unsupported order line type id: %s", orderLine.getTypeId());
            break;
        }

        if (newLine != null) {
            invoiceCtx.addResultLine(newLine.build());
        }
        return periodAmount;
    }

    private BigDecimal calculateAmountForPeriod (OrderLineDTO orderLine, PeriodOfTime period) {
        if (orderLine.getPurchaseOrder().getProrateFlag()) {
            return calculateProRatedAmountForPeriod(orderLine.getAmount(), period);
        }
        return orderLine.getAmount();
    }

    private Integer determineInvoiceLineType (Integer userId, OrderDTO order) {
        if (userId.equals(order.getUser().getId())) {
            return (order.isOneTime()) ? ServerConstants.INVOICE_LINE_TYPE_ITEM_ONETIME
                    : ServerConstants.INVOICE_LINE_TYPE_ITEM_RECURRING;
        }
        return ServerConstants.INVOICE_LINE_TYPE_SUB_ACCOUNT;
    }

    private BigDecimal calculateProRatedAmountForPeriod (BigDecimal fullPrice, PeriodOfTime period) {

        if (period == null || fullPrice == null) {
            LOG.warn("Called with null parameters");
            return null;
        }

        // this is an amount from a one-time order, not a real period of time
        if (period == PeriodOfTime.OneTimeOrderPeriodOfTime) {
            return fullPrice;
        }

        // if this is not a fraction of a period, don't bother making any calculations
        if (period.getDaysInCycle() == period.getDaysInPeriod()) {
            return fullPrice;
        }

        BigDecimal oneDayPrice = fullPrice.divide(new BigDecimal(period.getDaysInCycle()), ServerConstants.BIGDECIMAL_SCALE,
                ServerConstants.BIGDECIMAL_ROUND);

        return oneDayPrice.multiply(new BigDecimal(period.getDaysInPeriod())).setScale(ServerConstants.BIGDECIMAL_SCALE,
                ServerConstants.BIGDECIMAL_ROUND);
    }

    /**
     * Returns the index of a tax line with the matching description. Used to find an existing tax line so that similar
     * taxes can be consolidated;
     *
     * @param lines
     *            invoice lines
     * @param desc
     *            tax line description
     * @return index of tax line
     */
    private InvoiceLineDTO findTaxLine (List<InvoiceLineDTO> lines, String desc) {
        for (InvoiceLineDTO line : lines) {
            if (line.getTypeId() == ServerConstants.ORDER_LINE_TYPE_TAX && line.getDescription().equals(desc)) {
                return line;
            }
        }
        return null;
    }

    /**
     * Composes the actual invoice line description based off of set entity preferences and the order period being
     * processed.
     *
     * @param order
     *            order being processed
     * @param period
     *            period of time being processed
     * @param desc
     *            original order line description
     * @return invoice line description
     */
    protected String composeDescription (OrderLineDTO orderLine, PeriodOfTime period) {
        OrderDTO order = orderLine.getPurchaseOrder();
        // initialize resource bundle once, if not initialized
        if (!resourceBundleInitialized) {
            initializeResourceBundleProperties(order.getBaseUserByUserId().getUserId());
        }
        StringBuilder lineDescription = new StringBuilder(1000).append(orderLine.getDescription());

        /*
         * append the billing period to the order line for non one-time orders
         */
        if (order.isRecurring()) {
            // period ends at midnight of the next day (E.g., Oct 1 00:00, effectively end-of-day Sept 30th).
            // subtract 1 day from the end so the period print out looks human readable
            LocalDate start = period.getDateMidnightStart();
            LocalDate end = period.getDateMidnightEnd().minusDays(1);

            LOG.debug("Composing for period %s to %s. Using date format: %s", start, end, DATE_FORMAT);

            // now add this to the line
            lineDescription.append(' ').append(INVOICE_LINE_PERIOD).append(' ');
            lineDescription.append(dateFormatter.print(start)).append(' ');
            lineDescription.append(INVOICE_LINE_TO).append(' ');
            lineDescription.append(dateFormatter.print(end));
        }
        /*
         * optionally append the order id if the entity has the preference set
         */
        if (needAppendOrderId(order.getBaseUserByUserId().getCompany().getId())) {
            lineDescription.append(INVOICE_LINE_ORDER_NUMBER);
            lineDescription.append(' ');
            lineDescription.append(order.getId().toString());
        }
        return lineDescription.toString();
    }

    /**
     * Gets the locale for the given user.
     *
     * @param userId
     *            user to get locale for
     * @return users locale
     */
    protected Locale getLocale (Integer userId) {
        if (locale == null) {
            try {
                UserBL user = new UserBL(userId);
                locale = user.getLocale();
            } catch (Exception e) {
                throw new SessionInternalError("Exception occurred determining user locale for composition.", e);
            }
        }
        return locale;
    }

    /**
     * Returns true if the given entity wants the order ID appended to the invoice line description.
     *
     * @param entityId
     *            entity id
     * @return true if order ID should be appended, false if not.
     */
    protected boolean needAppendOrderId (Integer entityId) {
        int preferenceOrderIdInInvoiceLine = 0;
        try {
            preferenceOrderIdInInvoiceLine = PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId,
                    ServerConstants.PREFERENCE_ORDER_IN_INVOICE_LINE);
        } catch (Exception e) {
            /* use default value */
        }
        return preferenceOrderIdInInvoiceLine == 1;
    }

    private void initializeResourceBundleProperties (Integer userId) {
        LOG.debug("Initializing resource bundle properties");
        ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", getLocale(userId));

        DATE_FORMAT = bundle.getString("format.date");
        INVOICE_LINE_TO = bundle.getString("invoice.line.to");
        INVOICE_LINE_PERIOD = bundle.getString("invoice.line.period");
        INVOICE_LINE_ORDER_NUMBER = bundle.getString("invoice.line.orderNumber");
        INVOICE_LINE_DELEGATED = bundle.getString("invoice.line.delegated");
        INVOICE_LINE_DELEGATED_DUE = bundle.getString("invoice.line.delegated.due");

        dateFormatter = DateTimeFormat.forPattern(DATE_FORMAT);

        resourceBundleInitialized = true;
    }
}

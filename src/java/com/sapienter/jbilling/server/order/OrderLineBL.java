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

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.user.UserBL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 *
 * @author emilc
 */
public class OrderLineBL {

    private static final FormatLogger LOG = new FormatLogger(OrderLineBL.class);

    public static List<OrderLineDTO> diffOrderLines(List<OrderLineDTO> lines1, List<OrderLineDTO> lines2) {

        List<OrderLineDTO> diffLines = new ArrayList<OrderLineDTO>();

        Collections.sort(lines1, (a, b) -> new Integer(a.getId()).compareTo(b.getId()) );

        for (OrderLineDTO line : lines2) {
            int index = Collections.binarySearch(lines1, line, (a, b) -> new Integer(a.getId()).compareTo(b.getId()) );

            if (index >= 0) {
                // existing line
                OrderLineDTO diffLine = new OrderLineDTO(lines1.get(index));

                // will fail if amounts or quantities are null...
                diffLine.setAmount(line.getAmount().subtract(diffLine.getAmount()));
                diffLine.setQuantity(line.getQuantity().subtract(diffLine.getQuantity()));

                if (BigDecimal.ZERO.compareTo(diffLine.getAmount()) != 0
                        || BigDecimal.ZERO.compareTo(diffLine.getQuantity()) != 0) {
                    diffLines.add(diffLine);
                }
            } else {
                // This is a new line, in case where the new line
                // has amount=0 and quantity=0 than ignore this line
                // since it does not make any impact on the order
                if (BigDecimal.ZERO.compareTo(line.getAmount()) != 0
                        || BigDecimal.ZERO.compareTo(line.getQuantity()) != 0){
                    diffLines.add(new OrderLineDTO(line));
                }
            }
        }

        LOG.debug("Diff lines are %s", diffLines);
        return diffLines;
    }

    public static List<OrderLineDTO> copy(List<OrderLineDTO> lines) {
        List<OrderLineDTO> retValue = new ArrayList<OrderLineDTO>(lines.size());
        for (OrderLineDTO line : lines) {
            retValue.add(new OrderLineDTO(line));
        }
        return retValue;
    }


    public static void addLine(OrderDTO order, OrderLineDTO line, boolean persist) {
        if (persist) throw new IllegalArgumentException("persist is oboleted"); // TODO remove the argument
        UserBL user = new UserBL(order.getUserId());


        addItem(line.getItemId(), line.getQuantity(), user.getLanguage(), order.getUserId(), order.getCurrencyId(),
                order, line, persist);

    }

    /**
     * Adds a single item (quantity 1) to the given order for the given item id.
     * This method will not force persistence of the added item, instead changes will
     * be persisted when the transaction ends.
     *
     * @param order order to add item to
     * @param itemId id of item to add
     */
    public static void addItem(OrderDTO order, Integer itemId) {
        addItem(order, itemId, false);
    }

    /**
     * Adds a single item (quantity 1) to the given order for the given item id.
     *
     * @param order order to add item to
     * @param itemId id of item to add
     * @param persist save changes immediately if true
     */
    public static void addItem(OrderDTO order, Integer itemId, boolean persist) {
        addItem(order, itemId, new BigDecimal(1), persist);
    }

    /**
     * Adds a quantity of items to the given order for the given item id.
     * This method will not force persistence of the added item, instead changes will
     * be persisted when the transaction ends.
     *
     * @param order order to add item to
     * @param itemId id of item to add
     * @param quantity quantity to add
     */
    public static void addItem(OrderDTO order, Integer itemId, Integer quantity) {
        addItem(order, itemId, quantity, false);
    }

    /**
     * Adds a quantity of items to the given order for the given item id.
     * Use the given price for the addition.
     *
     * @param order order to add item to
     * @param itemId id of item to add
     * @param quantity quantity to add
     */
    public static void addItem(OrderDTO order, Integer itemId, Integer quantity, BigDecimal price) {
        UserBL user = new UserBL(order.getUserId());
        OrderLineDTO line =  new OrderLineDTO();
        line.setItemId(itemId);
        line.setQuantity(quantity);
        line.setPrice(price);
        addItem(itemId, new BigDecimal(quantity), user.getLanguage(), order.getUserId(),
                order.getCurrencyId(), order, line, false);
    }

    /**
     * Adds a quantity of items to the given order for the given item id.
     *
     * @param order order to add item to
     * @param itemId id of item to add
     * @param quantity quantity to add
     * @param persist save changes immediately if true
     */
    public static void addItem(OrderDTO order, Integer itemId, Integer quantity, boolean persist) {
        addItem(order, itemId, new BigDecimal(quantity), persist);
    }

    /**
     * Adds a quantity of items to the given order for the given item id.
     * This method will not force persistence of the added item, instead changes will
     * be persisted when the transaction ends.
     *
     * @param order order to add item to
     * @param itemId id of item to add
     * @param quantity quantity to add
     */
    public static void addItem(OrderDTO order, Integer itemId, BigDecimal quantity) {
        addItem(order, itemId, quantity, false);
    }

    /**
     * Adds a quantity of items to the given order for the given item id.
     *
     * @param order order to add item to
     * @param itemId id of item to add
     * @param quantity quantity to add
     * @param persist save changes immediately if true
     */
    public static void addItem(OrderDTO order, Integer itemId, BigDecimal quantity, boolean persist) {
        UserBL user = new UserBL(order.getUserId());
        addItem(itemId, quantity, user.getLanguage(), order.getUserId(),
                order.getCurrencyId(), order, null, persist);
    }


    private static void addItem(Integer itemID, BigDecimal quantity, Integer language, Integer userId,
                                Integer currencyId, OrderDTO newOrder, OrderLineDTO myLine, boolean persist) {

        if (persist) throw new IllegalArgumentException("persist is oboleted"); // TODO remove the argument
        // check if the item is already in the order
        OrderLineDTO line = (OrderLineDTO) newOrder.getLine(itemID);

        if (myLine == null) {
            myLine = new OrderLineDTO();
            ItemDTO item = new ItemDTO();
            item.setId(itemID);
            myLine.setItem(item);
            myLine.setQuantity(quantity);
        }
        populateWithSimplePrice(newOrder, myLine, language, userId, currencyId, itemID, CommonConstants.BIGDECIMAL_SCALE);
        myLine.setDefaults();

        // create a new line if an existing line does not exist
        // if the line has a different description than the existing, treat it as a new line
        if (line == null || (myLine.getDescription() != null && !myLine.getDescription().equals(line.getDescription()))) {
            OrderLineDTO newLine = new OrderLineDTO(myLine);
            newLine.getAssets().clear();
            newLine.addAssets(myLine.getAssets());
            newOrder.getLines().add(newLine);
            newLine.setPurchaseOrder(newOrder);
            LOG.debug("OrderLineBL line: " + line);
            LOG.debug("OrderLineBl newLine: " + newLine);
            // save the order (with the new line). Otherwise
            // the diff line will have a '0' for the order id and the
            // saving of the mediation record lines gets really complicated
            if (persist) {
                new OrderDAS().save(newOrder);
            }
        } else {
            // the item is there, I just have to update the quantity
        	
            line.setQuantity(line.getQuantity().add(quantity));
            // and also the total amount for this order line
            line.setAmount(line.getAmount().setScale(CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND));
            line.setAmount(line.getAmount().add(myLine.getAmount()));

            line.addAssets(myLine.getAssets());
            LOG.debug("OrderLineBL line: " + line);
            myLine.getAssets().clear();
            LOG.debug("OrderLineBL myLine: "+ myLine);
        }

    }
    /**
     * Returns an order line with everything correctly
     * initialized. It does not call plug-ins to set the price
     *
     * @param order
     * @param language
     * @param userId
     * @param currencyId
     * @param precision
     * @return
     */
    private static void populateWithSimplePrice(OrderDTO order, OrderLineDTO line, Integer language, Integer userId, Integer currencyId,
                                                Integer itemId, Integer precision) {

        ItemBL itemBl = new ItemBL(itemId);
        ItemDTO item = itemBl.getEntity();

        // it takes the line type of the first category this item belongs too...
        // TODO: redo, when item categories are redone
        Integer type = item.getItemTypes().iterator().next().getOrderLineTypeId();
        Boolean editable = OrderBL.lookUpEditable(type);

        if (line.getDescription() == null) {
            line.setDescription(item.getDescription(language));
        }

        if (line.getQuantity() == null) {
            line.setQuantity(new BigDecimal(1));
        }

        if (line.getPrice() == null) {
           
                Date pricingDate = order != null ? order.getPricingDate() : null;
                // set order company id so that price of that company is get 
                item.setPriceModelCompanyId(order.getUser().getCompany().getId());
                BigDecimal  price = itemBl.getPriceByCurrency(pricingDate, item, userId, currencyId); // basic price, ignoring current usage and
                // and quantity purchased for price calculations
                line.setPrice(price);
        }

        if (line.getAmount() == null) {
            BigDecimal additionAmount = item.getPrice();   // percentage ignores the quantity
                // normal price, multiply by quantity
                additionAmount = line.getPrice().setScale(precision, CommonConstants.BIGDECIMAL_ROUND);
                additionAmount = additionAmount.multiply(line.getQuantity());
            line.setAmount(additionAmount.setScale(precision, CommonConstants.BIGDECIMAL_ROUND));
        }

        line.setCreateDatetime(null);
        line.setDeleted(0);
        line.setTypeId(type);
        line.setEditable(editable);
        line.setItem(item);
    }
}

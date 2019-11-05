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

package com.sapienter.jbilling.server.order.validator;

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemDependencyDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import java.util.*;

/**
 * @author Alexander Aksenov
 * @since 18.06.13
 */
public class OrderHierarchyValidator {

    public final static String ERR_HIERARCHY_TOO_BIG = "OrderWS,hierarchy,error.order.hierarchy.too.big";
    public final static int MAX_ORDERS_COUNT = 100;
    public final static int MAX_LEVELS = 3;
    public final static String ERR_CYCLES_IN_HIERARCHY = "OrderWS,hierarchy,error.order.hierarchy.contains.cycles";
    public final static String ERR_INCORRECT_ORDERS_PARENT_CHILD_LINK = "OrderWS,hierarchy,error.order.hierarchy.incorrect.order.parent.child.link";
    public final static String ERR_INCORRECT_LINES_PARENT_CHILD_LINK = "OrderWS,hierarchy,error.order.hierarchy.incorrect.line.parent.child.link";
    public final static String ERR_INCORRECT_PARENT_CHILD_RELATIONSHIP = "OrderWS,hierarchy,error.order.hierarchy.incorrect.parent.child.relationship";
    public final static String ERR_INCORRECT_ACTIVE_SINCE = "OrderWS,hierarchy,error.order.hierarchy.incorrect.active.since";
    public final static String ERR_INCORRECT_ACTIVE_UNTIL = "OrderWS,hierarchy,error.order.hierarchy.incorrect.active.until";
    public final static String ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET = "OrderWS,hierarchy,error.order.hierarchy.product.mandatory.dependency.not.meet";
    public final static String ERR_NON_LEAF_ORDER_DELETE = "OrderWS,hierarchy,error.order.hierarchy.non.leaf.order.delete";
    public final static String ERR_UNLINKED_HIERARCHY = "OrderWS,hierarchy,error.order.hierarchy.unlinked";
    public final static String PRODUCT_DEPENDENCY_EXIST = "Product dependency Exist";

    private Map<Key<OrderDTO>, OrderHierarchyDto> ordersMap = new HashMap<Key<OrderDTO>, OrderHierarchyDto>();
    private Map<Key<OrderLineDTO>, OrderLineHierarchyDto> orderLinesMap = new HashMap<Key<OrderLineDTO>, OrderLineHierarchyDto>();

    class OrderHierarchyDto {
        protected Integer orderId;
        protected OrderHierarchyDto parentOrder;
        protected Set<OrderHierarchyDto> childOrders = new HashSet<OrderHierarchyDto>();
        protected Date activeSince;
        protected Date activeUntil;
        protected Set<OrderLineHierarchyDto> orderLines = new HashSet<OrderLineHierarchyDto>();

        protected int visited = -1;
        protected boolean updated;
    }

    class OrderLineHierarchyDto {
        protected OrderHierarchyDto order;
        protected Integer orderLineId;
        protected OrderLineHierarchyDto parentOrderLine;
        protected Set<OrderLineHierarchyDto> childOrderLines = new HashSet<OrderLineHierarchyDto>();
        protected Integer productId;
        protected Integer quantity;

        //list contains 2 integers, the minimum and maximum quantities
        protected Map<Integer, List<Integer>> dependentProducts = new HashMap<Integer, List<Integer>>();

        protected int visited = -1;
    }

    static class Key<T> {
        protected int id;

        Key(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            if (id != key.id) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public void buildHierarchy(OrderDTO order) {
        addOrderToHierarchy(order);
    }

    public String validate() {
        resetVisited();
        String error = validateCycles();
        if (error != null) return error;
        error = validateHierarchySize();
        if (error != null) return error;
        error = validateParentChildLinks();
        if (error != null) return error;
        error = validateIsAllEntitiesLinked();
        if (error != null) return error;
        error = validateActiveSince();
        if (error != null) return error;
        error = validateActiveUntil();
        if (error != null) return error;
        error = validateProductDependencies();
        return error;
    }

    public String deleteOrder(Integer id) {
        if (id == null) return null;
        OrderHierarchyDto targetOrder = findOrder(id);
        if (targetOrder != null) {
            if (targetOrder.childOrders.size() > 0) {
                int dependencyCount= dependencyCount(targetOrder.orderId);
                if(dependencyCount == 0) {
                    return ERR_NON_LEAF_ORDER_DELETE;
                } else {
                    return PRODUCT_DEPENDENCY_EXIST;
                }
            }
            for (OrderLineHierarchyDto lineDto : targetOrder.orderLines) {
                if (lineDto.parentOrderLine != null) {
                    lineDto.parentOrderLine.childOrderLines.remove(lineDto);
                }
                for (OrderLineHierarchyDto childLineDto : lineDto.childOrderLines) {
                    childLineDto.parentOrderLine = null;
                }
                for (Key<OrderLineDTO> key : new HashSet<Key<OrderLineDTO>>(orderLinesMap.keySet())) {
                    if (orderLinesMap.get(key).equals(lineDto)) {
                        orderLinesMap.remove(key);
                        break;
                    }
                }
            }
            if (targetOrder.parentOrder != null) {
                targetOrder.parentOrder.childOrders.remove(targetOrder);
            }
            for (Key<OrderDTO> key : new HashSet<Key<OrderDTO>>(ordersMap.keySet())) {
                if (ordersMap.get(key).equals(targetOrder)) {
                    ordersMap.remove(key);
                    break;
                }
            }
        }
        return null;
    }

    public int dependencyCount(Integer orderId) {
        OrderHierarchyDto targetOrder = findOrder(orderId);
        int dependencyCount = 0;
        if (targetOrder.parentOrder == null) {
            for (OrderLineHierarchyDto orderLineHierarchyDto : targetOrder.orderLines) {
                dependencyCount += orderLineHierarchyDto.dependentProducts.keySet().size();
            }
            for (OrderHierarchyDto childOrder : targetOrder.childOrders) {
                dependencyCount += dependencyCount(childOrder.orderId);
            }
        }
        return dependencyCount;
    }

    public void updateOrdersInfo(Collection<OrderDTO> updatedOrders) {
        // prepare with new objects
        for (OrderDTO updatedOrder : updatedOrders) {
            OrderHierarchyDto orderHierarchyDto = findOrder(updatedOrder);
            if (orderHierarchyDto == null) {
                addOrderToHierarchy(updatedOrder);
                orderHierarchyDto = findOrder(updatedOrder);
            }
            orderHierarchyDto.updated = true;
            if (updatedOrder.getLines() != null) {
                for (OrderLineDTO line : updatedOrder.getLines()) {
                    if (findLine(line) == null) {
                        addLineToHierarchy(line);
                    }
                }
            }
        }
        for (OrderDTO updatedOrder : updatedOrders) {
            updateOrderInfo(updatedOrder);
        }
    }

    private void updateOrderInfo(OrderDTO updatedOrder) {
        OrderHierarchyDto orderHierarchyDto = findOrder(updatedOrder);
        if (orderHierarchyDto != null) {
            orderHierarchyDto.activeSince = updatedOrder.getActiveSince();
            orderHierarchyDto.activeUntil = updatedOrder.getActiveUntil();
            updateParentOrderInfo(orderHierarchyDto, updatedOrder);

            List<OrderLineDTO> newLines = new LinkedList<OrderLineDTO>(updatedOrder.getLines());
            // update existed lines, remove not needed lines in old hierarchy
            for (Iterator<OrderLineHierarchyDto> iter = orderHierarchyDto.orderLines.iterator(); iter.hasNext(); ) {
                OrderLineHierarchyDto line = iter.next();
                boolean found = false;
                for (Iterator<OrderLineDTO> newLinesIterator = newLines.iterator(); newLinesIterator.hasNext(); ) {
                    OrderLineDTO newLine = newLinesIterator.next();
                    if (newLine.getDeleted() > 0) {
                        OrderLineHierarchyDto targetLine = findLine(newLine);
                        if (targetLine != null) {
                            for (OrderLineHierarchyDto childLine : targetLine.childOrderLines) {
                                if (childLine.parentOrderLine.equals(targetLine)) {
                                    childLine.parentOrderLine = null;
                                }
                            }
                            if (targetLine.parentOrderLine != null) {
                                targetLine.parentOrderLine.childOrderLines.remove(targetLine);
                            }
                            orderLinesMap.remove(key(newLine));
                        }
                    } else if (findLine(newLine).equals(line)) {
                        // update line
                        line.productId = newLine.getItemId();
                        line.quantity = newLine.getQuantityInt();
                        line.dependentProducts.clear();
                        if (newLine.getItem() != null) {
                            for (ItemDependencyDTO dependency : newLine.getItem().getDependencies()) {
                                if(dependency.getMinimum() > 0) {
                                    line.dependentProducts.put(dependency.getDependentObjectId(), Arrays.asList(dependency.getMinimum(), dependency.getMaximum() != null ? dependency.getMaximum() : new Integer(-1)));
                                }
                            }
                        }
                        updateParentOrderLineInfo(line, newLine);
                        newLinesIterator.remove();
                        found = true;
                        break;
                    }
                }
                if (!found) { // remove line from old hierarchy, new one does not exists
                    if (line.parentOrderLine != null) {
                        line.parentOrderLine.childOrderLines.remove(line);
                    }
                    iter.remove();
                }
            }
            // add new lines to order
            for (OrderLineDTO newLine : newLines) {
                orderHierarchyDto.orderLines.add(findLine(newLine));
            }
        }
    }

    private void updateParentOrderLineInfo(OrderLineHierarchyDto line, OrderLineDTO newLine) {
        OrderLineHierarchyDto newParent = newLine.getParentLine() != null ? findLine(newLine.getParentLine()) : null;
        if (newParent != null) { // newParent can be removed from order hierarchy
            if (line.parentOrderLine != null) {
                if (!newParent.equals(line.parentOrderLine)) {
                    line.parentOrderLine.childOrderLines.remove(line);
                    line.parentOrderLine = newParent;
                    newParent.childOrderLines.add(line);
                }
            } else {
                line.parentOrderLine = newParent;
                newParent.childOrderLines.add(line);
            }
        } else if (line.parentOrderLine != null) {
            // reset link to parent only if previous parent orde was presented in hierarchy for update
            if (line.parentOrderLine.order.updated) {
                line.parentOrderLine.childOrderLines.remove(line);
                line.parentOrderLine = null;
            }
        }
    }

    private void updateParentOrderInfo(OrderHierarchyDto orderHierarchyDto, OrderDTO updatedOrder) {
        if (updatedOrder.getParentOrder() != null) {
            OrderHierarchyDto newParent = findOrder(updatedOrder.getParentOrder());
            if (orderHierarchyDto.parentOrder != null) {
                if (!newParent.equals(orderHierarchyDto.parentOrder)) {
                    orderHierarchyDto.parentOrder.childOrders.remove(orderHierarchyDto);
                    orderHierarchyDto.parentOrder = newParent;
                    newParent.childOrders.add(orderHierarchyDto);
                }
            } else {
                orderHierarchyDto.parentOrder = newParent;
                newParent.childOrders.add(orderHierarchyDto);
            }
        } else if (orderHierarchyDto.parentOrder != null) {
            // reset link to parent only if previous parent was presented in hierarchy for update
            if (orderHierarchyDto.parentOrder.updated) {
                orderHierarchyDto.parentOrder.childOrders.remove(orderHierarchyDto);
                orderHierarchyDto.parentOrder = null;
            }
        }
    }

    private String validateProductDependencies() {
        for (OrderLineHierarchyDto lineHierarchyDto : allLines()) {
            for (Integer mandatoryProductId : lineHierarchyDto.dependentProducts.keySet()) {
                Integer min = lineHierarchyDto.dependentProducts.get(mandatoryProductId).get(0);
                Integer max = lineHierarchyDto.dependentProducts.get(mandatoryProductId).get(1);

                int qtyFound = 0;
                for (OrderLineHierarchyDto childLine : lineHierarchyDto.childOrderLines) {
                    IWebServicesSessionBean api = Context.getBean("webServicesSession");
                    ItemTypeDTO category = new ItemTypeDAS().getById(mandatoryProductId,api.getCallerCompanyId(),null);
                    if(null!=category && category.getId()>0 && null!=category.getItems() && !category.getItems().isEmpty()) {
                        for(ItemDTO itemdto : category.getEntity().getItems()) {
                            if (itemdto.getId()==childLine.productId) {
                                qtyFound += childLine.quantity;
                                break;
                            }
                        }
                    }
                    else {
                        if (mandatoryProductId.equals(childLine.productId)) {
                            qtyFound += childLine.quantity;
                            break;
                        }
                    }
                }
                if (qtyFound < min || (max > 0 && qtyFound > max)) {
                    return ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET;
                }
            }
        }
        return null;
    }

    private String validateActiveSince() {
        for (OrderHierarchyDto order : allOrders()) {
            if (order.parentOrder != null) {
                if (order.activeSince != null && order.parentOrder.activeSince != null &&
                        order.activeSince.before(order.parentOrder.activeSince)) {
                    return ERR_INCORRECT_ACTIVE_SINCE;
                }
            }
        }
        return null;
    }

    private String validateActiveUntil() {
        for (OrderHierarchyDto order : allOrders()) {
            if (order.parentOrder != null) {
                if (order.activeUntil != null && order.parentOrder.activeUntil != null &&
                        order.activeUntil.after(order.parentOrder.activeUntil)) {
                    return ERR_INCORRECT_ACTIVE_UNTIL;
                }
            }
        }
        return null;
    }

    private String validateParentChildLinks() {
        //validate parent-child filling for orders
        for (OrderHierarchyDto order : allOrders()) {
            if (order.parentOrder != null && !order.parentOrder.childOrders.contains(order)) {
                return ERR_INCORRECT_ORDERS_PARENT_CHILD_LINK;
            }
        }
        //validate parent-child filling for lines
        for (OrderLineHierarchyDto orderLine : allLines()) {
            if (orderLine.parentOrderLine != null && !orderLine.parentOrderLine.childOrderLines.contains(orderLine)) {
                return ERR_INCORRECT_LINES_PARENT_CHILD_LINK;
            }
        }
        return null;
    }

    private String validateIsAllEntitiesLinked() {
        int step = -10;
        if (!allOrders().isEmpty()) {
            visitOrder(allOrders().iterator().next(), step);
        }
        for (OrderHierarchyDto orderHierarchyDto : allOrders()) {
            if (orderHierarchyDto.visited != step) {
                return ERR_UNLINKED_HIERARCHY;
            }
        }
        return null;
    }

    private void visitOrder(OrderHierarchyDto order, int step) {
        if (order.visited != step) {
            order.visited = step;
            if (order.parentOrder != null) {
                visitOrder(order.parentOrder, step);
            }
            for (OrderHierarchyDto childOrder : order.childOrders) {
                visitOrder(childOrder, step);
            }
        }
    }

    private String validateCycles() {
        // put step to visited field and check it later, is this node already visited at current step
        // using step number instead of flag (true/false) prevents flag reset in all collection
        int step = 1;
        for (OrderHierarchyDto orderDto : allOrders()) {
            OrderHierarchyDto parentDto = orderDto;
            parentDto.visited = step;
            while ((parentDto = parentDto.parentOrder) != null) {
                if (parentDto.visited == step) return ERR_CYCLES_IN_HIERARCHY;
                parentDto.visited = step;
            }
            step++;
        }
        step = 1;
        for (OrderLineHierarchyDto orderLineDto : allLines()) {
            OrderLineHierarchyDto parentDto = orderLineDto;
            parentDto.visited = step;
            while ((parentDto = parentDto.parentOrderLine) != null) {
                if (parentDto.visited == step) return ERR_CYCLES_IN_HIERARCHY;
                parentDto.visited = step;
            }
            step++;
        }
        // validate, that parent line - in parent or same order, not in child order
        for (OrderLineHierarchyDto orderLineDto : allLines()) {
            if (orderLineDto.parentOrderLine != null) {
                // check in current order first
                boolean found = orderLineDto.order.orderLines.contains(orderLineDto.parentOrderLine);
                OrderHierarchyDto parentOder = orderLineDto.order;
                while (!found && parentOder != null) {
                    if (parentOder.orderLines.contains(orderLineDto.parentOrderLine)) {
                        found = true;
                    }
                    parentOder = parentOder.parentOrder;
                }
                if (!found) return ERR_INCORRECT_PARENT_CHILD_RELATIONSHIP;
            }
        }

        return null;
    }

    private String validateHierarchySize() {
        if (allOrders().size() > MAX_ORDERS_COUNT) {
            return ERR_HIERARCHY_TOO_BIG;
        }
        for (OrderHierarchyDto orderDto : allOrders()) {
            int deep = 1;
            OrderHierarchyDto parentDto = orderDto;
            while ((parentDto = parentDto.parentOrder) != null) {
                deep++;
                if (deep > MAX_LEVELS) {
                    return ERR_HIERARCHY_TOO_BIG;
                }
            }
        }
        return null;
    }

    private void resetVisited() {
        for (OrderHierarchyDto dto : allOrders()) {
            dto.visited = -1;
        }
    }


    private OrderHierarchyDto addOrderToHierarchy(OrderDTO order) {
        if (order == null || order.getDeleted() > 0) return null;
        OrderHierarchyDto result = ordersMap.get(key(order));
        if (result != null) return result;
        result = new OrderHierarchyDto();
        // put to map for references from childs
        ordersMap.put(key(order), result);
        result.orderId = order.getId();
        result.activeSince = order.getActiveSince();
        result.activeUntil = order.getActiveUntil();
        result.parentOrder = addOrderToHierarchy(order.getParentOrder());
        if (result.parentOrder != null) {
            result.parentOrder.childOrders.add(result); // for updated orders
        }
        if (order.getChildOrders() != null) {
            for (OrderDTO childOrder : order.getChildOrders()) {
                OrderHierarchyDto childDTO = addOrderToHierarchy(childOrder);
                if (childDTO != null) { // child order can be deleted
                    result.childOrders.add(childDTO);
                }
            }
        }
        if (order.getLines() != null) {
            for (OrderLineDTO line : order.getLines()) {
                OrderLineHierarchyDto lineDto = addLineToHierarchy(line);
                if (lineDto != null) {
                    result.orderLines.add(lineDto);
                }
            }
        }
        return result;
    }

    private OrderLineHierarchyDto addLineToHierarchy(OrderLineDTO lineDTO) {
        if (lineDTO == null || lineDTO.getDeleted() > 0 || lineDTO.getPurchaseOrder().getDeleted() > 0) return null;
        OrderLineHierarchyDto result = orderLinesMap.get(key(lineDTO));
        if (result != null) return result;
        result = new OrderLineHierarchyDto();
        orderLinesMap.put(key(lineDTO), result);
        result.order = addOrderToHierarchy(lineDTO.getPurchaseOrder());
        result.orderLineId = lineDTO.getId();
        result.productId = lineDTO.getItemId();
        result.quantity = lineDTO.getQuantityInt();

        if (lineDTO.getItem() != null) {
            List<ItemDTO> productList = new ArrayList<ItemDTO>();
            ItemDTO itemDTO = lineDTO.getItem();
                productList.add(itemDTO);
            for (ItemDTO product : productList) {
                for (ItemDependencyDTO dependency : product.getDependencies()) {
                    if (dependency.getMinimum() > 0) {
                        result.dependentProducts.put(dependency.getDependentObjectId(), Arrays.asList(dependency.getMinimum(), dependency.getMaximum() != null ? dependency.getMaximum() : new Integer(-1)));
                    }
                }
            }
        }
        result.parentOrderLine = addLineToHierarchy(lineDTO.getParentLine());
        if (result.parentOrderLine != null) {
            result.parentOrderLine.childOrderLines.add(result);
        }
        if (lineDTO.getChildLines() != null) {
            for (OrderLineDTO childLine : lineDTO.getChildLines()) {
                OrderLineHierarchyDto childDto = addLineToHierarchy(childLine);
                if (childDto != null) {
                    result.childOrderLines.add(childDto);
                }
            }
        }
        return result;
    }

    private OrderHierarchyDto findOrder(Integer id) {
        return ordersMap.get(key(id));
    }

    private OrderHierarchyDto findOrder(OrderDTO order) {
        return ordersMap.get(key(order));
    }

    private OrderLineHierarchyDto findLine(Integer id) {
        return orderLinesMap.get(lineKey(id));
    }

    private OrderLineHierarchyDto findLine(OrderLineDTO line) {
        return orderLinesMap.get(key(line));
    }

    private Collection<OrderHierarchyDto> allOrders() {
        return ordersMap.values();
    }

    private Collection<OrderLineHierarchyDto> allLines() {
        return orderLinesMap.values();
    }

    // change logic if exists orders with same hash, but not equals. Storing order needed for additional equality check
    private static Key<OrderDTO> key(OrderDTO order) {
        return new Key<OrderDTO>(order.getId() != null ? order.getId() : order.hashCode());
    }

    // change logic if exists orderLines with same hash, but not equals. Storing orderLine needed for additional equality check
    private static Key<OrderLineDTO> key(OrderLineDTO orderLine) {
        return new Key<OrderLineDTO>(orderLine.getId() > 0 ? orderLine.getId() : orderLine.hashCode());
    }

    private static Key<OrderDTO> key(int id) {
        return new Key<OrderDTO>(id);
    }

    private static Key<OrderLineDTO> lineKey(int id) {
        return new Key<OrderLineDTO>(id);
    }

}

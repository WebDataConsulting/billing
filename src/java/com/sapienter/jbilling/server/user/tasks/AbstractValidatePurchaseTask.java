package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.UsageBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Abstract validate purchase task that provides convenience methods and helpers to simplify
 * common purchase validation checks.
 *
 * @author Brian Cowdery
 * @since 05-Jul-2012
 */
public abstract class AbstractValidatePurchaseTask extends PluggableTask implements IValidatePurchaseTask {

    private static final FormatLogger LOG = new FormatLogger(AbstractValidatePurchaseTask.class);


    /**
     * Helper that holds the context of this purchase validation across multiple sets
     * of pricing fields being validated.
     *
     * This class should be used to help determine the current usage and plan subscriptions of
     * the user making the purchase.
     */
    protected static class PurchaseContext {
        private Integer userId;
        private BigDecimal totalCost;
        private OrderDTO currentOrder;
        private UsageBL usage;

        // fallback to old usage checks if the user doesn't have a main-subscription
        private List<OrderLineDTO> subscriptions;

        public PurchaseContext(Integer userId, BigDecimal totalCost) {
            this.userId = userId;
            this.totalCost = totalCost;
            this.currentOrder = new OrderBL().getCurrentOrder(userId, new Date());
            this.usage = new UsageBL(userId);
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }

        public OrderDTO getCurrentOrder() {
            return currentOrder;
        }

        public Usage getItemUsage(Integer itemId) {
            return usage.getItemUsage(itemId);
        }

        public Usage getItemTypeUsage(Integer itemTypeId) {
            return usage.getItemTypeUsage(itemTypeId);
        }
        
        public boolean isSubscribed(Integer lemonadeMonthlyPassItemId) {
			return new ItemDAS().isSubscribedByItem(userId, lemonadeMonthlyPassItemId);
		}
    }


    public ValidatePurchaseWS validate(CustomerDTO customer, List<ItemDTO> items, List<BigDecimal> prices,
                                       ValidatePurchaseWS result, List<List<PricingField>> fields) throws TaskException {

        LOG.debug("Validating purchase ...");

        // build the purchase context for checking usage & subscription statuses
        BigDecimal totalPurchaseCost = BigDecimal.ZERO;
        for (BigDecimal amount : prices) {
            totalPurchaseCost = totalPurchaseCost.add(amount);
        }

        PurchaseContext context = new PurchaseContext(customer.getBaseUser().getId(), totalPurchaseCost);

        // run purchase validations
        if (fields == null) {
            // no pricing fields to validate, run checks without
            validate(customer, items, null, context, result);
        } else {
            // validate each set of pricing fields
            for (List<PricingField> batch : fields) {
                validate(customer, items, batch, context, result);
            }
        }

        // fail the purchase if available quantity is less than, or equal to zero
        if (result.getQuantityAsDecimal().compareTo(BigDecimal.ZERO) <= 0) {
            LOG.debug("Available quantity %s is less than zero.", result.getQuantityAsDecimal());
            result.setAuthorized(false);
            result.setQuantity(BigDecimal.ZERO);
            result.addMessage("Insufficient quantity available for purchase.");
            return result;
        }

        return result;
    }



    /*
        Abstract methods to be implemented to do the actual validation work.
     */

    abstract void validate(CustomerDTO customer, List<ItemDTO> items, List<PricingField> fields, PurchaseContext context, ValidatePurchaseWS result);



    /**
     * Convenience method to find an item by ID.
     *
     * @param items items
     * @param itemId item id
     * @return found item or null if no item found.
     */
    public static ItemDTO findItem(List<ItemDTO> items, Integer itemId) {
        if (items != null) {
            for (ItemDTO item : items) {
                if (item.getId() == itemId)
                    return item;
            }
        }
        return null;
    }

    /**
     * Convenience method to find an item by internal number.
     *
     * @param items items
     * @param internalNumber name
     * @return found item or null if no item found.
     */
    public static ItemDTO findItem(List<ItemDTO> items, String internalNumber) {
        if (items != null) {
            for (ItemDTO item : items) {
                if (item.getInternalNumber().equals(internalNumber))
                    return item;
            }
        }
        return null;
    }

    /**
     * Convenience method to find a pricing field by name.
     *
     * @param fields pricing fields
     * @param fieldName name
     * @return found pricing field or null if no field found.
     */
    public static PricingField find(List<PricingField> fields, String fieldName) {
        if (fields != null) {
            for (PricingField field : fields) {
                if (field.getName().equals(fieldName))
                    return field;
            }
        }
        return null;
    }

    /**
     * Convenience method to find a specific order line by item ID.
     *
     * @param order order to search
     * @param itemId item id
     * @return order line
     */
    public static OrderLineDTO find(OrderDTO order, Integer itemId) {
        for (OrderLineDTO line : order.getLines()) {
            if (line.getItemId().equals(itemId)) {
                return line;
            }
        }
        return null;
    }
}

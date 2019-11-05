package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Example validate purchase plug-in used by jbilling tests.
 *
 * @author Brian Cowdery
 * @since 05-Jul-2012
 */
public class ExampleValidatePurchaseTask extends AbstractValidatePurchaseTask {

    private static final FormatLogger LOG = new FormatLogger(ExampleValidatePurchaseTask.class);

    private static final Integer LEMONADE_MONTHLY_PASS_ITEM_ID = 2;

    private static final Integer LEMONADE_ITEM_ID = 1;
    private static final BigDecimal MAX_LEMONADE = new BigDecimal("3.00");

    private static final Integer COFFEE_ITEM_ID = 3;
    private static final BigDecimal MAX_COFFEE = new BigDecimal("20.00");

    @Override
    void validate(CustomerDTO customer, List<ItemDTO> items, List<PricingField> fields, PurchaseContext context, ValidatePurchaseWS result) {

        // max 3 lemonades for plan subscribers
        ItemDTO lemonade = findItem(items, LEMONADE_ITEM_ID);
        if (lemonade != null && context.isSubscribed(LEMONADE_MONTHLY_PASS_ITEM_ID)) {
            BigDecimal existing = context.getItemUsage(LEMONADE_ITEM_ID).getQuantity();
            LOG.debug("Validating lemonade purchase, existing quantity %s, 3 max", existing);

            if (existing.compareTo(MAX_LEMONADE) >= 0) {
                result.setQuantity(BigDecimal.ZERO);
                result.addMessage("No more than 3 lemonades are allowed.");
            } else {
                result.setQuantity(MAX_LEMONADE.subtract(existing));
            }
        }


        // current order may only contain 20 coffees
        ItemDTO coffee = findItem(items, COFFEE_ITEM_ID);
        if (coffee != null) {
            BigDecimal existing = context.getItemUsage(COFFEE_ITEM_ID).getQuantity();
            LOG.debug("Validating coffee purchase, existing quantity %s, 20 max", existing);

            if (existing.compareTo(MAX_COFFEE) >= 0) {
                result.setQuantity(BigDecimal.ZERO);
                result.addMessage("No more than 20 coffees are allowed.");
            } else {
                result.setQuantity(MAX_COFFEE.subtract(existing));
            }
        }


        // throw an exception if we've been told to fail
        PricingField fail = find(fields, "fail");
        if (fail != null) throw new RuntimeException("Thrown exception for testing");
    }
}

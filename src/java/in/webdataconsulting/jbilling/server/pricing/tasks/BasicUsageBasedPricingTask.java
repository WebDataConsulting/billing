package in.webdataconsulting.jbilling.server.pricing.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.UsageBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pricing.PricingContext;
import com.sapienter.jbilling.server.pricing.tasks.IPricing;

import java.math.BigDecimal;

/**
 * @author Vikas Bodani
 * @since 23-Dec-2015.
 */
public class BasicUsageBasedPricingTask extends PluggableTask implements IPricing {

    private static final FormatLogger LOG = new FormatLogger(BasicUsageBasedPricingTask.class);

    /**
     * Get the price for the given item, user, and quantity being purchased. Pricing fields can be
     * provided to define specific pricing scenarios to be handled by the implementing class.
     *
     * @param pricingContext
     * @param defaultPrice
     * @param pricingOrder
     * @param orderLine
     * @param singlePurchase
     * @return
     * @throws com.sapienter.jbilling.server.pluggableTask.TaskException
     */
    @Override
    public BigDecimal getPrice(PricingContext pricingContext, BigDecimal defaultPrice, OrderDTO pricingOrder, OrderLineDTO orderLine, boolean singlePurchase) throws TaskException {

        UsageBL usageService = new UsageBL(pricingContext.getUser().getId(), pricingOrder);

        LOG.debug("Usage for Item %s is %s", pricingContext.getItem(), usageService.getItemUsage(pricingContext.getItem().getId()));

        LOG.debug("Returning default price %s", defaultPrice);

        return defaultPrice;
    }
}

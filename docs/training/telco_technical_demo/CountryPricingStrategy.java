/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.SortedMap;

import static com.sapienter.jbilling.server.pricing.db.AttributeDefinition.Type.STRING;

/**
 * Country pricing strategy.
 * <p/>
 * This pricing strategy will calculate the price depending on the country.
 * So in the GUI the user will be able to set a percentage in which the item price will be incremented on.
 *
 * @author Juan Vidal
 * @since 07-04-2012
 */
public class CountryPricingStrategy extends AbstractPricingStrategy {

    private static final Logger LOG = Logger.getLogger(CountryPricingStrategy.class);
    private static final String DEFAULT_PERCENTAGE = "default";

    public CountryPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(DEFAULT_PERCENTAGE, STRING, true)  // percentage to other countries that are not set in the price model configuration
        );

        setChainPositions(
                ChainPosition.START,
                ChainPosition.MIDDLE,
                ChainPosition.END
        );

        setRequiresUsage(false);
    }

    /**
     * Sets the price depending on the Country Code of the customer.
     * The user has to use the attributes fields with the key/value pair to set the percentages for each country code.
     * To use it the key field has to contain the Country Code and the value field should have a decimal value for the
     * percentage
     * <p/>
     * <p/>
     * <code>
     * So the user could set US = 10 or Canada = 15 and that would mean an increment of 10% for US customers and
     * 15% for Canadian customers. There's an attribute to set a default value for the case in which the customer has
     * no country set or the user's country is not configured in the price model.
     * </code>
     * <p/>
     * <code>
     * price = ((percent * rate) / 100) + rate
     * </code>
     *
     * @param pricingOrder target order for this pricing request (not used by this strategy)
     * @param result       pricing result to apply pricing to
     * @param fields       pricing fields (not used by this strategy)
     * @param planPrice    the plan price to apply
     * @param quantity     quantity of item being priced
     * @param usage        total item usage for this billing period (not used by this strategy)
     */
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields,
                        PriceModelDTO planPrice, BigDecimal quantity, Usage usage, boolean singlePurchase) {

        UserBL user = new UserBL(result.getUserId());
        ContactDTO contact = null;

        if (user != null) {
            contact = user.getDto().getContact();
        }

        // Get the percentage to apply depending on the country of the user.
        BigDecimal percentage = getCountryPercentage(contact, planPrice.getAttributes());

        LOG.debug("Percentage is: " + percentage + " because country is: " + (contact != null ? contact.getCountryCode() : "N/A"));

        // Set the final price
        if (percentage == null) {
            result.setPrice(planPrice.getRate());
        } else {
            result.setPrice(percentage.multiply(planPrice.getRate()).divide(new BigDecimal(100)).add(planPrice.getRate()));
        }

        LOG.debug("The new price was set to " + result.getPrice());
    }

    /**
     * Get the percentage value for the specified country code. If it's not in the attributes the the default value is set.
     *
     * @param contactDTO Contact information to get the country code from.
     * @param attributes Map of attributes configured in the price model.
     * @return Percentage to apply to the price.
     */
    private BigDecimal getCountryPercentage(ContactDTO contactDTO, SortedMap<String, String> attributes) {
        BigDecimal percentage;
        String countryCode = "";

        // Get the country code from the contact. If it's null then set it to Other to get the default value.
        if (contactDTO != null && contactDTO.getCountryCode() != null && attributes.containsKey(contactDTO.getCountryCode())) {
            countryCode = contactDTO.getCountryCode();
            percentage = new BigDecimal(attributes.get(countryCode));
        } else {
            percentage = new BigDecimal(attributes.get(DEFAULT_PERCENTAGE));
        }

        return percentage;
    }
}

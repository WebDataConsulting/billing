package com.sapienter.jbilling.server.discount.strategy;

import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type;

public class PeriodBasedDiscountStrategy extends AbstractDiscountStrategy {

    public PeriodBasedDiscountStrategy() {

        setAttributeDefinitions(
                new AttributeDefinition("periodUnit", Type.INTEGER, true),
                new AttributeDefinition("periodValue", Type.INTEGER, true),
                new AttributeDefinition("isPercentage", Type.INTEGER, true)
        );
    }

}

package com.sapienter.jbilling.server.discount.strategy;

import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;

import java.util.List;

public interface DiscountStrategy {

    public List<AttributeDefinition> getAttributeDefinitions();

}

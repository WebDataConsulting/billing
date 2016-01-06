package com.sapienter.jbilling.server.discount.strategy;

import java.util.ArrayList;
import java.util.List;


public enum DiscountStrategyType {

	/*AMOUNT		(new AmountBasedDiscountStrategy(), "discount.strategyType.amount"),
	
	PERCENTAGE	(new PercentageBasedDiscountStrategy(), "discount.strategyType.percentage"),
	
	PERIODBASED	(new PeriodBasedDiscountStrategy(), "discount.strategyType.periodBased");*/

    ONE_TIME_AMOUNT(new AmountBasedDiscountStrategy(), "discount.strategyType.amount"),

    ONE_TIME_PERCENTAGE(new PercentageBasedDiscountStrategy(), "discount.strategyType.percentage"),

    RECURRING_PERIODBASED(new PeriodBasedDiscountStrategy(), "discount.strategyType.periodBased");


    private final DiscountStrategy strategy;
    private final String messageKey;

    DiscountStrategyType(DiscountStrategy strategy, String messageKey) {
        this.strategy = strategy;
        this.messageKey = messageKey;
    }

    public static DiscountStrategyType getByName(String name) {
        DiscountStrategyType match = null;
        for (DiscountStrategyType type : DiscountStrategyType.values()) {
            if (type.name().equals(name)) {
                match = type;
                break;
            }
        }
        return match;
    }

    /**
     * Method for dropdown of enum values
     *
     * @return
     */
    public static List<String> getStrategyTypes() {
        List<String> strategyTypes = new ArrayList<String>();
        for (DiscountStrategyType type : DiscountStrategyType.values()) {
            strategyTypes.add(type.name());
        }
        return strategyTypes;
    }

    public DiscountStrategy getStrategy() {
        return strategy;
    }

    public String getMessageKey() {
        return messageKey;
    }

}

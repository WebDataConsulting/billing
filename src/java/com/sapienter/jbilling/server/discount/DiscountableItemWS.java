package com.sapienter.jbilling.server.discount;

import com.sapienter.jbilling.server.util.ServerConstants;

import java.io.Serializable;

/**
 * This class is used for making the order UI dropdown
 * for discountable items on an order.
 *
 * @author Amol Gadre
 *         Date 01-12-2012
 */
public class DiscountableItemWS implements Serializable {

    private Integer id;
    private String discountLevel;
    private String description;
    private String orderLineAmount;        // Used in case of product level discount.

    private String lineLevelDetails;

    public DiscountableItemWS() {

    }

    /**
     * This constructor will be invoked while setting the dropdown option
     * for plan item or order level discount. It does not set the order line amount.
     *
     * @param id
     * @param discountLevel
     * @param description
     */
    public DiscountableItemWS(Integer id, String discountLevel, String description) {
        this.id = id;
        this.discountLevel = discountLevel;
        this.description = description;

        // the format will be something like this: 200|planItem||Sail Prod Go 2
        // note orderLineAmount is not set so 2 pipe chars are joined.
        this.lineLevelDetails = id + ServerConstants.PIPE +
                discountLevel + ServerConstants.PIPE +
                ServerConstants.PIPE +
                description;
    }

    /**
     * This constructor to be used for product level discount line. For plan item,
     * no line amount is available. So this is used only for product level.
     *
     * @param id
     * @param discountLevel
     * @param description
     * @param orderLineAmount
     */
    public DiscountableItemWS(Integer id, String discountLevel, String description, String orderLineAmount) {
        this.id = id;
        this.discountLevel = discountLevel;
        this.description = description;
        this.orderLineAmount = orderLineAmount;

        // the format will be something like this: 200|item|25.50|Sail Prod Go 2
        // this string will be split on pipe character to retrive details
        this.lineLevelDetails = id + ServerConstants.PIPE +
                discountLevel + ServerConstants.PIPE +
                orderLineAmount + ServerConstants.PIPE +
                description;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDiscountLevel() {
        return discountLevel;
    }

    public void setDiscountLevel(String discountLevel) {
        this.discountLevel = discountLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLineLevelDetails() {
        return lineLevelDetails;
    }

    public void setLineLevelDetails(String lineLevelDetails) {
        this.lineLevelDetails = lineLevelDetails;
    }

    public String getOrderLineAmount() {
        return orderLineAmount;
    }

    public void setOrderLineAmount(String orderLineAmount) {
        this.orderLineAmount = orderLineAmount;
    }

}

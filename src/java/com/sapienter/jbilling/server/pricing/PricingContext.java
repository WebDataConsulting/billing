package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Vikas Bodani
 * @since 17/12/15.
 */
public final class PricingContext {

    final UserDTO user;
    final CurrencyDTO currency;
    final Date eventDate;
    final ItemDTO item;
    final BigDecimal quantity;
    final List<PricingField> pricingFields;

    public PricingContext(UserDTO user, CurrencyDTO currency, Date eventDate, ItemDTO item, BigDecimal quantity, List<PricingField> pricingFields) {
        this.user = user;
        this.currency = currency;
        this.eventDate = eventDate;
        this.item = item;
        this.quantity = quantity;
        this.pricingFields = pricingFields;
    }

    public UserDTO getUser() {
        return user;
    }

    public CurrencyDTO getCurrency() {
        return currency;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public ItemDTO getItem() {
        return item;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public List<PricingField> getPricingFields() {
        return pricingFields;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PricingContext{");
        sb.append("user=").append(user);
        sb.append(", currency=").append(currency);
        sb.append(", eventDate=").append(eventDate);
        sb.append(", item=").append(item);
        sb.append(", quantity=").append(quantity);
        sb.append(", pricingFields=").append(pricingFields);
        sb.append('}');
        return sb.toString();
    }
}

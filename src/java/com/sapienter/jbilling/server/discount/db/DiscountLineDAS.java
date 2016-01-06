package com.sapienter.jbilling.server.discount.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class DiscountLineDAS extends AbstractDAS<DiscountLineDTO> {

    @SuppressWarnings("unchecked")
    public List<DiscountLineDTO> findByDiscountId(Integer discountId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("discount.id", discountId));

        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<DiscountLineDTO> findByDiscountOrderLineId(Integer discountOrderLineId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("discountOrderLine.id", discountOrderLineId));

        return criteria.list();
    }

}

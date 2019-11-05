package com.sapienter.jbilling;

import com.sapienter.jbilling.server.user.db.CustomerNoteDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class CustomerNoteDAS extends AbstractDAS<CustomerNoteDTO> {

    public List<CustomerNoteDTO> findByCustomer(Integer customerId, Integer entityId) {
        Criteria criteria = getSession().createCriteria(CustomerNoteDTO.class)
                .createAlias("company", "e")
                .add(Restrictions.eq("e.id", entityId))
                .createAlias("customer", "c")
                .add(Restrictions.eq("c.id", customerId));
        return criteria.list();
    }
}

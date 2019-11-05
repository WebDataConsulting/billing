package com.sapienter.jbilling.server.payment.db;

import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Query;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class PaymentMethodTemplateDAS extends AbstractDAS<PaymentMethodTemplateDTO>{
	
	public PaymentMethodTemplateDTO findByName(String templateName) {
		// I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(PaymentMethodTemplateDTO.class)
                    .add(Restrictions.eq("templateName", templateName));
        return findFirst(criteria);
	}
	
	@SuppressWarnings("unchecked")
    public PaymentMethodTemplateDTO findFirst(Query query) {
        query.setFirstResult(0).setMaxResults(1);
        return (PaymentMethodTemplateDTO) query.uniqueResult();
    }
}

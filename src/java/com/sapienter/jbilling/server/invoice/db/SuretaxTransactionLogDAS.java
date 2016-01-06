package com.sapienter.jbilling.server.invoice.db;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class SuretaxTransactionLogDAS extends
		AbstractDAS<SuretaxTransactionLogDTO> {
	public SuretaxTransactionLogDTO findByResponseTransId(int respTxnId) {
		Criteria criteria = getSession().createCriteria(getPersistentClass())
				.add(Restrictions.eq("responseTransactionId", respTxnId));

		return (SuretaxTransactionLogDTO) criteria.uniqueResult();
	}
}

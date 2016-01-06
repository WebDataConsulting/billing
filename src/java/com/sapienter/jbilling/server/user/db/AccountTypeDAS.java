package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.io.Serializable;
import java.util.List;

/**
 * Class extending AbstractDAS which include generics methods to save,find and perform other queries
 */
public class AccountTypeDAS extends AbstractDAS<AccountTypeDTO> {

    public AccountTypeDTO find(Serializable id, Serializable companyId) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        crit.add(Restrictions.eq("id", id));
        crit.add(Restrictions.eq("company.id", companyId));
        return (AccountTypeDTO) crit.uniqueResult();
    }

    public List<AccountTypeDTO> findAll(Serializable companyId) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        crit.add(Restrictions.eq("company.id", companyId));
        return crit.list();
    }

}

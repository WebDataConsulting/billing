package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.Date;

public class AccountInformationTypeDAS extends AbstractDAS<AccountInformationTypeDTO> {


    public List<AccountInformationTypeDTO> getAvailableAccountInformationTypes(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
        query.add(Restrictions.eq("entity.id", entityId));

        return (List<AccountInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
    }

    public List<AccountInformationTypeDTO> getInformationTypesForAccountType(Integer accountTypeId) {
        DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
        query.add(Restrictions.eq("accountType.id", accountTypeId));
        query.addOrder(Order.asc("displayOrder"));

        return (List<AccountInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
    }

    public AccountInformationTypeDTO findByName(String name, Integer entityId, Integer accountTypeId) {
        DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
        query.add(Restrictions.eq("entity.id", entityId));
        query.add(Restrictions.eq("name", name));
        query.add(Restrictions.eq("accountType.id", accountTypeId));
        List<AccountInformationTypeDTO> list = (List<AccountInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
        return !list.isEmpty() ? list.get(0) : null;

    }
    
     public List<CustomerAccountInfoTypeMetaField> findByCustomerAndEffectiveDate(Integer customerId, Integer accountInfoTypeId, Date effectiveDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(effectiveDate);
        cal.add(Calendar.DATE, 1);
    	 
    	DetachedCriteria query = DetachedCriteria.forClass(CustomerAccountInfoTypeMetaField.class);
        query.add(Restrictions.eq("customer.id", customerId));
        query.add(Restrictions.eq("accountInfoType.id", accountInfoTypeId));
        query.add(Restrictions.ge("effectiveDate", effectiveDate));
        query.add(Restrictions.lt("effectiveDate", cal.getTime()));
        List<CustomerAccountInfoTypeMetaField> list = (List<CustomerAccountInfoTypeMetaField>)getHibernateTemplate().findByCriteria(query);
        return !list.isEmpty() ? list : null;

    }
}

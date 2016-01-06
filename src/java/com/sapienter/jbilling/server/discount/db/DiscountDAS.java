package com.sapienter.jbilling.server.discount.db;

import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;
import java.util.List;

public class DiscountDAS extends AbstractDAS<DiscountDTO> {

    /**
     * New method to find matching discount by discount code.
     * Currently this method is used in duplicate code validation in create.
     *
     * @param code
     * @return discount
     */
    public List<DiscountDTO> findByCodeAndEntity(String code, Integer entityId) {

        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("code", code).ignoreCase())
                .add(Restrictions.eq("entity.id", entityId));
        return criteria.list();
    }

    /**
     * This method checks if the discount code exists already
     * for the discount id other than the id passed to this method.
     * Its currently used for duplicate code validation in update.
     *
     * @param id
     * @param code
     * @return
     */
    public List<DiscountDTO> exists(Integer id, String code, Integer entityId) {

        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.ne("id", id))
                .add(Restrictions.eq("code", code).ignoreCase())
                .add(Restrictions.eq("entity.id", entityId));
        return criteria.list();
    }

    /**
     * This function is added to check duplicate discount description while adding new discount.
     * Since international_description table does not have entity_id, its required that a join is done with discount table.
     *
     * @param table
     * @param column
     * @param content
     * @param language
     * @param entityId
     * @return
     */
    public Collection<InternationalDescriptionDTO> descriptionExists(String table, String column, String content,
                                                                     Integer language, Integer entityId) {

        JbillingTableDAS jtDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);

        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a, DiscountDTO discount " +
                "WHERE a.id.foreignId = discount.id " +
                "AND discount.entity.id = :entityId " +
                "AND a.id.tableId = :tableId " +
                "AND a.id.psudoColumn = :psudoColumn " +
                "AND a.id.languageId = :languageId " +
                "AND UPPER(a.content) = UPPER(:content)";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("entityId", entityId);
        query.setParameter("tableId", jtDAS.findByName(table).getId());
        query.setParameter("psudoColumn", column);
        query.setParameter("languageId", language);
        query.setParameter("content", content);
        return query.list();
    }

    /**
     * This function is added to check duplicate discount description while updating an existing discount.
     * Its an overloaded variant of the above descriptionExists method.
     * It checks for all descriptions excluding the discount which is being updated.
     *
     * @param table
     * @param column
     * @param content
     * @param language
     * @param entityId
     * @param discountId
     * @return
     */
    public Collection<InternationalDescriptionDTO> descriptionExists(String table, String column,
                                                                     String content, Integer language, Integer entityId, Integer discountId) {

        JbillingTableDAS jtDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);

        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a, DiscountDTO discount " +
                "WHERE a.id.foreignId = discount.id " +
                "AND discount.entity.id = :entityId " +
                "AND discount.id <> :discountId " +
                "AND a.id.tableId = :tableId " +
                "AND a.id.psudoColumn = :psudoColumn " +
                "AND a.id.languageId = :languageId " +
                "AND UPPER(a.content) = UPPER(:content)";

        Query query = getSession().createQuery(QUERY);
        query.setParameter("entityId", entityId);
        query.setParameter("discountId", discountId);
        query.setParameter("tableId", jtDAS.findByName(table).getId());
        query.setParameter("psudoColumn", column);
        query.setParameter("languageId", language);
        query.setParameter("content", content);
        return query.list();
    }
}

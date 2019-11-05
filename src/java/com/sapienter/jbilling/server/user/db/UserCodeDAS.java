package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Gerhard Maree
 * @since 20 Nov 13
 */

public class UserCodeDAS extends AbstractDAS<UserCodeDTO> {

    /**
     * List all user codes fo the user.
     *
     * @param userId
     * @return
     */
    public List<UserCodeDTO> findForUser(int userId) {
        Query query = getSession().getNamedQuery("UserCodeDTO.findForUser");
        query.setInteger("user_id", userId);
        return query.list();
    }

    /**
     * List the activate user codes for a user
     * @param userId
     * @return
     */
    public List<UserCodeDTO> findActiveForUser(int userId) {
        Query query = getSession().getNamedQuery("UserCodeDTO.findActiveForUser");
        query.setInteger("user_id", userId);
        query.setDate("a_date", new Date());
        return query.list();
    }

    /**
     * List the activate user codes for a Partner
     * @param userIds
     * @return
     */
    public List<UserCodeDTO> findActiveForPartner(ArrayList userIds) {
        Query query = getSession().getNamedQuery("UserCodeDTO.findActiveForPartner");
        query.setParameterList("user_ids", userIds);
        query.setDate("a_date", new Date());
        return query.list();
    }

    /**
     * Find the user code for the UserCodeDTO.identifier
     * @param identifier
     * @return
     */
    public UserCodeDTO findForIdentifier(String identifier, Integer companyId) {
        Query query = getSession().getNamedQuery("UserCodeDTO.findForIdentifier");
        query.setString("identifier", identifier);
        query.setInteger("companyId", companyId);
        return (UserCodeDTO)query.uniqueResult();
    }

    /**
     * Find UserCodeDTO.identifiers linked to the object.
     *
     * @param objectType
     * @param objectId
     * @return
     */
    public List<String> findLinkedIdentifiers(UserCodeObjectType objectType, int objectId) {
        Query query = getSession().getNamedQuery("UserCodeDTO.findLinkedIdentifiers");
        query.setInteger("object_id", objectId);
        query.setString("object_type", objectType.name());
        return query.list();
    }
}

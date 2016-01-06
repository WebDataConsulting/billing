package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Query;

import java.util.List;

/**
 * @author Gerhard Maree
 * @since 20 Nov 13
 */

public class UserCodeLinkDAS extends AbstractDAS<UserCodeLinkDTO> {

    /**
     * Find ids of associated objects of type objectType linked to UserCode with identifier userCode.
     * @param userCode
     * @param objectType
     * @return
     */
    public List<Integer> getAssociatedObjectsByUserCodeAndType(String userCode, UserCodeObjectType objectType) {
        Query query = getSession().getNamedQuery("UserCodeLinkDTO.findForUserCodeAndObjectType");
        query.setString("user_code", userCode);
        query.setString("object_type", objectType.name());
        return query.list();
    }

    /**
     * Find ids of associated objects of type objectType linked to the user with the given id.
     * @param userId
     * @param objectType
     * @return
     */
    public List<Integer> getAssociatedObjectsByUserAndType(int userId, UserCodeObjectType objectType) {
        Query query = getSession().getNamedQuery("UserCodeLinkDTO.findForUserIdAndObjectType");
        query.setInteger("user_id", userId);
        query.setString("object_type", objectType.name());
        return query.list();
    }

    /**
     * Return the number of objects linked to the UserCode with the given id.
     *
     * @param userCodeId
     * @return
     */
    public int countLinkedObjects(int userCodeId) {
        Query query = getSession().getNamedQuery("UserCodeLinkDTO.countLinkedObjects");
        query.setInteger("user_code_id", userCodeId);
        return ((Number)query.uniqueResult()).intValue();
    }
}


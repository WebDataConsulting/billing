package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.user.db.UserCodeLinkDTO;

import java.util.Collection;
import java.util.Set;

/**
 * Interface for commmon functionality to link User Codes
 *
 * @author Gerhard
 * @since 28/11/13
 */
public interface UserCodeAssociate<T extends UserCodeLinkDTO> {
    public Set<T> getUserCodeLinks();
    public void addUserCodeLink(T userCodeLink);
}

package com.sapienter.jbilling.server.util.credentials;

import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * Interface for reset user passwords
 *
 * @author  Javier Rivero
 * @since  14/04/15.
 */
public interface PasswordService {
    public void createPassword(UserDTO user);
    /**
     * Method for reset user password for a given user
     *
     * @param user the user
     */
    public void resetPassword(UserDTO user);

}

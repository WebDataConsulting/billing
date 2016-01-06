package com.sapienter.jbilling.server.util.credentials;

import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * @author JavierRivero
 * @since 15/04/15.
 */
public class TestResetPasswordService implements PasswordService {
    private static final String PASSWORD = "Admin123@";

    @Override
    public void createPassword(UserDTO user) {
        UserDAS userDAS = new UserDAS();
        user.setPassword(PASSWORD);
        userDAS.save(user);
    }

    @Override
    public void resetPassword(UserDTO user) {
        UserDAS userDAS = new UserDAS();
        user.setPassword(PASSWORD);
        userDAS.save(user);
    }
}

package com.sapienter.jbilling.server.util.credentials;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDAS;
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.PreferenceBL;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service Class for reset users passwords
 *
 * @author Javier Rivero
 * @since 14/04/15.
 */
public class EmailResetPasswordService implements PasswordService {
    private final static FormatLogger LOG = new FormatLogger(EmailResetPasswordService.class);

    /**
     * Here we will have the initial credentials for a user be created
     *
     * @param user
     */
    @Override
    public void createPassword(UserDTO user) {
        ResetPasswordCodeDAS resetCodeDAS = new ResetPasswordCodeDAS();

        ResetPasswordCodeDTO resetCode = new ResetPasswordCodeDTO();
        resetCode.setUser(user);
        resetCode.setDateCreated(new Date());
        resetCode.setToken(RandomStringUtils.random(32, true, true));
        resetCodeDAS.save(resetCode);

        try {
            new UserBL().sendCredentials(user.getCompany().getId(), user.getId(), 1,
                    generateLink(resetCode.getToken()));
        } catch (SessionInternalError e) {
            LOG.error(e.getMessage(), e);
            throw new SessionInternalError("Exception while sending notification : " + e.getMessage());
        } catch (NotificationNotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new SessionInternalError("createCredentials.notification.not.found");
        }

    }

    /**
     * This method sends an email to the given user with the link to reset his password
     *
     * @param user the user
     */
    @Override
    public void resetPassword(UserDTO user) {
        ResetPasswordCodeDAS resetCodeDAS = new ResetPasswordCodeDAS();
        //find previous passwordCode

        ResetPasswordCodeDTO resetCode = resetCodeDAS.findByUser(user);
        if (resetCode == null) {
            resetCode = new ResetPasswordCodeDTO();
            resetCode.setUser(user);
            resetCode.setDateCreated(new Date());
            resetCode.setToken(RandomStringUtils.random(32, true, true));
            resetCodeDAS.save(resetCode);
            resetCodeDAS.flush();
        } else {
            DateTime dateResetCode = new DateTime(resetCode.getDateCreated());
            DateTime today = DateTime.now();
            Duration duration = new Duration(dateResetCode, today);
            Long minutesDifference = duration.getStandardMinutes();
            Long expirationMinutes = PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(),
                    CommonConstants.PREFERENCE_FORGOT_PASSWORD_EXPIRATION).longValue() * 60;
            if (minutesDifference > expirationMinutes) {
                resetCodeDAS.delete(resetCode);
                resetCodeDAS.flush();
                resetCode = new ResetPasswordCodeDTO();
                resetCode.setUser(user);
                resetCode.setDateCreated(new Date());
                resetCode.setToken(RandomStringUtils.random(32, true, true));
                resetCodeDAS.save(resetCode);
            }
        }

        try {
                new UserBL().sendLostPassword(user.getCompany().getId(), user.getId(), 1, generateLink( resetCode.getToken()));

        } catch (SessionInternalError e) {
            LOG.error("Exception while sending notification : " + e.getMessage());
            throw new SessionInternalError("forgotPassword.notification.not.found");
        } catch (NotificationNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to create the link
     * @param token the token for the link
     * @return the link for the email
     */
    private String generateLink(String token) {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> params = new HashMap<String, String>();
        params.put("token", token);
        map.put("controller", "resetPassword");
        map.put("action", "changePassword");
        map.put("params", params);
        String link = Util.getSysProp("url") + new ApplicationTagLib().createLink(map);

        return link;
    }
}


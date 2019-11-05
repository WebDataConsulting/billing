package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.client.util.ClientConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.user.IUserSessionBean;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by aadil on 1/26/15.
 */
public class InactiveUserManagementTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(InactiveUserManagementTask.class);
    private static IUserSessionBean userSessionBean = (IUserSessionBean) Context.getBean(Context.Name.USER_SESSION);

    public String getTaskName () {
        return "Inactive User Management Task";
    }

    public void execute (JobExecutionContext jobExecutionContext) throws JobExecutionException {
        super.execute(jobExecutionContext); // _init(jobExecutionContext);
        LOG.debug(Calendar.getInstance().getTime());

        // Create date object based on the preference value (preference 55 value)
        PreferenceBL preferenceBL = new PreferenceBL();
        Integer maxDaysOfInactivity = null;

        try {
            preferenceBL.set(getEntityId(), ClientConstants.PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS);
            LOG.debug("preferenceBL.getInt() %s", preferenceBL.getInt());
            maxDaysOfInactivity = preferenceBL.getInt();
        } catch ( Exception e) {
            LOG.error("Error occurred Cannot get preference : %s", e.getMessage());
        }

        LOG.debug("daysToMakeUserInActive %s", maxDaysOfInactivity);
        
        if (null != maxDaysOfInactivity && maxDaysOfInactivity.equals(new Integer(0))) {
            LOG.debug("Account Expiry Feature is Disabled. The plug-in will now exit." );
            return;
        }

        //Preference value greater then 0
        /* Account activity threshold date calculated by considering number of days value stored in preference */
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) - maxDaysOfInactivity);
        Date activityThresholdDate = cal.getTime();
        LOG.debug("Account activity threshold date calculated using preference value = %s", activityThresholdDate);

        // Get users that have not logged in since the provided date obtained in last step
        List<UserDTO> inActiveUserList = getUsersInactiveSince(activityThresholdDate);

        if (null != inActiveUserList && !inActiveUserList.isEmpty()) { // means there are users that have lastLogin before activityThresholdDate
            LOG.debug("Number of inactive users since %s = %s", activityThresholdDate, inActiveUserList.size());
        } else {
            LOG.debug("There are NO inactive users since %s", activityThresholdDate);
            return;
        }

        // Update user statuses of all the users of the above list
        for (UserDTO inactiveUser : inActiveUserList) {
            UserDTOEx userDTOEx = userSessionBean.getUserDTOEx(inactiveUser.getId());
            userSessionBean.updateUserAccountExpiryStatus(userDTOEx, true);
        }
    }

    private List<UserDTO> getUsersInactiveSince(Date activityThresholdDate) {
        UserDAS userDas = new UserDAS();
        try {
            List<UserDTO> inactiveUserList = userDas.findUsersInActiveSince(activityThresholdDate, getEntityId());
            return inactiveUserList;
        } catch (Exception e) {
            LOG.error("Exception caught while getting Inactive Users : %s", e.getMessage());
            return null;
        }
    }
}

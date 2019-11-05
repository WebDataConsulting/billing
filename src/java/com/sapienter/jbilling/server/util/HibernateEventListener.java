package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.FormatLogger;

import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;

import java.util.Arrays;

/**
 * This event listener can be used to find out which objects are dirty and it will print out the old and new
 * state for the dirty objects.
 * It is useful for debugging and needs to be enabled in resources.groovy.
 *
 * @author Gerhard
 * @since 03/10/13
 */
public class HibernateEventListener implements PreUpdateEventListener {

    private static final FormatLogger LOG = new FormatLogger(HibernateEventListener.class);

    public HibernateEventListener() {}

    @Override
    public boolean onPreUpdate(PreUpdateEvent ev) {
        LOG.info("Session [%s] Instance of: %s is found dirty, currentState: %s, previousState:%s", ev.getSession().hashCode(), ev.getEntity().getClass().getSimpleName(), Arrays.toString(ev.getState()), Arrays.toString(ev.getOldState()));
        return false;
    }
}

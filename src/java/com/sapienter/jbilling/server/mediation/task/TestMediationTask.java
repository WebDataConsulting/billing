package com.sapienter.jbilling.server.mediation.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.UserBL;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @author Vikas Bodani
 * @Since 3/12/15
 */
public class TestMediationTask extends AbstractResolverMediationTask {

    private static final FormatLogger LOG = new FormatLogger(TestMediationTask.class);

    public static final ParameterDescription PARAMETER_DEFAULT_ITEM =
            new ParameterDescription("DEFAULT ITEM ID", false, ParameterDescription.Type.STR);


    //initializer for pluggable params
    {
        descriptions.add(PARAMETER_DEFAULT_ITEM);
    }

    private static final Integer LD_CALL_ITEM = 2800;
    private static final Integer INTERSTATE_CALL_ITEM = 2801;


    @Override
    public void resolveUserCurrencyAndDate(MediationResult result, List<PricingField> fields) {
        // resolve user
        PricingField userField = find(fields, "userfield"); //asterisk.xml

        if (null != userField) {
            LOG.debug("Found username: %s", userField.getStrValue());
            UserBL user = new UserBL(userField.getStrValue(), getEntityId());
            if (null != user.getEntity()) {
                result.setUserId(user.getEntity().getUserId());
                result.setCurrencyId(user.getEntity().getCurrencyId());
            }
            LOG.debug("Resolved User %s, Currency %s", result.getUserId(), result.getCurrencyId());
        }

        // resolve event date
        PricingField eventDateField = find(fields, "start"); //asterisk.xml
        if (null != eventDateField) result.setEventDate(eventDateField.getDateValue());
        LOG.debug("Updated Event Date %s", result.getEventDate());
    }

    @Override
    public boolean isActionable(MediationResult result, List<PricingField> fields) {

        PricingField quantityField = find(fields, "duration");
        if (null == quantityField || quantityField.getIntValue().intValue() < 0) {
            result.setDone(true);
            result.addError("ERR-DURATION");
            LOG.debug("Invalid Duration %s", result.getRecordKey());
            return false;
        }

        // discard unanswered calls
        PricingField dispositionField = find(fields, "disposition");
        if (null == dispositionField || !"ANSWERED".equals(dispositionField.getStrValue())) {
            result.setDone(true);
            LOG.debug("Not a billable record %s", result.getRecordKey());
            return false;
        }

        // validate that we were able to resolve the billable user, currency and date
        if (null == result.getCurrentOrder()) {
            if (null != result.getUserId()
                    && null != result.getCurrencyId()
                    && null != result.getEventDate()) {

                OrderDTO currentOrder = OrderBL.getOrCreateCurrentOrder(result.getUserId(),
                        result.getEventDate(),
                        result.getCurrencyId(),
                        result.getPersist());

                result.setCurrentOrder(currentOrder);

                LOG.debug("Actionable Result %s, pending action...", result.getId());
                return true;
            }
        }

        LOG.debug("Mediation result %s cannot be processed!", result.getId());
        return false;
    }

    @Override
    public void doEventAction(MediationResult result, List<PricingField> fields) {

        PricingField duration = find(fields, "duration");
        PricingField destination = find(fields, "dst");

        LOG.debug("Destination = %s, Long Distance Call %s minutes", destination.getStrValue() , duration.getStrValue());

        Integer itemId = LD_CALL_ITEM;
        if (!StringUtils.isBlank(parameters.get(PARAMETER_DEFAULT_ITEM.getName()))) {
            try {
                itemId = getParameter(parameters.get(PARAMETER_DEFAULT_ITEM.getName()), itemId);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }

        //using static item
        OrderLineDTO line = newLine(itemId, duration.getDecimalValue());
        result.getLines().add(line);

        result.setDescription("Long Distance Call: " + destination.getStrValue());
    }
}

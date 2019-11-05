package com.sapienter.jbilling.server.user.partner;

import com.sapienter.jbilling.server.user.partner.db.PartnerRange;

/**
 * Created by jbilling on 28/8/17.
 */
public class PartnerRangeBL {

    public static PartnerRangeWS getWS(PartnerRange dto) {
        return dto != null ? PartnerRangeBL.getDTO(dto) : null;
    }


    public static PartnerRangeWS getDTO(PartnerRange dto) {
        PartnerRangeWS retval = new PartnerRangeWS();
        retval.setId(dto.getId());
        retval.setPartnerId(dto.getPartner() != null ? dto.getPartner().getId() : null);
        retval.setPercentageRate(dto.getPercentageRate());
        retval.setReferralFee(dto.getReferralFee());
        retval.setRangeTo(dto.getRangeFrom());
        retval.setRangeFrom(dto.getRangeTo());
        return retval;
    }


}

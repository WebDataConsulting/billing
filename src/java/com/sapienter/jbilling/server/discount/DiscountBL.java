package com.sapienter.jbilling.server.discount;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.discount.db.DiscountDAS;
import com.sapienter.jbilling.server.discount.db.DiscountDTO;
import com.sapienter.jbilling.server.discount.db.DiscountLineDAS;
import com.sapienter.jbilling.server.discount.db.DiscountLineDTO;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;


public class DiscountBL {

    private static final FormatLogger LOG = new FormatLogger(DiscountBL.class);

    private DiscountDAS discountDas = null;
    private DiscountDTO discount = null;

    public DiscountBL(Integer discountId)
            throws SessionInternalError {
        try {
            init();
            set(discountId);
        } catch (Exception e) {
            throw new SessionInternalError("Setting item", DiscountBL.class, e);
        }
    }

    public DiscountBL() {
        init();
    }

    public DiscountBL(DiscountDTO discount) {
        this.discount = discount;
        init();
    }

    public static final Integer getPeriodUnit(DiscountWS ws) {
        if (ws.isPeriodBased()) {
            return AttributeUtils.getInteger(ws.getAttributes(), "periodUnit");
        }

        return null;
    }

    public static final Integer getPeriodValue(DiscountWS ws) {
        if (ws.isPeriodBased()) {
            return AttributeUtils.getInteger(ws.getAttributes(), "periodValue");
        }

        return null;
    }

    public void set(Integer discountId) {
        discount = discountDas.find(discountId);
    }

    private void init() {
        discountDas = new DiscountDAS();
    }

    public DiscountDTO getEntity() {
        return discount;
    }

    public Integer create(DiscountDTO discount, Integer languageId) {
        DiscountDTO discountDto = discountDas.save(discount);
        if (discount.getDescription() != null) {
            discountDto.setDescription(discount.getDescription(), languageId);
        }
        return discountDto != null ? discountDto.getId() : null;
    }

    public void update(DiscountDTO discount, Integer languageId) {
        DiscountDTO discountDto = discountDas.save(discount);
        if (discount.getDescription() != null) {
            discountDto.setDescription(discount.getDescription(), languageId);
        }
    }

    public Integer createOrUpdate(DiscountWS discountWS, Integer languageId)
            throws SessionInternalError {
    	try {
	        DiscountDTO discountDto = getCreateOrUpdateDTO(discountWS);
	
	        // check that period value is not left blank and that its a valid integer
	        validatePeriodValue(discountDto);
	        checkDiscountRateIsNotZero(discountDto);
	        checkDuplicateDiscountCode(discountDto);
	        checkDuplicateDiscountDescription(discountWS, discountDto);
	        
	        discountDto.setLastUpdateDateTime(new Date());
	        discountDto = discountDas.save(discountDto);
	        discountDto.updateMetaFieldsWithValidation(discountDto.getEntityId(),null, discountDto);
	        for (InternationalDescriptionWS descriptionWS : discountWS.getDescriptions()) {
	            discountDto.setDescription(descriptionWS.getContent(), descriptionWS.getLanguageId());
	        }
	
	        return discountDto != null ? discountDto.getId() : null;
    	}catch(Exception e) {
    		System.out.println("Exception "+e.getMessage());
    		e.printStackTrace();
    		return null;
    	}
    }

    /**
     * Validation function to make sure the discount rate is not zero or less.
     *
     * @param discountDto
     * @throws SessionInternalError
     */
    private void checkDiscountRateIsNotZero(DiscountDTO discountDto) throws SessionInternalError {
        if (discountDto.getRate().compareTo(new BigDecimal(0)) <= 0) {
            String[] errmsgs = new String[1];
            errmsgs[0] = "DiscountDTO,rate,discount.error.rate.can.not.be.zero.or.less";
            throw new SessionInternalError("There is an error in discount data.", errmsgs);
        }
    }

    /**
     * This function checks if the discount code of the discount being saved is already existing.
     * It checks this in create by finding in db if the code is already present.
     * In case of update it checks this by checking if discount code exists for another id.
     *
     * @param discountDto
     * @throws SessionInternalError
     */
    private void checkDuplicateDiscountCode(DiscountDTO discountDto) throws SessionInternalError {

        boolean isDuplicateDiscountCode = false;
        String discountCode = discountDto.getCode();

        if (discountCode != null && !discountCode.isEmpty()) {

            if (discountDto.getId() == 0) {
                // its a create call
                List<DiscountDTO> discounts =
                        discountDas.findByCodeAndEntity(discountDto.getCode().trim(), discountDto.getEntityId());
                if (discounts != null && !discounts.isEmpty()) {
                    // duplicate code
                    isDuplicateDiscountCode = true;
                }
            } else {
                // its an update to existing discount
                List<DiscountDTO> discounts =
                        discountDas.exists(discountDto.getId(), discountDto.getCode().trim(), discountDto.getEntityId());
                if (discounts != null && !discounts.isEmpty()) {
                    // duplicate code
                    isDuplicateDiscountCode = true;
                }
            }
        }

        if (isDuplicateDiscountCode) {
            String[] errmsgs = new String[1];
            errmsgs[0] = "DiscountDTO,code,discount.error.code.already.exists";
            throw new SessionInternalError("The discount code is duplicate.", errmsgs);
        }
    }

    private void checkDuplicateDiscountDescription(DiscountWS discountWS, DiscountDTO discountDto) throws SessionInternalError {

        boolean isDuplicateDiscountDescription = false;
        DiscountDAS discountDas = new DiscountDAS();

        if (discountDto.getId() == 0) {
            // Its a create call
            for (InternationalDescriptionWS description : discountWS.getDescriptions()) {
                Collection<InternationalDescriptionDTO> descriptions =
                        discountDas.descriptionExists(ServerConstants.TABLE_DISCOUNT,
                                "description",
                                description.getContent(),
                                description.getLanguageId(),
                                discountDto.getEntityId());
                if (descriptions != null && !descriptions.isEmpty()) {
                    isDuplicateDiscountDescription = true;
                    break;
                }
            }
        } else {
            // its an update
            for (InternationalDescriptionWS description : discountWS.getDescriptions()) {
                Collection<InternationalDescriptionDTO> descriptions =
                        discountDas.descriptionExists(ServerConstants.TABLE_DISCOUNT,
                                "description",
                                description.getContent(),
                                description.getLanguageId(),
                                discountDto.getEntityId(),
                                discountDto.getId());
                if (descriptions != null && !descriptions.isEmpty()) {
                    isDuplicateDiscountDescription = true;
                    break;
                }
            }
        }

        if (isDuplicateDiscountDescription) {
            String[] errmsgs = new String[1];
            errmsgs[0] = "DiscountDTO,description,discount.error.description.already.exists";
            throw new SessionInternalError("The discount description is duplicate.", errmsgs);
        }
    }

    /**
     * This function validates if period value is provided in case of period based discount.
     * It also checks that period value is a valid integer. If either is not true, then
     * it throws SessionInternalError.
     *
     * @param discountDto
     * @throws SessionInternalError
     */
    private void validatePeriodValue(DiscountDTO discountDto) throws SessionInternalError {
        // make sure this check is done for period based discounts only.
        if (discountDto.getType() == null) {
            throw new SessionInternalError("Discount must have at least one discount type.",
                    new String[]{"DiscountDTO,type,discount.type.must.selected"});
        }

        if (discountDto.isPeriodBased()) {
            if (!discountDto.hasPeriodValue()) {
                // Period value is not specified for period based discount, report error.
                String[] errmsgs = new String[1];
                errmsgs[0] = "DiscountDTO,periodValue,discount.error.periodValue.required";
                throw new SessionInternalError("The discount period value is required.", errmsgs);
            } else {
                // period value is specified, lets check if its a valid integer value.
                Integer periodValue = null;
                try {
                    // try to parse as an integer value through AttributeUtils
                    periodValue = Integer.parseInt(discountDto.getPeriodValueAsString());
                } catch (Exception e) {
                    // if not an integer AttributeUtils.parseInteger will throw error.
                    throw new SessionInternalError("The discount period value should be an integer.",
                            new String[]{"DiscountDTO,periodValue,discount.error.periodValue.not.an.integer"});
                }
                // periodValue should be > 0, if less than  or equal to zero then message and exception
                if (periodValue <= new Integer(0)) {
                    throw new SessionInternalError("The discount period value should not be zero or negative.",
                            new String[]{"DiscountDTO,periodValue,discount.error.periodValue.should.not.be.zero.or.negative"});
                }
            }
        }
    }

    public DiscountWS getWS(DiscountDTO dto) {
        if (dto == null) {
            dto = this.discount;
        }
        DiscountWS ws = new DiscountWS();
        ws.setId(dto.getId());
        ws.setCode(dto.getCode());
        ws.setType(dto.getType() != null ? dto.getType().name() : null);
        ws.setRate(dto.getRate() != null ? dto.getRate().toString() : null);
        ws.setStartDate(dto.getStartDate());
        ws.setEndDate(dto.getEndDate());
        ws.setAttributes(new TreeMap<String, String>(dto.getAttributes()));
        ws.setDescription(dto.getDescription());
        ws.setEntityId(dto.getEntityId());
        ws.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(ws.getEntityId(), dto));
        return ws;
    }

    private DiscountDTO getCreateOrUpdateDTO(DiscountWS discountWs) {
        DiscountDTO discountDto = null;

        if (discountWs.getId() > 0) {
            discountDto = new DiscountDAS().findForUpdate(discountWs.getId());
        } else {
            discountDto = new DiscountDTO();
        }

        if (discountWs.getEntityId() != null) {
            discountDto.setEntity(new CompanyDTO(discountWs.getEntityId()));
        }
        MetaFieldBL.fillMetaFieldsFromWS(discountWs.getEntityId(), discountDto, discountWs.getMetaFields());
        discountDto.setCode(discountWs.getCode());
        discountDto.setStartDate(discountWs.getStartDate());
        discountDto.setEndDate(discountWs.getEndDate());
        discountDto.setRate(discountWs.getRateAsDecimal());
        discountDto.setType(DiscountStrategyType.getByName(discountWs.getType()));
        discountDto.setAttributes(new TreeMap<String, String>(discountWs.getAttributes()));
        discountDto.setEntity(new CompanyDAS().find(discountWs.getEntityId()));
        
        this.discount = discountDto;
        
        return discountDto;
    }

    public void delete() {
        Boolean isDeletable = isDeletable();

        if (isDeletable != null) {
            if (Boolean.TRUE.equals(isDeletable)) {
                new DiscountDAS().delete(discount);
            } else {
                throw new SessionInternalError("Discount " + this.discount.getCode() + " cannot be deleted, it is in use.");
            }
        }
    }
    
    public Boolean isDeletable() {
        if (this.discount != null) {
            List<DiscountLineDTO> discountLines =
                    new DiscountLineDAS().findByDiscountId(this.discount.getId());
            return discountLines == null || (discountLines != null && discountLines.isEmpty());
        }

        return null;
    }

    /**
     * This function queries the discount_line table by the created order line id.
     * Created Order Line Id (or discountOrderLineId) is the id of the order line
     * created by the discount line. If a matching record is found, it returns the
     * DiscountLineDTO, else returns null.
     *
     * @param discountOrderLineId
     * @return DiscountLineDTO
     */
    public DiscountLineDTO getDiscountLineByDiscountOrderLineId(Integer discountOrderLineId) {
        List<DiscountLineDTO> discountLines = new DiscountLineDAS().findByDiscountOrderLineId(discountOrderLineId);
        return discountLines != null && !discountLines.isEmpty() ? discountLines.get(0) : null;
    }

}

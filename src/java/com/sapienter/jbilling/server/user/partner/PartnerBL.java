/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.

 This source was modified by Web Data Technologies LLP (www.webdatatechnologies.in) since 15 Nov 2015.
 You may download the latest source from webdataconsulting.github.io.

 */

package com.sapienter.jbilling.server.user.partner;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.user.PartnerSQL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionExceptionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayout;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayoutDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerReferralCommissionDTO;
import com.sapienter.jbilling.server.user.partner.task.IPartnerCommissionTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.EventLogger;

/**
 * @author Emil
 */
public class PartnerBL extends ResultList implements PartnerSQL {
    private static final FormatLogger LOG = new FormatLogger(PartnerBL.class);

    private PartnerDAS partnerDAS = null;
    private PartnerDTO partner = null;
    private PartnerPayout payout = null;
    private EventLogger eLogger = null;

    public PartnerBL(Integer partnerId) {
        init();
        set(partnerId);
    }

    public PartnerBL() {
        init();
    }

    public PartnerBL(PartnerDTO entity) {
        partner = entity;
        init();
    }

    public void set(Integer partnerId) {
        partner = partnerDAS.find(partnerId);
    }

    public void setPayout(Integer payoutId) {
        payout = new PartnerPayoutDAS().find(payoutId);
    }

    private void init() {
        eLogger = EventLogger.getInstance();
        payout = null;
        partnerDAS = new PartnerDAS();
    }

    public PartnerDTO getEntity() {
        return partner;
    }

    public Integer create(PartnerDTO dto) throws SessionInternalError {
        LOG.debug("creating partner");

        dto.setTotalPayments(BigDecimal.ZERO);
        dto.setTotalPayouts(BigDecimal.ZERO);
        dto.setTotalRefunds(BigDecimal.ZERO);
        dto.setDuePayout(BigDecimal.ZERO);
        partner = partnerDAS.save(dto);

        LOG.debug("created partner id " + partner.getId());

        return partner.getId();
    }

    public void update(Integer executorId, PartnerDTO dto) {
        dto.getBaseUser();
        dto.getBaseUser().getId();
        partner.getId();
        eLogger.audit(executorId, dto.getBaseUser().getId(),
                Constants.TABLE_PARTNER, partner.getId(),
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.ROW_UPDATED, null, null,
                null);

        // update meta fields and run validations
        partner.updateMetaFieldsWithValidation(dto.getBaseUser().getCompany().getId(), null, dto);
    }


    /**
     * This will return the id of the lates payout that was successfull
     * @param partnerId
     * @return
     * @throws NamingException
     * @throws SQLException
     */
    private Integer getLastPayout(Integer partnerId)
            throws NamingException, SQLException {
        Integer retValue = null;
        Connection conn = ((DataSource) Context.getBean(Context.Name.DATA_SOURCE)).getConnection();
        PreparedStatement stmt = conn.prepareStatement(lastPayout);
        stmt.setInt(1, partnerId.intValue());
        ResultSet result = stmt.executeQuery();
        // since esql doesn't support max, a direct call is necessary
        if (result.next()) {
            retValue = new Integer(result.getInt(1));
        }
        result.close();
        stmt.close();
        conn.close();
        LOG.debug("Finding last payout ofr partner " + partnerId + " result = " + retValue);
        return retValue;
    }

    private int getCustomersCount()
            throws SQLException, NamingException {
        int retValue = 0;
        Connection conn = ((DataSource) Context.getBean(Context.Name.DATA_SOURCE)).getConnection();
        PreparedStatement stmt = conn.prepareStatement(countCustomers);
        stmt.setInt(1, partner.getId());
        ResultSet result = stmt.executeQuery();
        // since esql doesn't support max, a direct call is necessary
        if (result.next()) {
            retValue = result.getInt(1);
        }
        result.close();
        stmt.close();
        conn.close();
        return retValue;
    }

	public static final PartnerPayoutWS getPartnerPayoutWS(PartnerPayout dto) {

		PartnerPayoutWS ws = new PartnerPayoutWS();
		ws.setId(dto.getId());
		ws.setPartnerId(dto.getPartner() != null ? dto.getPartner().getId()
				: null);
		ws.setPaymentId(dto.getPayment() != null ? dto.getPayment().getId()
				: null);
		ws.setStartingDate(dto.getStartingDate());
		ws.setEndingDate(dto.getEndingDate());
		ws.setPaymentsAmount(dto.getPaymentsAmount());
		ws.setRefundsAmount(dto.getRefundsAmount());
		ws.setBalanceLeft(dto.getBalanceLeft());
		return ws;
	}
 
    public PartnerDTO getDTO() {
        return partner;
    }

    public PartnerPayout getLastPayoutDTO(Integer partnerId)
            throws SQLException, NamingException {
        PartnerPayout retValue = null;

        Integer payoutId = getLastPayout(partnerId);
        if (payoutId != null && payoutId.intValue() != 0) {
            payout = new PartnerPayoutDAS().find(payoutId);
            retValue = getPayoutDTO();
        }
        return retValue;
    }

    public PartnerPayout getPayoutDTO()
            throws NamingException {
        payout.touch();
        return payout;
    }
    
	public static final PartnerReferralCommissionWS getPartnerReferralCommissionWS(
			PartnerReferralCommissionDTO dto) {

		PartnerReferralCommissionWS ws = new PartnerReferralCommissionWS();
		ws.setId(dto.getId());
		ws.setReferralId(dto.getReferral() != null ? dto.getReferral().getId()
				: null);
		ws.setReferrerId(dto.getReferrer() != null ? dto.getReferrer().getId()
				: null);
		ws.setStartDate(dto.getStartDate());
		ws.setEndDate(dto.getEndDate());
		ws.setPercentage(dto.getPercentage());
		ws.setOwningUserId(getOwningUserId(ws));
		return ws;
	}
	
	 private static final Integer getOwningUserId (PartnerReferralCommissionWS ws) {
	        if (ws.getReferralId() != null && ws.getReferralId() > 0) {
	            return new PartnerBL(ws.getReferralId()).getEntity().getBaseUser().getId();
	        } else {
	            return null;
	        }
	    }
	
	public static final PartnerReferralCommissionDTO getDTO(PartnerReferralCommissionWS ws){
	        PartnerReferralCommissionDTO referralCommission = new PartnerReferralCommissionDTO();
	        referralCommission.setId(0);
	        referralCommission.setReferral(new PartnerDAS().find(ws.getReferralId()));
	        referralCommission.setReferrer(new PartnerDAS().find(ws.getReferrerId()));
	        referralCommission.setStartDate(ws.getStartDate());
	        referralCommission.setEndDate(ws.getEndDate());
	        referralCommission.setPercentage(ws.getPercentageAsDecimal());
	        return referralCommission;
	    }

    

    public CachedRowSet getList(Integer entityId)
            throws SQLException, Exception{

        prepareStatement(PartnerSQL.list);
        cachedResults.setInt(1,entityId.intValue());
        execute();
        conn.close();
        return cachedResults;
    }

    public CachedRowSet getPayoutList(Integer partnerId)
            throws SQLException, Exception{

        prepareStatement(PartnerSQL.listPayouts);
        cachedResults.setInt(1, partnerId.intValue());
        execute();
        conn.close();
        return cachedResults;
    }

    /**
     * Deletes the composed Partner object from the system
     * by first deleting the associated user and then deleting the Partner record.
     * @param executorId
     * @throws SessionInternalError
     */
    public void delete(Integer executorId) throws SessionInternalError {
        validateDelete();
        Integer userId= partner.getBaseUser().getId();
        Integer partnerId=partner.getId();

        UserBL userBl= new UserBL(userId);
        userBl.getEntity().setPartner(null);
        partner.setBaseUser(null);

        userBl.delete(executorId);
        
        for (CustomerDTO customer : partner.getCustomers()) {
        	customer.setPartner(null);
        }
        
        partner.getCustomers().clear();
        
        partnerDAS.delete(partner);
        
        if (executorId != null) {
            eLogger.audit(executorId, userId, Constants.TABLE_BASE_USER,
                    partnerId, EventLogger.MODULE_USER_MAINTENANCE,
                    EventLogger.ROW_DELETED, null, null, null);
        }
    }

    private void validateDelete() {
        if (partner == null) {
            throw new SessionInternalError("The partner has to be set before delete");
        }

        List<Integer> childList= partnerDAS.findChildList(partner.getId());
        if (CollectionUtils.isNotEmpty(childList)) {
            LOG.debug("Partner Id " + partner.getId() + " cannot be deleted. Child agents exists.");
            String errorMessages[] = new String[1];
            errorMessages[0] = "PartnerWS,childIds,partner.error.parent.cannot.be.deleted," + childList;
            throw new SessionInternalError("Cannot delete Parent Partner. Child ID(s) " + childList +" exists.", errorMessages);
        }
        if (partner.getCommissions() != null && partner.getCommissions().size() > 0) {
            String errorMessages[] = new String[1];
            errorMessages[0] = "PartnerWS,commissions,partner.error.cannot.be.deleted.commissions,";
            throw new SessionInternalError("Cannot delete Partner. Commissions exists.", errorMessages);
        }
    }

    /**
     * This method triggers the commission process calculation.
     * @param entityId
     */
    public void calculateCommissions(Integer entityId){
        try {
            PluggableTaskManager taskManager = new PluggableTaskManager(
                    entityId,
                    Constants.PLUGGABLE_TASK_PARTNER_COMMISSION);
            IPartnerCommissionTask task = (IPartnerCommissionTask) taskManager.getNextClass();
            if (task != null){
                task.calculateCommissions(entityId);
            }
        }catch (PluggableTaskException e){
            LOG.fatal("Problems handling partner commission task.", e);
            throw new SessionInternalError("Problems handling partner commission task.");
        }
    }

    /**
     * Convert a given Partner into a PartnerWS web-service object.
     *
     * @param dto dto to convert
     * @return converted web-service object
     */
	public static final PartnerWS getWS(PartnerDTO dto) {

		if (null == dto)
			return null;

		PartnerWS ws = new PartnerWS();

		ws.setId(dto.getId());
		ws.setUserId(dto.getUser() != null ? dto.getUser().getId() : null);
		ws.setTotalPayments(dto.getTotalPayments());
		ws.setTotalRefunds(dto.getTotalRefunds());
		ws.setTotalPayouts(dto.getTotalPayouts());
		ws.setDuePayout(dto.getDuePayout());

		// partner payouts
		ws.setPartnerPayouts(new ArrayList<PartnerPayoutWS>(dto
				.getPartnerPayouts().size()));
		for (PartnerPayout payout : dto.getPartnerPayouts())
			ws.getPartnerPayouts().add(PartnerBL.getPartnerPayoutWS(payout));

		// partner customer ID's
		ws.setCustomerIds(new ArrayList<Integer>(dto.getCustomers().size()));
		for (CustomerDTO customer : dto.getCustomers())
			ws.getCustomerIds().add(customer.getId());

		if (dto.getType() != null) {
			ws.setType(dto.getType().name());
		}

		ws.setParentId(dto.getParent() != null ? dto.getParent().getId() : null);

		ws.setChildIds(new Integer[dto.getChildren().size()]);
		int index = 0;
		for (PartnerDTO partner : dto.getChildren()) {
			ws.getChildIds()[index] = partner.getId();
			index++;
		}

		ws.setCommissions(new CommissionWS[dto.getCommissions().size()]);
		index = 0;
		for (CommissionDTO commission : dto.getCommissions()) {
			ws.getCommissions()[index] = CommissionProcessConfigurationBL
					.getCommissionWS(commission);
			index++;
		}

		ws.setCommissionExceptions(new PartnerCommissionExceptionWS[dto
				.getCommissionExceptions().size()]);
		index = 0;
		for (PartnerCommissionExceptionDTO commissionException : dto
				.getCommissionExceptions()) {
			ws.getCommissionExceptions()[index] = CommissionProcessConfigurationBL
					.getPartnerCommissionExceptionWS(commissionException);
			index++;
		}

		ws.setReferralCommissions(new PartnerReferralCommissionWS[dto
				.getReferralCommissions().size()]);
		index = 0;
		for (PartnerReferralCommissionDTO referralCommission : dto
				.getReferralCommissions()) {
			ws.getReferralCommissions()[index] = getPartnerReferralCommissionWS(referralCommission);
			index++;
		}

		ws.setReferrerCommissions(new PartnerReferralCommissionWS[dto
				.getReferrerCommissions().size()]);
		index = 0;
		for (PartnerReferralCommissionDTO referrerCommission : dto
				.getReferrerCommissions()) {
			ws.getReferrerCommissions()[index] = getPartnerReferralCommissionWS(referrerCommission);
			index++;
		}

		if (dto.getCommissionType() != null) {
			ws.setCommissionType(dto.getCommissionType().name());
		}
		return ws;
	}
	
	
	public static final PartnerDTO getPartnerDTO(PartnerWS ws) {

		PartnerDTO partner = null;
		if (ws.getId() != null) {
			PartnerBL partnerBl = new PartnerBL(ws.getId());
			partner = partnerBl.getEntity();
		} else {
			partner = new PartnerDTO();
			partner.setId(0);
		}

		if (null != ws.getUserId() && ws.getUserId().intValue() > 0) {
			partner.setBaseUser(new UserDAS().find(ws.getUserId()));
		}

		partner.setTotalPayments(ws.getTotalPaymentsAsBigDecimal());
		partner.setTotalRefunds(ws.getTotalRefundsAsDecimal());
		partner.setTotalPayouts(ws.getTotalPayoutsAsDecimal());
		partner.setDuePayout(ws.getDuePayoutAsDecimal());
		partner.setType(PartnerType.valueOf(ws.getType()));
		partner.setParent(new PartnerDAS().find(ws.getParentId()));

		if (ws.getCommissions() != null) {
			partner.getCommissions().clear();
			for (CommissionWS commissionWS : ws.getCommissions()) {
				CommissionDTO cm = CommissionProcessConfigurationBL
						.getDTO(commissionWS);
				cm.setPartner(partner);
				partner.getCommissions().add(cm);
			}
		}

		if (ws.getCommissionExceptions() != null) {
			partner.getCommissionExceptions().clear();
			for (PartnerCommissionExceptionWS commissionExceptionWS : ws
					.getCommissionExceptions()) {
				PartnerCommissionExceptionDTO commissionException = CommissionProcessConfigurationBL
						.getDTO(commissionExceptionWS);
				commissionException.setPartner(partner);
				partner.getCommissionExceptions().add(commissionException);
			}
		}

		if (ws.getReferralCommissions() != null) {
			partner.getReferralCommissions().clear();
			for (PartnerReferralCommissionWS referralCommissionWS : ws
					.getReferralCommissions()) {
				PartnerReferralCommissionDTO referralCommission = PartnerBL
						.getDTO(referralCommissionWS);
				referralCommission.setReferral(partner);
				partner.getReferralCommissions().add(referralCommission);
			}
		}

		if (ws.getReferrerCommissions() != null) {
			partner.getReferrerCommissions().clear();
			for (PartnerReferralCommissionWS referrerCommissionWS : ws
					.getReferrerCommissions()) {
				PartnerReferralCommissionDTO referrerCommission = PartnerBL
						.getDTO(referrerCommissionWS);
				referrerCommission.setReferrer(partner);
				partner.getReferrerCommissions().add(referrerCommission);
			}
		}

		if (StringUtils.isBlank(ws.getCommissionType())) {
			partner.setCommissionType(null);
		} else {
			partner.setCommissionType(PartnerCommissionType.valueOf(ws
					.getCommissionType()));
		}

		return partner;
	}

}

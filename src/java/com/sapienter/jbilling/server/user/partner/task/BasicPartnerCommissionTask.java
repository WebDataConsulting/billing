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
package com.sapienter.jbilling.server.user.partner.task;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.partner.CommissionType;
import com.sapienter.jbilling.server.user.partner.PartnerCommissionType;
import com.sapienter.jbilling.server.user.partner.PartnerType;
import com.sapienter.jbilling.server.user.partner.db.*;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.*;

/**
 * This implementation calculates the commissions for the partners,
 * for the period configured on the CommissionProcessConfigurationDTO
 */
public class BasicPartnerCommissionTask extends PluggableTask implements IPartnerCommissionTask{
    private static final FormatLogger LOG = new FormatLogger(BasicPartnerCommissionTask.class);

    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private Date startDate;
    private Date endDate;

    /**
     * Calculates all the agents' commissions for the given entity
     *
     * @param entityId company id
     */
    @Override
    public void calculateCommissions (Integer entityId) {
        CompanyDTO entity = new CompanyDAS().find(entityId);

        CommissionProcessConfigurationDAS configurationDAS = new CommissionProcessConfigurationDAS();
        CommissionProcessRunDAS processRunDAS = new CommissionProcessRunDAS();

        //Get the commission process configuration.
        CommissionProcessConfigurationDTO configuration = configurationDAS.findByEntity(entity);
        startDate = configuration.getNextRunDate();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(MapPeriodToCalendar.map(configuration.getPeriodUnit().getId()), configuration.getPeriodValue());
        endDate = calendar.getTime();

        //Create the commissionProcessRun object and save it.
        CommissionProcessRunDTO commissionProcessRun = new CommissionProcessRunDTO();
        commissionProcessRun.setEntity(entity);
        commissionProcessRun.setRunDate(new Date());
        commissionProcessRun.setPeriodStart(startDate);
        commissionProcessRun.setPeriodEnd(endDate);
        commissionProcessRun = processRunDAS.save(commissionProcessRun);

        LOG.debug("Calculating commissions for entity: %s, periodStart: %s, periodEnd: %s ", entity.getId(), startDate, endDate );

        //Calculate the invoiceCommissions.
        calculateInvoiceCommissions(commissionProcessRun, entityId);

        //Calculate referral commissions.
        calculateReferralCommissions(commissionProcessRun);

        //Calculate the actual commissions.
        calculateCommissions(commissionProcessRun);

        //update configuration with new nextRunDate.
        Date nextRunDate = new DateTime(endDate).plusDays(1).toDate();
        configuration.setNextRunDate(nextRunDate);
        configurationDAS.save(configuration);
    }

    private void calculateCommissions(CommissionProcessRunDTO commissionProcessRun){
        LOG.debug("Started calculating the commissions.");
        CommissionDAS commissionDAS = new CommissionDAS();

        List<PartnerDTO> partners = new PartnerDAS().findAll();
        for(PartnerDTO partner : partners){
            List<InvoiceCommissionDTO> invoiceCommissions = new InvoiceCommissionDAS().findByPartnerAndProcessRun(partner, commissionProcessRun);

            BigDecimal standardAmount = BigDecimal.ZERO;
            BigDecimal masterAmount = BigDecimal.ZERO;
            BigDecimal exceptionAmount = BigDecimal.ZERO;
            BigDecimal referralAmount = BigDecimal.ZERO;

            CurrencyDTO invoiceCurrency = null;
            CurrencyDTO agentCurrency = null;
            CurrencyDTO referralCurrency = null;

            for(InvoiceCommissionDTO invoiceCommission : invoiceCommissions) {

                try {
                    invoiceCurrency = invoiceCommission.getInvoice().getCurrency();
                    agentCurrency = partner.getBaseUser().getCurrency();
                } catch(Exception e) {
                    LOG.info("Cannot get currency from invoice/agent");
                }

                standardAmount = convertToCurrency(invoiceCommission.getStandardAmount(),invoiceCurrency,agentCurrency);
                masterAmount = convertToCurrency(invoiceCommission.getMasterAmount(),invoiceCurrency,agentCurrency);
                exceptionAmount = convertToCurrency(invoiceCommission.getExceptionAmount(),invoiceCurrency,agentCurrency);
                if(invoiceCommission.getReferralPartner()!=null) {
                    referralCurrency = invoiceCommission.getReferralPartner().getBaseUser().getCurrency();
                }
                referralAmount = convertToCurrency(invoiceCommission.getReferralAmount(),invoiceCurrency, referralCurrency);

                //update the invoice commision with the amounts converted to corresponding currency
                invoiceCommission.setStandardAmount(standardAmount);
                invoiceCommission.setMasterAmount(masterAmount);
                invoiceCommission.setExceptionAmount(exceptionAmount);
                invoiceCommission.setReferralAmount(referralAmount);
                new InvoiceCommissionDAS().reattach(invoiceCommission);
            }

            BigDecimal commissionAmount = standardAmount.add(masterAmount).add(exceptionAmount).add(referralAmount);

            if(commissionAmount != null && !commissionAmount.equals(BigDecimal.ZERO)){
                PartnerDTO parent = (partner.getParent() != null) ? partner.getParent() : partner;

                CommissionDTO commission = new CommissionDTO();
                commission.setPartner(parent);
                commission.setType(determineCommissionType(standardAmount, masterAmount, exceptionAmount, referralAmount));
                commission.setAmount(commissionAmount);
                commission.setCommissionProcessRun(commissionProcessRun);
                commission.setCurrency(parent.getBaseUser().getCurrency());

                LOG.debug("Commission created, partner: %s, amount: %s, type: %s", partner.getId(), commissionAmount, commission.getType());

                commission = commissionDAS.save(commission);

                for(InvoiceCommissionDTO invoiceCommission : invoiceCommissions){
                    invoiceCommission.setCommission(commission);
                }

            }
        }
    }

    /**
     * This method determines the commissionType according to which type of commission contributed the most.
     * @param standardAmount
     * @param masterAmount
     * @param exceptionAmount
     * @param referralAmount
     * @return CommissionType
     */
    private CommissionType determineCommissionType(BigDecimal standardAmount, BigDecimal masterAmount,
                                                   BigDecimal exceptionAmount, BigDecimal referralAmount){

        List<BigDecimal> amounts = Arrays.asList(standardAmount, masterAmount, exceptionAmount, referralAmount);

        BigDecimal max = Collections.max(amounts);

        if(standardAmount.equals(max)){
            return CommissionType.DEFAULT_STANDARD_COMMISSION;
        }else if(masterAmount.equals(max)){
            return CommissionType.DEFAULT_MASTER_COMMISSION;
        }else if(exceptionAmount.equals(max)){
            return CommissionType.EXCEPTION_COMMISSION;
        }else{
            return CommissionType.REFERRAL_COMMISSION;
        }
    }

    private void calculateInvoiceCommissions(CommissionProcessRunDTO commissionProcessRun, Integer entityId){
        LOG.debug("Started calculating the invoice commissions.");
        InvoiceDAS invoiceDAS = new InvoiceDAS();
        PaymentCommissionDAS paymentCommissionDAS = new PaymentCommissionDAS();

        List<PartnerDTO> partners = new PartnerDAS().findPartnersByCompany(entityId);
        for(PartnerDTO partner : partners){
            boolean paymentBasedCommission = isPaymentBasedCommission(partner, entityId);

            List<InvoiceDTO> invoices;
            if(paymentBasedCommission){
                List<Integer> invoiceIds = paymentCommissionDAS.findInvoiceIdsByPartner(partner);
                invoices = invoiceDAS.findAllByIdInList(invoiceIds);
            }else{
                //get the invoices for the current partner for the period
                invoices = invoiceDAS.findForPartnerCommissions(partner.getId(), endDate);
            }


            for(InvoiceDTO invoice: invoices){
                LOG.debug("Calculating commission for invoice: %s", invoice.getId());

                //Create the invoiceCommission object.
                InvoiceCommissionDTO invoiceCommission = new InvoiceCommissionDTO();
                invoiceCommission.setInvoice(invoice);
                invoiceCommission.setPartner(partner);
                invoiceCommission.setCommissionProcessRun(commissionProcessRun);

                //Calculate how much the invoice is payed and calculate a ratio between 0 & 1.
                BigDecimal payedRatio = BigDecimal.ZERO;
                if(paymentBasedCommission){
                    LOG.debug("Payment based commission");
                    List<PaymentCommissionDTO> paymentCommissions = paymentCommissionDAS.findByInvoiceId(invoice.getId());
                    for(PaymentCommissionDTO paymentCommission : paymentCommissions){
                        payedRatio = payedRatio.add(paymentCommission.getPaymentAmount());
                    }

                    payedRatio = payedRatio.divide(invoice.getTotal(), ServerConstants.BIGDECIMAL_SCALE, ServerConstants.BIGDECIMAL_ROUND);
                }else{
                    LOG.debug("Invoice based commission");
                    //For invoice based commissions the ratio is 1
                    payedRatio = BigDecimal.ONE;
                }

                BigDecimal discountPercentage = BigDecimal.ZERO;

                for (InvoiceLineDTO invoiceLine: invoice.getInvoiceLines()){

                    if(invoiceLine.getItem() != null){
                        BigDecimal percentage = invoiceLine.getItem().getPrice();

                        if(percentage != null && !percentage.equals(BigDecimal.ZERO)){
                            discountPercentage = discountPercentage.add(percentage);
                        }else{
                            calculateInvoiceLinesCommission(invoiceLine, partner, invoiceCommission, payedRatio);
                        }
                    }

                }

                //Apply invoice discounts & taxes
                invoiceCommission.setStandardAmount(calculateCommissionAmount(invoiceCommission.getStandardAmount(),
                        discountPercentage, invoiceCommission.getStandardAmount(), BigDecimal.ONE));
                invoiceCommission.setMasterAmount(calculateCommissionAmount(invoiceCommission.getMasterAmount(),
                        discountPercentage, invoiceCommission.getMasterAmount(), BigDecimal.ONE));
                invoiceCommission.setExceptionAmount(calculateCommissionAmount(invoiceCommission.getExceptionAmount(),
                        discountPercentage, invoiceCommission.getExceptionAmount(), BigDecimal.ONE));
                invoiceCommission.setReferralAmount(calculateCommissionAmount(invoiceCommission.getReferralAmount(),
                        discountPercentage, invoiceCommission.getReferralAmount(), BigDecimal.ONE));

                new InvoiceCommissionDAS().save(invoiceCommission);
                LOG.debug("Created invoice commission object, invoice: %s, partner %s", invoice.getId(), partner.getId());
                LOG.debug("Standard amount: %s", invoiceCommission.getStandardAmount());
                LOG.debug("Master amount: %s", invoiceCommission.getMasterAmount());
                LOG.debug("Exception amount: %s", invoiceCommission.getExceptionAmount());
                LOG.debug("Referral amount: %s", invoiceCommission.getReferralAmount());
            }

            //delete paymentCommissions
            List<PaymentCommissionDTO> paymentCommissions = paymentCommissionDAS.findByPartner(partner);
            for(PaymentCommissionDTO paymentCommission : paymentCommissions){
                paymentCommissionDAS.delete(paymentCommission);
            }
        }
    }

    private boolean isPaymentBasedCommission(PartnerDTO partner, Integer entityId){
        return (partner.getCommissionType() != null && partner.getCommissionType().equals(PartnerCommissionType.PAYMENT) ||
                                        (partner.getCommissionType() == null &&
                                                getCommissionTypePreference(entityId).equals(PartnerCommissionType.PAYMENT)));
    }

    private void calculateInvoiceLinesCommission (InvoiceLineDTO invoiceLine, PartnerDTO partner, InvoiceCommissionDTO invoiceCommission, BigDecimal payedRatio){
        if(payedRatio == null || payedRatio.equals(BigDecimal.ZERO))
            return;

        boolean isCommissionException = false;

        for (PartnerCommissionExceptionDTO commissionException: partner.getCommissionExceptions()){
            if(commissionException.getItem().equals(invoiceLine.getItem())){
                //Check if the commission exception is valid.
                if(!isCommissionValid(commissionException.getStartDate(), commissionException.getEndDate(),
                        invoiceLine.getInvoice().getCreateDatetime())){
                    break;
                }


                invoiceCommission.setExceptionAmount(calculateCommissionAmount(
                        invoiceCommission.getExceptionAmount(),
                        commissionException.getPercentage(),
                        invoiceLine.getAmount(),
                        payedRatio));

                isCommissionException = true;
                break;
            }
        }

        if(!isCommissionException){
            if(partner.getType().equals(PartnerType.STANDARD)){
                invoiceCommission.setStandardAmount(calculateCommissionAmount(
                        invoiceCommission.getStandardAmount(),
                        invoiceLine.getItem().getStandardPartnerPercentage(),
                        invoiceLine.getAmount(),
                        payedRatio));
            }else{
                invoiceCommission.setMasterAmount(calculateCommissionAmount(
                        invoiceCommission.getMasterAmount(),
                        invoiceLine.getItem().getMasterPartnerPercentage(),
                        invoiceLine.getAmount(),
                        payedRatio));
            }
        }
    }

    private void calculateReferralCommissions(CommissionProcessRunDTO commissionProcessRun){
        LOG.debug("Started calculating the referral commissions.");
        List<PartnerReferralCommissionDTO> referralCommissions = new PartnerReferralCommissionDAS().findAll();

        for(PartnerReferralCommissionDTO referralCommission : referralCommissions){
            PartnerDTO referral = referralCommission.getReferral(); //the one that gives.

            BigDecimal referralAmount = BigDecimal.ZERO;

            List<InvoiceCommissionDTO> invoiceCommissions = new InvoiceCommissionDAS().findByPartnerAndProcessRun(referral, commissionProcessRun);
            for(InvoiceCommissionDTO invoiceCommission : invoiceCommissions){
                if(isCommissionValid(referralCommission.getStartDate(), referralCommission.getEndDate(),
                        invoiceCommission.getInvoice().getCreateDatetime())) {
                    //standard commission
                    referralAmount = calculateCommissionAmount(referralAmount,
                            referralCommission.getPercentage(),
                            invoiceCommission.getStandardAmount(),
                            BigDecimal.ONE);

                    //master commission
                    referralAmount = calculateCommissionAmount(referralAmount,
                            referralCommission.getPercentage(),
                            invoiceCommission.getMasterAmount(),
                            BigDecimal.ONE);
                    //exception commission
                    referralAmount = calculateCommissionAmount(referralAmount,
                            referralCommission.getPercentage(),
                            invoiceCommission.getExceptionAmount(),
                            BigDecimal.ONE);
                }
            }

            PartnerDTO referrer = referralCommission.getReferrer(); //the one that receives.

            if(referralAmount != null && !referralAmount.equals(BigDecimal.ZERO)){
                InvoiceCommissionDTO invoiceCommission = new InvoiceCommissionDTO();
                invoiceCommission.setPartner(referrer);
                invoiceCommission.setReferralPartner(referral);
                invoiceCommission.setCommissionProcessRun(commissionProcessRun);
                invoiceCommission.setReferralAmount(referralAmount);
                new InvoiceCommissionDAS().save(invoiceCommission);
                LOG.debug("Created referral invoice commission object, referrerPartner: %s, referralPartner: %s, amount: %s", referrer.getId(), referral.getId(), referralAmount);
            }
        }
    }

    /**
     * Calculates the percentage of the newAmount adding it to the currentAmount.
     * This value is multiplied with the payedRatio (between 0 & 1) before returning the value.
     * @param currentAmount
     * @param percentage
     * @param newAmount
     * @param payedRatio
     * @return
     */
    private BigDecimal calculateCommissionAmount (BigDecimal currentAmount, BigDecimal percentage, BigDecimal newAmount, BigDecimal payedRatio) {
        if(percentage != null && newAmount != null){
            return currentAmount.add(percentage.divide(ONE_HUNDRED).multiply(newAmount)).multiply(payedRatio);
        }
        else {
            return currentAmount;
        }
    }

    /**
     * Convert the given amount, expresed in invoice currency to the commission currency.
     * @param currentAmount
     * @param invoiceCurrency
     * @param targetCurrency
     * @return
     */
    private BigDecimal convertToCurrency(BigDecimal currentAmount, CurrencyDTO invoiceCurrency, CurrencyDTO targetCurrency) {
        if(invoiceCurrency!=null && targetCurrency!=null) {
            return new CurrencyBL().convert(invoiceCurrency.getId(), targetCurrency.getId(), currentAmount, new Date(), this.getEntityId());
        }
        else return currentAmount;
    }

    /**
     * Gets the preference defined for the company.
     * @param entityId
     * @return PartnerCommissionType
     */
    private PartnerCommissionType getCommissionTypePreference(Integer entityId){
        String prefValue = new PreferenceBL(entityId, CommonConstants.PREFERENCE_PARTNER_DEFAULT_COMMISSION_TYPE).getString();
        return PartnerCommissionType.valueOf(prefValue);
    }

    /**
     * Determines if the commission is valid for the given period.
     * @param commissionStart
     * @param commissionEnd
     * @param invoiceDate
     * @return
     */
    private boolean isCommissionValid(Date commissionStart, Date commissionEnd, Date invoiceDate) {
        if(commissionEnd == null){
            return (commissionStart.compareTo(invoiceDate) <= 0);
        }
        else {
            return (commissionStart.compareTo(invoiceDate)<=0)
                    && (commissionEnd.compareTo(invoiceDate)>=0);
        }
    }
}

package com.sapienter.jbilling.server.payment;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.event.AbstractPaymentEvent;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.CreditCardSQL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PaymentInformationBL extends ResultList implements CreditCardSQL{
	
	 private static final FormatLogger LOG = new FormatLogger(PaymentInformationBL.class);
	 
	 public static final String OBSCURED_NUMBER_FORMAT = "************"; // + last four digits

	 private PaymentInformationDTO paymentInstrument = null;
	 private PaymentInformationDAS piDas = null;
	 
	 public PaymentInformationBL() {
		 init();
	 }
	 
	 public PaymentInformationBL(Integer id) {
		 init();
		 set(id);
	 }
	 
	 public void init() {
		 piDas = new PaymentInformationDAS();
	 }
	 
	 public void set(Integer id) {
		 paymentInstrument = piDas.find(id);
	 }
	
	 public PaymentInformationDTO get() {
		 return paymentInstrument;
	 }
	 
	 public PaymentInformationDTO create(PaymentInformationDTO dto) {
		 return piDas.create(dto, dto.getPaymentMethodType().getEntity().getId());
	 }
	 
	 public void delete(Integer id) {
		 if(id != null) {
			 piDas.delete(piDas.findNow(id));
		 }
	 }
	 
	 public static final PaymentInformationWS getWS(PaymentInformationDTO dto){
		 
		 	PaymentInformationWS ws = new PaymentInformationWS();
		 	ws.setId(dto.getId());
			ws.setProcessingOrder(dto.getProcessingOrder());
			ws.setPaymentMethodTypeId(dto.getPaymentMethodType().getId());
			ws.setPaymentMethodId(dto.getPaymentMethod() != null ? dto.getPaymentMethod().getId() : null);
			
			List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(0);
			for(MetaFieldValue value : dto.getMetaFields()) {
				metaFields.add(MetaFieldBL.getWS(value));
			}
			
			ws.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
			return ws;
	 }
	 /**
	  * Gets payment method of credit card if one exist for company, if do not then create it.
	  * Use that payment method to create a credit card and sets in template meta field values
	  * 
	  * @param cardNumber	Payment card number
	  * @param company	Company of credit card
	  * @return	PaymentInformationDTO
	  */
	 public PaymentInformationDTO getCreditCardObject(String cardNumber, CompanyDTO company) {
		 PaymentMethodTypeDTO pmtDto = getPaymentMethodTypeByTemplate(ServerConstants.PAYMENT_CARD, company);
		 
		 PaymentInformationDTO piDto = new PaymentInformationDTO();
		 piDto.setPaymentMethodType(pmtDto);

		 // set specific values
		 DateTimeFormatter dateFormat = DateTimeFormat.forPattern(ServerConstants.CC_DATE_FORMAT);
		 updateStringMetaField(piDto, cardNumber, MetaFieldType.PAYMENT_CARD_NUMBER);
		 updateStringMetaField(piDto, dateFormat.print(new Date().getTime()), MetaFieldType.DATE);
		 
		 return piDto;
	 }
	 
	 public PaymentMethodTypeDTO getPaymentMethodTypeByTemplate(String template, CompanyDTO company) {
		 PaymentMethodTypeDAS pmtDas = new PaymentMethodTypeDAS();
		 PaymentMethodTypeDTO pmtDto = pmtDas.getPaymentMethodTypeByTemplate(template, company.getId());
		 
		 if(pmtDto == null) {
			pmtDto = new PaymentMethodTypeDTO(); 
			
			pmtDto.setEntity(company);
			pmtDto.setIsRecurring(false);
			pmtDto.setMethodName(template);
			
			PaymentMethodTemplateDTO paymentTemplate = new PaymentMethodTemplateDAS().findByName(template);
			pmtDto.setPaymentMethodTemplate(paymentTemplate);
			
			for(MetaField field : paymentTemplate.getPaymentTemplateMetaFields()) {
				MetaField mf = new MetaField();
				mf.setEntity(company);
				mf.setName(field.getName());
				mf.setDataType(field.getDataType());
				mf.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
				mf.setDisabled(field.isDisabled());
				mf.setMandatory(field.isMandatory());
				mf.setDefaultValue(field.getDefaultValue());
				mf.setFieldUsage(field.getFieldUsage());
				mf.setDisplayOrder(field.getDisplayOrder());
				mf.setPrimary(field.getPrimary());
				mf.setValidationRule(field.getValidationRule());
				mf.setFilename(field.getFilename());
				pmtDto.addMetaField(mf);
			}
			
			pmtDto = pmtDas.save(pmtDto);
		 }
		 
		 return pmtDto;
	 }
	 
	 /**
     * Only used from the API, thus the usage of PaymentAuthorizationDTOEx
     * @param entityId
     * @param userId
     * @param cc
     * @param amount
     * @param currencyId
     * @return
     * @throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException
     */
    public PaymentAuthorizationDTOEx validatePreAuthorization(Integer entityId, Integer userId, PaymentInformationDTO cc,
                                                              BigDecimal amount, Integer currencyId, Integer executorUserId) throws PluggableTaskException {

    // create a new payment record
        PaymentDTOEx paymentDto = new PaymentDTOEx();
        paymentDto.setAmount(amount);
        paymentDto.setCurrency(new CurrencyDAS().find(currencyId));
        
        PaymentMethodDTO paymentMethod = new PaymentMethodDAS().find(getPaymentMethodForPaymentMethodType(cc));
        cc.setPaymentMethod(paymentMethod);
        paymentDto.getPaymentInstruments().clear();
        paymentDto.getPaymentInstruments().add(cc);
        paymentDto.setInstrument(cc);
        
        paymentDto.setUserId(userId);
        paymentDto.setIsPreauth(1);

        // filler fields, required
        paymentDto.setIsRefund(0);
        paymentDto.setPaymentMethod(paymentMethod);
        paymentDto.setAttempt(1);
        paymentDto.setPaymentResult(new PaymentResultDAS().find(ServerConstants.RESULT_ENTERED)); // to be updated later
        paymentDto.setPaymentDate(Calendar.getInstance().getTime());
        paymentDto.setBalance(amount);

        PaymentBL payment = new PaymentBL();
        payment.create(paymentDto, executorUserId); // this updates the id

        // use the payment processor configured 
        PluggableTaskManager taskManager = new PluggableTaskManager(entityId, ServerConstants.PLUGGABLE_TASK_PAYMENT);
        PaymentTask task = (PaymentTask) taskManager.getNextClass();

        boolean processNext = true;
        while (task != null && processNext) {
            processNext = task.preAuth(paymentDto);
            // get the next task
            task = (PaymentTask) taskManager.getNextClass();

            // at the time, a pre-auth acts just like a normal payment for events
            AbstractPaymentEvent event = AbstractPaymentEvent.forPaymentResult(entityId, paymentDto);
            if (event != null) {
                EventManager.process(event);
            }
        }

        // update the result
        payment.getEntity().setPaymentResult(paymentDto.getPaymentResult());

        //create the return value
        PaymentAuthorizationDTOEx retValue = null;

        if (paymentDto.getAuthorization() != null){
            retValue  = new PaymentAuthorizationDTOEx(paymentDto.getAuthorization().getOldDTO());
            if (paymentDto.getPaymentResult().getId() != ServerConstants.RESULT_OK) {
                // if it was not successfull, it should not have balance
                payment.getEntity().setBalance(BigDecimal.ZERO);
                retValue.setResult(false);
            } else {
                retValue.setResult(true);
            }
        }

        return retValue;
    }
	    
	/**
     * Returns true if it makes sense to send this cc to the processor.
     * Otherwise false (like when the card is now expired). 
     */
    public boolean validateCreditCard(Date expiryDate, String creditCardNumber) {
        boolean retValue = true;

        if (expiryDate.before(Calendar.getInstance().getTime())) {
            retValue = false;
        } else {
            if (Util.getPaymentMethod(creditCardNumber) == null) {
                retValue = false;
            }
        }

        return retValue;
    }
    
    public void notifyExipration(Date today) 
    		throws SQLException, SessionInternalError {
        
    	LOG.debug("Sending credit card expiration notifications. Today %s", today);
        prepareStatement(CreditCardSQL.expiring);
        cachedResults.setDate(1, new java.sql.Date(today.getTime()));

        execute();
        while (cachedResults.next()) {
            Integer userId = new Integer(cachedResults.getInt(1));
            Integer paymentInstrumentId = new Integer(cachedResults.getInt(2));
            Date ccExpiryDate = cachedResults.getDate(3);

            set(paymentInstrumentId);
            NotificationBL notif = new NotificationBL();
            UserBL user = new UserBL(userId);
            
            try {
                MessageDTO message = notif.getCreditCardMessage(user.getEntity().
                        getEntity().getId(), user.getEntity().getLanguageIdField(),
                        userId, ccExpiryDate, piDas.find(paymentInstrumentId));

                INotificationSessionBean notificationSess = 
                        (INotificationSessionBean) Context.getBean(
                        Context.Name.NOTIFICATION_SESSION);
                notificationSess.notify(userId, message);
            } catch (NotificationNotFoundException e) {
                LOG.warn("credit card message not set to user %s because the entity lacks notification text", userId);
            }
        }
        conn.close();

    }

    /**
     * Gets a list of instruments that exist in database and returns a dto if its a credit card
     * 
     * @param paymentInstruments	list of instruments
     * @return	payment instrument that is credit card
     */
    public PaymentInformationDTO findCreditCard(List<PaymentInformationDTO> paymentInstruments) {
    	if(paymentInstruments.size() > 0) {
    		for(PaymentInformationDTO dto : paymentInstruments) {
    			if(piDas.isCreditCard(dto.getId())) {
    				return dto;
    			}
    		}
    	}
    	return null;
    }
    
    /**
     * Gets a list of instruments that exist in database and returns all credit cards
     * 
     * @param paymentInstruments	list of instruments
     * @return	payment instrument that is credit card
     */
    public List<PaymentInformationDTO> findAllCreditCards(List<PaymentInformationDTO> paymentInstruments) {
    	List<PaymentInformationDTO> instruments = new ArrayList<PaymentInformationDTO>();
    	if(paymentInstruments.size() > 0) {
    		for(PaymentInformationDTO dto : paymentInstruments) {
    			if(piDas.isCreditCard(dto.getId())) {
    				instruments.add(dto);
    			}
    		}
    	}
    	if(instruments.isEmpty()) {
    		return null;
    	} else {
    		return instruments;
    	}
    }
    
    /**
     * Verifies if payment instrument is a credit card
     * 
     * @param instrument	PaymentInformationDTO
     * @return	true if payment instrument is credit card
     */
    public boolean isCreditCard(PaymentInformationDTO instrument) {
    	return getStringMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER) != null;
    }
    
    /**
     * Verifies if payment instrument is a ach
     * 
     * @param instrument	PaymentInformationDTO
     * @return	true if payment instrument is an ach
     */
    public boolean isACH(PaymentInformationDTO instrument) {
    	return (getStringMetaFieldByType(instrument, MetaFieldType.BANK_ACCOUNT_NUMBER) != null) &&
    			(getStringMetaFieldByType(instrument, MetaFieldType.BANK_ROUTING_NUMBER) != null);
    }
    
    /**
     * Verifies if payment instrument is a cheque
     * 
     * @param instrument	PaymentInformationDTO
     * @return	true if payment instrument is an ach
     */
    public boolean isCheque(PaymentInformationDTO instrument) {
    	return getStringMetaFieldByType(instrument, MetaFieldType.CHEQUE_NUMBER) != null;
    }
    
    public boolean useGatewayKey(PaymentInformationDTO instrument) {        
        return (ServerConstants.PAYMENT_METHOD_GATEWAY_KEY.equals(instrument.getPaymentMethod().getId()) || getStringMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY) != null);
    }
    
    public boolean updateStringMetaField(PaymentInformationDTO instrument, String metaFieldValue, MetaFieldType metaFieldName) {        
         MetaFieldValue value = getMetaField(instrument, metaFieldName);
         if(value != null) {
        	 value.setValue(metaFieldValue);
        	 return true;
         }
         else {
        	 int entityId = instrument.getPaymentMethodType().getEntity().getId();
        	 MetaFieldValue gateway = null;
        	 for(MetaField field : instrument.getPaymentMethodType().getPaymentMethodTemplate().getPaymentTemplateMetaFields()) {
        		 if(field.getEntity().getId() == entityId && field.getFieldUsage() == metaFieldName) {
        			 value = field.createValue();
        			 break;
        		 }
        	 }
        	 
        	 if(value != null) {
        		 instrument.addMetaField(value);
        		 return true;
        	 }
         }
         return false;
    }
    
    public Integer getPaymentMethod(String credtiCardNumber) {
    	if(credtiCardNumber != null) {
    		return Util.getPaymentMethod(credtiCardNumber);
    	}
    	return null;
    }
    
    /**
     * Gets a payment instrument and then obscures the card number if the instrument is a credit card
     * 
     * @param instrument	PaymentInforamtionDTO
     */
    public void obscureCreditCardNumber(PaymentInformationDTO instrument) {
    	MetaFieldValue cardNumber = getMetaField(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
    	if(cardNumber != null && cardNumber.getValue() != null) {
    		String number = (String) cardNumber.getValue();
    		cardNumber.setValue(OBSCURED_NUMBER_FORMAT + number.substring(number.length()-4));
    	}
    }

    public String getObscureCreditCardNumber(PaymentInformationDTO instrument) {
        MetaFieldValue cardNumber = getMetaField(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        String number = null;
        if(cardNumber != null && cardNumber.getValue() != null) {
            number = (String) cardNumber.getValue();
            number = OBSCURED_NUMBER_FORMAT + number.substring(number.length()-4);
        }
        return number;
    }

    public void obscureCreditCardNumber(PaymentInformationWS instrument) {
        MetaFieldValueWS cardNumber = getMetaField(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        if(cardNumber != null && cardNumber.getValue() != null) {
            String number = (String) cardNumber.getValue();
            cardNumber.setValue(OBSCURED_NUMBER_FORMAT + number.substring(number.length()-4));
        }
    }

    /**
     * Gets a payment instrument and then obscures the bank account number if the instrument is an ach
     * 
     * @param instrument	PaymentInforamtionDTO
     */
    public void obscureBankAccountNumber(PaymentInformationDTO instrument) {
    	MetaFieldValue baNumber = getMetaField(instrument, MetaFieldType.BANK_ACCOUNT_NUMBER);
    	if(baNumber != null && baNumber.getValue() != null) {
    		String number = (String) baNumber.getValue();
    		// obscure credit card number
    		int len = number.length() - 4;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < len; i++) {
                sb.append('*');
            }
            sb.append(number.substring(len));

    		baNumber.setValue(sb.toString());
    	}
    }
    
    /**
     * This method gets a payment information object and then depending upon its type returns a payment method id
     * 
     * @param instrument	PaymentInformationDTO
     * @return	Integer, payment method id
     */
    public Integer getPaymentMethodForPaymentMethodType(PaymentInformationDTO instrument) {
    	if(instrument.getPaymentMethod() == null) {
			if(isCreditCard(instrument)) {
				return getPaymentMethod(getStringMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER));
			} else if(isACH(instrument)) {
				return ServerConstants.PAYMENT_METHOD_ACH;
			} else if(isCheque(instrument)) {
				return ServerConstants.PAYMENT_METHOD_CHEQUE;
			}
		} else {
			return instrument.getPaymentMethod().getId();
		}
		return null;
    }
    
    public String getStringMetaFieldByType(PaymentInformationDTO instrument, MetaFieldType type) {
    	MetaFieldValue value = getMetaField(instrument, type);

    	if(value != null) {
            return (String) value.getValue();
    	}
    	return null;
    }

    public String getStringMetaFieldByType(PaymentInformationWS instrument, MetaFieldType type) {
        MetaFieldValueWS value = getMetaField(instrument, type);
        if(value != null) {
            return value.getStringValue();
        }
        return null;
    }

    private MetaFieldValueWS getMetaField(PaymentInformationWS instrument, MetaFieldType type) {
        PaymentInformationDTO paymentInformationDTO = new PaymentInformationDAS().find(instrument.getId());
        MetaFieldValue metaField = getMetaField(paymentInformationDTO, type);
        if(type != null && instrument.getMetaFields() != null) {
            for(MetaFieldValueWS value : instrument.getMetaFields()) {
                if (value.getId() == metaField.getId()) {
                    return value;
                }
            }
        }
        return null;
    }

    public Date getDateMetaFieldByType(PaymentInformationDTO instrument, MetaFieldType type) {
    	MetaFieldValue value = getMetaField(instrument, type);
    	try {
    		if(value != null) {
    			return DateTimeFormat.forPattern(ServerConstants.CC_DATE_FORMAT).parseDateTime((String) value.getValue()).toDate();
    		}
    	}catch(Exception e) {
    		LOG.error("Error occurred while parsing credit card date object: %s",e);
    	}
    	return null;
    }

    private Date getDateMetaFieldByType(PaymentInformationWS cc, MetaFieldType date) {
        String value = getStringMetaFieldByType(cc, date);
        try {
            if(value != null) {
                return DateTimeFormat.forPattern(ServerConstants.CC_DATE_FORMAT).parseDateTime(value).toDate();
            }
        }catch(Exception e) {
            LOG.error("Error occurred while parsing credit card date object: %s",e);
        }
        return null;
    }
    
	public Integer getIntegerMetaFieldByType(PaymentInformationDTO instrument, MetaFieldType type) {
    	MetaFieldValue value = getMetaField(instrument, type);
    	if(value != null) {
    		return (Integer) value.getValue();
    	}
    	return null;
    }
	
	public MetaFieldValue getMetaField(PaymentInformationDTO instrument, MetaFieldType type) {
    	if(type != null && instrument.getMetaFields() != null) {
    		for(MetaFieldValue value : instrument.getMetaFields()) {
			    MetaFieldType fieldType = value.getField().getFieldUsage();
                if(null != fieldType && fieldType == type) {
                    return value;
                }
    		}
    	}
    	return null;
    }
	
    public Date getCardExpiryDate(PaymentInformationDTO cc) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(getDateMetaFieldByType(cc, MetaFieldType.DATE));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }

	
	public String get4digitExpiry(PaymentInformationDTO cc) {
        String expiry = null;
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(getDateMetaFieldByType(cc, MetaFieldType.DATE));
        expiry = String.valueOf(
                cal.get(GregorianCalendar.MONTH) + 1) + String.valueOf(
                cal.get(GregorianCalendar.YEAR)).substring(2);
        if (expiry.length() == 3) {
            expiry = "0" + expiry;
        }

        return expiry;
    }

    public String get4digitExpiry(PaymentInformationWS cc) {
        String expiry = null;
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(getDateMetaFieldByType(cc, MetaFieldType.DATE));
        expiry = String.valueOf(
                cal.get(GregorianCalendar.MONTH) + 1) + String.valueOf(
                cal.get(GregorianCalendar.YEAR)).substring(2);
        if (expiry.length() == 3) {
            expiry = "0" + expiry;
        }

        return expiry;
    }

    /**
	 * Compares saved credit card with the changed one and verifies if 
	 * credit card values are changed.
	 * 
	 * @param changed	new payment instrument
	 * @return	true if credit card values has changed
	 */
	public boolean isCCUpdated(PaymentInformationDTO changed) {
		boolean updated = true;
		
		String oldTitle = getStringMetaFieldByType(paymentInstrument, MetaFieldType.TITLE);
		String newTitle = getStringMetaFieldByType(changed, MetaFieldType.TITLE);
		
		String oldNumber = getStringMetaFieldByType(paymentInstrument, MetaFieldType.PAYMENT_CARD_NUMBER);
		String newNumber = getStringMetaFieldByType(changed, MetaFieldType.PAYMENT_CARD_NUMBER);
		
		Date oldDate = getDateMetaFieldByType(paymentInstrument, MetaFieldType.DATE);
        Date newDate = getDateMetaFieldByType(changed, MetaFieldType.DATE);

        LOG.debug("Verifying if the credit card is updated. %s.%s.%s.%s", oldTitle, newTitle, oldDate, newDate);
		if( String.valueOf(oldTitle).equals(String.valueOf(newTitle))
                && String.valueOf(oldNumber).equals(String.valueOf(newNumber))
                && oldDate.equals(newDate)) {
			updated = false;
		}

		return updated;
	}

}

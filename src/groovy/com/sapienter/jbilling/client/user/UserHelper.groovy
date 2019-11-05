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

package com.sapienter.jbilling.client.user

import com.sapienter.jbilling.common.FormatLogger
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDTO
import com.sapienter.jbilling.client.authentication.CompanyUserDetails
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.partner.PartnerCommissionExceptionWS
import com.sapienter.jbilling.server.user.partner.PartnerReferralCommissionWS
import com.sapienter.jbilling.server.user.partner.PartnerWS
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import com.sapienter.jbilling.server.user.MainSubscriptionWS
import com.sapienter.jbilling.server.user.UserWS

import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod

import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO


import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.security.authentication.encoding.PasswordEncoder

import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.payment.PaymentInformationWS
import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper


/**
 * UserHelper 
 *
 * @author Brian Cowdery
 * @since 04/04/11
 */
class UserHelper {

    private static def log = new FormatLogger(this.class)

    /**
     * Constructs a UserWS object and populates it with data from the given parameter map. The user
     * and all associated objects (ContactWS, CreditCarDTO, AchDTO, custom contact fields etc.) are
     * also bound as needed.
     *
     * @param user user object to bind parameters to
     * @param params parameters to bind
     * @return bound UserWS object
     */
    static def UserWS bindUser(UserWS user, GrailsParameterMap params) {
        bindData(user, params, 'user')


        // default main role to TYPE_CUSTOMER if not set
        if (!user.mainRoleId) {
            user.setMainRoleId(CommonConstants.TYPE_CUSTOMER)
        }

        log.debug("User ${user}")
		
		// bind main Subscription object if parameters present
		if (params.mainSubscription) {
			def mainSubscription = new MainSubscriptionWS()
			bindData(mainSubscription, params, 'mainSubscription')
			user.setMainSubscription(mainSubscription)
			log.debug("Main Subscrption ${mainSubscription}")
		}

        return user
    }

    /**
     * Binds user contacts. The given UserWS object will be populated with the primary contact type, and the
     * given list will be populated with all the contacts.
     *
     * @param user user object to bind primary contact to
     * @param contacts list to populate with remaining secondary contacts
     * @param company company
     * @param params parameters to bind
     * @return list of bound contact types
     */
    static def Object[] bindContacts(UserWS user, List contacts, CompanyDTO company, GrailsParameterMap params) {
        // bind user contact
        def contact = new ContactWS()
        bindData(contact, params, "contact")

        // manually bind contact "include in notifications" flag
        contact.include = params."contact".include != null ? 1 : 0

        user.setContact(contact)
        contacts << contact

        log.debug("Contact : ${contact}")

        return contacts;
    }


    /**
     * Binds the password parameters to the given new user object, ensuring that the password entered is
     * valid and that if the user already exists that the old password is verified before changing.
     *
     * @param newUser user to bind password to
     * @param oldUser existing user (may be null)
     * @param params parameters to bind
     */
    static def bindPassword(UserWS newUser, UserWS oldUser, GrailsParameterMap params, flash) {
        if (oldUser) {
            // validate that the entered confirmation password matches the users existing password
            if (params.newPassword) {

                //read old password directly from DB. API does not reveal password hashes
                def oldPassword = UserDTO.get(oldUser.userId).password

                PasswordEncoder passwordEncoder = Context.getBean(Context.Name.PASSWORD_ENCODER)
                //fake user details so we can verify the customers password
                //should we move this to the server side validation?
                CompanyUserDetails userDetails = new CompanyUserDetails(
                        oldUser.getUserName(), oldPassword, true, true, true, true,
                        Collections.EMPTY_LIST, null, null, oldUser.getMainRoleId(), oldUser.getEntityId(),
                        oldUser.getCurrencyId(), oldUser.getLanguageId()
                )
                if (!passwordEncoder.isPasswordValid(oldPassword, params.oldPassword, userDetails)) {
                    flash.error = 'current.password.doesnt.match.existing'
                    return
                }

            } else {
				newUser.setPassword(null)
            }
        }
		
		// verify passwords only when new password is present
		if (params.newPassword) {
			if (params.newPassword == params.verifiedPassword) {
				if (params.newPassword)
					newUser.setPassword(params.newPassword)
			} else {
				flash.error = 'passwords.dont.match'
			}
		} else {
			newUser.setPassword(null)
        }
    }

    /**
     * Binds the Commission Exceptions for a partner.
     *
     * @param partner Partner object to bind the commission exceptions to.
     * @param params Params object with the information filled in the form.
     * @param dateFormat Format to parse the dates to.
     * @return
     */
    static def bindPartnerCommissionExceptions(PartnerWS partner, GrailsParameterMap params, String dateFormat) {
        // Get the number of commission exceptions that were defined.
        def exceptionSize = params.list('exception.itemId')?.size()

        // Initialize the array.
        partner.commissionExceptions = new PartnerCommissionExceptionWS[exceptionSize]

        if (exceptionSize == 1) {
            PartnerCommissionExceptionWS commissionException = new PartnerCommissionExceptionWS()

            // Parse the Item Id.
            commissionException.itemId = bindPartnerCommissionEntityId(params.exception?.itemId)

            // Parse the dates.
            commissionException.startDate = bindPartnerCommissionDate(params?.exception?.startDate, dateFormat)
            commissionException.endDate = bindPartnerCommissionDate(params?.exception?.endDate, dateFormat)

            // Parse the percentage.
            commissionException.percentage = params.exception?.percentage

            partner.commissionExceptions[0] = commissionException
        } else if (exceptionSize > 1) {
            PartnerCommissionExceptionWS commissionException

            (0..(exceptionSize - 1)).each {
                commissionException = new PartnerCommissionExceptionWS()

                // Parse the Item Id.
                commissionException.itemId = bindPartnerCommissionEntityId(params.exception?.itemId[it])

                // Parse the dates.
                commissionException.startDate = bindPartnerCommissionDate(params?.exception?.startDate[it], dateFormat)
                commissionException.endDate = bindPartnerCommissionDate(params?.exception?.endDate[it], dateFormat)

                // Parse the percentage.
                commissionException.percentage = params.exception?.percentage[it]

                partner.commissionExceptions[it] = commissionException
            }
        }
    }

    /**
     * Binds the Referral Commissions for a partner.
     *
     * @param partner Partner object to bind the commission exceptions to.
     * @param params Params object with the information filled in the form.
     * @param dateFormat Format to parse the dates to.
     */
    static def bindPartnerReferralCommissions(PartnerWS partner, GrailsParameterMap params, String dateFormat) {
        // Get the number of referral commissions that were defined.
        def referralSize = params.list('referrer.referralId')?.size()

        partner.referrerCommissions = new PartnerReferralCommissionWS[referralSize];

        if (referralSize == 1) {
            PartnerReferralCommissionWS referralCommission = new PartnerReferralCommissionWS()

            // Parse the Partner Id.
            referralCommission.referralId = bindPartnerCommissionEntityId(params?.referrer?.referralId)
            
            referralCommission.referrerId = partner.id

            // Parse the dates.
            referralCommission.startDate = bindPartnerCommissionDate(params?.referrer?.startDate, dateFormat)
            referralCommission.endDate = bindPartnerCommissionDate(params?.referrer?.endDate, dateFormat)

            // Parse the percentage.
            referralCommission.percentage = params.referrer?.percentage

            partner.referrerCommissions[0] = referralCommission
        } else if (referralSize > 1) {
            PartnerReferralCommissionWS referralCommission

            (0..(referralSize - 1)).each {
                referralCommission = new PartnerReferralCommissionWS()
                
                referralCommission.referrerId = partner.id

                // Parse the Partner Id.
                referralCommission.referralId = bindPartnerCommissionEntityId(params?.referrer?.referralId[it])

                // Parse the dates.
                referralCommission.startDate = bindPartnerCommissionDate(params?.referrer?.startDate[it], dateFormat)
                referralCommission.endDate = bindPartnerCommissionDate(params?.referrer?.endDate[it], dateFormat)

                // Parse the percentage.
                referralCommission.percentage = params.referrer?.percentage[it]

                partner.referrerCommissions[it] = referralCommission
            }
        }
    }

    /**
     * Parses a date for the Partner Commissions. If an exception is thrown we set them to null so the validator knows it's an error.
     *
     * @param date String of the Date to parse.
     * @param dateFormat String with the format the date should be parsed to.
     * @return a Date object if the parameter was valid. Otherwise, null.
     */
    private static def bindPartnerCommissionDate(String date, String dateFormat) {
        DateTimeFormatter dtf = DateTimeFormat.forPattern(dateFormat)

        try {
            return dtf.parseDateTime(date).toDate()
        } catch (IllegalArgumentException ex) {
            return null
        }
    }

    /**
     * Used for the Partner Commissions to parse the id of an Item or a Partner. If it's invalid we set it to zero so the validator knows it's an error.
     * @param id
     * @return
     */
    private static def bindPartnerCommissionEntityId(String id) {
        try {
            return Integer.parseInt(id)
        } catch (NumberFormatException e) {
            return 0
        }
    }

    static def bindMetaFields(UserWS userWS, Collection<MetaField> metaFields, GrailsParameterMap params) {
        def fieldsArray = MetaFieldBindHelper.bindMetaFields(metaFields, params)
        if (userWS.metaFields && userWS.metaFields.length > 0) {
            def prevFields = userWS.metaFields ? Arrays.asList(userWS.metaFields) : null;
            if (prevFields) fieldsArray.addAll(prevFields)
        }
		userWS.metaFields = fieldsArray.toArray(new MetaFieldValueWS[fieldsArray.size()])
    }

    static def bindMetaFields(UserWS userWS, Map<Integer, Collection<MetaField>> metaFields, GrailsParameterMap params) {
        def fieldsArray = MetaFieldBindHelper.bindMetaFields(metaFields, params)
        if (userWS.metaFields && userWS.metaFields.length > 0) {
            def prevFields = userWS.metaFields ? Arrays.asList(userWS.metaFields) : null;
            if (prevFields) fieldsArray.addAll(prevFields)
        }
        userWS.metaFields = fieldsArray.toArray(new MetaFieldValueWS[fieldsArray.size()])
    }

	static def bindPaymentInformations(UserWS user, Integer modelIndex, GrailsParameterMap params) {
		List<PaymentInformationWS> paymentInstruments = new ArrayList<PaymentInformationWS>(0)
		def errorMessages = null
		for(Integer ind = 0 ; ind <= modelIndex ; ind++) {
			PaymentInformationWS ws = new PaymentInformationWS()
            bindData(ws,params,"paymentMethod_${ind}")
            if(PaymentMethodTypeDTO.get(ws.paymentMethodTypeId)==null) {
                user.setPaymentInstruments(new ArrayList<PaymentInformationWS>(0))
                return
            }
            List<MetaField> metaFieldList = PaymentMethodTypeDTO.get(ws.paymentMethodTypeId).metaFields?.toList()
			bindMetaFields(ws, metaFieldList, params, ind)
            // Use stored value in case of obscure cc.number
            PaymentMethodTemplateDTO paymentMethodTemplate = PaymentMethodTypeDTO.get(ws.paymentMethodTypeId).getPaymentMethodTemplate();
            if (paymentMethodTemplate?.templateName.equals(CommonConstants.PAYMENT_CARD)) {
                ws.metaFields.each { metaField ->
                    if (metaField.fieldName.equals(CommonConstants.METAFIELD_NAME_CC_NUMBER)) {
                        // update credit card only if not obscured
                        if (ws.id != null) {
                            PaymentInformationDTO oldInformation = PaymentInformationDTO.get(ws.id)
                            oldInformation?.metaFields?.each { oldMetaField ->
                                if (oldMetaField.getField()?.name?.equals(CommonConstants.METAFIELD_NAME_CC_NUMBER)) {
                                    if(isCreditNumberUpdated(oldMetaField.getValue(), metaField.getStringValue())){
                                        metaField.setValue(oldMetaField.getValue())
                                    }
                                    return;
                                }
                            }
                        }
                        return;
                    }
                }
            }
            metaFieldList.each { metaField ->
                if(metaField.dataType.equals(DataType.DATE)) {
                    MetaFieldValueWS metaFieldValueWS = ws.metaFields.find { it.fieldName == metaField.name }
                    if(metaFieldValueWS.getValue() == null && StringUtils.isNotEmpty(params.get(ind + "_metaField_" + metaField?.id)?.value)) {
                        errorMessages = metaField.name;
                        return
                    }
                }
            }
            paymentInstruments.add(ws)
		}
		user.setPaymentInstruments(paymentInstruments)
        if(errorMessages) {
            throw new SessionInternalError("Please insert valid date format for " + errorMessages, [
                    "metaField.date.format.errorMessage," + errorMessages
            ] as String[])
        }
	}

    private static boolean isCreditNumberUpdated(String ccNumber, String newCCNumber){
        if(ccNumber == null || newCCNumber == null) return false
        return ccNumber.replaceFirst('^\\d{12}', '************').equals(newCCNumber)
    }

	static def bindMetaFields(PaymentInformationWS paymentWS, Collection<MetaField> metaFields, GrailsParameterMap params, Integer modelIndex) {
		def fieldsArray = MetaFieldBindHelper.bindMetaFields(metaFields, modelIndex, params)
		paymentWS.metaFields = fieldsArray.toArray(new MetaFieldValueWS[fieldsArray.size()])
	}
	
    private static def bindData(Object model, modelParams, String prefix) {
        def args = [ model, modelParams, [exclude:[], include:[]]]
        if (prefix) args << prefix

        new BindDynamicMethod().invoke(model, 'bind', (Object[]) args)
    }

    static def getDisplayName(user, contact) {
        if (contact?.firstName || contact?.lastName) {
            return "${contact.firstName} ${contact.lastName}".trim()

        } else if (contact?.organizationName) {
            return "${contact.organizationName}".trim()
        }

        return user?.userName
    }
}

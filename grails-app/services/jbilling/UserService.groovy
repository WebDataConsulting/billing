/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package jbilling

import com.sapienter.jbilling.client.authentication.AuthenticationUserService
import com.sapienter.jbilling.common.LastPasswordOverrideError
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.security.JBCrypto
import com.sapienter.jbilling.server.user.IUserSessionBean
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDTO
import com.sapienter.jbilling.server.user.db.UserCodeDTO
import com.sapienter.jbilling.server.user.db.UserPasswordDAS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.CriteriaSpecification
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpSession

class UserService implements Serializable {

    static transactional = true

    def messageSource
    AuthenticationUserService jbillingUserService
    IUserSessionBean userSession
    IWebServicesSessionBean webServicesSession

    /**
     * Returns a list of User Codes filtered by simple criteria. The given filterBy parameter will
     * be used match either the user code or external reference. T
     *
     * @param company company
     * @param params parameter map containing filter criteria
     * @return filtered list of products
     */
    def getFilteredUserCodes(GrailsParameterMap params) {
        // default filterBy message used in the UI
        def defaultFilter = messageSource.resolveCode('userCode.filterBy.default', session.locale).format((Object[]) [])

        // apply pagination arguments or not
        def pageArgs = [max: params.max, offset: params.offset,
                sort: (params.sort && params.sort != 'null') ? params.sort : 'id',
                order: (params.order && params.order != 'null') ? params.order : 'desc']

        def userId = params.int("id")

        // filter on identifier, external reference and validity
        def userCodes = UserCodeDTO.createCriteria().list(
                pageArgs
        ) {
            createAlias("user", "user", CriteriaSpecification.LEFT_JOIN)
            and {
                eq("user.id", userId)

                if (params.filterBy && params.filterBy != defaultFilter) {
                    or {
                        ilike('identifier', "%${params.filterBy}%")
                        ilike('externalReference', "%${params.filterBy}%")
                    }
                }
                if (params.active) {
                    or {
                        isNull('validTo')
                        gt('validTo', new Date())
                    }
                } else {
                    le('validTo', new Date())
                }
            }
        }

        return userCodes
    }

    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    def HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

    def updatePassword (ResetPasswordCodeDTO resetCode, String newPassword) {
        UserWS userWS = webServicesSession.getUserWS(resetCode.user.id)
        //encode the password
        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(userWS.mainRoleId)
		String newPasswordEncoded = JBCrypto.encodePassword(passwordEncoderId, newPassword)
        //compare current password with last six
        List<String> passwords = new UserPasswordDAS().findLastSixPasswords(resetCode.user, newPasswordEncoded)
        for(String password: passwords) {
            if(JBCrypto.passwordsMatch(passwordEncoderId, password, newPassword)) {
				LastPasswordOverrideError lastEx = new LastPasswordOverrideError("Password is similar to one of the last six passwords. Please enter a unique Password.");
				
				throw lastEx;
            }
        }
        // do the actual password change
        jbillingUserService.saveUser(userWS.getUserName(),userWS.entityId, newPasswordEncoded, passwordEncoderId)
		userSession.deletePasswordCode(resetCode);
    }
}

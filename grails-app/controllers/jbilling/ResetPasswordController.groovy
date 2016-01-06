package jbilling

import com.sapienter.jbilling.client.authentication.JBillingPasswordEncoder
import com.sapienter.jbilling.common.LastPasswordOverrideError;
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.util.credentials.PasswordService

import org.joda.time.DateMidnight
import org.joda.time.DateTime

import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.security.JBCrypto
import com.sapienter.jbilling.server.user.IUserSessionBean
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserDTOEx
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDAS
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDTO
import com.sapienter.jbilling.server.user.db.UserDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.db.UserPasswordDAS
import com.sapienter.jbilling.server.user.db.UserPasswordDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.IWebServicesSessionBean

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.apache.commons.lang.StringUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.RandomStringUtils
import org.hibernate.ObjectNotFoundException
import org.joda.time.Days
import org.joda.time.Duration
import org.joda.time.Hours

import javax.validation.ConstraintViolationException

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.RequestAttributes

class ResetPasswordController {

    IWebServicesSessionBean webServicesSession
    def userService

    def index (){
        //set default company
        if(!params.company){
            def list = CompanyDTO.list();
            params.company = null != list && list.size() > 0 ? list.get(0).getId() : null;
        }

        [publicKey: Util.getSysProp('recaptcha.public.key'),
         captchaEnabled: Boolean.parseBoolean(Util.getSysProp('forgot.password.captcha')),
         useEmail: doUseEmail(params.int('company'))
        ]
    }

    def captcha (){
        boolean result
        Boolean captchaEnabled = Boolean.parseBoolean(Util.getSysProp('forgot.password.captcha'))
        if (captchaEnabled) {
            try{
                def remoteAddress = request.remoteAddr
                def http = new HTTPBuilder('http://www.google.com')
                http.request(Method.POST, ContentType.TEXT) {
                    uri.path = '/recaptcha/api/verify'
                    uri.query = [privatekey: Util.getSysProp('recaptcha.private.key'),
                            remoteip: remoteAddress,
                            challenge: params.recaptcha_challenge_field,
                            response: params.recaptcha_response_field]

                    response.success = { resp, value ->
                        StringWriter writer = new StringWriter();
                        IOUtils.copy(value, writer);
                        result = Boolean.parseBoolean((writer?.toString()?.split("\n") as List)[0])
                    }
                }
            }catch(Exception e){
                flash.error = message(code: 'forgotPassword.captcha.error')
                forward action: 'index'
                return
            }
        } else {
            result = true
        }

        if (result) {
            //find user
            UserDTO user

            boolean useEmail = doUseEmail(params.int('company'))
			
			if(StringUtils.isEmpty(params.userName.trim()) && !useEmail) {
				flash.error = message(code: 'forgotpassword.user.username.not.blank')
				forward action: 'index'
				return
			}
			
            if (useEmail){
                user = new UserBL().findUsersByEmail(params.email, params.int('company')).get(0)
            }else{
                user = new UserDAS().findByUserName(params.userName, params.int('company'))
            }

            if (!user?.id) {
                flash.error = (useEmail) ? message(code: 'forgotPassword.user.email.not.found', args: [useEmail]) : message(code: 'forgotPassword.user.username.not.exist', args: [params.userName])
                forward action: 'index'
                return
            }


            try{
                //send email to reset password
                webServicesSession.resetPassword(user.userId)
            } catch (SessionInternalError e) {
                flash.error = message(code: 'forgotPassword.notification.not.found')
                forward action: 'index'
                return
            }
        } else {
            flash.error = message(code: 'forgotPassword.captcha.wrong')
            forward action: 'index'
            return
        }
        flash.message = message(code: 'forgotPassword.email.sent')
        forward controller:'login', action: 'auth'
    }

    def changePassword (){

        ResetPasswordCodeDAS resetCodeDAS = new ResetPasswordCodeDAS()

        try{
            ResetPasswordCodeDTO resetCode =  resetCodeDAS.find(params.token)

            DateTime dateResetCode = new DateTime(resetCode.getDateCreated())
            DateTime today = DateTime.now()
            Duration duration = new Duration(dateResetCode, today)
            Long minutesDifference = duration.getStandardMinutes()
            Long expirationMinutes = PreferenceBL.getPreferenceValueAsIntegerOrZero(resetCode.user.company.id,
                    CommonConstants.PREFERENCE_FORGOT_PASSWORD_EXPIRATION).longValue() * 60
            if (!resetCode || minutesDifference > expirationMinutes){
                flash.error= message(code: 'forgotPassword.expired.token')
                forward controller:'login', action: 'auth'
            } else {
                render view: 'changePassword', model: [ token: params.token ]
            }

        }catch(ObjectNotFoundException e){
            flash.error= message(code: 'forgotPassword.invalid.token')
            forward controller:'login', action: 'auth'
        }
    }

    def updatePassword () {
        ResetPasswordCodeDAS resetCodeDAS = new ResetPasswordCodeDAS()
        try{
            String newPassword = params.newPassword.trim()
            String confirmedNewPassword = params.confirmedNewPassword.trim()

            // password validation
            if (newPassword.length() == 0 || confirmedNewPassword.length() == 0 ) {
                flash.error = message(code: 'password.required')
                render view: 'changePassword', model: [ token: params.token ]
                return
            }

            if (newPassword.length() != confirmedNewPassword.length() || !newPassword.equals(confirmedNewPassword) ) {
                flash.error = message(code: 'passwords.dont.match')
                render view: 'changePassword', model: [ token: params.token ]
                return
            }

            if (newPassword.length() < 8 || newPassword.length() > 40 || !newPassword.matches(Constants.PASSWORD_PATTERN_4_UNIQUE_CLASSES) ) {
                flash.error = message(code: 'validation.error.password.size')
                flash.args = [ 8, 40]
                render view: 'changePassword', model: [ token: params.token ]
                return
            }
            ResetPasswordCodeDTO resetCode =  resetCodeDAS.find(params.token)
            userService.updatePassword(resetCode, newPassword)
            flash.message = message(code: 'forgotPassword.success')
            forward controller:'login', action: 'auth'

        } catch(ObjectNotFoundException e){
            flash.error= message(code: 'validation.error.password.object.not.found')
            redirect controller:'login', action: 'auth'
        }
		  catch(LastPasswordOverrideError passEx){
			  flash.error = message(code: 'forgotPassword.user.password.last.six.unique')
			  forward controller:'resetPassword', action: 'changePassword'
		  } 
		catch(Exception ex) {
			log.error("Exception occurred during reset password." + ex)
            flash.error= message(code: 'forgotPassword.failure')
            forward controller:'login', action: 'auth'
        }
    }

    private boolean doUseEmail(Integer companyId){
        def uniqueEmails = PreferenceBL.getPreferenceValueAsIntegerOrZero(
        					companyId, CommonConstants.PREFERENCE_FORCE_UNIQUE_EMAILS)
        (uniqueEmails == 1) ? true : false
    }

    private String generateLink(String action, linkParams) {
        createLink(base: "$request.scheme://$request.serverName:$request.serverPort$request.contextPath",
                controller: 'resetPassword', action: action,
                params: linkParams)

    }

    def resetExpiryPassword () {
        //set default company
		def request= RequestContextHolder?.currentRequestAttributes()
		def forwordEntityId = request.getAttribute("login_company",RequestAttributes.SCOPE_SESSION)
        [publicKey: Util.getSysProp('recaptcha.public.key'),
         captchaEnabled: Boolean.parseBoolean(Util.getSysProp('forgot.password.captcha')),
         forwordEntityId:forwordEntityId as Integer
        ]}

    def resetPassword () {
        boolean result
        def remoteAddress = request.remoteAddr
        def body = [privatekey: Util.getSysProp('recaptcha.private.key'),
                    remoteip: remoteAddress,
                    challenge: params.recaptcha_challenge_field,
                    response: params.recaptcha_response_field]
        def http = new HTTPBuilder('https://www.google.com')
        http.request(Method.POST, ContentType.TEXT) {
            uri.path = '/recaptcha/api/verify'
            uri.query = body
            body = body

            response.success = { resp, value ->
                StringWriter writer = new StringWriter();
                IOUtils.copy(value, writer);
                result = Boolean.parseBoolean((writer?.toString()?.split("\n") as List)[0])
            }
        }
        String newPassword = params.newPassword
        String oldPassword = params.oldPassword
        if (result) {
            //find user
            UserDTO user = new UserDAS().findByUserName(params.username, params.int('company'))
			if(!user) {
				flash.error = message(code: 'forgotPassword.user.username.not.exist', args: [params?.username] )
				forward action: 'resetExpiryPassword'
				return
			}
            UserWS userWS = webServicesSession.getUserWS(user.getId())
            Integer methodId = JBCrypto.getPasswordEncoderId(userWS.getMainRoleId());
            if (!user?.id || !JBCrypto.passwordsMatch(methodId, user.password, oldPassword)) {
                flash.error = message(code: 'forgotPassword.user.username.not.found')
                forward action: 'resetExpiryPassword'
                return
            }
            if(!newPassword.equals(params.confirmPassword)){
                flash.error = message(code: 'forgotPassword.user.password.not.match')
                forward action: 'resetExpiryPassword'
                return
            }
            if ( !newPassword.matches(com.sapienter.jbilling.server.util.Constants.PASSWORD_PATTERN_4_UNIQUE_CLASSES) ) {
                flash.error = message(code: 'validation.error.password.size', args: [8,40])
                forward action: 'resetExpiryPassword'
                return
            }

            UserPasswordDAS resetCodeDAS = new UserPasswordDAS()
            IUserSessionBean myRemoteSession = (IUserSessionBean) Context.getBean(
                    Context.Name.USER_SESSION)

            // do the actual password change
            UserDTOEx userDTOEx = new UserDTOEx(userWS, user?.entity?.id);
            List<String> passwords = resetCodeDAS.findLastSixPasswords(user,newPassword)
            Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(userDTOEx.mainRoleId);
            for(String password: passwords){
                if(JBCrypto.passwordsMatch(passwordEncoderId, user.getPassword(), newPassword)){
                    flash.error = message(code: 'forgotPassword.user.password.last.six.unique')
                    forward action: 'resetExpiryPassword' 
                    return
                }
            }
            try {
                //create a new password code
                UserPasswordDTO resetCode = new UserPasswordDTO();
                resetCode.setUser(user);
                resetCode.setDateCreated(new Date());
                resetCode.setPassword(newPassword,userDTOEx.mainRoleId);
                resetCode.setPassword(newPassword)
                resetCodeDAS.save(resetCode);

                userDTOEx.setPassword(newPassword);
                myRemoteSession.update(userDTOEx.id, userDTOEx)

                flash.message = message(code: 'forgotPassword.success')
                forward controller:'login', action: 'auth'
            }catch (ConstraintViolationException e){
                flash.error = message(code: 'validation.error.password.size')
                flash.args = [ 8,40]
                log.debug(e.getMessage())
                forward action: 'resetExpiryPassword'
            }


        }else{
            flash.error = message(code: 'forgotPassword.captcha.wrong')
            forward action: 'resetExpiryPassword'
        }
    }

}

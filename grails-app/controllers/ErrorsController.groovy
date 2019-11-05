import com.sapienter.jbilling.common.SessionInternalError

/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

/**
 * Handles all 404 and 500 errors
 *
 * Mapped to "500" and "404", see UrlMappings.groovy
 *
 * @author Vojislav Stanojevikj
 * @since  30-04-2015
 */

class ErrorsController {

    static scope = 'singleton'

    def handleErrors() {

        def errorMessage = null
        def exception = request['exception']
        if(exception){
            log.error(String.format("Unhandled error occurred in %s!!!\nCause :%s", exception.className, exception.cause))
            if(exception instanceof SessionInternalError || exception.cause instanceof SessionInternalError){
                errorMessage = exception.message
            }
        }

        def ajaxError = params['ajaxError']
        if(ajaxError){
            def logError = "Ajax error occurred!!"
            def errorThrown = params['errorThrown']
            if(errorThrown){
                logError += "Error Thrown : " + errorThrown
            }
            log.error(logError)
        }

        render view: '/error', model: [ajaxError: ajaxError, exception: exception, errorMessage: errorMessage]
    }

    def pageNotFound() {
        log.error(message(code: 'flash.exception.message.title.page.not.found'))
        render view: '/pageNotFound'
    }
}

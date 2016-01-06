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


import com.mchange.v2.c3p0.ComboPooledDataSource
import grails.util.Holders
import org.springframework.jdbc.core.JdbcTemplate

beans = {

    /*
        Database configuration
     */
    dataSource(ComboPooledDataSource) { bean ->
        bean.destroyMethod = 'close'

        // database connection properties from DataSource.groovy
        user = Holders.getConfig().dataSource.username
        password =  Holders.getConfig().dataSource.password
        driverClass =  Holders.getConfig().dataSource.driverClassName
        jdbcUrl =  Holders.getConfig().dataSource.url

        // Connection pooling using c3p0
        acquireIncrement = 2
        initialPoolSize = 10
        minPoolSize = 10
        maxPoolSize = 50
        maxIdleTime = 300
        checkoutTimeout = 10000

        /*
           Periodically test the state of idle connections and validate connections on checkout. Handles
           potential timeouts by the database server. Increase the connection idle test period if you
           have intermittent database connection issues.
         */
        testConnectionOnCheckout = false
        idleConnectionTestPeriod = 30        
		preferredTestQuery = "/* ping */ SELECT 1"
		
        /*
           Destroy un-returned connections after a period of time (in seconds) and throw an exception
           that shows who is still holding the un-returned connection. Useful for debugging connection
           leaks.
         */
        // unreturnedConnectionTimeout = 10
        // debugUnreturnedConnectionStackTraces = true
    }

    jdbcTemplate(JdbcTemplate) {
        dataSource = ref('dataSource')
    }

    /*
        Custom data binding and property parsing rules
     */
    customPropertyEditorRegistrar(com.sapienter.jbilling.client.editor.CustomPropertyEditorRegistrar) {
        messageSource = ref('messageSource')
    }

    /*
        Spring security
     */
    // populates session attributes and locale from the authenticated user
    securitySession(com.sapienter.jbilling.client.authentication.util.SecuritySession) {
        localeResolver = ref('localeResolver')
    }

    // normal username / password authentication
    authenticationProcessingFilter(com.sapienter.jbilling.client.authentication.CompanyUserAuthenticationFilter) {
        authenticationManager = ref("authenticationManager")        
        authenticationSuccessHandler = ref('authenticationSuccessHandler')
        authenticationFailureHandler = ref('authenticationFailureHandler')
        rememberMeServices = ref('rememberMeServices')
        securitySession = ref('securitySession')
    }

	// remember me cookie authentication
	rememberMeAuthenticationFilter(com.sapienter.jbilling.client.authentication.CompanyUserRememberMeFilter) {
		authenticationManager = ref('authenticationManager')
		rememberMeServices = ref('rememberMeServices')
		securitySession = ref('securitySession')
	}
	
    /*
        Automatic authentication using a defined username and password that removes the need for the caller
        to authenticate themselves. This is used with web-service protocols that don't support authentication,
        but can also be used to create "pre-authenticated" URLS by updating the filter chain in 'Config.groovy'.
     */
    staticAuthenticationProcessingFilter(com.sapienter.jbilling.client.authentication.StaticAuthenticationFilter) {
        authenticationManager = ref("authenticationManager")
        authenticationDetailsSource = ref('authenticationDetailsSource')
        username = "admin;1"
        password = "123qwe"
    }

    
    /*
        Stateless SecurityContextPersistenceFilter disables creation of a session for the requests. It is used for the API calls.
        To use it, updating the filter chain in 'Config.groovy' is necessary.
     */
    statelessHttpSessionSecurityContextRepository(org.springframework.security.web.context.HttpSessionSecurityContextRepository){
        allowSessionCreation = false;
    }

    statelessSecurityContextPersistenceFilter(org.springframework.security.web.context.SecurityContextPersistenceFilter){
        securityContextRepository = ref("statelessHttpSessionSecurityContextRepository")
    }
	
	jbillingUserService(com.sapienter.jbilling.client.authentication.AuthenticationUserService) {
	    userSession = ref("userSession")
	}

    userDetailsService(com.sapienter.jbilling.client.authentication.CompanyUserDetailsService) {
        springSecurityService = ref("springSecurityService")
    }

    //used as password encoder router to get to the real password encoder that will do the job
    passwordEncoder(com.sapienter.jbilling.client.authentication.JBillingPasswordEncoder){
		userService = ref("jbillingUserService")
	}

    plainTextPasswordEncoder(org.springframework.security.authentication.encoding.PlaintextPasswordEncoder)
    md5PasswordEncoder(org.springframework.security.authentication.encoding.Md5PasswordEncoder)
    sha1PasswordEncoder(org.springframework.security.authentication.encoding.ShaPasswordEncoder, 1)
    sha256PasswordEncoder(org.springframework.security.authentication.encoding.ShaPasswordEncoder, 256)
    bCryptPasswordEncoder(grails.plugin.springsecurity.authentication.encoding.BCryptPasswordEncoder, 10)
	
	saltSource(com.sapienter.jbilling.client.authentication.JBillingSaltSource)

    webExpressionVoter(com.sapienter.jbilling.client.authentication.SafeWebExpressionVoter) {
        expressionHandler = ref("webExpressionHandler")
    }
	
    appAuthResultHandler(com.sapienter.jbilling.client.authentication.AuthenticationResultHandler) {
        userSession = ref("userSession")
    }

    /*
        Remoting
     */
    // HTTP request handler for remote beans
    httpRequestAdapter (org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter)


    /*
        Others
     */
    // resolves exceptions into messages for the view
    viewUtils(com.sapienter.jbilling.client.ViewUtils) {
        messageSource = ref("messageSource")
    }

    // bean for managing the scheduled background processes
    schedulerBootstrapHelper(com.sapienter.jbilling.server.util.SchedulerBootstrapHelper) { bean ->
        bean.singleton = true
    }
	
	// encryption
	dataEncrypter ( com.sapienter.jbilling.server.util.PlainDataEncrypter )
	
    /*
        Debugging
     */
    //The listener will print all the old and new values for a object before it gets updated by hibernate.
    /*
    debugHibernateListener(HibernateEventListener)

    hibernateEventListeners(HibernateEventListeners) {
        listenerMap = [ 'pre-update': debugHibernateListener ]
    }
    */
}

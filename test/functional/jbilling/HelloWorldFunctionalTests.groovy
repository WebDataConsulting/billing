package jbilling

//import com.grailsrocks.functionaltest.*
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

class HelloWorldFunctionalTests { // extends BrowserTestCase {
    void testSomeWebsiteFeature() {
        // Here call get(uri) or post(uri) to start the session
        // and then use the custom assertXXXX calls etc to check the response
        //
        // get('/something')
        // assertStatus 200
        // assertContentContains 'the expected text'
    }
	
	
	void test01Login() {
		get('/jbilling/login/auth')
		assertStatus 200
		assertContentContains "login"
		
		/*JbillingAPI api = JbillingAPIFactory.getAPI();
		Integer userId = api.getUserId("john")
		assertNotNull("customer created", userId);*/
				
	}
	
	/*void test02CreateCustomer() {
		get('/jbilling/customer/edit?user.userName=john&oldPassword=123qwe&newPassword=123qwe&contact-2.email=test@test.com&contact-2.countryCode=IN&user.invoiceDeliveryMethodId=1&user.balanceType=1&mainSubscription.nextInvoiceDayOfPeriod=1&mainSubscription.periodId=2')
		JbillingAPI api = JbillingAPIFactory.getAPI();
		Integer userId = api.getUserId("john")
		assertNotNull("customer created", userId);
				
	}*/
}

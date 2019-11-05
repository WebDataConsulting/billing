package in.webdata.ws

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.TestCase;
import spock.lang.Specification

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.provisioning.IProvisioningProcessSessionBean
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

public class ProvisioningTestSpec extends Specification {

	private static final int           ORDER_LINES_COUNT  = 6;

	private static final int           USER_ID            = 1000;

	private static Integer[]           itemIds            = [1, 2, 3, 24, 240, 14];

	private Integer[]           provisioningStatus = new Integer[6];

	private static IProvisioningProcessSessionBean remoteProvisioning = null;

	JbillingAPI   api = JbillingAPIFactory.getAPI();

	def "FirstMethod"() {

		//super.setUp();

		setup:

		api = JbillingAPIFactory.getAPI();

		remoteProvisioning = (IProvisioningProcessSessionBean)RemoteContext.getBean(RemoteContext.Name.PROVISIONING_PROCESS_REMOTE_SESSION);
	}

	def "pause"(long t) {

		//setup:
		Thread.sleep(t);
	}

	def "testNewQuantityEvent"() {

		setup:

		provisioningStatus[0] = Constants.PROVISIONING_STATUS_ACTIVE;
		provisioningStatus[1] = Constants.PROVISIONING_STATUS_INACTIVE;
		provisioningStatus[2] = null;
		provisioningStatus[3] = Constants.PROVISIONING_STATUS_PENDING_ACTIVE;
		provisioningStatus[4] = Constants.PROVISIONING_STATUS_PENDING_INACTIVE;
		provisioningStatus[5] = null;

		OrderWS newOrder = createMockOrder(USER_ID, ORDER_LINES_COUNT, new BigDecimal("77"));

		newOrder.setActiveSince(null);

		// create order through api
		Integer ret = api.createOrder(newOrder);

		System.out.println("Created order." + ret);

		expect:

		null		!=		 ret;

		when:

		System.out.println("running provisioning batch process..");
		//pause(2000);

		remoteProvisioning.trigger();

		pause(2000);

		System.out.println("Getting back order " + ret);

		OrderWS retOrder = api.getOrder(ret);

		System.out.println("got order: " + retOrder);

		OrderLineWS[] retLine = retOrder.getOrderLines();

		then:

		println("Still not Printing anything here.");

		for (int i = 0; i < retLine.length; i++) {

			when:
			i == 0

			then:
			retLine[i].getProvisioningStatusId()		==    Constants.PROVISIONING_STATUS_ACTIVE;


			when:
			i 	== 	1

			then:
			retLine[i].getProvisioningStatusId()		==    Constants.PROVISIONING_STATUS_ACTIVE;


			when:
			i == 2

			then:
			retLine[i].getProvisioningStatusId()		==    Constants.PROVISIONING_STATUS_INACTIVE;


			when:
			i == 3

			then:
			retLine[i].getProvisioningStatusId()		==    Constants.PROVISIONING_STATUS_PENDING_ACTIVE;


			when:
			i == 4

			then:
			retLine[i].getProvisioningStatusId()		==    Constants.PROVISIONING_STATUS_PENDING_INACTIVE;


			when:
			i == 5

			then:
			retLine[i].getProvisioningStatusId()		==    Constants.PROVISIONING_STATUS_INACTIVE;

		}
	}



	def	"testSubscriptionActiveEvent"() {

		setup:

		provisioningStatus[0] = Constants.PROVISIONING_STATUS_ACTIVE;

		provisioningStatus[1] = Constants.PROVISIONING_STATUS_INACTIVE;

		provisioningStatus[2] = null;

		provisioningStatus[3] = Constants.PROVISIONING_STATUS_PENDING_ACTIVE;

		provisioningStatus[4] = Constants.PROVISIONING_STATUS_PENDING_INACTIVE;

		provisioningStatus[5] = null;

		OrderWS newOrder = createMockOrder(USER_ID, ORDER_LINES_COUNT, new BigDecimal("77"));

		// newOrder.setActiveSince(weeksFromToday(1));
		Calendar cal = Calendar.getInstance();

		cal.clear();
		cal.set(2008, 9, 29, 0, 0, 0);
		newOrder.setActiveSince(cal.getTime());

		// create order through api
		Integer ret = api.createOrder(newOrder);

		System.out.println("Created order." + ret);

		expect:

		null		!=		 ret;

		when:

		System.out.println("running provisioning batch process..");
		pause(2000);

		remoteProvisioning.trigger();

		pause(2000);

		System.out.println("Getting back order " + ret);

		OrderWS retOrder = api.getOrder(ret);

		System.out.println("got order: " + retOrder);

		OrderLineWS[] retLine = retOrder.getOrderLines();

		then:

		for (int i = 0; i < retLine.length; i++) {

			when:
			i == 0

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_ACTIVE;


			when:
			i == 1

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_ACTIVE;


			when:
			i == 2

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_INACTIVE;


			when:
			i == 3

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_PENDING_ACTIVE;


			when:
			i == 4

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_PENDING_INACTIVE;


			when:
			i == 5

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_INACTIVE;

		}
	}


	def "testSubscriptionInActiveEvent"() {

		setup:

		provisioningStatus[0] = Constants.PROVISIONING_STATUS_INACTIVE;
		provisioningStatus[1] = Constants.PROVISIONING_STATUS_ACTIVE;
		provisioningStatus[2] = null;
		provisioningStatus[3] = Constants.PROVISIONING_STATUS_PENDING_ACTIVE;
		provisioningStatus[4] = Constants.PROVISIONING_STATUS_PENDING_INACTIVE;
		provisioningStatus[5] = null;

		OrderWS newOrder = createMockOrder(USER_ID, ORDER_LINES_COUNT, new BigDecimal("77"));

		// newOrder.setActiveSince(weeksFromToday(1));
		Calendar cal = Calendar.getInstance();

		cal.clear();
		cal.set(2008, 9, 29, 0, 0, 0);
		newOrder.setActiveUntil(cal.getTime());

		// create order through api
		Integer ret = api.createOrder(newOrder);

		System.out.println("Created order." + ret);

		expect:

		null			!=			ret;


		when:
		System.out.println("running provisioning batch process..");
		//pause(2000);
		remoteProvisioning.trigger();
		pause(2000);
		System.out.println("Getting back order " + ret);

		OrderWS retOrder = api.getOrder(ret);

		System.out.println("got order: " + retOrder);

		OrderLineWS[] retLine = retOrder.getOrderLines();

		then:

		for (int i = 0; i < retLine.length; i++) {

			when:
			i == 0

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_INACTIVE;

			when:
			i == 1
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_INACTIVE;

			when:
			i == 2

			then:
			retLine[i].getProvisioningStatusId()		==		       Constants.PROVISIONING_STATUS_INACTIVE;


			when:
			i == 3

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_PENDING_ACTIVE;


			when:
			i == 4

			then:
			retLine[i].getProvisioningStatusId()		==		Constants.PROVISIONING_STATUS_PENDING_INACTIVE;

			when:
			i == 5

			then:
			retLine[i].getProvisioningStatusId()		==		    Constants.PROVISIONING_STATUS_INACTIVE;


		}
	}

	def OrderWS "createMockOrder"(int userId, int orderLinesCount, BigDecimal linePrice) {
		OrderWS order = new OrderWS();

		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1);    // once
		order.setCurrencyId(1);

		ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(orderLinesCount);

		for (int i = 0; i < orderLinesCount; i++) {
			OrderLineWS nextLine = new OrderLineWS();

			nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			nextLine.setDescription("Order line: " + i);
			nextLine.setItemId(itemIds[i]);
			nextLine.setQuantity(1);
			nextLine.setPrice(linePrice);
			nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));
			nextLine.setProvisioningStatusId(provisioningStatus[i]);
			lines.add(nextLine);
		}

		order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));

		return order;
	}

	/*private Date weeksFromToday(int weekNumber) {
	 Calendar calendar = new GregorianCalendar();
	 calendar.setTimeInMillis(System.currentTimeMillis());
	 calendar.add(Calendar.WEEK_OF_YEAR, weekNumber);
	 return calendar.getTime();
	 }*/

	def "testExternalProvisioning"() {

		setup:

		OrderWS order = new OrderWS();
		order.setUserId(USER_ID);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1);
		order.setCurrencyId(1);

		OrderLineWS line = new OrderLineWS();
		line.setItemId(251);
		line.setQuantity(1); // trigger 'external_provisioning_test' rule
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setUseItem(true);
		line.setProvisioningStatusId(Constants.PROVISIONING_STATUS_INACTIVE);


		def ar = new OrderLineWS[1];
		ar[0]  = line;
		order.setOrderLines(ar);

		System.out.println("Creating order ...");
		Integer ret = api.createOrder(order);

		expect:

		null		!=		 ret;

		when:
		pause(4000); // wait for MDBs to complete
		System.out.println("Getting back order " + ret);

		// check TestExternalProvisioningMDB was successful
		OrderWS retOrder = api.getOrder(ret);
		OrderLineWS orderLine = retOrder.getOrderLines()[0];

		then:

		5					==		orderLine.getProvisioningStatusId();
		

	}

	def "testCAIProvisioning"() {

		setup:


		OrderWS order = new OrderWS();
		order.setUserId(USER_ID);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1);
		order.setCurrencyId(1);

		OrderLineWS line = new OrderLineWS();
		line.setItemId(251);
		line.setQuantity(2);
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setUseItem(true);
		line.setProvisioningStatusId(Constants.PROVISIONING_STATUS_INACTIVE);

		def ar = new OrderLineWS[1];
		ar[0]  = line;
		order.setOrderLines(ar);

		System.out.println("Creating order ...");
		Integer ret = api.createOrder(order);


		expect:

		null		!=		 ret;

		when:

		pause(2000); // wait for MDBs to complete

		System.out.println("Getting back order " + ret);

		// check TestExternalProvisioningMDB was successful
		OrderWS retOrder = api.getOrder(ret);
		OrderLineWS orderLine = retOrder.getOrderLines()[0];

		then:

		Constants.PROVISIONING_STATUS_ACTIVE		==		orderLine.getProvisioningStatusId();


	}

	def "testMMSCProvisioning"() {

		// create the order
		OrderWS order = new OrderWS();
		order.setUserId(USER_ID);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(1);
		order.setCurrencyId(1);

		OrderLineWS line = new OrderLineWS();
		line.setItemId(251);
		line.setQuantity(3); // trigger 'mmsc_test' rule
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setUseItem(true);
		line.setProvisioningStatusId(Constants.PROVISIONING_STATUS_INACTIVE);

		def ar = new OrderLineWS[1];
		ar[0]  = line;
		order.setOrderLines(ar);

		order.setOrderLines(ar);

		System.out.println("Creating order ...");
		Integer ret = api.createOrder(order);

		expect:

		null			!=		 ret;

		when:

		pause(2000); // wait for MDBs to complete

		System.out.println("Getting back order " + ret);

		// check TestExternalProvisioningMDB was successful
		OrderWS retOrder = api.getOrder(ret);
		OrderLineWS orderLine = retOrder.getOrderLines()[0];

		then:

		Constants.PROVISIONING_STATUS_ACTIVE			==        orderLine.getProvisioningStatusId();

	}
}

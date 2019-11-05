package in.webdata.integration

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;
import spock.lang.Specification

import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.user.IUserSessionBean;
import com.sapienter.jbilling.server.user.partner.db.Partner;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayout;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.RemoteContext;

public class PartnerTestSpec extends Specification {

	def testPartnerGeneral() {

		when:

		IUserSessionBean session = (IUserSessionBean) RemoteContext.getBean(
				RemoteContext.Name.USER_REMOTE_SESSION);
		Partner partner = null;

		Calendar cal = Calendar.getInstance();
		cal.clear();


		cal.set(2009, Calendar.MARCH, 15);
		session.processPayouts(cal.getTime());

		// partner 1
		partner = session.getPartnerDTO(new Integer(10));

		// no payouts

		then:

		1		==		 partner.getPartnerPayouts().size();

		cal.set(2009, Calendar.APRIL, 1);

		partner.getNextPayoutDate().getTime()		==		 cal.getTime().getTime();

		// partner 2

		when:

		partner = session.getPartnerDTO(new Integer(11));

		// no payouts, this guy doens't get paid in the batch
		then:

		0		==		 partner.getPartnerPayouts().size();
		//
		//		// still she should get paid
		//		// note: value should come from the ranged commission
		//
		new BigDecimal("2.2999999523")		==		 partner.getDuePayout();

		cal.set(2009, Calendar.MARCH, 1);

		partner.getNextPayoutDate().getTime()		==		 cal.getTime().getTime();


		// partner 3
		//
		when:

		partner = session.getPartnerDTO(new Integer(12));

		// a new payout
		Set<PartnerPayout> payouts = partner.getPartnerPayouts();

		then:
		1		==		 payouts.size();

		Iterator<PartnerPayout> payoutsIter = payouts.iterator();

		PartnerPayout payout = payoutsIter.next();

		null		!=		 payout;

		PaymentDTO payment = payout.getPayment();

		null		!=		 payment;

		new BigDecimal("2.5")		==		 payment.getAmount();
		Constants.RESULT_OK		==		 payment.getResultId();
		BigDecimal.ZERO		==		 partner.getDuePayout();

		cal.set(2009, Calendar.MARCH, 25);
		partner.getNextPayoutDate().getTime()		==		 cal.getTime().getTime();

		/*
		 * second run
		 */
		when:
		// partner 2
		when:

		partner = session.getPartnerDTO(new Integer(11));

		// no payouts, this guy doens't get paid in the batch

		then:

		0		==		 partner.getPartnerPayouts().size()	;

		// still she should get paid
		new BigDecimal("2.2999999523")		==		 partner.getDuePayout();
		cal.set(2009, Calendar.MARCH, 1);
		partner.getNextPayoutDate().getTime()		==		 cal.getTime().getTime();

		// partner 3

		when:
		partner = session.getPartnerDTO(new Integer(12));

		// a new payout
		payouts = partner.getPartnerPayouts();

		then:

		1		==		 payouts.size();

		when:

		payoutsIter = payouts.iterator();
		payout = payoutsIter.next();

		// make sure we have the lastest payout
		PartnerPayout payout2 = payoutsIter.next();

		then:

		println("Object of PartnerPayout created sucessfully.")

		when:

		if(payout2.getId() > payout.getId()) {
			payout = payout2;
		}

		payment = payout.getPayment();


		then:

		null	!=		 payment;
		BigDecimal.ZERO		==		 payment.getAmount();
		BigDecimal.ZERO		==		 partner.getDuePayout();

		cal.set(2009, Calendar.MARCH, 25);
		cal.add(Calendar.DATE, 10);
		partner.getNextPayoutDate().getTime()		==		 cal.getTime().getTime();

	}
}

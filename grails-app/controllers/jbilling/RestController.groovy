package jbilling

import com.sapienter.jbilling.server.payment.PaymentInformationBL
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.UserDTOEx
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import org.joda.time.format.DateTimeFormat


class RestController {
	
	static scope = "prototype"
	
    def dataBinderService
    IWebServicesSessionBean webServicesSession
	
    def index() { }

    def createUser () {

        //extract xml from request
        def xml = request.XML
       // def List userProperties=['userId','userName','password','languageId','mainRoleId','statusId','currencyId','balanceType']
        try{
            // def List user=dataBinderService.bindAllXmlData(UserWS.class,xml,userProperties)
            //def markup
            UserWS newUser = new UserWS()
            if(xml.user.userId.size() ==1)
            newUser.setUserId(Integer.parseInt(xml.user.userId.text())) // it is validated
            if(xml.user.userName.size() ==1)
            newUser.setUserName(xml.user?.userName.text())
            if(xml.user.password.size() ==1)
            newUser.setPassword(xml.user?.password.text())
            if(xml.user.languageId.size() ==1)
            newUser.setLanguageId(Integer.parseInt(xml.user.languageId.text()))
            if(xml.user.mainRoleId.size() ==1)
            newUser.setMainRoleId(Integer.parseInt(xml.user.mainRoleId.text()))
            //newUser.setParentId(xml.user?.parentId); // this parent exists
            if(xml.user.statusId.size() ==1)
            newUser.setStatusId(Integer.parseInt(xml.user.statusId.text()))
            else
                newUser.setStatusId(UserDTOEx.STATUS_ACTIVE)
            if(xml.user.currencyId.size() ==1)
            newUser.setCurrencyId(Integer.parseInt(xml.user.statusId.text()))
            //newUser.setInvoiceChild(new Boolean(false));
            // add a contact
            if (xml.user.contact.size() ==1) {
            ContactWS contact = new ContactWS();
            if(xml.user.contact.email.size() ==1)
            contact.setEmail(xml.user?.contact?.email.text());
            if(xml.user.contact.firstName.size() ==1)
            contact.setFirstName(xml.user?.contact?.firstName.text());
            if(xml.user.contact.lastName.size() ==1)
            contact.setLastName(xml.user?.contact?.lastName.text());
            newUser.setContact(contact);
            }
            // add a credit card
           if (xml.user.creditCard.size() == 1) {
			PaymentInformationBL bl = new PaymentInformationBL()
			CompanyDTO company = new CompanyDAS().find(session['company_id'])
			
			if(xml.user.creditCard.number.size() == 1)
			String cardNumber = xml.user?.creditCard?.number.text()
			
			PaymentInformationDTO cc = bl.getCreditCardObject(cardNumber, company);
            bl.updateStringMetaField(cc, cardNumber, MetaFieldType.PAYMENT_CARD_NUMBER)
            if(xml.user.creditCard.name.size() == 1)
            bl.updateStringMetaField(cc, xml.user?.creditCard?.name.text(), MetaFieldType.TITLE)

            //valid credit card must have a future expiry date to be valid for payment processing
            Calendar expiry = Calendar.getInstance();
            expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
            bl.updateStringMetaField(cc, DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).print(expiry.getTime().getTime()), MetaFieldType.DATE)

            newUser.getPaymentInstruments().add(cc);
           }
           //currently don't allow duplicate user
            /*def oldUser = (newUser.userId && newUser.userId != 0) ? webServicesSession.getUserWS(newUser.userId) : null
            if(oldUser){
             throw new Exception("a user with id ${newUser.userid} already exists.")
             }*/
            //now create user in jbilling system
            webServicesSession.createUser(newUser)
            
            response.status = 201
            
            render(contentType: "text/xml; charset=utf-8") {
                info {
                    message(" user created : ${newUser} ")
                }
            }
        } catch (Exception e) {
            log.error("An error occurred during User creation.", e)
            response.status = 403
            
            render(contentType: "text/xml; charset=utf-8") {
                error {
                    message("An error occurred during User creation: ${e}")
                }
            }
        }
                /* render contentType: "text/xml; charset=utf-8",
             markup   */

        /* newUser.setUserId(0); // it is validated
      newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
      newUser.setPassword("asdfasdf1");
      newUser.setLanguageId(new Integer(1));
      newUser.setMainRoleId(new Integer(5));
      newUser.setParentId(parentId); // this parent exists
      newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
      newUser.setCurrencyId(currencyId);
      newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
      newUser.setInvoiceChild(new Boolean(false)); */

       /* MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor

        newUser.setMetaFields(new MetaFieldValueWS[]{metaField1, metaField2});


        //parse user contact
        if (xml.user.contact){

        }


def markup
if (user.save()) {
 markup = {
     status("OK")
 }
}
else {
 markup = {
     status("FAIL")
 }
}
render contentType: "text/xml; charset=utf-8",
     markup   */

    }

    def getUser () {}

}

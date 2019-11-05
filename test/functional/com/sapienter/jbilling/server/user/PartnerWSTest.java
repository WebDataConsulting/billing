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

package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.partner.PartnerType;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.util.ServerConstants;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.junit.After;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test of Partner web-service API
 *
 * @author Brian Cowdery
 * @since 31-Oct-2011
 */
@Test(groups = { "web-services", "partner" })
public class PartnerWSTest {

    private static final Integer PARTNER_ROLE_ID = 4;
    private static Integer MORDOR_CUSTOMER_ROLE_ID = 5;

    private final List<Integer> partnerIdsToClean = new LinkedList<Integer>();

    public void cleanAfterTest() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        for (Integer partnerId: partnerIdsToClean) {
            if (partnerId != null) api.deletePartner(partnerId);
        }
        partnerIdsToClean.clear();
    }

    @Test
    public void testCreatePartner() throws Exception {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();

            //create partner
            Integer partnerId = createPartnerForTestOnDatabase(1, "create new", api);
            partnerIdsToClean.add(partnerId);
            PartnerWS partner = api.getPartner(partnerId);

            assertNotNull("partner created", partner);
            assertNotNull("partner has an id", partner.getId());
        } finally { cleanAfterTest(); }
    }

    @Test
    public void testUpdatePartner() throws Exception {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();

            // create partner
            Integer partnerId = createPartnerForTestOnDatabase(2, "update", api);
            partnerIdsToClean.add(partnerId);
            PartnerWS partner = api.getPartner(partnerId);

            assertNotNull("partner created", partner);

            // just save changes to partner, nothing changes on the base user
            api.updatePartner(null, partner);
        } finally { cleanAfterTest(); }
    }

    @Test
    public void testDeletePartner() throws Exception {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();

            // create partner
            Integer partnerId = createPartnerForTestOnDatabase(3, "delete", api);
            PartnerWS partner = api.getPartner(partnerId);
            assertNotNull("partner created", partner);

            // delete partner
            api.deletePartner(partner.getId());

            // verify that partner cannot be fetched after deleting
            try {
                api.getPartner(partner.getId());
                fail("deleted partner should throw exception");
            } catch (SessionInternalError e) {
                assertTrue(e.getMessage().contains("Error calling jBilling API, method=getPartner"));
            }

            // verify that the base user was deleted with the partner
            UserWS deletedUser = api.getUserWS(partner.getUserId());
            assertEquals(1, deletedUser.getDeleted());
        } finally { cleanAfterTest(); }
    }

    private Integer createPartnerForTestOnDatabase(int testNumber, String contactLastName, JbillingAPI api) throws Exception {
        // new partner
        UserWS user = createUserForTest(testNumber, contactLastName);

        PartnerWS partner = new PartnerWS();
        partner.setType(PartnerType.STANDARD.name());

        // create partner
        return api.createPartner(user, partner);
    }

    private UserWS createUserForTest(Integer testNumber, String contactLastName) {
        UserWS user = new UserWS();
        user.setUserName("partner-0" + testNumber + "-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(PARTNER_ROLE_ID);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);
        setContactTestOnUser(user, contactLastName);
        return user;
    }

    private void setContactTestOnUser(UserWS user, String lastName) {
        ContactWS contact = new ContactWS();
        contact.setEmail(user.getUserName() + "@test.com");
        contact.setFirstName("Partner Test");
        contact.setLastName(lastName);
        user.setContact(contact);
    }

    @Test
    public void testGetPartner() throws Exception {
        JbillingAPI prancingPonyEntity = JbillingAPIFactory.getAPI();
        JbillingAPI mordorEntity= JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_MORDOR.name());

        // partner that does not exist throws exception
        try {
            prancingPonyEntity.getPartner(999);
            fail("non-existent partner should throw exception");
        } catch (SessionInternalError e) {
            assertTrue(e.getMessage().contains("Error calling jBilling API, method=getPartner"));
        }
        Integer partnerOnOtherEntity = null;
        try {
            UserWS userForTest = createUserForTest(4, "partner on other entity");
            userForTest.setMainRoleId(MORDOR_CUSTOMER_ROLE_ID);
            PartnerWS partner = new PartnerWS();
            partner.setType(PartnerType.STANDARD.name());
            // create partner on other entity
            partnerOnOtherEntity = mordorEntity.createPartner(userForTest, partner);

            // partner belonging to a different entity throws a security exception
            try {
                prancingPonyEntity.getPartner(20); // belongs to mordor entity
                fail("partner does not belong to entity 1, should throw security exception.");
            } catch (SecurityException e) {
                assertTrue(e.getMessage().contains("Unauthorized access to entity 2"));
            }
        } finally {
            if (partnerOnOtherEntity != null) mordorEntity.deletePartner(partnerOnOtherEntity);
        }

    }
}

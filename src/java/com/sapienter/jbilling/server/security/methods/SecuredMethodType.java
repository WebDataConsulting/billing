package com.sapienter.jbilling.server.security.methods;

import com.sapienter.jbilling.server.discount.db.DiscountDAS;
import com.sapienter.jbilling.server.discount.db.DiscountDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDAS;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.security.MappedSecuredWS;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;

import java.io.Serializable;

import org.apache.commons.collections.CollectionUtils;

/**
 * Created by IntelliJ IDEA.
 * User: bcowdery
 * Date: 14/05/12
 * Time: 9:28 PM
 * To change this template use File | Settings | File Templates.
 */
public enum SecuredMethodType {

        USER {
            public WSSecured getMappedSecuredWS(Serializable id) {
                return id != null ? new MappedSecuredWS(null, (Integer) id) : null;
            }
        },

        PARTNER {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PartnerDTO partner = new PartnerDAS().find(id);
                return partner != null ? new MappedSecuredWS(null, partner.getUser().getId()) : null;
            }
        },

        ITEM {
            public WSSecured getMappedSecuredWS(Serializable id) {
                ItemDTO item = new ItemDAS().find(id);
                if ( item.getEntities().size() == 1 ) {
                	return new MappedSecuredWS(item.getEntities().iterator().next().getId(), null);
                }
                return null;//an item may not be owned by the caller company anymore, It is now a shared/shareable entity
            }
        },

        ITEM_CATEGORY {
            public WSSecured getMappedSecuredWS(Serializable id) {
                ItemTypeDTO itemType = new ItemTypeDAS().find(id);
                if ( itemType.getEntities().size() == 1 ) {
                	return new MappedSecuredWS(itemType.getEntities().iterator().next().getId(), null);
                }
                return null;
            }
        },

        ORDER {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderDTO order = new OrderDAS().find(id);
                return order != null ? new MappedSecuredWS(null, order.getUserId()) : null;
            }
        },

        ORDER_LINE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderLineDTO line = new OrderLineDAS().find(id);
                return line != null ? new MappedSecuredWS(null, line.getPurchaseOrder().getUserId()) : null;
            }
        },

        INVOICE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                InvoiceDTO invoice = new InvoiceDAS().find(id);
                return invoice != null ? new MappedSecuredWS(null, invoice.getUserId()) : null;
            }
        },

        PAYMENT {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PaymentDTO payment = new PaymentDAS().find(id);
                return payment != null ? new MappedSecuredWS(null, payment.getBaseUser().getId()) : null;
            }
        },

        BILLING_PROCESS {
            public WSSecured getMappedSecuredWS(Serializable id) {
                BillingProcessDTO process = new BillingProcessDAS().find(id);
                return process != null ? new MappedSecuredWS(process.getEntity().getId(), null) : null;
            }
        },

        PLUG_IN {
            public WSSecured getMappedSecuredWS(Serializable id) {
                PluggableTaskDTO task = new PluggableTaskBL((Integer)id).getDTO();
                return task != null ? new MappedSecuredWS(task.getEntityId(), null) : null;
            }
        },

        ASSET {
            public WSSecured getMappedSecuredWS(Serializable id) {
                AssetDTO asset = new AssetDAS().find(id);
	            return asset != null ? new MappedSecuredWS(asset.getEntity().getId(), null) : null;
            }
        },
        
        DISCOUNT {
            public WSSecured getMappedSecuredWS(Serializable id) {
                DiscountDTO discount = new DiscountDAS().find(id);
                return discount != null ? new MappedSecuredWS(discount.getEntity().getId(), null) : null;
            }                
        },

        ORDER_PROCESS {
            public WSSecured getMappedSecuredWS(Serializable id) {
                OrderProcessDTO dto = new OrderProcessDAS().find(id);
                return dto != null ? new MappedSecuredWS(dto.getBillingProcess().getEntity().getId(),
                        dto.getPurchaseOrder().getUserId()) : null;
            }
        },

        ACCOUNT_TYPE {
            public WSSecured getMappedSecuredWS(Serializable id) {
                AccountTypeDTO dto = new AccountTypeDAS().find(id);
                return dto != null ? new MappedSecuredWS(dto.getCompany().getId(), null) : null;
            }
        },

        META_FIELD {
            public WSSecured getMappedSecuredWS(Serializable id) {
                MetaField dto = new MetaFieldDAS().find(id);
                return dto != null ? new MappedSecuredWS(dto.getEntity().getId(), null) : null;
            }
        };


        /**
         * implemented by each Type to return a secure object for validation based on the given ID.
         *
         * @param id id of the object type
         * @return secure object for validation
         */
        public abstract WSSecured getMappedSecuredWS(Serializable id);
}

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

package jbilling

import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.AssetStatusBL
import com.sapienter.jbilling.server.item.db.AssetDTO
import com.sapienter.jbilling.server.item.db.AssetReservationDTO
import com.sapienter.jbilling.server.item.db.AssetStatusDTO
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.ServerConstants
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.item.ItemTypeBL
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.db.JbillingTable
import com.sapienter.jbilling.server.util.db.JbillingTableDAS
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.Criteria
import org.joda.time.format.DateTimeFormat
import org.springframework.web.context.request.RequestContextHolder
import javax.servlet.http.HttpSession
import java.util.regex.Pattern

import org.hibernate.criterion.CriteriaSpecification

class ProductService implements Serializable {

    static transactional = true

    def messageSource
	def companyService

    def getFilteredProducts(CompanyDTO company, ItemDTOEx product, GrailsParameterMap params, AccountTypeDTO accountType, boolean includePlans, boolean pagination) {
        return getFilteredProductsForCustomer(company, product, params, accountType, includePlans, pagination, null)
    }

    /**
     * Returns a list of products filtered by simple criteria. The given filterBy parameter will
     * be used match either the ID, internalNumber or description of the product. The typeId parameter
     * can be used to restrict results to a single product type.
     *
     * @param company company
     * @param params parameter map containing filter criteria
     * @param includePlans true if the filter should include plans, false otherwise
     * @param pagination true if the results should be paginated, false otherwise
     * @return filtered list of products
     */
    def getFilteredProductsForCustomer(CompanyDTO company, ItemDTOEx product, GrailsParameterMap params, AccountTypeDTO accountType, boolean includePlans, boolean pagination, CompanyDTO customerCo) {
		// default filterBy message used in the UI
        def defaultFilter = messageSource.resolveCode('products.filter.by.default', session.locale).format((Object[]) [])

        // apply pagination arguments or not
        def pageArgs = pagination ? [max: params.max, offset: params.offset] : [:]
		
		// prepare list of all the entities that were selected
		List companies = new ArrayList<Integer>()
		if(product != null) {
			companies.add(product.entityId)
			for (Integer entity : product.entities) {
				companies.add(entity);
			}
		}
		// find out if company is root
		def isRoot = new CompanyDAS().isRoot(session['company_id'])

        // filter on item type, item id and internal number
        def products = ItemDTO.createCriteria().list(
            pageArgs
        ) {
		    createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
            and {
                if (params.filterBy && params.filterBy != defaultFilter) {
                    or {
                        eq('id', params.int('filterBy'))
                        ilike('internalNumber', "%${params.filterBy}%")
                    }
                }

                if (null != params.typeId && !params.typeId.toString().isEmpty()) {
                    itemTypes {
                        eq('id', params.int('typeId'))
                    }
                }

                if (accountType) {
                    createAlias('accountTypeAvailability', 'acc', Criteria.LEFT_JOIN)
                    or {
                        eq('standardAvailability', true)
                        eq('acc.id', accountType.id)
                    }
                }

                eq('deleted', 0)
                if(customerCo!=null){
                	or {
                		'in'('entities.id', [customerCo.id])
						 eq('global', true)
						if (customerCo.parent) {
							eq ('entity.id', customerCo.id)
	                		and {
								eq('global', true)
								'in'('entity.id', customerCo.parent.id )
	                		}
						}
                	}
				}
				
				if(product != null) {
					or {
						//'in'('entity.id', companies)
						'in'('entities.id', companyService.getEntityAndChildEntities()*.id)
						and {
							eq('global', true)
							'in'('entity.id', companyService.getRootCompanyId() )
						}
					}
				} else {
					or {
						//'in'('entity',company)
						'in'('entities.id', companyService.getEntityAndChildEntities()*.id )
						
						//list all gloal entities as well
						and {
							eq('global', true)
							eq('entity.id', companyService.getRootCompanyId() )
						}
					}
				}
				

                //only products valid for the period of the plan
                if(product) {
                    if(product.activeSince) {
                        or {
                            le('activeSince', product.activeSince)
                            isNull('activeSince')
                        }
                    }

                    if(product.activeUntil) {
                        or {
                            ge('activeUntil', product.activeUntil)
                            isNull('activeUntil')
                        }
                    }
                }
            }
			resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            order('id', 'desc')
        }

        // if no results found, try filtering by description
        if (!products  && params.filterBy) {
            JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
            JbillingTable table = tableDas.findByName(ServerConstants.TABLE_ITEM);

            def description = params.filterBy.toLowerCase()
            def languageId = session['language_id']

            products = ItemDTO.createCriteria().list(
                pageArgs
            ) {
                and {

                    if (accountType) {
                        createAlias('accountTypeAvailability', 'acc', Criteria.LEFT_JOIN)
                        or {
                            eq('standardAvailability', true)
                            eq('acc.id', accountType.id)
                        }
                    }
                    eq('deleted', 0)
                    eq('entity', company)
                    sqlRestriction(
                    """ exists (
                                select a.foreign_id
                                from international_description a
                                where a.foreign_id = {alias}.id
                                    and a.table_id = ?
                                    and a.language_id = ?
                                    and lower(a.content) like '%?%'
                            )
                    """,[table.getId(),languageId,description]
                    )

                }
                order('id', 'desc')
            }

        }


        return products.unique()
    }
	
	def getFilteredProducts(CompanyDTO company, GrailsParameterMap params, AccountTypeDTO accountType, boolean includePlans, boolean pagination){
		return getFilteredProducts(company, null, params, accountType, includePlans, pagination)
	}

    /**
     * Returns a list of visible item types.
     *
     * @return list of item types
     */
    def getItemTypes(companyId, typeIds) {
		
		List result = ItemTypeDTO.createCriteria().list {
			 and {
				 eq('internal', false)
				 createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
				 or {
					 'in'('entities.id', companyId?companyId:companyService.getEntityAndChildEntities()*.id )
					 //list all gloal entities as well
					 and {
						 eq('global', true)
						 eq('entity.id', companyService.getRootCompanyId())
					 }
				 }
			 	 if (typeIds) {
					  'in'('id', typeIds)
				 }
			 }
			 order('id', 'desc')
		 }
		 
		 return result.unique()
	}
	
	def getDependencyItems(typeId, excludedItemIds){
		ItemDTO.metaClass.toString = {return delegate.id + " : "+ delegate.description }
		return ItemDTO.createCriteria().list() {
			createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
			and {
				or {
					'in'('entities.id', companyService.getEntityAndChildEntities()*.id)
					and {
						eq('global', true)
						eq('entity.id', companyService.getRootCompanyId())
					}
				}
				eq('deleted', 0)

				if( null != excludedItemIds && excludedItemIds.size() > 0 ){
					not { 'in'("id", excludedItemIds) }
				}

				itemTypes {
					eq('id', typeId)
				}
			}
			order('id', 'desc')
		}
	}

    /**
     * Returns all assets filter by identifier, id or status for a given product.
     * Possible values in params
     *  - max                   (Required) max no of results to get
     *  - offset                (Required) start index in result list
     *  - sort                  Attribute of asset to sort by
     *  - order                 Sort direction
     *  - deleted               If the asset has been deleted
     *  - filterBy              Either an AssetDTO id or identifier
     *  - statusId              AssetStatusDTO id linked to the asset
     *  - filterByMetaFieldId-i   Id of meta field to filter by. i - index
     *  - filterByMetaFieldValue-i Value of meta field to filter by
     *  - itemId                ItemDTO id linked to the asset
     *  - orderLineId           id of line or 'NULL'
     *  - groupId               asset group id
     *  - groupIdNull           if true will filter by group id null or groupId
     *
     * @param companyId         only bring assets for this company
     * @param filteredAssets    assets filtered from the results
     * @param assetsToInclude   assets which must be excluded from the availability check
     * @param params
     * @return
     */
    def getFilteredAssets(Integer companyId, List filteredAssetIds, List assetsToInclude, GrailsParameterMap params, boolean available) {
        // default filterBy messages used in the UI
		
        def defaultFilter           = messageSource.resolveCode('assets.filter.by.default', session.locale).format((Object[]) [])
        def defaultMetaFieldValue   = messageSource.resolveCode('assets.filter.by.metafield.default', session.locale).format((Object[]) [])

		def customerCompany = null
		def orderUserId = params.int("userId")
		if(orderUserId) {
			// assets are being get for order
			customerCompany = UserDTO.get(params.int("userId"))?.company
		}
		
        // apply pagination arguments or not
        def pageArgs = [max: params.max, offset: params.offset,
                sort: (params.sort && params.sort != 'null') ? params.sort: 'id',
                order: (params.order && params.order != 'null') ? params.order : 'desc']

        //indexes of metafields we have filter data for
        def metaFieldIdxs = []
        Pattern pattern = Pattern.compile(/filterByMetaFieldId(\d+)/)
        //get all the ids in an array
        params.each{
            def m = pattern.matcher(it.key)
            if( m.matches()) {
                metaFieldIdxs << m.group(1)
            }
        }

        // filter on id, identifier and state
        def assets = AssetDTO.createCriteria().list(
                pageArgs
        ) {
			createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
            and {
					 if (params.filterBy && params.filterBy != defaultFilter) {
						 or {
								 eq('id', params.int('filterBy'))
								 ilike('identifier', "%${params.filterBy}%")
						 	}
					 }
					 if (null != params.statusId && !params.statusId.toString().isEmpty() && params.statusId.toString()!='0') {
						 assetStatus {
							 eq('id', params.int('statusId'))
						 }

                         def reservedAssets = AssetReservationDTO.createCriteria().list() {
                             gt('endDate', new Date())
                         }
                         if (reservedAssets) {
                             not {
                                 'in'('id', reservedAssets*.asset.id as List)
                             }
                         }
					 }

				or {
					'in'('entities.id', session['company_id'] as Integer )
					and {
						eq('global', true)
						eq('entity.id', getRootCompanyId())
					}
					'in'('entity.id', companyService.getEntityAndChildEntities()*.id )
				}
				
				if(available) {
					or{
						'in'('entities.id', session['company_id'] as Integer )
						and {
							eq('global' , true)
							eq('entity.id', getRootCompanyId())
							//'in'('entity.id', companyService.getHierarchyEntities(session['company_id'] as Integer)*.id)
						}
					}
				}

                String[] metaFieldIdAndType
                DataType dataType = null
                String filterByMetaFieldValue
                metaFieldIdxs.each {
                    try {
                        if(params['filterByMetaFieldId'+it]) {
                            metaFieldIdAndType = params['filterByMetaFieldId'+it].split(":")
                            dataType = DataType.valueOf(metaFieldIdAndType[1])
                        }
                        if (dataType == DataType.BOOLEAN || (params['filterByMetaFieldValue'+it]
                                && params['filterByMetaFieldValue'+it] != defaultMetaFieldValue
                                && params['filterByMetaFieldValue'+it].toString().trim().length() > 0)) {
                            filterByMetaFieldValue = params['filterByMetaFieldValue'+it]

                            String join = "";
                            String where = "";
                            def parameter
                            if(dataType == DataType.STRING || dataType == DataType.JSON_OBJECT || dataType == DataType.ENUMERATION || dataType == DataType.TEXT_AREA) {
                                where = " lower(mv.string_value) like ?"
                                parameter = "%" + filterByMetaFieldValue.toLowerCase() + "%"
                            } else if(dataType == DataType.BOOLEAN) {
                                where = " mv.boolean_value=?"
                                parameter = (filterByMetaFieldValue ? true : false)
                            } else if(dataType == DataType.INTEGER) {
                                Integer.parseInt(filterByMetaFieldValue)
                                where = " mv.integer_value=?"
                                parameter = filterByMetaFieldValue as int
                            } else if(dataType == DataType.DECIMAL) {
                                new BigDecimal(filterByMetaFieldValue)
                                where = " mv.decimal_value=?"
                                parameter = filterByMetaFieldValue as double
                            } else if(dataType == DataType.DATE) {
                                Date theDate = DateTimeFormat.forPattern(messageSource.resolveCode('datepicker.format', session.locale).format((Object[]) [])).parseDateTime(filterByMetaFieldValue).toDate()
                                where = " mv.date_value=?"
                                parameter = theDate
                            } else if(dataType == DataType.LIST) {
                                join = "join list_meta_field_values lmv on lmv.meta_field_value_id=mv.id "
                                where = " lmv.list_value=?"
                                parameter = filterByMetaFieldValue
                            }

                            if(where.length() > 0) {
                                sqlRestriction(
                                    """exists (select mv.id from meta_field_value mv
                                    join asset_meta_field_map am on am.meta_field_value_id=mv.id
                                    ${join}
                                    where mv.meta_field_name_id=?
                                    and am.asset_id = {alias}.id
                                    and ${where})
                                    """,[metaFieldIdAndType[0] as int, parameter]
                                )
                            }

                        }
                    } catch (Throwable t) {
                        log.debug("Unable to parse meta field value", t)
                        throw new SessionInternalError("Unable to parse meta field value "+filterByMetaFieldValue, "asset.search.error.type.parse,"+filterByMetaFieldValue)
                    }
                }

                if(params.groupId || params.groupIdNull) {
                    or {
                        if(params.groupIdNull) {
                            isNull('group')
                        }

                        if(params.groupId) {
                            group {
                                eq('id', params.int('groupId'))
                            }
                        }
                    }
                }

                if(params.orderLineId) {
                    if(params.orderLineId == 'NULL') {
                        isNull('orderLine')
                    } else {
                        orderLine {
                            eq('id', params.int('orderLineId'))
                        }
                    }
                }

                if(filteredAssetIds?.size() > 0) {
                    not {
                        inList('id', filteredAssetIds)
                    }
                }
                if(assetsToInclude?.size() > 0) assetsToInclude = assetsToInclude - null

                if(assetsToInclude?.size() > 0) {
                    or {
                        assetStatus {
                            eq('isAvailable', 1)
                        }
                        inList('id', assetsToInclude)
                    }
                }

                if (params.int('statusId') == 0) {
                    List ownReservedAssets = AssetReservationDTO.createCriteria().list() {
                        if (orderUserId) eq('user.id', orderUserId)
                        gt('endDate', new Date())
                    }
                    'in'('id', (ownReservedAssets) ? (ownReservedAssets*.asset.id as List) : [null])
                } else if (orderUserId && StringUtils.trimToNull(params.statusId) == null) {
                    assetStatus {
                        eq('isAvailable', 1)
                    }
                }

                if (orderUserId && params.int('statusId') != 0) {
                        or {
                            eq('entity', customerCompany)
                            and {
                                //isNull('entity')
                                item {
                                    or {
                                        eq('id', params.int('itemId'))
                                    }
                                }
                            }
                        }

                        def reservedAssets = AssetReservationDTO.createCriteria().list() {
                            if(orderUserId) ne('user.id', orderUserId)
                            gt('endDate', new Date())
                        }
                        if (reservedAssets) {
                            not {
                                'in'('id', reservedAssets*.asset.id as List)
                            }
                        }

                    }
				if(params.int('itemId')) {
					item {
						  eq('id', params.int('itemId'))
				    }
				}
                if(params.int('categoryId')) {
                    item {
                        itemTypes {
                            eq('id', params.int('categoryId'))
                        }
                    }
                }
                if (params.assetId) {
                    or {
                        eq('id', params.int('assetId'))
                        ilike('identifier', "%${params.assetId}%")
                    }
                }
                eq('deleted', params.deleted ? params.int('deleted') : 0)
            }
        }
        setReservedFlag(assets)
        // Sort on the basis of asset status
        if (pageArgs.sort == 'assetStatus.id') assets.sort({ a, b ->
            def aStatus = a.isReserved ? 'Reserved' : a.assetStatus?.description
            def bStatus = b.isReserved ? 'Reserved' : b.assetStatus?.description
            def comparator = aStatus <=> bStatus
            pageArgs.order=='desc'? -comparator:comparator
        })
        return assets.unique()
    }

    def setReservedFlag(def assets) {
        if(!assets) return;
        def activeReservationAssets = AssetReservationDTO.createCriteria().list(){
            projections{
                property("asset.id")
            }
            gt('endDate', new Date())
        }
        assets.each {asset->
            if(activeReservationAssets.contains(asset.id)){
                asset.setReserved(true)
            }
        }
    }

    /**
     * List of all possible AssetStatusDTOEx objects linked to the
     * ItemTypes of the Item specified by itemId
     *
     * @param itemId    ItemDTO id.
     * @return
     */
    def getStatusesForProduct(Integer itemId) {
        def statusList = AssetStatusDTO.createCriteria().list() {
            and {
                itemType {
                    items {
                        eq('id', itemId)
                    }
                }

                eq('deleted', 0)
            }
        }

        return AssetStatusBL.convertAssetStatusDTOs(statusList);
    }

    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    def HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

    /**
     * List of all AssetStatusDTOEx objects linked to the
     * ItemTypes of the Item specified by itemId and filtered by status = available
     *
     * @param itemId    ItemDTO id.
     * @return
     */
    def getAvailableStatusesForProduct(Integer itemId) {
        def statusList = AssetStatusDTO.createCriteria().list() {
            and {
                itemType {
                    items {
                        eq('id', itemId)
                    }
                }
                eq('deleted', 0)
                eq('isAvailable', 1)
            }
        }

        return AssetStatusBL.convertAssetStatusDTOs(statusList);
    }

	/**
	 * If the current user's company is the Root Company we return a list with its id PLUS all its children ids.
	 * If the logged in user's company is a child company then we return only its id.
	 */
	def getEntityAndChildEntities() {
		CompanyDTO loggedInUserCompany = CompanyDTO.get( session['company_id'] as Integer )
		def childEntities = []
		childEntities << loggedInUserCompany
		childEntities += CompanyDTO.findAllByParent(loggedInUserCompany)
	}

	def getRootCompanyId() {
		CompanyDTO loggedInUserCompany = CompanyDTO.get( session['company_id'] as Integer )
		loggedInUserCompany.parent == null ? loggedInUserCompany.id : loggedInUserCompany.parent.id
	}

    def createInternalTypeCategory(CompanyDTO company) {

        ItemTypeDTO type = new ItemTypeDTO();
        type.entity = company
        type.allowAssetManagement = 0
        type.description = ServerConstants.PLANS_INTERNAL_CATEGORY_NAME
        type.internal = true
        type.orderLineTypeId = ServerConstants.ORDER_LINE_TYPE_ITEM
        Set<CompanyDTO> entities = new HashSet<CompanyDTO>();
        entities.add(company);
        type.entities = entities
        type.save()
    }
}

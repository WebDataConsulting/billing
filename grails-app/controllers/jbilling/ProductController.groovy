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

import com.sapienter.jbilling.client.util.BindHelper
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.item.AssetBL
import com.sapienter.jbilling.server.item.AssetStatusBL
import com.sapienter.jbilling.server.item.AssetStatusDTOEx
import com.sapienter.jbilling.server.item.AssetWS
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.item.ItemDependencyDTOEx
import com.sapienter.jbilling.server.item.ItemDependencyType
import com.sapienter.jbilling.server.item.ItemPriceDTOEx

import com.sapienter.jbilling.server.item.ItemTypeWS
import com.sapienter.jbilling.server.item.db.AssetDTO
import com.sapienter.jbilling.server.item.db.AssetReservationDAS
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup
import com.sapienter.jbilling.server.user.EntityBL
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import org.apache.commons.io.FilenameUtils
import com.sapienter.jbilling.server.order.db.OrderDAS

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS

import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper


import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import org.joda.time.format.DateTimeFormat

import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.ItemTypeBL
import com.sapienter.jbilling.server.item.ItemBL

import org.hibernate.FetchMode
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Property
import org.hibernate.criterion.Projections

import org.apache.commons.lang.StringUtils

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import java.text.ParseException
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern

@Secured(["isAuthenticated()"])
class ProductController {
	static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]
    static versions = [ max: 25 ]

    static final viewColumnsToFields =
            ['categoryId': 'id',
             'productId': 'id',
             'lineType': 'orderLineTypeId',
             'number': 'internalNumber',
             'company': 'ce.description']

    IWebServicesSessionBean webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def productService
	def companyService

    def auditBL

    def index () {
        list()
    }

    /**
     * Get a list of categories and render the "_categories.gsp" template. If a category ID is given as the
     * "id" parameter, the corresponding list of products will also be rendered.
     */
    def list () {
        def filters = filterService.getFilters(FilterType.PRODUCT, params)

        def categoryId = params.int('id')
        def category = categoryId ? ItemTypeDTO.get(categoryId) : null

		if ( category && !companyService.isAvailable(category.global, category.entity?.id, category.entities*.id) ) {
			category= null
			flash.info = "validation.error.company.hierarchy.invalid.categoryid"
			flash.args = [ categoryId ]
		}
		
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'), category?.description)

        def contactFieldTypes = params['contactFieldTypes']

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'categoriesTemplate', model: [selectedCategoryId: category?.id, filters: filters, filterRender: 'second', filterAction: 'allProducts']
            }else {
                render view: 'list', model: [selectedCategoryId: category?.id, filters: filters, filterRender: 'second', filterAction: 'allProducts']
            }
            return
        }

        def categories = getProductCategories(true, null)
        def products = category ? getProducts(category.id, filters) : null

        if (params.applyFilter || params.partial) {
            render template: 'productsTemplate', model: [ products: products, selectedCategoryId: category?.id, contactFieldTypes: contactFieldTypes ]
        } else {
            render view: 'list', model: [ categories: categories, products: products, selectedCategoryId: category?.id, filters: filters, filterRender: 'second', filterAction: 'allProducts' ]
        }
    }

    def categories () {
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'categoriesTemplate', model: []
            }else {
                render view: 'list', model: []
            }
            return
        }
        def categories = getProductCategories(true, null)
        render template: 'categoriesTemplate', model: [ categories: categories ]
    }

    def findCategories () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def categories = getProductCategories(true, null)

        def categoryId = params.int('id')
        def category = categoryId ? ItemTypeDTO.get(categoryId) : null

        try {
            render getItemsJsonData(categories, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    def findProducts () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def filters = filterService.getFilters(FilterType.PRODUCT, params)

        def categoryId = params.int('id')
        def category = categoryId ? ItemTypeDTO.get(categoryId) : null

        def products = category ? getProducts(category.id, filters) : null

        try {
            render getItemsJsonData(products, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    def findAllProducts () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def filters = filterService.getFilters(FilterType.PRODUCT, params)

        def products =  getProducts(null, filters)

        try {
            render getItemsJsonData(products, params) as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }

    }

    /**
     * Converts Products and Categories to JSon
     */
    private def Object getItemsJsonData(items, GrailsParameterMap params) {
        def jsonCells = items
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def getProductCategories(def paged, excludeCategoryId) {
        if (paged) {
            params.max = params?.max?.toInteger() ?: pagination.max
            params.offset = params?.offset?.toInteger() ?: pagination.offset
            params.sort = params?.sort ?: pagination.sort
            params.order = params?.order ?: pagination.order
        }

        def childEntities = companyService.getEntityAndChildEntities()
        def company_id = session['company_id'] as Integer

        List result = ItemTypeDTO.createCriteria().list(
                max: paged ? params.max : null,
                offset: paged ? params.offset : null
        ) {
            createAlias("entities", "ce", CriteriaSpecification.LEFT_JOIN)
            and {
                eq('internal', false)
                or {
                    'in'('entity.id', childEntities.id.flatten())
                    //list all global entities as well BUT only if they were created by me or my parent NOT other root companies.
                    and {
                        eq('global', true)
                        eq('entity.id', companyService.getRootCompanyId())
                    }
					'in'('ce.id', company_id)
                }
                if (null != excludeCategoryId) {
                    notEqual('id', excludeCategoryId)
                }
                if (params.categoryId) {
                    eq('id', params.int('categoryId'))
                }
                if (params.company) {
                    addToCriteria(Restrictions.ilike("entity.description", params.company, MatchMode.ANYWHERE));
                }
            }
            resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            SortableCriteria.sort(params, delegate)
        }

        return result
    }

    def getAvailableAccountTypes() {

        return AccountTypeDTO.createCriteria().list() {
            and {
                eq('company', new CompanyDTO(session['company_id']))
            }
            order('id', 'desc')
        }
    }

    def getDependencyItemTypes(excludedTypeIds){
        return ItemTypeDTO.createCriteria().list() {
        	createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
			and {
				or {
                    'in'('entities.id', companyService.getEntityAndChildEntities()*.id)
                    and {
                        eq('global', true)
                        eq('entity.id', companyService.getRootCompanyId())
                    }
				}
                eq('internal', false)
                if( null != excludedTypeIds && excludedTypeIds.size() > 0 ){
                   not { 'in'("id", excludedTypeIds) }
                }
            }
            order('id', 'desc')
        }
    }

    def getDependencyItems(typeId, excludedItemIds) {
        productService.getDependencyItems(typeId, excludedItemIds)
    }

    def getItems(itemIds) {
		
		Integer company_id = session['company_id'] as Integer
        return ItemDTO.createCriteria().list() {
        	createAlias("entities","ce")
            and {
                or {
					//query based on item_entity_map always
					'in'('ce.id', company_id)
					//list all gloal entities as well
					eq('global', true)
				}
                eq('deleted', 0)

                if(null != itemIds && itemIds.size()>0){
                    'in'('id', itemIds)
                } else {
                    eq('id', null)
                }
            }
            order('id', 'desc')
        }
    }

    def getItemsByItemType () {
        def typeId = params.int('typeId')
        List<Integer> toExcludeItemIds = []
        params["toExcludeItemIds[]"].grep{it}.each{
            toExcludeItemIds << Integer.valueOf(it)
        }
        render g.select(
                from: getDependencyItems(typeId, toExcludeItemIds),
                id: 'product.dependencyItems',
                name: 'product.dependencyItems',
                optionKey: 'id',
                noSelection: ['':'-'])
    }

    def addDependencyRow () {
        def typeId = (params.typeId!=null && !StringUtils.isBlank(params.typeId))?params.int('typeId'):null
        def itemId = (params.itemId!=null && !StringUtils.isBlank(params.itemId))?params.int('itemId'):null
        def min = (params.min!=null && !StringUtils.isBlank(params.min))?params.int('min'):null
        def max = (params.max!=null && !StringUtils.isBlank(params.max))?params.int('max'):null

        ItemDependencyDTOEx dep = new ItemDependencyDTOEx(type: typeId?ItemDependencyType.ITEM_TYPE : ItemDependencyType.ITEM,
                            dependentId: itemId?:typeId, minimum: min, maximum: max)

        if(typeId!=null && itemId==null){
            ItemTypeBL bl = new ItemTypeBL()
            bl.set(typeId)
            def obj = bl.getEntity()
            dep.dependentDescription = obj.description
            render template: 'dependencyRow', model: [obj:dep, type: true]
        } else if(typeId!=null && itemId!=null){
            ItemBL bl = new ItemBL()
            bl.set(itemId)
            def obj = bl.getEntity()
            dep.dependentDescription = obj.description
            render template: 'dependencyRow', model: [obj:dep, type: false]
        }
    }

    def getDependencyList () {
        Integer typeId = (params.typeId!=null && !StringUtils.isBlank(params.typeId))?params.int('typeId'):null
        Integer itemId = (params.itemId!=null && !StringUtils.isBlank(params.itemId))?params.int('itemId'):null

        List<Integer> typeIds = [], itemIds = []
        List<Integer> toExcludeTypeIds = [], toExcludeItemIds = []
        params["typeIds[]"].grep{it}.each{
            typeIds << Integer.valueOf(it)
        }
        params["itemIds[]"].grep{it}.each{
            itemIds << Integer.valueOf(it)
        }
        params["toExcludeTypeIds[]"].grep{it}.each{
            toExcludeTypeIds << Integer.valueOf(it)
        }
        toExcludeTypeIds << typeId

        params["toExcludeItemIds[]"].grep{it}.each{
            toExcludeItemIds << Integer.valueOf(it)
        }
        toExcludeItemIds << itemId

        if(typeId!=null && itemId==null){
            typeIds.removeAll(toExcludeTypeIds)
            render g.select(
                    from: productService.getItemTypes(session['company_id'], typeIds),
                    id: 'product.dependencyItemTypes',
                    name: 'product.dependencyItemTypes',
                    optionValue: "description",
                    optionKey: 'id',
                    noSelection: ['':'-'])
        } else if(typeId!=null && itemId!=null){
            itemIds.removeAll(toExcludeItemIds)
            render g.select(
                    from: getItems(itemIds),
                    id: 'product.dependencyItems',
                    name: 'product.dependencyItems',
                    optionKey: 'id',
                    noSelection: ['':'-'])
        }
    }
    /**
     * Get a list of products for the given item type id and render the "_products.gsp" template.
     */
    def products () {
        if (params.id) {
            def filters = filterService.getFilters(FilterType.PRODUCT, params)
            def category = ItemTypeDTO.get(params.int('id'))
            def contactFieldTypes = params['contactFieldTypes']

            breadcrumbService.addBreadcrumb(controllerName, 'list', null, category.id, category?.description)

            def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
            //If JQGrid is showing, the data will be retrieved when the template renders
            if (usingJQGrid){
                render template: 'productsTemplate', model: [selectedCategory: category, contactFieldTypes: contactFieldTypes ]
            }else {
                def products = getProducts(category.id, filters)
                render template: 'productsTemplate', model: [ products: products, selectedCategory: category, contactFieldTypes: contactFieldTypes ]
            }

        }
    }

    /**
     * Applies the set filters to the product list, and exports it as a CSV for download.
     */
    def csv () {
        def filters = filterService.getFilters(FilterType.PRODUCT, params)

        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS
        params.max = CsvExporter.MAX_RESULTS

        def products = getProducts(params.int('id'), filters)

        if (products.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "products.csv")
            Exporter<ItemDTO> exporter = CsvExporter.createExporter(ItemDTO.class);
            render text: exporter.export(products), contentType: "text/csv"
        }
    }

    def getProducts(Integer id, filters) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
		def language_id = session['language_id'] as Integer
		def company_id = session['company_id'] as Integer
        def products = ItemDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
		    createAlias("entities","ce", CriteriaSpecification.LEFT_JOIN)
            and {

                filters.each { filter ->
                    if (filter.value != null) {
                        if (filter.field == 'description') {
                            def description = filter.stringValue?.toLowerCase()
                            sqlRestriction(
                                    """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and lower(a.content) like ?
                                        )
                                    """, [Constants.TABLE_ITEM, language_id, "%" + description + "%"]
                            )
                        } else if (filter.field == 'contact.fields') {
                            String typeId = params['contact.fields.fieldKeyData']?params['contact.fields.fieldKeyData']:filter.fieldKeyData
                            String ccfValue = filter.stringValue;
                            log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"

                            if (typeId && ccfValue) {
                                MetaField type = findMetaFieldType(typeId.toInteger());
                                if (type != null) {
                                    createAlias("metaFields", "fieldValue")
                                    createAlias("fieldValue.field", "type")
                                    setFetchMode("type", FetchMode.JOIN)
                                    eq("type.id", typeId.toInteger())

                                    switch (type.getDataType()) {
                                        case DataType.STRING:
                                        	def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                                        					.setProjection(Projections.property('id'))
										    				.add(Restrictions.like('stringValue.value', ccfValue + '%').ignoreCase())

                                        	addToCriteria(Property.forName("fieldValue.id").in(subCriteria))
                                            break;
                                        case DataType.INTEGER:
                                        	def subCriteria = DetachedCriteria.forClass(IntegerMetaFieldValue.class, "integerValue")
                                        					.setProjection(Projections.property('id'))
										    				.add(Restrictions.eq('integerValue.value', ccfValue.toInteger()))

                                        	addToCriteria(Property.forName("fieldValue.id").in(subCriteria))
                                            break;
                                        case DataType.ENUMERATION:
                                        case DataType.JSON_OBJECT:
                                            addToCriteria(Restrictions.ilike("fieldValue.value", ccfValue, MatchMode.ANYWHERE))
                                            break;
                                        default:
                                        // todo: now searching as string only, search for other types is impossible
//                                            def fieldValue = type.createValue();
//                                            bindData(fieldValue, ['value': ccfValue])
//                                            addToCriteria(Restrictions.eq("fieldValue.value", fieldValue.getValue()))

                                            addToCriteria(Restrictions.eq("fieldValue.value", ccfValue))
                                            break;
                                    }

                                }
                            }
                        } else if(filter.field == 'u.company.description') {
                            ilike('ce.description', "%${filter.stringValue}%")
                        } else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }

                if (id != null) {
                    itemTypes {
                        eq('id', id)
                    }
                }

                eq('deleted', 0)

                def childEntities = companyService.getEntityAndChildEntities()
                or {
                    'in'('ce.id', childEntities.id.flatten())
                    //list all gloal entities as well
                    and {
						eq('global', true)
						eq('entity.id', companyService.getRootCompanyId())
                    }
                }
                if(params.productId) {
                    eq('id', params.int('productId'))
                }
                if(params.company) {
                    addToCriteria(Restrictions.ilike("ce.description",  params.company, MatchMode.ANYWHERE) );
                }
                if(params.number) {
                    addToCriteria(Restrictions.ilike("internalNumber",  params.number, MatchMode.ANYWHERE))
                }
            }
			resultTransformer org.hibernate.Criteria.DISTINCT_ROOT_ENTITY
            // apply sorting
            SortableCriteria.sort(params, delegate)
        }

        params.totalCount = products.totalCount
        log.debug "Products from filter: ${products}"
        log.debug "Entities in first product: ${products[0]?.entities}"

        return products.unique()
    }

    /**
     * Get a list of ALL products regardless of the item type selected, and render the "_products.gsp" template.
     */
    def allProducts () {
		def filters = filterService.getFilters(FilterType.PRODUCT, params)
		def item = ItemDTO.get(params.int('id'))
		def catList = item?.getItemTypes();
		def category = catList?.getAt(0)
        def contactFieldTypes = params['contactFieldTypes']

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid) {
            render template: 'productsTemplate', model: [contactFieldTypes: contactFieldTypes ]
        } else if (category) {
				list()
        } else {
            def products =  getProducts(null, filters)
            render template: 'productsTemplate', model: [ products: products, contactFieldTypes: contactFieldTypes ]
        }

    }

    /**
     * Show details of the selected product. By default, this action renders the entire list view
     * with the product category list, product list, and product details rendered. When rendering
     * for an AJAX request the template defined by the "template" parameter will be rendered.
     */
    def show () {
        ItemDTO product = ItemDTO.get(params.int('id'))
		ItemDTOEx productEx =  params.id ? webServicesSession.getItem(params.int('id'), session['user_id'], null) : null
        if (!product) {
            log.debug "redirecting to list"
            redirect(action: 'list')
            return
        }
		
		if ( product && !companyService.isAvailable(product.global, product.entity?.id, product.entities*.id) ) {
			//category= null
			flash.info = "validation.error.company.hierarchy.invalid.productid"
			flash.args = [ product.id ]
		}
		
        recentItemService.addRecentItem(product?.id, RecentItemType.PRODUCT)
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'), product?.internalNumber)

        //check if asset management is possible by checking if a linked item type allows asset management
        def assetManagementPossible = false
        product?.itemTypes?.each{
            if(it.allowAssetManagement) {
                assetManagementPossible = true
            }
        }

        if (params.template) {
            // render requested template, usually "_show.gsp"
            render template: params.template, model: [ selectedProduct: product, selectedCategoryId: params.category, assetManagementPossible: assetManagementPossible, productEx: productEx ]

        } else {
            // render default "list" view - needed so a breadcrumb can link to a product by id
            def filters = filterService.getFilters(FilterType.PRODUCT, params)
            def categories = getProductCategories(false, null);

            def productCategory = params.category ?: product?.itemTypes?.asList()?.get(0)
            def products = getProducts(productCategory.id, filters);

            render view: 'list', model: [ categories: categories, products: products, selectedProduct: product,
                    assetManagementPossible: assetManagementPossible, selectedCategory: productCategory, filters: filters, uploadAsset: params.uploadAsset ?: "", productEx: productEx]
        }
    }

    /**
     * Delete the given category id
     */
    def deleteCategory () {

		def category = params.id ? ItemTypeDTO.get(params.id) : null
		
        if (params.id && !category) {
            flash.error = 'product.category.not.found'
            flash.args = [ params.id  as String]
            render template: 'productsTemplate', model: [ products: products ]
            return
        }

        if (!params.id && !params.boolean('add')) {
            flash.error = 'product.category.not.selected'
            flash.args = [ params.id  as String]

            render template: 'productsTemplate', model: [ products: products ]
            return
        }

        if (params.id) {
            try {
                webServicesSession.deleteItemCategory(params.int('id'))

                log.debug("Deleted item category ${params.id}.");

                flash.message = 'product.category.deleted'
                flash.args = [ params.id as String]
				
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            } catch (Exception e) {
                flash.error = 'product.category.delete.error'
                flash.args = [ params.id as String ]
            }
        }

		params.id = null
		redirect action: 'index'
    }

    /**
     * Delete the given product id
     */
    def deleteProduct () {
        if (params.id) {
            try {
                webServicesSession.deleteItem(params.int('id'))

                log.debug("Deleted item ${params.id}.");

                flash.message = 'product.deleted'
                flash.args = [ params.id  as String]
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            } catch (Exception e) {
                flash.error = 'product.delete.errorr'
                flash.args = [ params.id as String ]
            }
        }

        // call the rendering action directly instead of using 'chain' or 'redirect' which results
        // in a second request that clears the flash messages.
        if (params.category) {
            // return the products list, pass the category so the correct set of products is returned.
            list()
        } else {
            // no category means we deleted from the 'allProducts' view
			allProducts()
        }
    }

    /**
     * List assets linked to a product. ID parameter identifies a product
     */
    def assets () {
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        if(!params.id){
            list()
            return
        }

        ItemDTO product = ItemDTO.get(params.int('id'))
        if(!product){
            return response.sendError(Constants.ERROR_CODE_404)
        }
        ItemTypeDTO assetManType = new ItemTypeBL().findItemTypeWithAssetManagementForItem(product.id);
        assetManType.assetMetaFields.size();
        assetManType.assetStatuses.size();

        //add breadcrumb
        breadcrumbService.addBreadcrumb(controllerName, 'assets', null, params.int('id'), product?.internalNumber)

        params.put("itemId", product.id)
        params.put("deleted", 'on' == params.showDeleted ? 1 : 0)

        def assets
        try {
            assets = productService.getFilteredAssets(session['company_id'], [] as List, [] as List, params, false)
        } catch (SessionInternalError e) {
            assets = []
            viewUtils.resolveException(flash, session.locale, e);
        }

        def assetStatuses = assetManType.assetStatuses as List
        assetStatuses << new AssetStatusDTOEx(0,Constants.RESERVAED_STATUS, 0,0,0,0)
        def model = [ assets: assets, product: product, metaFields:  assetManType.assetMetaFields.findAll{it.dataType != DataType.STATIC_TEXT}, assetStatuses: assetStatuses]
        if(usingJQGrid) {
            render view: 'assetList', model: model
            return
        }

        if (params.applyFilter || params.partial) {
            render template: 'assets', model: model
        } else {
            render view: 'assetList', model: model
        }
    }

    def findAssets () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS
        ItemDTO product = ItemDTO.get(params.int('id'))
        params.put("itemId", product.id)
        params.put("deleted", 'on' == params.showDeleted ? 1 : 0)
        def assets
        try {
            assets = productService.getFilteredAssets(session['company_id'], [] as List, [] as List, params, false)
        } catch (SessionInternalError e) {
            assets = []
            viewUtils.resolveException(flash, session.locale, e);
        }
        try {
            render getItemsJsonData(assets, params) as JSON
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Delete the selected asset as specified by the 'id' parameter
     */
    def deleteAsset () {
        if (params.id) {
            try {
                webServicesSession.deleteAsset(params.int('id'))

                log.debug("Deleted asset ${params.id}.");

                flash.message = 'asset.deleted'
                flash.args = [ params.id  as String]
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            }
        }

        params.id = params.itemId
        //reload and display the assets
        assets()

    }

    /**
     * Release reservation of the selected asset as specified by the 'id' parameter
     */
    def releaseAssetReservation (){
        def assetID = params.int('id')
        if (assetID) {
            try {
                webServicesSession.releaseAsset(assetID, null);
                log.debug("Reservation released for asset id: ${params.id}.");
                flash.message = 'asset.reservation.released'
                flash.args = [ params.id  as String]
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e);
            }
        }
        params.id = params.itemId
        assets()
    }

    /**
     * Load an asset for editing or display an empty screen for creating an asset.
     */
    def editAsset () {
        def asset = params.id ? AssetDTO.get(params.id) : new AssetDTO()
        //if param id is provided we are editing
        if (params.id && !asset) {
            flash.error = 'product.asset.not.found'
            flash.args = [ params.id  as String]

            redirect controller: 'product', action: 'list'
            return
        }

        //if id is not provided we must be adding
        if (!params.id && !params.boolean('add')) {
            flash.error = 'product.asset.not.selected'
            flash.args = [ params.id  as String]

            redirect controller: 'product', action: 'list'
            return
        }

        //if we are adding we must know which product it belongs to
        if (params.boolean('add') && !params.prodId) {
            flash.error = 'product.not.selected'
            flash.args = [ params.prodId  as String]
            redirect controller: 'product', action: 'list'
            return
        }

        if (params.boolean('add')) {
            ItemDTO itemDTO = ItemDTO.get(params.int('prodId'))
            if(!itemDTO){
                return response.sendError(Constants.ERROR_CODE_404)
            }
            asset.item = itemDTO
        }

        //find the item type which allows asset management. The asset identifier label is defined in the type.
        def categoryWithAssetManagement = asset.item.findItemTypeWithAssetManagement();

        //get the alowed statuses from the item type
        List orderedStatuses = new AssetStatusBL().getStatuses(categoryWithAssetManagement.id, false);
        def allowedStatuses = []
        orderedStatuses.each {
            if(it.isOrderSaved==0) {
                allowedStatuses << it
            }
        }
        if(!asset.id) {
            asset.assetStatus = orderedStatuses.find {it.isDefault}
        }
        //if we are editing we can create a breadcrumb
        if (params.id) {
            breadcrumbService.addBreadcrumb(controllerName, actionName, 'update', params.int('id'), asset.identifier)
        }

        def companies = []

        if(asset.item.isGlobal()){
            //if this asset can belong to other companies besides the user's
            if((params.userCompanyMandatory ? !params.boolean('userCompanyMandatory') : true)) {
                companies = retrieveChildCompanies()
            }
            companies << CompanyDTO.get(session['company_id'])
        }else{
            // only show the companies which are visible to the product.
            companies = asset.item.getEntities()
        }


		def availableCategories= productService.getItemTypes(session['company_id'], null)
		
        if(params.partial) {
            render template: 'editAssetContent', model: [asset : asset, availableCategories: availableCategories, statuses: allowedStatuses, categoryAssetMgmt: categoryWithAssetManagement, companies : companies, partial : true, userCompanyMandatory: params.userCompanyMandatory ?: 'false']
        } else {
            [ asset : asset, availableCategories: availableCategories, statuses: allowedStatuses, categoryAssetMgmt: categoryWithAssetManagement, companies : companies, userCompanyMandatory: params.userCompanyMandatory ?: 'false']
        }
    }

    /**
     * Display an asset. Asset ID is required.
     */
    def showAsset () {
        if (!params.id) {
            flash.error = 'product.asset.not.selected'
            flash.args = [ params.id  as String]
            redirect controller: 'product', action: 'list'
            return
        }

        //load the asset
        def asset = AssetDTO.get(params.id)
        def reservation = asset?.getId()?new AssetReservationDAS().findActiveReservationByAsset(asset?.getId()):null
        asset.setReserved((reservation?true:false) as Boolean)

        //find the category the asset belongs to
        def categories = ItemTypeDTO.createCriteria().list() {
            eq("allowAssetManagement", 1)
            createAlias("items","its")
            eq("its.id", asset.item.id)
        }

        if (params.id) {
            breadcrumbService.addBreadcrumb(controllerName, actionName, 'show', params.int('id'), asset.identifier)
        }

        //if we must show a template
        if(params.template) {
            render template: 'showAsset', model: [ asset : asset, category: categories?.first(), reservation : reservation]

        //else show the asset list
        } else {

            def itemId = asset.item.id

            def assets = AssetDTO.createCriteria().list(
                    max:    params.max
            ) { eq("item.id", itemId)
            }

            ItemTypeDTO assetManType = new ItemTypeBL().findItemTypeWithAssetManagementForItem(itemId)
            render view: 'assetList', model: [assets: assets, selectedAsset: asset, product: asset.item, id: itemId, category: categories?.first(),
                    metaFields:  assetManType.assetMetaFields.findAll{it.dataType != DataType.STATIC_TEXT}, assetStatuses:  assetManType.assetStatuses, reservation:reservation]
        }
    }

    /**
     * Validate and save an asset
     */
    def saveAsset () {
		def availableFields = new ArrayList<MetaField>()
        def asset = new AssetWS()
        //bind the parameters to the asset
        bindData(asset, params, [exclude: ['id', 'identifier']] )
        //bind the meta fields
		boolean isGlobal = asset?.global?: false
		def isRoot = new CompanyDAS().isRoot(session['company_id'] as Integer)
		
		if (!isRoot) {
			asset.global = false
			asset.entities = [session['company_id'] as Integer];
		} else if(isGlobal) {
			asset.global = true
			asset.entities = new ArrayList<Integer>(0);
		} else {
				//Validate for entities
				if(asset.getEntities() == null || asset.getEntities().size() == 0) {
					flash.error = 'validation.error.no.company.selected'
					return
				}
		}	
		
        ItemTypeDTO itemType = ItemDTO.get(asset.itemId).findItemTypeWithAssetManagement()
        asset.metaFields = MetaFieldBindHelper.bindMetaFields(itemType.assetMetaFields, params)

        asset.id = !params.id?.equals('') ? params.int('id') : null

		log.debug "entity id ${params['asset.entityId']}"

		if (params['asset.entityId']) {
			asset.entityId= params.int('asset.entityId')
		} else {
            asset.entityId = session['company_id'].toInteger();
        }
        asset.identifier = params.identifier.trim()
		
        //if this is an asset group bind the contained assets
        if(params.isGroup) {
            asset.containedAssetIds = params.containedAssetIds.split(',').findAll { it.length() > 0} .collect{ new Integer(it) } as Integer[]
        } else {
            asset.containedAssetIds = []
        }

        try {
            if (asset.id) {
                //if the user has access update the asset
                    webServicesSession.updateAsset(asset, session['company_id'] as Integer)
            } else {
                    asset.id = webServicesSession.createAsset(asset)
            }
        } catch (SessionInternalError e) {
            //got an exception, show the edit page again
            viewUtils.resolveException(flash, session.locale, e);
            List orderedStatuses = new AssetStatusBL().getStatuses(params.int('categoryId'), false);
            def dto = new AssetBL().getDTO(asset);
            dto.discard()

            def companies = []
            //if this asset can belong to other companies besides the user's
            if((params.userCompanyMandatory ? !params.boolean('userCompanyMandatory') : true)) {
                companies = retrieveChildCompanies()
            }
            companies << CompanyDTO.get(session['company_id'])
			def availableCategories= productService.getItemTypes(session['company_id'], null)
            render template: 'editAssetContent', 
					model: [asset : dto, statuses: orderedStatuses, categoryAssetMgmt: ItemTypeDTO.get(params.categoryId), 
							availableCategories: availableCategories, companies : companies, partial : true, 
							userCompanyMandatory: params.userCompanyMandatory ?: 'false']
            return
        }

        render "<asset id='"+asset.id+"' itemId='"+asset.itemId+"' />";
    }

    /**
     * Display the uploadAssets template.
     *
     * @param prodId    ItemDTO id that will be linked to the new assets
     */
    def showUploadAssets () {
        ItemDTO itemDTO                 = ItemDTO.get(params.prodId)
        ItemTypeDTO itemTypeDTO         = itemDTO.findItemTypeWithAssetManagement()
        AssetStatusDTO defaultStatus    = itemTypeDTO.findDefaultAssetStatus() //status that the new assets will have

        render template: 'uploadAssets', model: [product:  itemDTO, category: itemTypeDTO, defaultStatus: defaultStatus]
    }

    /**
     *  When searching for asset to add to the group, this page will load the status, product and meta fields filter for
     *  a given category.
     *
     *  @param categoryId   ItemTypeDTO id.
     */
    def loadAssetGroupFilters () {
        ItemTypeDTO itemTypeDTO         = ItemTypeDTO.get(params.categoryId)
        render template:  'groupSearchFilter', model:  [assetStatuses: itemTypeDTO?.assetStatuses, products: itemTypeDTO?.items?.findAll { it.deleted == 0 }, metaFields: itemTypeDTO?.assetMetaFields]
    }

    /**
     * Search function when trying to add assets to a group.
     */
    def groupAssetSearch () {
        def assets
        try {
            params.max = params?.max?.toInteger() ?: pagination.max
            params.offset = params?.offset?.toInteger() ?: pagination.offset
            params.sort = params?.sort ?: pagination.sort
            params.order = params?.order ?: pagination.order

            params['groupIdNull'] = 'true'
            params['orderLineId'] = 'NULL'
            params['categoryId'] = params['searchCategoryId']
            params['itemId'] = params['searchItemId']
            params['statusId'] = params['searchStatusId']

            //always the exclude the group we are editing from the search results.
            def assetsToExclude = (params.searchAssetId ? [params.int('searchAssetId')] : []) as List

            //exclude all currently selected assets
            if(params['searchExcludedAssetId']) {
                assetsToExclude.addAll(params.searchExcludedAssetId.split(',').collect{new Integer(it)})
            }

            //include the assets which are contained in the persisted group in case the user removed it in the UI and wants to add it back
            //if an ID is in the include and exclude set, the exclude will take precedence
            def assetsToInclude = (params.searchIncludedAssetId ? params.searchIncludedAssetId.split(',').collect{new Integer(it)} : []) as List

            //include assets which are part of this persisted asset group.
            if (params.searchAssetId) {
                params['groupId'] = params['searchAssetId']
            }

            assets = productService.getFilteredAssets(session['company_id'], assetsToExclude, assetsToInclude, params, false)
        } catch (SessionInternalError e) {
            assets = []
            viewUtils.resolveException(flash, session.locale, e);
        }
        render template:  'groupSearchResults', model:  [assets: assets]
    }

    /**
     * Upload a file containing new assets. Start a batch job to import the assets.
     *
     * @param assetFile - CSV file containing asset definitions
     * @param prodId - product the assets will belong to
     */
    def uploadAssets () {
        def file = request.getFile('assetFile');
        def reportAssetDir = new File(Util.getSysProp("base_dir") + File.separator + "reports" + File.separator + "assets");

        //csv file we are uploading
        String fileExtension = FilenameUtils.getExtension(file.originalFilename)
        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb
        def csvFile = File.createTempFile("assets", ".csv", reportAssetDir)
        if (fileExtension && !fileExtension.equals("csv")) {
            flash.error = "csv.error.found"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId, showAssetUploadTemplate: true])
            return
        } else if(!fileExtension) {
            flash.error = "validation.file.upload"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId, showAssetUploadTemplate: true])
            return
        }
        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile("assetsError", ".csv", reportAssetDir)

        //copy the uploaded file to a temp file
        file.transferTo(csvFile)

        ItemDTO itemDTO = ItemDTO.get(params.int('prodId'))

        def executionId = 0
		try {
	        //start a batch job to import the assets
	        executionId = webServicesSession.startImportAssetJob(itemDTO.id,
	                itemDTO.findItemTypeWithAssetManagement().assetIdentifierLabel ?: message([code:  'asset.detail.identifier']),
	                message([code:  'asset.detail.notes']),message([code:  'asset.detail.global']),message([code:  'asset.detail.entities'])
	                ,csvFile.absolutePath, csvErrorFile.absolutePath)
		} catch (SessionInternalError e) {
			viewUtils.resolveException(flash, session.locale, e);
			render view: 'uploadAssets'
		}
        render view: 'processAssets', model: [jobId: executionId, jobStatus: 'busy']
    }

    /**
     * Validate and save a category.
     */
    
    def saveCategory () {
        def category = new ItemTypeWS()
        def metaFieldIdxs = []
        def metafields = []
        def statuses = []

        def isRoot = new CompanyDAS().isRoot(session['company_id'])

        // grails has issues binding the ID for ItemTypeWS object...
        // bind category ID manually
        bindData(category, params, 'id')
        bindMetaFields(category,params,isRoot,EntityType.PRODUCT_CATEGORY)
        category.id = !params.id?.equals('') ? params.int('id') : null

		def isNew = false
		def rootCreated = false

		if(!category.id || category.id == 0) {
			isNew = true
		}

		try {
			//Validation during edit category.
			if(!isNew && !category.global){
				//Load all product for this category to make sure none of the products refer a company thats not part of this category
				def itemTypesArr = new ArrayList()
				itemTypesArr.add(ItemTypeDTO.get(category.id))
				log.debug "TYPES:"+itemTypesArr
		        def items = ItemDTO.createCriteria().list() {
		            createAlias("itemTypes", "itemTypes")
		            and {
						'in'('itemTypes.id', itemTypesArr?.id)
		                eq('deleted', 0)
		            }
		        }
		        def companies = new java.util.HashSet()
		        for(ItemDTO item: items){
		        	companies.addAll(item.entities)
		        }

		        for(CompanyDTO co: companies){
		        	def found = false
		        	def notFoundEntity = null
		        	for(Integer entId: category.entities){
		        		if(co.id==entId){
		        			found = true
		        		}
		        	}

		        	if(!found){
						SessionInternalError exception = new SessionInternalError("Validation of Entities");
	                    String[] errmsgs = new String[1]
	                    errmsgs[0] = "ItemTypeWS,companies,validation.error.wrong.company.selected.category," + CompanyDTO.get(co.id)?.description
	                    exception.setErrorMessages(errmsgs);
	                    throw exception;
					}
		        }
			}

            category.allowAssetManagement = params.allowAssetManagement ? 1 : 0

            //BIND THE STATUSES
            def assetIdxs = []
            Pattern pattern = Pattern.compile(/assetStatus.(\d+).id/)
            //get all the ids in an array
            params.each{
                def m = pattern.matcher(it.key)
                if( m.matches()) {
                    assetIdxs << m.group(1)
                }
            }

            //get the status values for each id and create the statuses
            assetIdxs.each {
                def name = params['assetStatus.'+it+'.description']
                AssetStatusDTOEx status = new AssetStatusDTOEx(
                        description: params['assetStatus.'+it+'.description']
                )

                BindHelper.bindPropertyPresentToInteger(params, status, ["isDefault", "isAvailable", "isOrderSaved"], 'assetStatus.'+it+'.')
                BindHelper.bindInteger(params, status, ["id"], 'assetStatus.'+it+'.')

                if(status.description.length() > 0 || status.id > 0) {
                    statuses << status
                }
            }

            //BIND THE META FIELDS
            pattern = Pattern.compile(/metaField(\d+).id/)
            //get all the ids in an array
            params.each{
                def m = pattern.matcher(it.key)
                if( m.matches()) {
                    metaFieldIdxs << m.group(1)
                }
            }

            //get the meta field values for each id a
            metaFieldIdxs.each {
                MetaFieldWS metaField = MetaFieldBindHelper.bindMetaFieldName(params, it)
                metaField.primary = false
                metaField.entityId = session['company_id']

                metafields << metaField
            }

            //if asset management is enabled, set statuses and meta fields
            if(category.allowAssetManagement == 1) {
                category.assetStatuses.addAll(statuses)
                category.assetMetaFields.addAll(metafields)
            }

			if(category.isGlobal()) {
				//Empty entities
				category.entities = new ArrayList<Integer>(0);
			} else {
				//Validate for entities
				if(category.getEntities() == null || category.getEntities().size() == 0) {
					String [] errors = ["ItemTypeWS,companies,validation.error.no.company.selected"]
					throw new SessionInternalError("validation.error.no.company.selected", errors)
				}
			}
		} catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            renderEditCategoryView(category, statuses, metafields)
            return
        }

        // save or update
        try {
			
			if(category.description) {
				category.description= category.description.trim()
			}

            if (!category.id || category.id == 0) {
                    if (category.description?.trim()) {
                        log.debug("creating product category ${category}")


						category.id = webServicesSession.createItemCategory(category)

                        flash.message = 'product.category.created'
                        flash.args = [category.id as String]
                    } else {
                        log.debug("there was an error in the product category data.")

                        category.description = StringUtils.EMPTY

                        flash.error = message(code: 'product.category.error.name.blank')

                        render view: "editCategory", model: [category: category]
                        return
                    }
            } else {
                    if (category.description?.trim()) {
                        log.debug("saving changes to product category ${category.id}, ${category.isGlobal()}")

						webServicesSession.updateItemCategory(category)

                        flash.message = 'product.category.updated'
                        flash.args = [category.id as String]
                    } else {
                        log.debug("there was an error in the product category data.")

                        category.description = StringUtils.EMPTY

                        flash.error = message(code: 'product.category.error.name.blank')
                        render view: "editCategory", model: [category: category]
                        return
                    }
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            renderEditCategoryView(category, statuses, metafields)
            return
        }

        chain action: 'list', params: [id: category.id]
    }

    /**
     * copy input fields onto the category before rendering the editCategory view.
     *
     * @param category      category to display
     * @param metaFieldIdxs meta field indexes as passed in through the params
     */
    private void renderEditCategoryView(ItemTypeWS category, Collection statuses, Collection metafields) {
        List<MetaField> availableFields = new ArrayList<MetaField>()
		ItemTypeDTO categoryForUI = new ItemTypeDTO();
		bindData(categoryForUI, params, 'id')
		categoryForUI.setDescription(category.getDescription());
		categoryForUI.setGlobal(category.isGlobal());
		categoryForUI.setOrderLineTypeId(category.getOrderLineTypeId());
		categoryForUI.setAssetIdentifierLabel(category.getAssetIdentifierLabel());
		categoryForUI.setOnePerCustomer(category.isOnePerCustomer());
		categoryForUI.setOnePerOrder(category.isOnePerOrder());

		Set<CompanyDTO> childEntities = new HashSet<CompanyDTO>(0);

		for(Integer entity : category.getEntities()){
			childEntities.add(new CompanyDAS().find(entity));
		}

        //select meta fields
        if (category.isGlobal()) {
            for(Integer companyId : retrieveCompaniesIds()) {
                availableFields.addAll(retrieveAvailableCategoryMetaFields(companyId))
            }
        } else if(category.entities){
            for(def company : category.entities) {
                availableFields.addAll(retrieveAvailableCategoryMetaFields(company))
            }
        }else{
            availableFields.addAll(retrieveAvailableCategoryMetaFields(session['company_id']))
        }

		categoryForUI.setEntities(childEntities)

        categoryForUI.allowAssetManagement = params.allowAssetManagement ? 1 : 0

        categoryForUI.assetStatuses = new AssetStatusBL().convertAssetStatusDTOExes(statuses)
        categoryForUI.assetMetaFields = MetaFieldBL.convertMetaFieldsToDTO(metafields, session['company_id']);

        ItemTypeBL.fillMetaFieldsFromWS(categoryForUI, category)

        render view: "editCategory", model: [category: categoryForUI,companies : retrieveChildCompanies(), allCompanies : retrieveCompanies(),
                orderedStatuses: (params.id ? categoryForUI.assetStatuses.findAll { it.isInternal == 0 } : []),
                availableFields: availableFields, availableFieldValues:category.metaFields,
                parentCategories: getProductCategories(false, category?.id ?: null), entityId: category.entityId]
    }

    /**
     * Get the item category to be edited and show the "editCategory.gsp" view. If no ID is given
     * this view will allow creation of a new category.
     */
    def editCategory () {
        def category = params.id ? ItemTypeDTO.get(params.id) : new ItemTypeDTO()
        List<MetaField> availableFields
        if (params.id && !category) {
            flash.error = 'product.category.not.found'
            flash.args = [ params.id  as String]

            redirect controller: 'product', action: 'list'
            return
        }

        if (!params.id && !params.boolean('add')) {
            flash.error = 'product.category.not.selected'
            flash.args = [ params.id  as String]

            redirect controller: 'product', action: 'list'
            return
        }

        if(category.isGlobal()){
            availableFields  = MetaFieldBL.getMetaFields(retrieveCompaniesIds(), EntityType.PRODUCT_CATEGORY)
        }else if (params.id && new CompanyDAS().isRoot(Integer.parseInt(session['company_id'].toString())) ){
            availableFields = MetaFieldBL.getMetaFields(category.getEntities()*.id, EntityType.PRODUCT_CATEGORY)
        }else if (params.id){
            availableFields = MetaFieldBL.getMetaFields([Integer.parseInt(session['company_id'].toString())], EntityType.PRODUCT_CATEGORY)
        }else{
            availableFields = retrieveAvailableCategoryMetaFields(session['company_id'])
        }

        MetaFieldValueWS[] availableFieldValues = MetaFieldBL.convertMetaFieldsToWS(availableFields, category);
        List orderedStatuses = (params.id ? new AssetStatusBL().getStatuses(category.id, false) : [])

        breadcrumbService.addBreadcrumb(controllerName, actionName, params.id ? 'update' : 'create', params.int('id'), category?.description)

        [category : category, orderedStatuses: orderedStatuses, parentCategories: getProductCategories(false, category?.id ?: null),
         companies: retrieveChildCompanies(), allCompanies: retrieveCompanies(), entityId: category?.entity?.id,
         availableFields: availableFields,availableFieldValues:availableFieldValues]
    }

    /**
     * Use the meta fields which are part of a metafield group to act as template for category meta fields
     *
     * @param groupId - MetaFieldGroup id
     */
    def populateCategoryMetaFieldsForEdit () {
        List<MetaFieldWS> metaFields;
        if(params.groupId && params.groupId != 'null') {
            MetaFieldGroup group = MetaFieldGroup.get(params.int('groupId'))
            metaFields = group.metaFields.collect {MetaFieldBL.getWS(it)}
            metaFields.each {
                it.id = 0
            }
        } else {
            metaFields = new ArrayList<>(0);
        }
        render template: 'editCategoryMetaFieldsCollection', model: [ metaFields: metaFields, startIdx: params.startIdx ? params.int('startIdx'): 0, moveMetaFields: true]
    }

    /**
     * Use the meta field specified by 'mfId' to act as template for category meta field
     *
     * @param mfId - MetaField id
     */
    def populateMetaFieldForEdit () {
        MetaFieldWS metaField;
        if(params.mfId && params.mfId != 'null') {
            metaField = MetaFieldBL.getWS(MetaField.read(params.int('mfId')))
            metaField.id = 0
        } else {
            metaField = null
        }
        render template: 'editCategoryMetaField', model: [ metaField: metaField, metaFieldIdx: params.startIdx ?: 0, moveMetaFields: true ]
    }


    /**
     * Get the item to be edited and show the "editProduct.gsp" view. If no ID is given
     * this screen will allow creation of a new item.
     */
    def editProduct () {

        ItemDTOEx product
        def availableFields

        try {
            //product = params.id ? webServicesSession.getItem(params.int('id')) : null
            ItemDTO itemDto= ItemDTO.get(params.int('id'))
            if (itemDto) {
                if (itemDto.entityId == session['company_id'] as Integer || itemDto.entities.contains(session['company_id'] as Integer) ) {
                    product = params.id ? webServicesSession.getItem(params.int('id'), session['user_id'] as Integer, null) : null
                } else {
                    //item does not belong to hierarchy
                }
            }

            if (product && product.deleted == 1) {
            	productNotFoundErrorRedirect(params.id)
            	return
            }
        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)
            productNotFoundErrorRedirect(params.id)
            return
        }

		// Combine child entities and root entity into a single list
		def entities = new ArrayList<Integer>(0);
		if(!product?.isGlobal()) {
			if(product?.entities?.size() > 0) {
				for(def entity : product.entities) {
					entities.add(entity)
				}
			}
		}

        if (params.copyFrom) {
            cleanProduct(product)
        }

		availableFields = getMetaFields(product)

        breadcrumbService.addBreadcrumb(controllerName, actionName, params.id ? 'update' : 'create', params.int('id'), product?.number)
        def categories = getProductCategories(false, null)
        def typeSet = (product ? product.types : []) as Set
        def allowAssetManagement = false
		def subscriptionCategory = false
        List<CompanyDTO> categoriesRelatedCompanies

        List<ItemTypeDTO> selectedItemTypes;
        try{
            selectedItemTypes = categories.findAll {
                params.list('category').collect { Integer.valueOf(it as String) }.contains(it.id)
            }
        } catch (NumberFormatException nfe){
            return response.sendError(Constants.ERROR_CODE_404)
        }

        if(selectedItemTypes){
            categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(selectedItemTypes)
        }else{
            categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(categories)
        }

        categories.each {
            if(typeSet.contains(it.id) && it.allowAssetManagement) {
                allowAssetManagement = true
            }
            if(it.orderLineTypeId == Constants.ORDER_LINE_TYPE_SUBSCRIPTION) {
                def currentCategoryId = it.id;
                if (params.category == "" + currentCategoryId) subscriptionCategory = true
                product?.types.each {
                    if ("" + it == currentCategoryId) subscriptionCategory = true
                }

			}
        }
        Integer[] excludedItemTypeIds = new ArrayList() as Integer[]
        if(product) {
            excludedItemTypeIds = product.getDependencyIdsOfType(ItemDependencyType.ITEM_TYPE)
        }

		def showEntityListAndGlobal = CompanyDTO.get(product?.entityId)?.parent == null
        def isCategoryGlobal

        if( product?.id || params.copyFrom ) {
            isCategoryGlobal = categories.any {typeSet.contains(it.id) && it.global}
        } else {
            ItemTypeDTO category = ItemTypeDTO.get(params.int('category'))
            if(!category){
                return response.sendError(Constants.ERROR_CODE_404)
            }
            isCategoryGlobal = category?.global ?: false
            allowAssetManagement = category.allowAssetManagement > 0
        }
        Integer assetReservationDefaultValue = PreferenceBL.getPreferenceValue(session['company_id'] as int, CommonConstants.PREFERENCE_ASSET_RESERVATION_DURATION) as int

        [product: product, currencies: retrieveCurrencies(), categories: categories, categoryId: params.int('category'),
                availableFields: availableFields,
                dependencyItemTypes: getDependencyItemTypes(excludedItemTypeIds), dependencyItems: null,
                dependentTypes: product?.getDependenciesOfType(ItemDependencyType.ITEM_TYPE),
                dependentItems: product?.getDependenciesOfType(ItemDependencyType.ITEM),
                availableAccountTypes: getAvailableAccountTypes(), allowAssetManagement: allowAssetManagement, subscriptionCategory : subscriptionCategory,
                orderLineMetaFields: product?.orderLineMetaFields, companies : retrieveChildCompanies(), allCompanies : categoriesRelatedCompanies,
                entities: entities, showEntityListAndGlobal: showEntityListAndGlobal, isCategoryGlobal: isCategoryGlobal, assetReservationDefaultValue: assetReservationDefaultValue]
    }

    def getCategoriesCompanies() {

        def categories = getProductCategories(false, null)
        List<CompanyDTO> categoriesRelatedCompanies

        List<ItemTypeDTO> selectedItemTypes = categories.findAll {
            params.list('productTypes[]').collect { Integer.valueOf(it) }.contains(it.id)
        }

        if(selectedItemTypes){
            categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(selectedItemTypes)
        }else{
            categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(categories)
        }

        StringBuilder options = new StringBuilder()

        categoriesRelatedCompanies.each{ CompanyDTO company ->
            options.append("<option value='${company.id}'>${company.description} </option>")
        }

        render( options.toString() )
    }

    /**
     * Remove all entities Id, in this way the product will be saved like a new entity
     * (with all new subentities associated to it)
     * @param product
     */
    def cleanProduct(product) {
        if (product != null) {
            product.id = null
            params.id = null

            cleanPrice(product?.defaultPrice)
            for (price in product.defaultPrices) {
                cleanPrice(price.value)
            }

            for (orderLineMetafield in product.orderLineMetaFields) {
                orderLineMetafield.id = 0
                orderLineMetafield.entityId = null
            }
            for (dependency in product.dependencies) {
                dependency.id = null
            }
            for (metaField in product.metaFields) {
                metaField.id = null
            }
            for (metaFieldMapEntry in product.metaFieldsMap) {
                for (metaField in metaFieldMapEntry.value) {
                    metaField.id = null
                }
            }
        }
    }

    def cleanPrice(price) {
        if (price != null) {
            price.id = null
            cleanPrice(price.next)
        }
    }
    /**
     * Use the meta field specified by 'mfId' to act as template for product orderline meta field
     *
     * @param mfId - MetaField id
     */
    def populateProductOrderLineMetaFieldForEdit (){
        MetaFieldWS metaField;
        if(params.mfId && params.mfId != 'null') {
            metaField = MetaFieldBL.getWS(MetaField.read(params.int('mfId')))
            metaField.id = 0
        } else {
            metaField = null
        }
        render template: 'editProductMetaField', model: [ metaField: metaField, metaFieldIdx: params.startIdx ?: 0, moveMetaFields: true ]
    }


    private void productNotFoundErrorRedirect(productId) {
    	flash.error = 'product.not.found'
		flash.args = [ productId as String ]
		redirect controller: 'product', action: 'list'
    }

	def validateProductSave(product) {
		if(product.global) {
			//If product is global.. then check if it is associated with a global category, else render validation error
			boolean flag = false;
			Integer[] types = product.types
			List<CompanyDTO> companyDTOList = retrieveCompanies()
			List<ItemTypeDTO> itemTypeDTOList = ItemTypeDTO.getAll(types)
			List<CompanyDTO> associatedCompanyDTOList = itemTypeDTOList*.entities.flatten()
			associatedCompanyDTOList = associatedCompanyDTOList.unique()
			if(!itemTypeDTOList.any { it.isGlobal() }){
				String [] errors = [
					"ItemDTOEx,companies,validation.error.no.company.category.mismatch," + ((companyDTOList - associatedCompanyDTOList)*.description)?.first()
				]
				throw new SessionInternalError("validation.error.no.company.category.mismatch", errors)
			}
			return;
		}
		List<Integer> entityIds = product.entities
		Integer[] types = product.types
		for(Integer entityId : entityIds){
			CompanyDTO co = CompanyDTO.get(entityId)
			boolean flag = false;
			for(Integer typeId : types){
				flag = false;
				ItemTypeDTO itemType = ItemTypeDTO.get(typeId)
				if(itemType.global){
					//If any of the selected types is global.. then the product will have visibility.. no need to validate
					flag=true;
				}
				Set<CompanyDTO> entities = itemType.entities
				for(CompanyDTO compDTO : entities){
					if(compDTO.id.equals(co.id)){
						flag = true;
						break;
					}
				}
				//if any category is neither global nor visible to the selected entity then show validation message
				if (!flag) {
					break;
				}
			}
			if(!flag){
				String [] errors = [
					"ItemDTOEx,companies,validation.error.no.company.category.mismatch," + co?.description?.decodeHTML()
				]
				throw new SessionInternalError("validation.error.no.company.category.mismatch", errors)
			}
		}
	}
	
    /**
     * Validate and save a product.
     */
    def saveProduct () {
		def oldProduct = params."product.id" ? webServicesSession.getItem(params.int('product.id'), session['user_id'], null) : null
        def product = new ItemDTOEx()

		def availableFields = new ArrayList<MetaField>()

        //BIND THE META FIELDS
        def metaFieldIdxs = []
        def pattern = Pattern.compile(/metaField(\d+).id/)
        //get all the ids in an array
        params.each{
            def m = pattern.matcher(it.key)
            if( m.matches()) {
                metaFieldIdxs << m.group(1)
            }
        }

        product.orderLineMetaFields = new MetaFieldWS[metaFieldIdxs.size()];
        int index = 0;
        //get the meta field values for each id
        metaFieldIdxs.each {
            MetaFieldWS metaField = MetaFieldBindHelper.bindMetaFieldName(params, it)
            metaField.primary = false
            metaField.entityType = EntityType.ORDER_LINE
            metaField.entityId = session['company_id']
            product.orderLineMetaFields[index] = metaField;
            index++;
        }

        try {

			def isRoot = new CompanyDAS().isRoot(session['company_id'])

			bindProduct(product, oldProduct, params, isRoot)

            / * #11258 need to check if there is a comma or dot, because grails data binder removes the numbers after the dot
            and, in the case of the comma, grails removes the comma, i.e.: 56,9 is 569 */
            validateReservationDuration(params.product.reservationDuration, product)

			//validateProductSave(product)			

			boolean isGlobal = product?.global
			def isNew = false
			def rootCreated = false
			def existing

			if(!product.id || product.id == 0) {
				isNew = true
			} else {
				existing = ItemDTO.get(product?.id)?.entity
				rootCreated =existing == null || existing?.parent == null
			}

			if(isGlobal) {
				product.entities = new ArrayList<Integer>(0);
			} else {
				if( org.apache.commons.collections.CollectionUtils.isEmpty(product.entities) ) {
					String [] errors = ["ItemDTOEx,companies,validation.error.no.company.selected"]
					throw new SessionInternalError("validation.error.no.company.selected", errors)
				}
				//Find all entities belonging to the categories of this product
				def catEntities = new java.util.HashSet()
				for (def typeId in product?.types){
					def itemType = ItemTypeDTO.get(typeId)
					if(itemType.isGlobal()){
                        if(new CompanyDAS().isRoot(Integer.valueOf(session['company_id']))){
                            // if current company is root then find children
                            catEntities.addAll(retrieveCompanies())
                        }else{
                            // else find all the companies from root of caller company
                            catEntities.addAll(retrieveCompaniesForNonRootCompany())
                        }
                        break
					}
					catEntities.addAll(itemType.entities)
				}

				log.debug "CAT ENT:"+catEntities
				//Now ensure the entities selected is present in the list of companies for the category
				for(Integer entId : product.entities){
					def found=false
					for(CompanyDTO entity: catEntities){
						if(entId==entity.id){
							found=true
						}
					}

					if(!found){
						SessionInternalError exception = new SessionInternalError("Validation of Entities");
                        String[] errmsgs = new String[1]
                        errmsgs[0] = "ItemDTOEx,companies,validation.error.wrong.company.selected," + CompanyDTO.get(entId)?.description;
                        exception.setErrorMessages(errmsgs);
                        throw exception;
					}
				}

			}

			//select meta fields
			if (isGlobal) {
				for(Integer companyId : retrieveCompaniesIds()) {
					availableFields.addAll(retrieveAvailableMetaFields(companyId))
				}
			} else {
				for(def company : product.entities) {
					availableFields.addAll(retrieveAvailableMetaFields(company))
				}
			}

			if(oldProduct?.global && !isGlobal) {
				if(new OrderDAS().findOrdersOfChildsByItem(product?.id) > 0) {
					String [] errors = ["ProductWS,global,validation.error.cannot.restrict.visibility"]
					throw new SessionInternalError("validation.error.cannot.restrict.visibility", errors)
				}
			}

            // validate cycle in dependencies only for product edit
            def mandatoryItems = product.getMandatoryDependencyIdsOfType(ItemDependencyType.ITEM)
            if (product.id && product.id > 0 && mandatoryItems) {
                 if (findCycleInDependenciesTree(product.id, Arrays.asList(mandatoryItems) )) {
                     String[] errmsgs= new String[1];
                     errmsgs[0]= "ItemDTOEx,mandatoryItems,product.error.dependencies.cycle"
                     throw new SessionInternalError("There is an error in product data.", errmsgs );
                 }
            }

			
            // save or update
            if (!product.id || product.id == 0) {
                    log.debug("creating product ${product}")
                    product.id = webServicesSession.createItem(product)
                    flash.message = 'product.created'
                    flash.args = [product.id]
            } else {
                    log.debug("saving changes to product ${product.id}")
                    log.debug("Child entities =  ${product.entities}")
					webServicesSession.updateItem(product)
                    flash.message = 'product.updated'
                    flash.args = [product.id]
            }

        } catch (SessionInternalError e) {
            log.error("Error is: " + e)
            viewUtils.resolveException(flash, session.locale, e);

            if(product.standardPartnerPercentage){
                try{
                    Double.parseDouble(product.standardPartnerPercentage)
                }
                catch(NumberFormatException ex){
                    product.standardPartnerPercentage = null
                }
            }

            if(product.masterPartnerPercentage){
                try{
                    Double.parseDouble(product.masterPartnerPercentage)
                }
                catch(NumberFormatException ex){
                    product.masterPartnerPercentage = null
                }
            }

			def startDate = params.startDate ? DateTimeFormat.forPattern(message(code: 'date.format')).parseDateTime(params.startDate).toDate() : new Date();

            Integer[] excludedItemTypeIds = new ArrayList() as Integer[]
            if(product) {
                excludedItemTypeIds = product.getDependencyIdsOfType(ItemDependencyType.ITEM_TYPE)
            }

			def showEntityListAndGlobal = CompanyDTO.get(oldProduct?.entityId)?.parent == null
            def categories = getProductCategories(false, null)
            List<CompanyDTO> categoriesRelatedCompanies

            List<ItemTypeDTO> selectedItemTypes = categories.findAll {
                params.list('product.types').collect { Integer.valueOf(it) }.contains(it.id)
            }

            if(selectedItemTypes){
                categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(selectedItemTypes)
            }else{
                categoriesRelatedCompanies = retrieveCategoryRelatedCompanies(categories)
            }

            def isCategoryGlobal=selectedItemTypes.any{it.global}
            Boolean allowAssetManagement = selectedItemTypes.any { it.allowAssetManagement > 0 }

            render view: 'editProduct',
			 	model: [ product: product,
                         categories: categories,
                         currencies: retrieveCurrencies(),
						 companies : retrieveChildCompanies(),
						 allCompanies : categoriesRelatedCompanies,
					 	 category: params?.selectedCategoryId,
                         availableAccountTypes : getAvailableAccountTypes(),
                         availableFields: availableFields,
                         dependencyItemTypes: getDependencyItemTypes(excludedItemTypeIds),
                         dependencyItems: null,
                         dependentTypes: product?.getDependenciesOfType(ItemDependencyType.ITEM_TYPE),
                         dependentItems: product?.getDependenciesOfType(ItemDependencyType.ITEM),
                         orderLineMetaFields: product.orderLineMetaFields,
						 entities : product?.entities,
						 showEntityListAndGlobal : showEntityListAndGlobal,
                         allowAssetManagement : allowAssetManagement,
                         isCategoryGlobal: isCategoryGlobal]
				 		 return
        }
		chain action: 'show', params: [id: product.id, selectedCategory: product.types[0]]
    }

	def retrieveMetaFields () {
		List entities = params['entities'].tokenize(",")
        EntityType entityType = params['entityType'] ? EntityType.valueOf(params['entityType']) : null
        MetaFieldValueWS[] availableFieldValues = null
        ItemTypeDTO category = params.int('categoryId') ? ItemTypeDTO.get(params.int('categoryId')) : null

		def availableFields
        List<Integer> entityIds = entities.collect { Integer.parseInt(it) }
        availableFields = MetaFieldBL.getMetaFields(entityIds, entityType)

        if(category){
            availableFieldValues = MetaFieldBL.convertMetaFieldsToWS(availableFields, category);
        }

		render template : '/metaFields/editMetaFields',
			   model : [availableFields: availableFields, fieldValues: availableFieldValues]
	}

	def retrieveAllMetaFields (){
        EntityType entityType = params['entityType'] ? EntityType.valueOf(params['entityType']) : null
        MetaFieldValueWS[] availableFieldValues = null
        ItemTypeDTO category = params.int('categoryId') ? ItemTypeDTO.get(params.int('categoryId')) : null

		def availableFields  = MetaFieldBL.getMetaFields(retrieveCompaniesIds(), entityType)

        if(category){
            availableFieldValues = MetaFieldBL.convertMetaFieldsToWS(availableFields, category);
        }

		render template : '/metaFields/editMetaFields',
				model : [availableFields: availableFields, fieldValues: availableFieldValues]
	}

	def getAvailableMetaFields () {
		render template : '/metaFields/editMetaFields',
				model : [availableFields: null, fieldValues: null]
	}

    def bindProduct(product, oldProduct, params, isRoot) {
		bindData(product, params, 'product')

		// set new product's map is equal to old one, so in case of child values are preserved
		if (oldProduct != null) {
			product.metaFieldsMap = oldProduct.metaFieldsMap
    	}

        bindMetaFields(product, params, isRoot);

		// bind parameters with odd types (integer booleans, string integers etc.)
		product.priceManual = params.product.priceManual ? 1 : 0
		product.hasDecimals = params.product.hasDecimals ? 1 : 0
		
		//bind prices
			def prices = params.prices.collect { currencyId, price ->
				
				def itemPrice = new ItemPriceDTOEx()
				itemPrice.price = price.equals("") ? null : price
				itemPrice.currencyId = currencyId as Integer
				return itemPrice
			}
			
			product.prices = prices
		
        //bind dependencies
        def dependencies = []
        params.each { key, value ->
            if (key.startsWith("dependency.")) {
                String[] tokens = key.substring(key.indexOf('.')+1).split(":")
                dependencies << new ItemDependencyDTOEx(type: tokens[0]=='Types'?ItemDependencyType.ITEM_TYPE : ItemDependencyType.ITEM,
                        dependentId: new Integer(tokens[3]),  dependentDescription: params[(key)],
                        minimum: (tokens[1].length() > 0 && !tokens[1].equals("null") ? new Integer(tokens[1]) : 0),
                        maximum: (tokens[2].length() > 0 && !tokens[2].equals("null") ? new Integer(tokens[2]) : null) )
            }
        }
        product.dependencies = dependencies as ItemDependencyDTOEx[]

        // if a non-numeric value is entered for product's percentage, standardPartnerPercentage and masterPercentage

        if(params?.product?.standardPartnerPercentageAsDecimal && !product?.standardPartnerPercentage){
            product.standardPartnerPercentage = params?.product?.standardPartnerPercentageAsDecimal
        }

        if(params?.product?.masterPartnerPercentageAsDecimal && !product?.masterPartnerPercentage){
            product.masterPartnerPercentage = params?.product?.masterPartnerPercentageAsDecimal
        }

        // bind parameters with odd types (integer booleans, string integers  etc.)
        product.hasDecimals = params.product.hasDecimals ? 1 : 0
        product.assetManagementEnabled = params.product.assetManagementEnabled ? 1 : 0

    }

    def priceIsChanged(def pricesDateMap, def price, def startDateForPrice) {
        if (pricesDateMap?.containsKey(startDateForPrice)) {
            def oldPrice = PriceModelBL.getWS(pricesDateMap.get(startDateForPrice))
            if (oldPrice &&
                    oldPrice.getType().equals(price.getType()) &&
                    oldPrice.getRateAsDecimal().compareTo(price.getRateAsDecimal()) == 0 &&
                    oldPrice.getCurrencyId().equals(price.getCurrencyId())) {
                def oldPricesAttributes = oldPrice.getAttributes()
                if (oldPricesAttributes.isEmpty() && price.getAttributes().isEmpty()) {
                    return false;
                } else if (oldPricesAttributes.size() != price.getAttributes().size()) {
                    return true;
                } else {
                    for (String oldAttributKey: oldPricesAttributes.keySet()) {
                        if (!(price.getAttributes().containsKey(oldAttributKey) &&
                                price.getAttributes().get(oldAttributKey).equals(oldPricesAttributes.get(oldAttributKey)))) {
                            return true
                        }
                    }
                }
                return false
            }
        }
        return true
    }

    private def isMappingAlreadyDefined(def mappings, def otherMapping){
        for(mapping in mappings){
            if (mapping.routeId == otherMapping.routeId &&
                    mapping.routeValue == otherMapping.routeValue){
                return true
            }
        }
        return false
    }

    def retrieveCurrencies() {
        def currencies = new CurrencyBL().getCurrenciesWithoutRates(session['language_id'].toInteger(), session['company_id'].toInteger(),true)
		return currencies.findAll { it.inUse }
    }

	def retrieveChildCompanyIds() {
		def ids = new ArrayList<Integer>(0);
		for(CompanyDTO company : retrieveChildCompanies()){
			ids.add(company.getId())
		}
		return ids;
	}

	def retrieveChildCompanies() {
        List<CompanyDTO> companies = CompanyDTO.findAllByParent(CompanyDTO.get(session['company_id']))
        return companies
	}

	def retrieveCompanies() {
		def companies = retrieveChildCompanies()
		companies.add(CompanyDTO.get(session['company_id']))

		return companies
	}

    def retrieveCompaniesForNonRootCompany() {
        CompanyDTO childCompany = CompanyDTO.get(session['company_id'])
        List<CompanyDTO> companies = CompanyDTO.findAllByParent(childCompany.parent)
        companies.add(childCompany.parent)
        return companies
    }

    private List<CompanyDTO> retrieveCategoryRelatedCompanies(List<ItemTypeDTO> itemTypeDTOList){

        if(itemTypeDTOList.any {it.isGlobal()}){
             retrieveCompanies()
        }else{
            CompanyDTO.createCriteria().list(){
                createAlias("itemTypes", "itemTypes",CriteriaSpecification.LEFT_JOIN);
                'in'('itemTypes.id',itemTypeDTOList*.id)
            }.unique()
        }
    }

	def retrieveCompaniesIds() {
		def ids = new ArrayList<Integer>();
		for(CompanyDTO dto : retrieveCompanies()){
			ids.add(dto.getId())
		}
		return ids
	}

    def retrieveAvailableMetaFields(entityId) {
		return MetaFieldBL.getAvailableFieldsList(entityId, EntityType.PRODUCT)
    }

    List<MetaField> retrieveAvailableCategoryMetaFields(entityId) {
		return MetaFieldBL.getAvailableFieldsList(entityId, EntityType.PRODUCT_CATEGORY)
    }

	private def bindMetaFields(product, params, isRoot, EntityType entityType) {
		def fieldsArray
		MetaFieldValueWS[] metaFields = null
		List<MetaFieldValueWS> values = new ArrayList<MetaFieldValueWS>()
		if(isRoot) {
			for(Integer entityId : retrieveCompaniesIds()) {
                if(entityType && (entityType == EntityType.PRODUCT_CATEGORY)){
                    fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableCategoryMetaFields(entityId), params)
                }else{
                    fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableMetaFields(entityId), params)
                }
				metaFields = fieldsArray
				values.addAll(fieldsArray)
				product.metaFieldsMap.put(entityId, metaFields)
			}
		} else {
            if(entityType && (entityType == EntityType.PRODUCT_CATEGORY)){
                fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableCategoryMetaFields(session["company_id"]), params)
            }else{
                fieldsArray = MetaFieldBindHelper.bindMetaFields(retrieveAvailableMetaFields(session["company_id"]), params)
            }
			metaFields = fieldsArray
			values.addAll(fieldsArray)
			product.metaFieldsMap.put(session["company_id"], metaFields)
		}
		product.metaFields = values
    }

    private def bindMetaFields(product, params, isRoot) {
        bindMetaFields(product, params, isRoot, null)
    }

    def findMetaFieldType(Integer metaFieldId) {
        for (MetaField field : retrieveAvailableMetaFields(session["company_id"])) {
            if (field.id == metaFieldId) {
                return field;
            }
        }
        return null;
    }

	/**
	 * This call returns meta fields according to an entity
	 * @param product
	 * @return
	 */
	def getMetaFields (product) {
		def isRoot = new CompanyDAS().isRoot(session['company_id'])
		def availableFields = new HashSet<MetaField>();
		if(!product || !product?.id || product?.id == 0) {
			//availableFields.addAll(retrieveAvailableMetaFields(session['company_id']))
		} else {
			if(product.global) { //TODO Global Meta Fields only?
				for(Integer entityId : retrieveCompaniesIds()) {
					availableFields.addAll(retrieveAvailableMetaFields(entityId))
				}
			} else if (isRoot) { //TODO Root specific product meta fields copied to child entities?
				if(product?.entityId) {
					availableFields.addAll(retrieveAvailableMetaFields(product.entityId))
				} else {
					for(Integer entityId : product.entities) {
						availableFields.addAll(retrieveAvailableMetaFields(entityId))
					}
                }
			} else  {
				availableFields.addAll(retrieveAvailableMetaFields(session['company_id']))
			}
		}

		return availableFields;
	}

	def copyPrices(product) {
		def filterPrice = new ItemDTOEx()

		return filterPrice
	}

	def filterPrices(product) {
		def filtered
		//filter prices by company if it aint root
		if(!new CompanyDAS().isRoot(session['company_id'] as Integer)) {
			def filteredPriceProduct = copyPrices(product)
			new ItemBL().filterPricesByCompany(filteredPriceProduct, session['company_id'] as Integer)
			filtered = filteredPriceProduct
		} else {
			filtered = product
		}
		return filtered
	}

    private def boolean findCycleInDependenciesTree(Integer targetProductId, List dependencies) {
        if (!dependencies) return false
        if (dependencies.contains(targetProductId)) {
            return true
        }

        for (ItemDTO item : ItemDTO.getAll(dependencies)) {
             if (findCycleInDependenciesTree(targetProductId, Arrays.asList(item.getMandatoryDependencyIdsOfType(ItemDependencyType.ITEM)) )) {
                 return true
             }
        }
        return false
    }
    
    def validateReservationDuration(String assetReservation, def product) {
        if(product.assetManagementEnabled != 0) {
            if (assetReservation && !assetReservation.isInteger()) {
                product.reservationDuration = 0
                String[] errors = ["ProductWS,reservationDuration,validation.error.reservation.duration.not.integer"]
                throw new SessionInternalError("validation.error.reservation.duration.not.integer", errors)
            }
        }
    }
	
}

%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2012] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.server.pricing.db.PriceModelStrategy" %>

<%--
  Country pricing form.

  @author Juan Vidal
  @since  04-Jul-2012
--%>

<g:hiddenField name="model.${modelIndex}.id" value="${model?.id}"/>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="plan.model.type"/></content>
    <content tag="label.for">model.${modelIndex}.type</content>
    <g:select name="model.${modelIndex}.type" class="model-type"
              from="${types}"
              valueMessagePrefix="price.strategy"
              value="${model?.type ?: type.name()}"/>

    <g:hiddenField name="model.${modelIndex}.oldType" value="${model?.type ?: type.name()}"/>

    <g:if test="${modelIndex > 0}">
        <a onclick="removeChainModel(this, ${modelIndex});">
            <img src="${resource(dir:'images', file:'cross.png')}" alt="remove"/>
        </a>
    </g:if>
</g:applyLayout>

<g:applyLayout name="form/input">
    <content tag="label"><g:message code="plan.model.rate"/></content>
    <content tag="label.for">model.${modelIndex}.rateAsDecimal</content>
    <g:textField class="field" name="model.${modelIndex}.rateAsDecimal" value="${formatNumber(number: model?.rate ?: BigDecimal.ZERO, formatName: 'money.format')}"/>
</g:applyLayout>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="prompt.user.currency"/></content>
    <content tag="label.for">model.${modelIndex}.currencyId</content>
    <g:select name="model.${modelIndex}.currencyId"
              from="${currencies}"
              optionKey="id" optionValue="${{it.getDescription(session['language_id'])}}"
              value="${model?.currencyId}" />
</g:applyLayout>

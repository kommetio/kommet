<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="valueKey" required="false" rtexprvalue="true" %>
<%@ attribute name="required" required="false" rtexprvalue="true" %>
<%@ attribute name="cssClass" required="false" rtexprvalue="true" %>
<%@ attribute name="cssStyle" required="false" rtexprvalue="true" %>
<td class="label<c:if test="${!empty cssClass}"> ${cssClass}</c:if>"<c:if test="${!empty cssStyle}"> style="${cssStyle}"</c:if>>
	<c:if test="${not empty value}">${value}</c:if>
	<c:if test="${empty value}">
		<c:if test="${not empty valueKey}">
			<kolmu:label key="${valueKey}" />
		</c:if>
	</c:if>
<c:if test="${required == true}"><span style="color:red;padding-left:3px;font-weight:bold">*</span></c:if></td>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="cssClass" required="false" rtexprvalue="true" %>
<%@ attribute name="cssStyle" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<%@ attribute name="title" required="true" rtexprvalue="true" %>
<tbody class="property-section<c:if test="${!empty cssClass}"> ${cssClass}</c:if>" <c:if test="${!empty cssStyle}">style="${cssStyle}"</c:if><c:if test="${!empty id}"> id="${id}"</c:if>>
	<tr class="title">
		<td colspan="100">${title}</td>
	</tr>
	<jsp:doBody />
</tbody>
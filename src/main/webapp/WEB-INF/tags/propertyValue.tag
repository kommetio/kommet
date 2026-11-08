<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ attribute name="value" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<td class="value"<c:if test="${not empty id}"> id="${id}"</c:if>>
	<c:if test="${not empty value}">${value}</c:if>
	<jsp:doBody />
</td>
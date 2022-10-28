<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="cssStyle" required="false" rtexprvalue="true" %>
<%@ attribute name="id" required="false" rtexprvalue="true" %>
<div id="${id}" class="page-header km-title"<c:if test="${not empty cssStyle}"> style="${cssStyle}"</c:if>>
	<jsp:doBody />
</div>
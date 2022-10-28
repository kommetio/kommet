<%@ attribute name="name" required="true" rtexprvalue="true" %>
<%@ attribute name="id" required="true" rtexprvalue="true" %>
<a href="${pageContext.request.contextPath}/views/${id}">${name}</a>
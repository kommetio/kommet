<%@ attribute name="name" required="true" rtexprvalue="true" %>
<%@ attribute name="id" required="true" rtexprvalue="true" %>
<a href="${pageContext.request.contextPath}/pages/${id}">${name}</a>
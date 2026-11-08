<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page session="false" %>

<tags:guestLayout>

	<jsp:body>
		<div class="action-errors">
		Sorry, you don't have sufficient privileges to view this page.
		</div>
	</jsp:body>
	
</tags:guestLayout>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ page session="false" %>

<tags:guestLayout>

	<jsp:body>
	
		<kolmu:errors messages="${errorMsgs}" />
		<kolmu:messages messages="${actionMsgs}" />
		
	</jsp:body>
	
</tags:guestLayout>
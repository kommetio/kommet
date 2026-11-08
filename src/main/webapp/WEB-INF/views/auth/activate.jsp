<%@ taglib prefix="faq" uri="/WEB-INF/tld/faq-tags.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ page session="false" %>

<tags:homeLayout>

	<jsp:body>
	
		<faq:errors messages="${errorMsgs}" />
		<faq:messages messages="${actionMsgs}" />
		
	</jsp:body>
	
</tags:homeLayout>
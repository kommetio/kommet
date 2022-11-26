<%@ taglib prefix="faq" uri="/WEB-INF/tld/faq-tags.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ page session="false" %>

<tags:homeLayout>

	<jsp:body>
	
		<faq:errors messages="${errorMsgs}" />
		<faq:messages messages="${actionMsgs}" />
		
	</jsp:body>
	
</tags:homeLayout>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:userLayout titleKey="tasks.list.title" importRMJS="true" layoutPath="${layoutPath}">

	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.tasks.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.utils.js"></script>
		<link href="${pageContext.request.contextPath}/resources/km/css/km.ui.css" rel="stylesheet" type="text/css" />
	
		<script>
	
			$(document).ready(function() {
			
				km.js.tasks.show({
					target: $(".km-tasklist"),
					taskId: "${taskId}"
				});
			
			});
		
		</script>
	
		<div id="km-tasklist"></div>
	
	</jsp:body>
	
</ko:userLayout>
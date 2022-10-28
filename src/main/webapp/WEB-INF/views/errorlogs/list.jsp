<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Error Logs" importRMJS="true">

	<jsp:body>
	
		<script>
		
		$(document).ready(function() {
			km.js.utils.openMenuItem("Error Logs");
		});
		
		</script>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.table.js"></script>
	
		<km:breadcrumbs isAlwaysVisible="true"/>
	
		<div class="ibox">
	
			<ko:pageHeader>Error Logs</ko:pageHeader>
	
			<km:dataTable query="select id, message, severity, createdDate, affectedUser.userName, codeClass from kommet.basic.ErrorLog" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="message" />
					<km:dataTableSearchField name="codeClass" />
				</km:dataTableSearch>
				<km:dataTableColumn name="message" label="Message" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/errorlogs/{id}" />
				<km:dataTableColumn name="severity" label="Severity" sortable="true" link="false" />
				<km:dataTableColumn name="codeClass" label="Class" sortable="true" link="false" />
				<km:dataTableColumn name="affectedUser.userName" label="User" sortable="true" link="false" />
				<km:dataTableColumn name="createdDate" label="Date/Time" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
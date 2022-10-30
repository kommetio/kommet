<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Document Templates">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<script>
		
		$(document).ready(function() {
			km.js.utils.openMenuItem("Doc Templates");
		});
		
		</script>
	
		<div class="ibox">
	
			<ko:pageHeader>Document Templates</ko:pageHeader>
			
			<km:dataTable query="select id, name, createdDate from kommet.basic.DocTemplate" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
					<km:dataTableSearchField name="content" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New template" url="${pageContext.request.contextPath}/km/doctemplates/new" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/doctemplates/{id}" />
				<km:dataTableColumn name="createdDate" label="Date/Time" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
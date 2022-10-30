<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Web Resources" importRMJS="true">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
	
			<ko:pageHeader>Web Resources</ko:pageHeader>
			
			<km:dataTable query="select id, name, mimeType, createdDate from WebResource" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="file.name" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button labelKey="btn.new" url="${pageContext.request.contextPath}/km/webresources/new" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/webresources/{id}" />
				<km:dataTableColumn name="mimeType" labelKey="webresource.mimeType" sortable="true" linkStyle="false" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" linkStyle="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
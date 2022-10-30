<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Libraries" importRMJS="true">

	<jsp:body>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
			<ko:pageHeader>Libraries</ko:pageHeader>
			
			<km:dataTable query="select id, name, provider, createdDate, version, source from Library" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" url="${pageContext.request.contextPath}/km/libraries/new" />
					<km:button label="Import" url="${pageContext.request.contextPath}/km/libraries/import" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/libraries/{id}" />
				<km:dataTableColumn name="provider" label="Provider" sortable="true" linkStyle="true" />
				<km:dataTableColumn name="version" label="Version" sortable="true" linkStyle="true" />
				<km:dataTableColumn name="source" label="Source" sortable="true" linkStyle="true" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" link="false" />
			</km:dataTable>
			
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
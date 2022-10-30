<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Business Processes" importRMJS="true">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true"/>
	
		<div class="ibox">
	
			<ko:pageHeader>Business Processes</ko:pageHeader>
	
			<km:dataTable query="select id, name, label, description, createdBy.userName from kommet.basic.BusinessProcess" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
					<km:dataTableSearchField name="description" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" labelKey="btn.new" url="${pageContext.request.contextPath}/km/bp/builder" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/bp/processes/{id}" />
				<km:dataTableColumn name="createdBy.userName" label="User" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
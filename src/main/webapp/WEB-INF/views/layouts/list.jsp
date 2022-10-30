<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Layouts">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true"/>
	
		<div class="ibox">
	
			<ko:pageHeader>Layouts</ko:pageHeader>
			
			<km:dataTable query="select id, name, createdDate from kommet.basic.Layout where name <> 'Blank'" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New layout" url="${pageContext.request.contextPath}/km/layouts/new" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/layouts/{id}" />
				<km:dataTableColumn name="createdDate" label="Created" sortable="true" />
			</km:dataTable>
	
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
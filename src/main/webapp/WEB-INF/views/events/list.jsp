<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Calendar events" importRMJS="true">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
	
			<ko:pageHeader>Calendar events</ko:pageHeader>
	
			<km:dataTable query="select id, name, startDate, endDate from kommet.basic.Event" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
					<km:dataTableSearchField name="description" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" url="${pageContext.request.contextPath}/km/events/new" />
					<km:button label="Calendar" url="${pageContext.request.contextPath}/km/calendar" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/events/{id}" />
				<km:dataTableColumn name="startDate" label="Start Date" sortable="true" link="false" />
				<km:dataTableColumn name="endDate" label="End Date" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
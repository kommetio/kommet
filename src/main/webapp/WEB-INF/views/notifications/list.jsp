<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Notifications">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true"/>
	
		<div class="ibox">
	
			<ko:pageHeader>Notifications</ko:pageHeader>
	
			<km:dataTable query="select id, title, text, assignee.id, assignee.userName, createdDate from kommet.basic.Notification" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="title" />
					<km:dataTableSearchField name="text" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" labelKey="btn.new" url="${pageContext.request.contextPath}/km/notifications/new" />
				</km:buttons>
				<km:dataTableColumn name="title" label="Title" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/errorlogs/{id}" />
				<km:dataTableColumn name="text" label="Text" sortable="true" link="false" />
				<km:dataTableColumn name="assignee.userName" label="Assignee" sortable="true" link="false" />
				<km:dataTableColumn name="createdDate" label="Date/Time" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Tasks" importRMJS="true">

	<jsp:body>
	
		<div class="ibox">
			<ko:pageHeader>Tasks</ko:pageHeader>
	
			<km:dataTable query="select id, dueDate, title, status, priority from kommet.basic.Task" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="title" />
					<km:dataTableSearchField name="content" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New task" url="${pageContext.request.contextPath}/km/tasks/new" />
				</km:buttons>
				<km:dataTableColumn name="title" label="Title" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/tasks/{id}" />
				<km:dataTableColumn name="dueDate" label="Due Date" sortable="true" linkStyle="false" />
				<km:dataTableColumn name="status" label="Status" sortable="true" />
				<km:dataTableColumn name="priority" label="Priority" sortable="true" />
				<km:dataTableColumn name="createdDate" label="Created" sortable="true" />
			</km:dataTable>
			
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout titleKey="user.list.title" importRMJS="true">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
	
			<ko:pageHeader><kolmu:label key="user.list.title" /></ko:pageHeader>
			
			<km:dataTable query="select id, userName, profile.name, createdDate from User" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="userName" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" url="${pageContext.request.contextPath}/km/users/new" />
				</km:buttons>
				<km:dataTableColumn name="userName" labelKey="user.username" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/user/{id}" />
				<km:dataTableColumn name="profile.name" labelKey="user.profile" sortable="true" linkStyle="false" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" linkStyle="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
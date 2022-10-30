<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Login history" importRMJS="true">

	<jsp:body>
	
		<div class="ibox">
	
			<ko:pageHeader>Login history</ko:pageHeader>
			
			<km:dataTable query="select id, loginUser.userName, method, createdDate from LoginHistory" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="loginUser.userName" />
				</km:dataTableSearch>
				<km:dataTableColumn name="loginUser.userName" label="Username" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/user/{id}" />
				<km:dataTableColumn name="method" label="Method" sortable="true" link="false" />
				<km:dataTableColumn name="createdDate" label="Date" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
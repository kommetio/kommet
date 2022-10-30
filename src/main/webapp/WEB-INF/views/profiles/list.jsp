<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Profiles">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
	
			<ko:pageHeader>Profiles</ko:pageHeader>
			
			<km:dataTable query="select id, name, label, createdDate, systemProfile from Profile where name <> 'Root'" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
					<km:dataTableSearchField name="label" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" labelKey="btn.new" url="${pageContext.request.contextPath}/km/profiles/new" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/profile/{id}" />
				<km:dataTableColumn name="label" label="Label" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/profile/{id}" />
				<km:dataTableColumn name="systemProfile" label="Is System Profile" sortable="true" link="false" />
				<km:dataTableColumn name="createdDate" label="Date/Time" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
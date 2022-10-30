<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Views">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
	
			<ko:pageHeader>Views</ko:pageHeader>
			
			<km:dataTable query="select id, name, packageName, createdDate from View where isSystem = false" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" url="${pageContext.request.contextPath}/km/views/new" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/views/{id}" />
				<km:dataTableColumn name="packageName" label="Package name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/views/{id}" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" link="false" />
			</km:dataTable>
	
	<%--
			<table class="std-table" style="margin-top: 30px" id="viewList">
				<thead>
					<tr class="cols">
						<th>Name</th>
						<th>Package</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="view" items="${views}">
						<tr>
							<td><a href="${pageContext.request.contextPath}/km/views/${view.id}">${view.interpretedName}</a></td>
							<td>${view['packageName']}</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		--%>
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
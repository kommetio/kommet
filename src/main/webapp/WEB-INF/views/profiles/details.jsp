<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${profile.name}" importRMJS="true">
	<jsp:body>
	
		<script>
		
		$(document).ready(function() {
			
			// create tabs
			var tabs = km.js.tabs.create({
				tabs: [
					{ content: $("#profile-details"), label: "${profile.name} - profile" },
					{ content: $("#profile-users"), label: "Users" }
				]
			});
			
			tabs.render(function(code) {
				$("#tab-container").html(code);
			});
			
			// open the first tab
			tabs.open(0);
		});
		
	
	</script>
	
		<div id="tab-container"></div>
	
		<div id="profile-details">
		
			<ko:pageHeader>${profile.label}</ko:pageHeader>
			<c:if test="${profile.systemProfile != true}">
				<a href="${pageContext.request.contextPath}/km/profiles/edit/${profile.id}" class="sbtn"><kolmu:label key="btn.edit" /></a>
			</c:if>
			<a href="${pageContext.request.contextPath}/km/profiles/typepermissions/${profile.id}" class="sbtn"><kolmu:label key="btn.permissions" /></a>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label"><kolmu:label key="profile.name" /></td>
						<td class="value">${profile.name}</td>
						<td class="sep"></td>
						<td class="label"><kolmu:label key="profile.label" /></td>
						<td class="value">${profile.label}</td>
					</tr>
					<tr>
						<td class="label"><kolmu:label key="profile.issystem" /></td>
						<td class="value">${profile.systemProfile}</td>
						<td class="sep"></td>
						<td class="label"><kolmu:label key="profile.landing.url" /></td>
						<td class="value">${profileSettings.landingURL}</td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
		<div id="profile-users">
			<ko:pageHeader>Users</ko:pageHeader>
			
			<km:dataTable var="userTable" query="select id, userName, createdDate from User where profile.id = '${profile.id}'" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="userName" />
				</km:dataTableSearch>
				<km:dataTableColumn url="${pageContext.request.contextPath}/km/users/{id}" name="userName" label="User Name" sortable="true" linkStyle="true" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" linkStyle="false" />
			</km:dataTable>
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Standard Actions">

	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-ui-1.10.3.custom.min.js"></script>
		<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-lightness/jquery-ui-1.10.3.custom.min.css" />
	
		<div class="ibox">
		
			<form action="${pageContext.request.contextPath}/km/savepages/${typePrefix}" method="POST">
	
				<table class="std-table">
					<thead>
						<tr>
							<td>Profile</td>
							<td>List Action</td>
							<td>View Action</td>
							<td>Edit Action</td>
							<td>Create Action</td>
						</tr>
					</thead>
					<tbody>
						<c:forEach items="${profiles}" var="profile">
							<tr>
								<td>${profile.name}</td>
								<td>
									<km:lookup fields="id,name"
												linkFields="id"
												name="stdpage_${profile.id}_list"
												displayFields="name"
												value="${stdPagesByProfile.get('profile.id').listPage}"
												type="kommet.objects.basic.Page">
									</km:lookup>
								</td>
								<td>
									<km:lookup fields="id,name"
												linkFields="id"
												name="stdpage_${profile.id}_view"
												displayFields="name"
												value="${stdPagesByProfile.get('profile.id').detailsPage}"
												type="kommet.objects.basic.Page">
									</km:lookup>
								</td>
								<td>
									<km:lookup fields="id,name"
												linkFields="id"
												name="stdpage_${profile.id}_edit"
												displayFields="name"
												filter=""
												value="${stdPagesByProfile.get('profile.id').editPage}"
												type="kommet.objects.basic.Page">
									</km:lookup>
								</td>
								<td>
									<km:lookup fields="id,name"
												linkFields="id"
												name="stdpage_${profile.id}_edit"
												displayFields="name"
												value="${stdPagesByProfile.get('profile.id').createPage}"
												type="kommet.objects.basic.Page">
									</km:lookup>
								</td>
							</tr>
						</c:forEach>
					</tbody>
				</table>
				
				<p></p>
				<input type="submit" class="sbtn" value="Save" />
			
			</form>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
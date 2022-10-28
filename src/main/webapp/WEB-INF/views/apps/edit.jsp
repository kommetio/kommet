<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}">
	<jsp:body>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/apps/save">
				<input type="hidden" name="appId" value="${app.id}" />
				
				<ko:pageHeader>${pageTitle}</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Label" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="label" value="${app.label}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${app.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Type" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="type">
								<option value="Internal app"<c:if test="${app.type == 'Internal app'}"> selected</c:if>>Internal app</option>
								<option value="Website"<c:if test="${app.type == 'Website'}"> selected</c:if>>Website</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Landing URL"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="landingUrl" value="${app.landingUrl}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
						<c:if test="${not empty app.id}">
							<a href="${pageContext.request.contextPath}/km/apps/${app.id}" class="sbtn">Cancel</a>
						</c:if>
						<c:if test="${empty app.id}">
							<a href="${pageContext.request.contextPath}/km/apps/list" class="sbtn">Cancel</a>
						</c:if>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
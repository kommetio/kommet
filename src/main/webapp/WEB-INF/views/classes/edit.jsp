<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit class">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				km.js.ui.autoFormatName({
					target: $("input[name='name']")
				});
			});
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<form method="post" action="${pageContext.request.contextPath}/km/classes/save">
				<input type="hidden" name="fileId" value="${file.id}"></input>
				
				<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
				<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
				
				<ko:pageHeader>
					<c:if test="${not empty file.id}">${file.qualifiedName}</c:if>
					<c:if test="${empty file.id}">New class</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${file.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Package" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="package" value="${packageName}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
						<ko:buttonCell>
							<c:if test="${not empty file.id}">
								<a href="${pageContext.request.contextPath}/km/classes/${file.id}" class="sbtn">Cancel</a>
							</c:if>
							<c:if test="${empty file.id}">
								<a href="${pageContext.request.contextPath}/km/classes/list" class="sbtn">Cancel</a>
							</c:if>
						</ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
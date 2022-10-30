<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="New view">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				km.js.ui.autoFormatName({
					target: $("input[name='name']")
				});
				
				km.js.utils.bind($("#name"), $("#pageTitle"), "New view");
			});
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<ko:pageHeader id="pageTitle">
				<c:if test="${empty view.id}">New view</c:if>
				<c:if test="${not empty view.id}">view.name</c:if>
			</ko:pageHeader>
		
			<form method="post" action="${pageContext.request.contextPath}/km/views/create">
				<input type="hidden" name="viewId" value="${view.id}" />
				
				<ko:buttonPanel>
					<c:if test="${not empty view.id}">
						<a href="${pageContext.request.contextPath}/km/ide/${view.id}" class="sbtn">Open in IDE</a>
					</c:if>
					<c:if test="${empty view.id}">
						<input type="submit" value="Save" id="saveViewBtn" />
					</c:if>
					<c:if test="${empty view.id}">
						<a href="${pageContext.request.contextPath}/km/views/list" class="sbtn">Cancel</a>
					</c:if>
					<c:if test="${not empty view.id}">
						<a href="${pageContext.request.contextPath}/km/views/${view.id}" class="sbtn">Cancel</a>
					</c:if>
				</ko:buttonPanel>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" id="name" value="${view.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Package" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="package" value="${view['packageName']}" />
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
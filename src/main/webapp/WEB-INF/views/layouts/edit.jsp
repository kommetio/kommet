<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="New layout">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				km.js.ui.autoFormatName({
					target: $("input[name='name']"),
					onKeyUp: false
				});
			});
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/layout/save">
				<input type="hidden" name="layoutId" value="${layout.id}" />
				
				<ko:pageHeader>
					<c:if test="${empty layout.id}">New layout</c:if>
					<c:if test="${not empty layout.id}">Edit layout</c:if>
				</ko:pageHeader>
				
				<ko:buttonPanel>
					<c:if test="${not empty layout.id}">
						<a href="${pageContext.request.contextPath}/km/ide/${layout.id}" class="sbtn">Open in IDE</a>
					</c:if>
				</ko:buttonPanel>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${layout.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
						<c:if test="${not empty layout.id}">
								<a href="${pageContext.request.contextPath}/km/layouts/${layout.id}" class="sbtn">Cancel</a>
							</c:if>
							<c:if test="${empty layout.id}">
								<a href="${pageContext.request.contextPath}/km/layouts/list" class="sbtn">Cancel</a>
							</c:if>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
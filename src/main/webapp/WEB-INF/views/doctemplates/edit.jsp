<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit document template">
	<jsp:body>
	
		<style>
			
			textarea.content {
				height: 300px;
				width: 100%;
			}
		
		</style>
		
		<script>
		
			$(document).ready(function() {
				km.js.ui.autoFormatName({
					target: $("input[name='name']")
				});
			});
		
		</script>
		
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/doctemplates/save">
				<input type="hidden" name="templateId" value="${template.id}" />
				
				<ko:pageHeader>
					<c:if test="${empty template.id}">New template</c:if>
					<c:if test="${not empty template.id}">Edit template</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${template.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Content"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="content" class="content">${template.content}</textarea>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
						<ko:buttonCell>
							<c:if test="${not empty template.id}">
								<a href="${pageContext.request.contextPath}/km/doctemplates/${template.id}" class="sbtn">Cancel</a>
							</c:if>
							<c:if test="${empty template.id}">
								<a href="${pageContext.request.contextPath}/km/doctemplates" class="sbtn">Cancel</a>
							</c:if>
						</ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
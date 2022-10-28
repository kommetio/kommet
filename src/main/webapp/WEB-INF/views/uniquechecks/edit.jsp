<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit unique check">
	<jsp:body>
	
		<style>
		
			select"km-field-ids {
				height: 20em;
				padding: 0;
			}
			
			select"km-field-ids > option {
				padding: 0.5em;
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
		
			<form method="post" action="${pageContext.request.contextPath}/km/uniquechecks/save">
				<input type="hidden" name="typeId" value="${typeId}" />
				<input type="hidden" name="uniqueCheckId" value="${uc.id}" />
				
				<ko:pageHeader>
					<c:if test="${empty vr.id}">New unique check</c:if>
					<c:if test="${not empty vr.id}">Edit unique check</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${uc.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Fields"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="fieldIds" multiple="multiple" id="km-field-ids">
								<c:forEach var="field" items="${fields}">
									<option value="${field.KID}"<c:if test="${selectedFieldIds.contains(field.KID)}"> selected</c:if>>${field.apiName}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell>
							<input type="submit" value="Save" />
							<c:if test="${not empty label.id}">
								<a href="${pageContext.request.contextPath}/km/uniquechecks/${label.id}" class="sbtn">Cancel</a>
							</c:if>
							<c:if test="${empty label.id}">
								<a href="${pageContext.request.contextPath}/km/type/${keyPrefix}/#rm.tab.6" class="sbtn">Cancel</a>
							</c:if>
						</ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
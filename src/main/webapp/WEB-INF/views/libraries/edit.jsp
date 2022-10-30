<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				initTooltips()
			});
			
			function initTooltips()
			{
				km.js.ui.tooltip({
					afterTarget: $("input[name='name']"),
					text: "Must be a qualified name containing a package, e.g. kommet.libs.MyLib"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='provider']"),
					text: "Organization that provides this library - this will usually be the name of your company"
				});
			}
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/libraries/save">
				<input type="hidden" name="libId" value="${lib.id}" />
				
				<ko:pageHeader>${pageTitle}</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${lib.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Provider" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="provider" value="${lib.provider}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Version" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="version" value="${lib.version}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Access Level" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="accessLevel">
								<option value="Editable"<c:if test="${lib.accessLevel == 'Editable'}"> selected</c:if>>Editable</option>
								<option value="Read-only"<c:if test="${lib.accessLevel == 'Read-only'}"> selected</c:if>>Read-only</option>
								<option value="Read-only methods"<c:if test="${lib.accessLevel == 'Read-only methods'}"> selected</c:if>>Read-only methods</option>
								<option value="Closed"<c:if test="${lib.accessLevel == 'Closed'}"> selected</c:if>>Closed</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Description" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="description">${lib.description}</textarea>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
						<c:if test="${not empty lib.id}">
								<a href="${pageContext.request.contextPath}/km/libraries/${lib.id}" class="sbtn">Cancel</a>
							</c:if>
							<c:if test="${empty lib.id}">
								<a href="${pageContext.request.contextPath}/km/libraries/list" class="sbtn">Cancel</a>
							</c:if>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
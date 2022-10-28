<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit view resource ${resource.name}">
	<jsp:body>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script>
			
			$("#fileName").click(function() {
				uploadDialog.show()	
			});
			
			$(document).ready(function() {
				
				$("#diskFile").change(function() {
					var path = $(this).val();
					var fileName = "";
					if (path)
					{
						var pathBits = path.split(/[\/\\]/g);
						fileName = pathBits[pathBits.length-1];
					}
					
					$("#resourceName").val(fileName);
				});
				
			});
		
		</script>
	
		<div class="ibox">
		
			<form:form id="uploadForm" enctype="multipart/form-data" modelAttribute="uploadItem" method="post" action="${pageContext.request.contextPath}/km/viewresources/save">
				<input type="hidden" name="resourceId" value="${resource.id}"></input>
				
				<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
				<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
				
				<c:if test="${resource.id == null}">
				<ko:pageHeader>New view resource</ko:pageHeader>
				</c:if>
				<c:if test="${resource.id != null}">
				<ko:pageHeader>${resource.name}</ko:pageHeader>
				</c:if>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="resourceName" id="resourceName" value="${resource.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="MIME type" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="mimeType" value="${resource.mimeType}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
						<c:if test="${empty resource.id}">
						<a href="${pageContext.request.contextPath}/km/viewresources/list" class="sbtn">Cancel</a>
						</c:if>
						<c:if test="${not empty resource.id}">
							<a href="${pageContext.request.contextPath}/km/viewresources/${resource.id}" class="sbtn">Cancel</a>
						</c:if>
					</ko:buttonRow>
				</ko:propertyTable>

			</form:form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
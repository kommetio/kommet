<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit web resource ${resourceName}">
	<jsp:body>
	
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
					
					// resource name must be a fully qualified name, so if it is not, we prepend a default package "images" to it
					if (fileName.indexOf(".") < 0)
					{
						fileName = "images." + km.js.utils.capitalize(fileName);
					}
					
					if (!$("#resourceName").val())
					{
						$("#resourceName").val(fileName);
					}
				});
				
			});
		
		</script>
	
		<div class="ibox">
		
			<form:form id="uploadForm" enctype="multipart/form-data" modelAttribute="uploadItem" method="post" action="${pageContext.request.contextPath}/km/webresources/save">
				<input type="hidden" name="resourceId" value="${resource.id}"></input>
				
				<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
				<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
				
				<c:if test="${resource.id == null}">
				<ko:pageHeader>New web resource</ko:pageHeader>
				</c:if>
				<c:if test="${resource.id != null}">
				<ko:pageHeader>${resourceName}</ko:pageHeader>
				</c:if>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="resourceName" id="resourceName" value="${resource.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="File" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<form:input path="fileData" type="file" id="diskFile" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Is Public" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="isPublic" value="true" <c:if test="${resource.isPublic == true}"> checked</c:if> />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
						<c:if test="${empty resource.id}">
						<a href="${pageContext.request.contextPath}/km/webresources/list" class="sbtn">Cancel</a>
						</c:if>
						<c:if test="${not empty resource.id}">
							<a href="${pageContext.request.contextPath}/km/webresources/${resource.id}" class="sbtn">Cancel</a>
						</c:if>
					</ko:buttonRow>
				</ko:propertyTable>

			</form:form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
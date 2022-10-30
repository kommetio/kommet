<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${vr.name} - Validation rule">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<div class="ibox">
		
			<script>
			
				function deleteVR()
				{
					$.post("${pageContext.request.contextPath}/km/validationrules/delete", { ruleId: "${vr.id}" }, function(data) {
						if (data.success === true)
						{
							openUrl("${pageContext.request.contextPath}/km/type/" + data.data.keyPrefix + "#rm.tab.5");
						}
						else
						{
							showMsg("warnPrompt", data.messages, "error");
						}
					}, "json");
				}
			
			</script>
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="">
				
				<ko:pageHeader>${vr.name}</ko:pageHeader>
				
				<ko:buttonPanel>
					<c:if test="${vr.isSystem == false}">
						<a href="${pageContext.request.contextPath}/km/validationrules/edit/${vr.id}" class="sbtn">Edit</a>						
						<a href="javascript:;" onclick="ask('Are you sure you want to delete this validation rule?', 'warnPrompt', function() { deleteVR(); })" id="deleteVRBtn" class="sbtn">Delete</a>
					</c:if>
				</ko:buttonPanel>
				
				<div id="warnPrompt" style="margin-top:10px"></div>
				
				<ko:propertyTable>
					
					<ko:propertyRow>
						<ko:propertyLabel value="Name"></ko:propertyLabel>
						<ko:propertyValue>${vr.name}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="REL evaluation"></ko:propertyLabel>
						<ko:propertyValue>${vr.code}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Error message"></ko:propertyLabel>
						<ko:propertyValue>${vr.errorMessage}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Error message text label"></ko:propertyLabel>
						<ko:propertyValue>${vr.errorMessageLabel}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Is Active"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="isActive" value="true"<c:if test="${vr.active == true}"> checked</c:if> disabled></input>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Type"></ko:propertyLabel>
						<ko:propertyValue><a href="${pageContext.request.contextPath}/km/type/${typePrefix}">${typeName}</a></ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}">
	<jsp:body>
		
		<style>
			
			/*.action-choice {
				display: none;
			}*/
		
		</style>
		
		<script>
			
			function deleteBtn()
			{
				$.post("${pageContext.request.contextPath}/km/buttons/delete", { id: "${button.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/type/${type.keyPrefix}#rm.tab.3");
					}
					else
					{
						km.js.ui.error(resp.messages, $("#error-container"));
					}
				}, "json");
			}
			
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true"/>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
			
			<div id="error-container"></div>
		
			<input type="hidden" name="buttonId" id="buttonId" value="${button.id}" />
			
			<ko:pageHeader>${pageTitle}</ko:pageHeader>
			
			<ko:buttonPanel>
					<a href="${pageContext.request.contextPath}/km/buttons/edit/${button.id}" class="sbtn">Edit</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this button?', 'warnPrompt', function() { deleteBtn(); })" id="deleteBtn" class="sbtn">Delete</a>
			</ko:buttonPanel>
			
			<div id="warnPrompt" style="margin-top:10px"></div>
			
			<ko:propertyTable>
				<ko:propertyRow>
					<ko:propertyLabel value="Label" required="true"></ko:propertyLabel>
					<ko:propertyValue>${button.label}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Label Key"></ko:propertyLabel>
					<ko:propertyValue>${button.labelKey}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
					<ko:propertyValue>${button.name}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow cssClass="action-choice action-choice-url">
					<ko:propertyLabel value="URL" required="true"></ko:propertyLabel>
					<ko:propertyValue>${button.url}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow cssClass="action-choice action-choice-action">
					<ko:propertyLabel value="Action" required="true"></ko:propertyLabel>
					<ko:propertyValue><a href="${pageContext.request.contextPath}/km/actions/${button.action.id}">${button.action.name}</a>
					</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow cssClass="action-choice action-choice-onClick">
					<ko:propertyLabel value="On Click event" required="true"></ko:propertyLabel>
					<ko:propertyValue>${button.onClick}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Display condition"></ko:propertyLabel>
					<ko:propertyValue>${button.displayCondition}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Type"></ko:propertyLabel>
					<ko:propertyValue><a href="${pageContext.request.contextPath}/km/type/${type.keyPrefix}">${type.qualifiedName}</a></ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
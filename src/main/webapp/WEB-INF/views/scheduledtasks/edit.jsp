<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit scheduled task" importRMJS="true">
	<jsp:body>
	
		<script>
		
			function createClassLookup()
			{
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.Class",
					properties: [
						{ name: "id" },
						{ name: "name" },
						{ name: "packageName" }
					],
					restrictions: [
						{ property_name: "accessType", operator: "eq", args: [ 0 ] }
					]
				};
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true },
							{ name: "packageName", label: "Package", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: "Classes",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var clsLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "fileId",
					selectedRecordId: "${task.file.id}"
				});
				
				clsLookup.render($("#cls-id"));
			}
			
			$(document).ready(function() {
				createClassLookup();
			});
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<ko:pageHeader>Edit scheduled task</ko:pageHeader>
			
			<form method="post" action="${pageContext.request.contextPath}/km/scheduledtasks/save">
				<input type="hidden" name="taskId" value="${task.id}" />
				
				<ko:buttonPanel>
					<input type="submit" value="Save" />
					<c:if test="${not empty task.id}">
						<a href="${pageContext.request.contextPath}/km/scheduledtask/${task.id}" class="sbtn">Cancel</a>
					</c:if>
					<c:if test="${empty task.id}">
						<a href="${pageContext.request.contextPath}/km/scheduledtasks" class="sbtn">Cancel</a>
					</c:if>
				</ko:buttonPanel>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${task.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="CRON expression" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="cronExpression" value="${task.cronExpression}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Class" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<%--<select name="fileId">
								<option value="">-- Select --</option>
								<c:forEach items="${kollFiles}" var="file">
									<option value="${file.id}"<c:if test="${file.id == task.file.id}"> selected</c:if>>${file['packageName']}<c:if test="${not empty file['packageName']}">.</c:if>${file.name}</option>
								</c:forEach>
							</select>--%>
							<input id="cls-id">
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Method" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="method" value="${task.method}" />
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
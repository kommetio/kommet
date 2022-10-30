<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Scheduled task">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script type="text/javascript">
		
			function deleteTask()
			{
				$.post("${pageContext.request.contextPath}/km/scheduledtasks/delete", { taskId: "${task.id}" }, function(data) {
					if (data.status == "success")
					{
						openUrl("${pageContext.request.contextPath}/km/scheduledtasks");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}

			function executeNow()
			{
				km.js.ui.statusbar.show("Executing scheduled job...");
				//showMsg("warnPrompt", "Executing...", "info", null, null, "${pageContext.request.contextPath}/rm");
				
				$.post("${pageContext.request.contextPath}/km/scheduledtasks/execute", { taskId: "${task.id}" }, function(data) {
					if (data.status == "success")
					{
						km.js.ui.statusbar.show("Execution scheduled successful", 10000);
					}
					else
					{
						km.js.ui.statusbar.hide();
						showMsg("warnPrompt", data.messages, "error", null, null, "${pageContext.request.contextPath}");	
					}
				}, "json");	 
			}
			

		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<ko:pageHeader>${task.name}</ko:pageHeader>
			
			<form method="post" action="${pageContext.request.contextPath}/km/scheduledtasks/save">
				<input type="hidden" name="taskId" value="${task.id}" />
				
				<ko:buttonPanel>
					<a href="${pageContext.request.contextPath}/km/scheduledtasks/edit/${task.id}" class="sbtn">Edit</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this task?', 'warnPrompt', function() { deleteTask(); })" id="deleteTaskBtn" class="sbtn">Delete</a>
					<a href="javascript:;" onclick="executeNow()" class="sbtn">Execute now</a>
				</ko:buttonPanel>
				<div id="warnPrompt" style="margin: 10px 0 20px 0"></div>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name"></ko:propertyLabel>
						<ko:propertyValue>${task.name}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Method"></ko:propertyLabel>
						<ko:propertyValue>${task.method}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="CRON expression"></ko:propertyLabel>
						<ko:propertyValue>${task.cronExpression}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Class"></ko:propertyLabel>
						<ko:propertyValue>
							<a href="${pageContext.request.contextPath}/km/classes/${task.file.id}">${task.file['packageName']}<c:if test="${not empty task.file['packageName']}">.</c:if>${task.file.name}</a>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Is scheduled?"></ko:propertyLabel>
						<ko:propertyValue>
							<c:if test="${not empty scheduledJob.trigger}">Yes</c:if>
							<c:if test="${empty scheduledJob.trigger}">No</c:if>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Next fire time"></ko:propertyLabel>
						<ko:propertyValue>
							<km:dateTime value="${scheduledJob.trigger.nextFireTime}" format="dd-MM-yyyy HH:mm:ss" />
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
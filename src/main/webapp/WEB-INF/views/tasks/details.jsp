<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${task.title}">

	<jsp:body>
		
		<script>
		
		function deleteTask()
		{
			$.post("${pageContext.request.contextPath}/km/tasks/delete", { id: "${task.id}" }, function(data) {
				if (data.success === true)
				{
					openUrl("${pageContext.request.contextPath}/km/tasks/list");
				}
				else
				{
					showMsg("warnPrompt", data.messages, "error");
				}
			}, "json");
		}
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<ko:pageHeader>${task.title}</ko:pageHeader>
			
			<ko:buttonPanel>
				<a href="${pageContext.request.contextPath}/km/tasks/edit/${task.id}" class="sbtn"><kolmu:label key="btn.edit" /></a>						
				<a href="javascript:;" onclick="ask('<kolmu:label key="tasks.delete.confirm" />', 'warnPrompt', function() { deleteTask(); })" id="deleteTaskBtn" class="sbtn"><kolmu:label key="btn.delete" /></a>
			</ko:buttonPanel>
			
			<div id="warnPrompt" style="margin-top:10px"></div>
			
			<ko:propertyTable>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="tasks.title"></ko:propertyLabel>
					<ko:propertyValue>${task.title}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="tasks.duedate"></ko:propertyLabel>
					<ko:propertyValue>${task.dueDate}</ko:propertyValue>
				</ko:propertyRow>
				<c:if test="${task.assignedUser != null}">
				<ko:propertyRow>
					<ko:propertyLabel valueKey="tasks.assigneduser"></ko:propertyLabel>
					<ko:propertyValue>
						<a href="${pageContext.request.contextPath}/km/users/${task.assignedUser.id}">${task.assignedUser.userName}</a>
					</ko:propertyValue>
				</ko:propertyRow>
				</c:if>
				<c:if test="${task.assignedGroup != null}">
				<ko:propertyRow>
					<ko:propertyLabel valueKey="tasks.assignedgroup"></ko:propertyLabel>
					<ko:propertyValue>
						<a href="${pageContext.request.contextPath}/km/usergroups/${task.assignedGroup.id}">${task.assignedGroup.name}</a>
					</ko:propertyValue>
				</ko:propertyRow>
				</c:if>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="tasks.priority"></ko:propertyLabel>
					<ko:propertyValue>
						<select name="priority">
							<option value="0"<c:if test="${task.priority == 0}"> selected</c:if>><kolmu:label key="tasks.priority.0" /></option>
							<option value="1"<c:if test="${task.priority == 1}"> selected</c:if>><kolmu:label key="tasks.priority.1" /></option>
							<option value="2"<c:if test="${task.priority == 2}"> selected</c:if>><kolmu:label key="tasks.priority.2" /></option>
							<option value="3"<c:if test="${task.priority == 3}"> selected</c:if>><kolmu:label key="tasks.priority.3" /></option>
							<option value="4"<c:if test="${task.priority == 4}"> selected</c:if>><kolmu:label key="tasks.priority.4" /></option>
						</select>
					</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="tasks.status"></ko:propertyLabel>
					<ko:propertyValue>${task.status}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="tasks.content"></ko:propertyLabel>
					<ko:propertyValue>
						<div class="inactive-textbox">${task.content}</div>
					</ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}">
	<jsp:body>
	
		<link href="${pageContext.request.contextPath}/resources/css/smoothness/jquery-ui-1.10.3.custom.min.css" rel="stylesheet" type="text/css" />
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ui.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ref.js"></script>
		
		<%-- date time picker dependencies --%>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-timepicker-addon.min.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-sliderAccess.js"></script>
		<link href="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-timepicker-addon.min.css" rel="stylesheet" type="text/css" />
	
		<script>
		
			$(document).ready(function() {
				initUserLookup();
				initGroupLookup();
				
				$.timepicker.setDefaults($.timepicker.regional['pl']);
				
				// init date time picker
				$("input#duedate").datepicker({ dateFormat: "yy-mm-dd" });
				/*$("input#duedate").datetimepicker({
					controlType: "select",
					timeFormat: "HH:mm"
				});*/
			})
			
			function initUserLookup()
			{
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.User",
					properties: [
						{ name: "id" },
						{ name: "userName" }
					]
				};
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "userName", label: km.js.config.i18n["user.username"], linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: km.js.config.i18n["tasks.users"],
					tableSearchOptions: {
						properties: [ { name: "userName", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var userLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "userName" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "assignedUserId",
					selectedRecordId: "${task.assignedUser.id}"
				});
				
				userLookup.render($("#assigneduser"));
			}
			
			function initGroupLookup()
			{
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.UserGroup",
					properties: [
						{ name: "id" },
						{ name: "name" }
					]
				};
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: km.js.config.i18n["usergroups.groupname"], linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: km.js.config.i18n["usergroups.list.title"],
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var userLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "assignedGroupId",
					selectedRecordId: "${task.assignedGroup.id}"
				});
				
				userLookup.render($("#assignedgroup"));
			}
		
		</script>
		
		<style>
		
			textarea#task-content {
				height: 20em;
  				width: 50em;
			}
			
			.kdetails select.time-picker {
				width: 5em;
				min-width: 5em;
			}
			
			div.km-lookup {
				width: 25em;
			}
		
		</style>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/tasks/save">
				<input type="hidden" name="taskId" value="${task.id}" />
				
				<ko:pageHeader>${pageTitle}</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="tasks.title" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="title" value="${task.title}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="tasks.duedate"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="dueDate" id="duedate" />
							<span class="label"><kolmu:label key="tasks.hour" /></span>
							<select name="hour" class="time-picker">
								<c:forEach var="hour" items="${hourList}">
									<option value="${hour}"<c:if test="${hour == task.dueDate.hours}"> selected</c:if>>${hour}</option>
								</c:forEach>
							</select>
							<select name="minute" class="time-picker">
								<c:forEach var="minute" items="${minuteList}">
									<option value="${minute}"<c:if test="${minute == task.dueDate.minutes}"> selected</c:if>>${minute}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="tasks.assigneduser"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="assigneduser" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="tasks.assignedgroup"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="assignedgroup" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="tasks.priority" required="true"></ko:propertyLabel>
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
						<ko:propertyLabel valueKey="tasks.status" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="status">
								<option value="Open"<c:if test="${task.status == 'Open'}"> selected</c:if>><kolmu:label key="tasks.status.open" /></option>
								<option value="In progress"<c:if test="${task.status == 'In progress'}"> selected</c:if>><kolmu:label key="tasks.status.inprogress" /></option>
								<option value="Waiting for information"<c:if test="${task.status == 'Waiting for information'}"> selected</c:if>><kolmu:label key="tasks.status.wfi" /></option>
								<option value="Rejected"<c:if test="${task.status == 'Rejected'}"> selected</c:if>><kolmu:label key="tasks.status.rejected" /></option>
								<option value="Resolved"<c:if test="${task.status == 'Resolved'}"> selected</c:if>><kolmu:label key="tasks.status.resolved" /></option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="tasks.content" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="content" id="task-content">${task.content}</textarea>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value='<kolmu:label key="btn.save" />' /></ko:buttonCell>
						<c:if test="${not empty task.id}">
								<a href="${pageContext.request.contextPath}/km/tasks/${task.id}" class="sbtn"><kolmu:label key="btn.cancel" /></a>
							</c:if>
							<c:if test="${empty task.id}">
								<a href="${pageContext.request.contextPath}/km/tasks/list" class="sbtn"><kolmu:label key="btn.cancel" /></a>
							</c:if>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
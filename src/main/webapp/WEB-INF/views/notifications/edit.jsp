<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:userLayout layoutPath="${layoutPath}" title="New notification" importRMJS="true">
	<jsp:body>
	
		<script>

			function saveNotification()
			{
				var payload = { 
					title: $("#ntForm #title").val(), 
					text: $("#ntForm #text").val(),
					assigneeId: $("#userId").val(),
					id: "${notification.id}",
					parentDialog: "${parentDialog}"
				}
				
				$.post("${pageContext.request.contextPath}/km/notifications/save", payload, function(data) {
					if (data.status == "success")
					{
						if (data.parentDialog)
						{
							console.log("PD [" + data.parentDialog + "]");
							parent.window[data.parentDialog].close();
						}
						else
						{
							// go back to notification list
							km.js.utils.openURL(km.js.config.contextPath + "/km/notifications/list");
						}
						// close dialog
						//parent.window.$.closeRialog();
					}
					else
					{
						//showMsg ("err", data.messages, "error", null, null, "${pageContext.request.contextPath}/rm");
						km.js.ui.message(data.messages, $("#err"), "error");
					}	
				}, "json");
			}
			
			function initUserLookup()
			{
				// jcr to query users
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
							{ name: "userName", label: "User name", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					tableSearchOptions: {
						properties: [ { name: "userName", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var userLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "userName" },
					jcr: jcr,
					availableItemsOptions: availableItemsOptions,
					inputName: "userId",
					inputId: "userId",
					selectedRecordId: "${assigneeId}"
				});
				
				userLookup.render($("#userLookup"));
			}
			
			$(document).ready(function() {
				initUserLookup();
			});
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" id="ntForm">
				
				<ko:pageHeader>
					<c:if test="${empty layout.id}">New notification</c:if>
					<c:if test="${not empty layout.id}">Edit notification</c:if>
				</ko:pageHeader>
				
				<div id="err"></div>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="User"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="userLookup" />
							<!--<a href="${pageContext.request.contextPath}/km/user/${assignee.id}">${assignee.userName}</a>-->
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Title"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="title" id="title" value="${notification.title}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Text"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="text" id="text">${notification.text}</textarea>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="button" class="sbtn" onclick="saveNotification()" value="Save" /></ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:userLayout>
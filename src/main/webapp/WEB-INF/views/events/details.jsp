<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Event">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<script type="text/javascript">
		
			function deleteEvent()
			{
				$.post("${pageContext.request.contextPath}/km/events/delete", { id : "${event.id}" } , function(data) {
					// delete warn message
					$("#warnPrompt").html("");
					
					if (data.status == "success")
					{
						// redirect to view list
						openUrl("${pageContext.request.contextPath}/km/events/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error", null, null);
					}
				}, "json")
			}
			
			$(document).ready(function() {
				renderGuestList();
				
				$("#guestAddBtn").click(function() {
					saveGuest();
				});
			});
			
			function saveGuest()
			{
				var guestId = $("#newGuest").val();
				$.post(km.js.config.contextPath + "/km/events/addguest", { guestId: guestId, eventId: "${event.id}" }, function(data) {
					
					if (data.success === true)
					{
						km.js.ui.statusbar.show("Guest has been added");
						
						guestList.update();
					}
					else
					{
						km.js.ui.statusbar.err("Error adding guest");
					}
					
				});
			}
			
			function renderGuestList()
			{
				window.guestInput = km.js.userlookup.create({ inputName: "newGuest", inputId: "newGuest", visibleInput: { cssClass: "km-input" } });
				window.guestInput.render(function(code) {
					$("#newGuestWrapper").empty().append(code);
				});
			}
			
			function renderDeleteLink(guestId)
			{
				var deleteLink = $("<a></a>").text("delete").addClass("km-events-delete-btn");
				
				var deleteGuest = (function(guestId) {
					
					return function() {
						$.post(km.js.config.contextPath + "/km/events/deleteguest", { guestId: guestId, eventId: "${event.id}" }, function(data) {
							
							guestList.update();
							if (data.success === true)
							{
								km.js.ui.statusbar.show("Guest has been deleted");
							}
							else
							{
								km.js.ui.statusbar.err("Error deleting guest");
							}
							
						});
					}
				
				})(guestId);
				
				km.js.ui.confirm({
					callback: deleteGuest,
					question: "Are you sure?",
					target: deleteLink
				});
				
				// return jQuery object
				return deleteLink;
			}
		
		</script>
		
		<style>
		
			div#newGuestForm {
				margin: 1em 0 1em 0;
			}
			
			div#newGuestWrapper {
				display: inline-block;
				margin-right: 2em;
			}
		
		</style>
		
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
				
			<ko:pageHeader>${event.name}</ko:pageHeader>
			<ko:buttonPanel>
				<a href="${pageContext.request.contextPath}/km/events/edit/${event.id}" class="sbtn">Edit</a>
				<a href="javascript:;" onclick="ask('Are you sure you want to delete this event?', 'warnPrompt', function() { deleteEvent(); })" class="sbtn" id="deleteEventBtn">Delete</a>
				<a href="${pageContext.request.contextPath}/km/calendar" class="sbtn">Calendar</a>
			</ko:buttonPanel>
			<div id="warnPrompt" style="margin-top:10px"></div>
			
			<ko:propertyTable>
				<ko:propertyRow>
					<ko:propertyLabel value="Name"></ko:propertyLabel>
					<ko:propertyValue>${event.name}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Start Date"></ko:propertyLabel>
					<ko:propertyValue><km:dateTime value="${event.startDate}" format="dd-MM-yyyy HH:mm:ss" /></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="End Date"></ko:propertyLabel>
					<ko:propertyValue><km:dateTime value="${event.endDate}" format="dd-MM-yyyy HH:mm:ss" /></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Description"></ko:propertyLabel>
					<ko:propertyValue>${event.description}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="User"></ko:propertyLabel>
					<ko:propertyValue><a href="${pageContext.request.contextPath}/km/user/${event.owner.id}">${event.owner.userName}</a></ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
			
			<h3 style="margin-top: 2em">Guests</h3>
			
			<div id="newGuestForm">
				<div id="newGuestWrapper"></div>
				<input type="button" class="sbtn" id="guestAddBtn" value='<kolmu:label key="event.btn.addguest" />'></input>
			</div>
			
			<km:dataTable var="window.guestList" query="select id, guest.id, guest.userName, response from EventGuest where event.id = '${event.id}'" paginationActive="true" pageSize="25">
				<km:dataTableColumn url="${pageContext.request.contextPath}/km/users/{id}" name="guest.userName" label="User Name" sortable="true" linkStyle="true" />
				<km:dataTableColumn name="response" labelKey="Response" sortable="true" link="false" />
				<km:dataTableColumn label="Delete" name="guest.id" formatFunction="renderDeleteLink" />
			</km:dataTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
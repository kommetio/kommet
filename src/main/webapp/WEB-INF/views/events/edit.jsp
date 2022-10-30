<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}" importRMJS="true">
	<jsp:body>
	
		<link href="${pageContext.request.contextPath}/resources/css/smoothness/jquery-ui-1.10.3.custom.min.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-sliderAccess.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-timepicker-addon.min.js"></script>
		<link href="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-timepicker-addon.min.css" rel="stylesheet" type="text/css" />
		
		<script>
		
			$(document).ready(function() {
				
				// init date time picker
				var startDatePicker = $("input#startdate").datepicker({ dateFormat: "yy-mm-dd" });
				var endDatePicker = $("input#enddate").datepicker({ dateFormat: "yy-mm-dd" });
				
				<c:if test="${not empty event.startDate}">
				startDatePicker.datepicker("setDate", new Date(${event.startDate.year} + 1900, ${event.startDate.month}, ${event.startDate.date}));
				</c:if>
				<c:if test="${empty event.startDate}">
				startDatePicker.datepicker("setDate", new Date());
				</c:if>
				
				<c:if test="${not empty event.endDate}">
				endDatePicker.datepicker("setDate", new Date(${event.endDate.year} + 1900, ${event.endDate.month}, ${event.endDate.date}));
				</c:if>
				<c:if test="${empty event.endDate}">
				endDatePicker.datepicker("setDate", new Date());
				</c:if>
				
				setSelectedHours();
				
				var ownerId = "${event.owner.id}";
				
				// init owner lookup
				var ownerLookup = km.js.userlookup.create({
					inputName: "ownerId",
					inputId: "ownerId",
					visibleInput: { cssClass: "km-input" },
					selectedRecordId: ownerId ? ownerId : null
				});
				
				ownerLookup.render(function(code) {
					$("#event-owner").empty().append(code);
				});
			});
			
			function setSelectedHours()
			{
				<c:if test="${not empty event.startDate}">
				var startHours = km.js.utils.padLeft("${event.startDate.hours}", 2, "0");
				var startMinutes = km.js.utils.padLeft("${event.startDate.minutes}", 2, "0");
				var startValue = startHours + ":" + startMinutes;
				$("#startDateHour").val(startValue);
				</c:if>
				
				<c:if test="${not empty event.endDate}">
				var endHours = km.js.utils.padLeft("${event.endDate.hours}", 2, "0");
				var endMinutes = km.js.utils.padLeft("${event.endDate.minutes}", 2, "0");
				var endValue = endHours + ":" + endMinutes;
				$("#endDateHour").val(endValue);
				</c:if>
			}
		
		</script>
		
		<style>
			
			.kdetails select.time-picker {
				width: 6em;
				min-width: 6em;
			}
			
			span.hour-label {
				margin-left: 1em;
    			margin-right: 0.5em;
    			font-weight: bold;
			}
		
		</style>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/events/save">
				<input type="hidden" name="eventId" value="${event.id}" />
				
				<ko:pageHeader>${pageTitle}</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${event.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Start Date" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="startDate" id="startdate" />
							<span class="label hour-label">hour</span>
							<select name="startDateHour" id="startDateHour" class="time-picker">
								<c:forEach var="hour" items="${hourList}">
									<option value="${hour}">${hour}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="End Date" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="endDate" id="enddate" />
							<span class="label hour-label">hour</span>
							<select name="endDateHour" id="endDateHour" class="time-picker">
								<c:forEach var="hour" items="${hourList}">
									<option value="${hour}">${hour}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Description"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="description">${event.description}</textarea>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Owner" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<div id="event-owner"></div>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
						<c:if test="${not empty event.id}">
							<a href="${pageContext.request.contextPath}/km/events/${event.id}" class="sbtn">Cancel</a>
						</c:if>
						<c:if test="${empty event.id}">
							<a href="${pageContext.request.contextPath}/km/events/list" class="sbtn">Cancel</a>
						</c:if>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
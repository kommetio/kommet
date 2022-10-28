<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Calendar" importRMJS="true">

	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.calendar.js"></script>
	
		<km:breadcrumbs isAlwaysVisible="true" />
			
		<script>

			/*$(document).ready(function() {
			
				var myCal = km.js.calendar.create({
					startDate: new Date(2015, 1, 1),
					endDate: new Date(2015, 5, 31)
				});
				
				myCal.render(function(code) {
					$("#calendar").append(code);
				});
				
				var events = [
					{ id: 1, startDate: new Date (2015, 1, 2, 7), name: "Brunch with a lengthy title bla bla bla", duration: 3600000 },
					{ id: 2, startDate: new Date (2015, 1, 2, 10, 30), name: "Meeting with all the team members", duration: 7200000 },
					{ id: 3, startDate: new Date (2015, 1, 2, 10), name: "Scrum 30min.", duration: 1800000 },
					{ id: 4, startDate: new Date (2015, 1, 2, 13), name: "45min. meeting", duration: 2700000 }
				];
				
				myCal.addEvents(events);
				
				myCal.showRange(new Date(2015, 1, 1), new Date(2015, 1, 7));
			
			});*/
			
			$(document).ready(function() {
				loadCalendar();	
			});
			
			function loadCalendar()
			{
				var today = new Date();
				today.setHours(0);
				today.setMinutes(0);
				today.setSeconds(0);
				
				// add seven days
				var endDate = new Date(today);
				endDate.setDate(endDate.getDate() + 6);
				endDate.setHours(23);
				endDate.setMinutes(59);
				endDate.setSeconds(59);
				
				$.get(km.js.config.contextPath + "/km/rest/events", { startDate: today.getTime(), endDate: endDate.getTime(), userId: km.js.config.authData.user.id }, (function(startDate, endDate) {
					
					return function(data) {
					
						var myCal = km.js.calendar.create({
							startDate: startDate,
							endDate: endDate
						});
						
						myCal.render(function(code) {
							$("#calendar").append(code);
						});
						
						myCal.addEvents(data.data.events);
						
						myCal.showRange(startDate, endDate);
					
					}
					
				})(today, endDate));
			}
			
		</script>
		
		<div class="ibox">
			<ko:pageHeader cssStyle="border-bottom: none">Calendar</ko:pageHeader>
			<div id="calendar"></div>
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
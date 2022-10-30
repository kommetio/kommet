<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Scheduled Tasks">

	<jsp:body>
	
		<div class="ibox">
	
			<ko:pageHeader>Active jobs</ko:pageHeader>
	
			<table class="std-table" style="margin-top: 30px">
				<thead>
					<tr class="cols">
						<th>Task</th>
						<th>CRON</th>
						<th>Next execution</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="job" items="${jobs}">
						<tr>
							<td><a href="${pageContext.request.contextPath}/km/scheduledtask/${job.task.id}">${job.task.name}</a></td>
							<td>${job.task.cronExpression}</td>
							<td><km:dateTime value="${job.trigger.nextFireTime}" format="dd-MM-yyyy HH:mm:ss" /></td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
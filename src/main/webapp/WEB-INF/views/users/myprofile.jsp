<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:userLayout layoutPath="${layoutPath}" titleKey="title.myprofile">
	<jsp:body>

			<c:if test="${showNotifications == true}">

			<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/notifications.js"></script>

			<script>
				$(document).ready(function() {
					notifications.init('${pageContext.request.contextPath}', '<kolmu:label key="notif.notification.title" />', '<kolmu:label key="notif.nonotifications" />');
					notifications.getNotifications($("#notifications"));
				});
			</script>
			
			</c:if>
		
	
		<div class="ibox">
		
			<ko:pageHeader>${user.userName}</ko:pageHeader>
			<c:if test="${canEdit == true}">
				<a href="${pageContext.request.contextPath}/km/users/edit/${user.id}" class="sbtn"><kolmu:label key="btn.edit" /></a>
			</c:if>
			<c:if test="${canChangePwd == true}">
				<a href="${pageContext.request.contextPath}/km/changepassword" class="sbtn"><kolmu:label key="btn.changepwd" /></a>
			</c:if>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label"><kolmu:label key="user.username" /></td>
						<td class="value">${user.userName}</td>
						<td class="sep"></td>
						<td class="label"><kolmu:label key="user.email" /></td>
						<td class="value">${user.email}</td>
					</tr>
					<tr>
						<td class="label"><kolmu:label key="user.timezone" /></td>
						<td class="value">${user.timezone}</td>
						<td class="sep"></td>
						<td class="label"><kolmu:label key="user.locale" /></td>
						<td class="value">${user.localeSetting.language}</td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
		<c:if test="${showNotifications == true}">
		<div class="ibox" style="margin-top:15px">
			<div id="notifications"></div>
		</div>
		</c:if>
		
	</jsp:body>
</ko:userLayout>

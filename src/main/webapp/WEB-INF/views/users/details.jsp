<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${user.userName} - user details" importRMJS="true">
	<jsp:body>
	
		<script>

			function addNotification()
			{	
				window.userNotificationDialog = km.js.ui.dialog.create({
					id: "add-notification",
					size: {
						height: "600px",
						width: "800px"
					},
					url: "${pageContext.request.contextPath}/km/notifications/new?assigneeId=${user.id}&parentDialog=userNotificationDialog"
				});
				
				window.userNotificationDialog.show();
			}
			
			function forceActivate()
			{	
				$.post("${pageContext.request.contextPath}/km/users/forceactivate?userId=${user.id}", function(data) {
					
					if (data.status == "success")
					{
						km.js.ui.statusbar.show(km.js.config.i18n["user.forceactivation.success"]);
					}
					else
					{
						km.js.ui.statusbar.show(km.js.config.i18n["user.forceactivation.failure"]);
					}
				});
			}

			$(document).ready(function() {
				initTabs();
				
				$("a#activationLinkBtn").click(function() {
					
					$.get(km.js.config.contextPath + "/km/user/getactivationlink", { userId: "${user.id}" }, function(data) {
						
						if (data.success)
						{
							var code = $("<div></div>").addClass("km-activation-dialog");
							code.append(data.data.link);
							
							km.js.ui.dialog.create({
								id: "act-link",
								size: {
									width: "50%",
									height: "10rem"
								}
							}).show(code);
						}
						
					}, "json");
					
				});
			});
			
			function initComments()
			{
				var userCmt = km.js.comments.create({
					id: "user-comments",
					recordId: "${user.id}",
					addComments: ${canAddComments}
				});
				
				userCmt.render($("#user-comments"));
			}
			
			function initTabs()
			{
				// create tabs
				var tabs = km.js.tabs.create({
					tabs: [
						{ content: $("#user-details"), label: "${user.userName}" },
						{ content: $("#user-comments"), label: "Comments" },
						{ content: $("#login-history"), label: "Login history" }
					],
					originalContentHandling: "remove",
					afterRender: function() {
						initComments();
						loadLoginHistory();
					}
				});
				
				tabs.render(function(code) {
					$("#tab-container").html(code);
				});
				
				// open the first tab
				tabs.openActiveTab();
			}

			function loadLoginHistory()
			{
				$.get("${pageContext.request.contextPath}/km/users/loginhistory?userId=${user.id}&maxResults=20", function(data) {

					if (data.status == "success")
					{
						var options = {
							id: "lht",
							columns: [ 
								{ title: "Date", property: "createdDate" },
								{ title: "Result", property: "result" },
								{ title: "Method", property: "method" },
								{ title: "IP", property: "ip4Address" }
							],
							cssClasses: [ "std-table" ] 
						}
						
						var loginTable = tables.create(options);
						$("#login-history").html("<h3>Login history</h3><div id=\"histTable\"></div>");
						loginTable.render($("#histTable"), data.data);	
					}
					else
					{
						showMsg ("login-history", data.messages, "error", null, null, "${pageContext.request.contextPath}/rm");
					}
					
				}, "json");
			}
		
		</script>
		
		<style>
		
			div.km-activation-dialog {
				padding: 2rem;
				background: #fff;
			}
		
		</style>
		
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<div id="tab-container"></div>
	
		<div id="user-details">
		
			<form id="secondaryLogin" action="${pageContext.request.contextPath}/km/users/secondarylogin" method="post">
				<input type="hidden" name="userId" value="${user.id}" />
			</form>
			
			<div id="cmt"></div>
		
			<ko:pageHeader>${user.userName}</ko:pageHeader>
			<c:if test="${canEdit == true}">
				<a href="${pageContext.request.contextPath}/km/users/edit/${user.id}" class="sbtn"><kolmu:label key="btn.edit" /></a>
				<a href="javascript:;" class="sbtn" id="activationLinkBtn">Generate activation link</a>
			</c:if>
			<c:if test="${canLogin == true}">
				<a href="javascript:;" onclick="document.getElementById('secondaryLogin').submit()" class="sbtn"><kolmu:label key="btn.login.as" /></a>
			</c:if>
			<c:if test="${canSendNotification == true}">
				<a href="javascript:;" onclick="addNotification()" class="sbtn"><kolmu:label key="notif.create.btn" /></a>
			</c:if>
			<c:if test="${canForceActivate == true}">
				<a href="javascript:;" onclick="forceActivate()" class="sbtn"><kolmu:label key="user.activate.btn" /></a>
			</c:if>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">User name</td>
						<td class="value">${user.userName}</td>
						<td class="sep"></td>
						<td class="label">Profile</td>
						<td class="value">${user.profile.name}</td>
					</tr>
					<tr>
						<td class="label">E-mail</td>
						<td class="value">${user.email}</td>
						<td class="sep"></td>
						<td class="label">Locale</td>
						<td class="value">${user.localeSetting.language}</td>
					</tr>
					<tr>
						<td class="label">Time zone</td>
						<td class="value">${user.timezone}</td>
						<td class="sep"></td>
						<td class="label">Default layout</td>
						<td class="value">${userSettings.layout.name}</td>
					</tr>
					<tr>
						<td class="label">Is active</td>
						<td class="value">
							<c:if test="${user.isActive == true}">Yes</c:if>
							<c:if test="${user.isActive == false}">No</c:if>
						</td>
						<td class="sep"></td>
						<td class="label">Is activation hash set</td>
						<td class="value">
							<c:if test="${empty user.activationHash}">No</c:if>
							<c:if test="${not empty user.activationHash}">Yes</c:if>
						</td>
					</tr>
				</tbody>
			</table>
			
			<div id="ntdialog"></div>
		
		</div>
		
		<div id="user-comments"></div>
		<div id="login-history"></div>
		
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/tables.js"></script>
		
	</jsp:body>
</ko:homeLayout>
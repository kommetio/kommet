<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="auth" uri="/WEB-INF/tld/kolmu-auth-tags.tld" %>

<script>

	$(document).ready(function() {
		collapseMenu();
		initMenu();
		km.js.utils.openCurrentMenuItem();
	});

	function collapseMenu()
	{
		$("#left-menu > ul > li > ul").hide();
	}

	function initMenu()
	{
		$("#left-menu a").click(function() {
			$("#left-menu a").css("font-weight", "normal");
			$(this).closest("li").find("ul").toggle();
		});
	}

</script>

<div id="left-menu" class="km-menu">

	<ul>
  		<auth:check profile="Root,SystemAdministrator">
  			<li>
				<a target="_self" href="${pageContext.request.contextPath}/km/me" class="menu-item"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/setup-white.png"></img></span><span class="km-menu-item-title">Open app</span></a>
			</li>
			<li>
				<a target="_self" href="${pageContext.request.contextPath}/km/appmarketplace" class="menu-item"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/setup-white.png"></img></span><span class="km-menu-item-title">App marketplace</span></a>
			</li>
			<li>
				<a href="javascript:;"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Database</span></a>
				<ul>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/types/list" class="menu-item">Objects</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/datamodeldiagram" class="menu-item">Data Model Diagram</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/validationrules/list" class="menu-item">Validation Rules</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/dictionaries/list" class="menu-item">Dictionaries</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/querydb" class="menu-item">Query database</a></li>
				</ul>
			</li>
			<li>
				<a href="javascript:;"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Web</span></a>
				<ul>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/actions/list" class="menu-item">Actions</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/views/list" class="menu-item">Views</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/layouts/list" class="menu-item">Layouts</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/textlabels/list" class="menu-item">Text Labels</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/webresources/list" class="menu-item">Web Resources</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/viewresources/list" class="menu-item">View Resources</a></li>
				</ul>
			</li>
			<li>
				<a target="_self" href="${pageContext.request.contextPath}/km/classes/list" class="menu-item"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Classes</span></a>
			</li>
			<li>
				<a target="_self" href="${pageContext.request.contextPath}/km/ide" class="menu-item"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">IDE</span></a>
			</li>
			<li>
				<a href="javascript:;"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Business Processes</span></a>
				<ul>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/bp/processes/list" class="menu-item">Business Processes</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/bp/builder" class="menu-item">Process Builder</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/bp/actions/list" class="menu-item">Business Actions</a></li>
				</ul>
			</li>
			<li>
				<a href="javascript:;"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Administration</span></a>
				<ul>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/usersettings/list" class="menu-item">User settings</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/apps/list" class="menu-item">Apps</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/libraries/list" class="menu-item">Libraries</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/errorlogs" class="menu-item">Error Logs</a></li>
					<auth:check profile="Root">
					<li><a target="_self" href="${pageContext.request.contextPath}/km/systemsettings/details" class="menu-item">System settings</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/adminpanel" class="menu-item">Admin Panel</a></li>
					</auth:check>
				</ul>
			</li>
			<li>
				<a href="javascript:;"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Users and Profiles</span></a>
				<ul>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/profiles/list" class="menu-item">Profiles</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/users/list" class="menu-item">Users</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/usergroups/list" class="menu-item">User groups</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/loginhistory" class="menu-item">Login History</a></li>
				</ul>
			</li>
			<li>
				<a href="javascript:;"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Tools</span></a>
				<ul>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/scheduledtasks" class="menu-item">Scheduled Tasks</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/scheduledtasks/jobs" class="menu-item">Active scheduled jobs</a></li>
					<auth:check profile="Root">
					<li><a target="_self" href="${pageContext.request.contextPath}/km/generatefieldid" class="menu-item">Generate Field ID</a></li>
					</auth:check>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/dataimport" class="menu-item">Import Data</a></li>
				</ul>
			</li>
			<li>
				<a href="javascript:;"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Utilities</span></a>
				<ul>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/doctemplates" class="menu-item">Doc Templates</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/files/list" class="menu-item">Files</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/notifications/list" class="menu-item">Notifications</a></li>
					<%--<li><a target="_self" href="${pageContext.request.contextPath}/km/reporttypes/list" class="menu-item">Report Types</a></li>--%>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/tasks/list" class="menu-item">Tasks</a></li>
					<li><a target="_self" href="${pageContext.request.contextPath}/km/events/list" class="menu-item">Calendar events</a></li>
				</ul>
			</li>
		</auth:check>
		
		<li>
			<a target="_self" href="${pageContext.request.contextPath}/km/me" class="menu-item"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">My Profile</span></a>
		</li>
		
		<li>
			<a target="_self" href="${pageContext.request.contextPath}/km/logout" class="menu-item"><span class="km-menu-icon"><img src="${pageContext.request.contextPath}/resources/images/extlinkwhite.png"></img></span><span class="km-menu-item-title">Log out</span></a>
		</li>
  	</ul>
</div>
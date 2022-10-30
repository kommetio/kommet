<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-lightness/jquery-ui-1.10.3.custom.min.css" />

<style>
	
	a.def-page-edit {
		margin-left: 10px;
	}
	
	/*a.def-action {
		text-overflow: ellipsis;
  		width: 10em;
  		white-space: nowrap;
  		display: inline-block;
  		overflow: hidden;
	}*/
	
	table#std-actions > tbody > tr > td {
		white-space: nowrap;
	}
	
	table#std-actions > tbody > tr > td > a.def-page-edit {
		color: #3F4FA3;
	}
	
	div#default-views {
		margin-bottom: 2em;
	}
	
	div#default-views .label {
		width: 10rem;
	}
	
	div#default-views .km-rd-property {
		width: 100%;
	}
	
	div#default-views .km-lookup {
		min-width: 25em;
	}
	
	div.view-edit-value > div {
		display: inline-block;
		padding: 0 0.5rem;
	}

	div.view-edit-value .km-lookup {
		width: 25em;
		padding: 0;
	}
	
	div.view-edit-value select {
		width: 10rem;
	}
	
	div.customize-wrapper > a {
		text-decoration: none;
		color: #3F4FA3;
	}
	
</style>

	<script type="text/javascript">
			
			function saveDefaultView(settingKey)
			{
				$.post(km.js.config.contextPath + "/km/savedefaulttypeview", { settingKey: settingKey, viewId: $("input[name='" + settingKey + "']").val(), typeId: "${typeId}" }, function(data) {
					
					if (data.success)
					{
						km.js.ui.statusbar.show("Default view saved", 5000);
					}
					else
					{
						km.js.ui.statusbar.err("Error saving default view", 10000);
					}
					
				}, "json");
			}
			
			function initViews()
			{
				$(".view-customize-btn").click(function() {
					$(this).closest(".view-edit-value").find(".view-edit-wrapper").css("display", "inline-block");
				});
				
				$("select.custom-view-choice").change(function() {
					
					updateViewChoice($(this));
					
				});
			}
			
			function updateViewChoice(select)
			{
				console.log("Choice");
				var wrapper = select.closest(".view-edit-value");
				wrapper.find(".view-edit-wrapper").hide();
				
				var val = select.val();
				
				if (val === "customview")
				{
					wrapper.find("div.list-view-edit-wrapper").css("display", "inline-block");
				}
			}
			
	</script>
	
<div class="section-subtitle">Default views</div>
	
	<div class="km-rd-table" id="default-views">
		<div class="km-rd-row">
			<div class="km-rd-property">
				<%--<div class="label">List View</div>
				<div class="value view-edit-value">
					<div>
						<select class="custom-view-choice">
							<option value="default">default</option>
							<option value="customview">custom view</option>
							<option value="customlayout">custom layout</option>
						</select>
					</div>
					<div class="view-edit-wrapper list-view-edit-wrapper"><div id="defaultListView"></div></div>
				</div>--%>
				<div class="label">List View</div>
				<div class="value view-edit-value"><div id="defaultListView"></div></div>
			</div>
		</div>
		<div class="km-rd-row">
			<div class="km-rd-property">
				<div class="label">Details View</div>
				<div class="value view-edit-value"><div id="defaultDetailsView"></div></div>
			</div>
		</div>
		<div class="km-rd-row">
			<div class="km-rd-property">
				<div class="label">Edit View</div>
				<div class="value view-edit-value"><div id="defaultEditView"></div></div>
			</div>
		</div>
		<div class="km-rd-row">
			<div class="km-rd-property">
				<div class="label">Create View</div>
				<div class="value view-edit-value"><div id="defaultCreateView"></div></div>
			</div>
		</div>
	</div>
	
<div class="section-subtitle">Default actions</div>

<table class="std-table" id="std-actions">
	<thead>
		<tr class="cols">
			<th>Profile</th>
			<th>List Action</th>
			<th>View Action</th>
			<th>Edit Action</th>
			<th>Create Action</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${profiles}" var="profile">
			<tr>
				<td>${profile.name}</td>
				<td>
					<c:if test="${stdPagesByProfile[profile.id].listAction.id == typeInfo.defaultListAction.id}">
						<a class="def-action" href="${pageContext.request.contextPath}/km/actions/${stdPagesByProfile[profile.id].listAction.id}">default</a>
					</c:if>
					<c:if test="${stdPagesByProfile[profile.id].listAction.id != typeInfo.defaultListAction.id}">
						<a class="def-action" href="${pageContext.request.contextPath}/km/actions/${stdPagesByProfile[profile.id].listAction.id}">${stdPagesByProfile[profile.id].listAction.interpretedName}</a>
					</c:if>
					<a href="${pageContext.request.contextPath}/km/standardpages/modify?typeId=${typeId}&profileId=${profile.id}&action=list" class="def-page-edit">[ edit ]</a>
				</td>
				<td>
					<c:if test="${stdPagesByProfile[profile.id].detailsAction.id == typeInfo.defaultDetailsAction.id}">
						<a class="def-action" href="${pageContext.request.contextPath}/km/actions/${stdPagesByProfile[profile.id].detailsAction.id}">default</a>
					</c:if>
					<c:if test="${stdPagesByProfile[profile.id].detailsAction.id != typeInfo.defaultDetailsAction.id}">
						<a class="def-action" href="${pageContext.request.contextPath}/km/actions/${stdPagesByProfile[profile.id].detailsAction.id}">${stdPagesByProfile[profile.id].detailsAction.interpretedName}</a>
					</c:if>
					<a href="${pageContext.request.contextPath}/km/standardpages/modify?typeId=${typeId}&profileId=${profile.id}&action=view" class="def-page-edit">[ edit ]</a>
				</td>
				<td>
					<c:if test="${stdPagesByProfile[profile.id].editAction.id == typeInfo.defaultEditAction.id}">
						<a class="def-action" href="${pageContext.request.contextPath}/km/actions/${stdPagesByProfile[profile.id].editAction.id}">default</a>
					</c:if>
					<c:if test="${stdPagesByProfile[profile.id].editAction.id != typeInfo.defaultEditAction.id}">
						<a class="def-action" href="${pageContext.request.contextPath}/km/actions/${stdPagesByProfile[profile.id].editAction.id}">${stdPagesByProfile[profile.id].editAction.interpretedName}</a>
					</c:if>
					<a href="${pageContext.request.contextPath}/km/standardpages/modify?typeId=${typeId}&profileId=${profile.id}&action=edit" class="def-page-edit">[ edit ]</a>
				</td>
				<td>
					<c:if test="${stdPagesByProfile[profile.id].createAction.id == typeInfo.defaultCreateAction.id}">
						<a class="def-action" href="${pageContext.request.contextPath}/km/actions/${stdPagesByProfile[profile.id].createAction.id}">default</a>
					</c:if>
					<c:if test="${stdPagesByProfile[profile.id].createAction.id != typeInfo.defaultCreateAction.id}">
						<a class="def-action" href="${pageContext.request.contextPath}/km/actions/${stdPagesByProfile[profile.id].createAction.id}">${stdPagesByProfile[profile.id].createAction.interpretedName}</a>
					</c:if>
					<a href="${pageContext.request.contextPath}/km/standardpages/modify?typeId=${typeId}&profileId=${profile.id}&action=create" class="def-page-edit">[ edit ]</a>
				</td>
			</tr>
		</c:forEach>
	</tbody>
</table>
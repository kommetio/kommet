<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${group.name}" importRMJS="true">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<style>
		
			table.new-member-form {
				margin: 2em 0 2em 0;
			}
		
		</style>
	
		<script type="text/javascript">
		
			var pageData = {
				userLookup: null,
				groupLookup: null
			};
		
			function deleteUserGroup()
			{
				$.post("${pageContext.request.contextPath}/km/usergroups/delete", { id : "${group.id}" } , function(data) {
					// delete warn message
					$("#warnPrompt").html("");
					
					if (data.status == "success")
					{
						// redirect to view list
						openUrl("${pageContext.request.contextPath}/km/usergroups/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error", null, null);
					}
				}, "json")
			}
			
			function getMembersTable()
			{
				var ds = km.js.datasource.create({
					type: "database"
				});
				
				var jcr = {
					baseTypeName: "kommet.basic.UserGroupAssignment",
					properties: [
						{ name: "id" },
						{ name: "createdDate" },
						{ name: "childUser.id" },
						{ name: "childUser.userName" },
						{ name: "childGroup.id" },
						{ name: "childGroup.name" }
					],
					restrictions: [ { property_name: "parentGroup.id", operator: "eq", args: [ "${group.id}" ]} ]
				};
				
				var btnCallback = function (record, recordId, cell) {
					var btn = $("<a></a>").attr("href", "javascript:;").html("Delete");
					
					km.js.ui.confirm({
						target: btn,
						question: "Are you sure?",
						callback: function() {
							$.post("${pageContext.request.contextPath}/km/usergroups/removemember", { assignmentId: recordId }, function(data) {
								getMembersTable();
							}, "json");
						}
					});
					
					cell.empty().append(btn);
				};
				
				var displayOptions = {
					properties: [
						{ name: "childUser.userName", label: "User name", linkStyle: true },
						{ name: "childGroup.name", label: "User group", linkStyle: true },
						{ name: "id", content: btnCallback, label: "Actions" }
					],
					idProperty: { name: "id" }
				};
				
				var tableOptions = {
					id: "group-members"
				}
				
				var memberTable = km.js.table.create(ds, jcr, displayOptions, tableOptions);
				
				memberTable.render(jcr, function(table) {
					$("#member-list-container").empty().append(table);
				});
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
					inputId: "userId"
				});
				
				userLookup.render($("#userLookup"));
				
				pageData.userLookup = userLookup;
			}
			
			function initUserGroupLookup()
			{
				// jcr to query users
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
							{ name: "name", label: "Group name", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var groupLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsOptions: availableItemsOptions,
					inputName: "userGroupId",
					inputId: "userGroupId"
				});
				
				groupLookup.render($("#groupLookup"));
				
				pageData.groupLookup = groupLookup;
			}
			
			function addMember()
			{
				$("#add-errors").empty();
				
				var userGroupId = $("#userGroupId").val();
				if (userGroupId && userGroupId == "${group.id}")
				{
					showMsg ("add-errors", "Cannot assign group to itself", "error", null, null, km.js.config.contextPath);
				}
				
				$.post("${pageContext.request.contextPath}/km/usergroups/addmember", { parentGroupId: "${group.id}", userId: $("#userId").val(), userGroupId: userGroupId }, function(data) {
					
					if (data.success === true)
					{
						getMembersTable();
						pageData.userLookup.clear();
						pageData.groupLookup.clear();
						km.js.ui.statusbar.show("Group member has been added", 10000);
					}
					else
					{
						showMsg ("add-errors", data.message, "error", null, null, km.js.config.contextPath);
					}
				}, "json");
			}
			
			$(document).ready(function() {
				getMembersTable();
				initUserLookup();
				initUserGroupLookup();
			})
		
		</script>
	
		<div class="ibox">
		
			<ko:pageHeader>${group.name}</ko:pageHeader>
			<ko:buttonPanel>
				<a href="${pageContext.request.contextPath}/km/usergroups/edit/${group.id}" class="sbtn">${i18n.get('btn.edit')}</a>
				<a href="javascript:;" onclick="ask('Are you sure you want to delete this group?', 'warnPrompt', function() { deleteUserGroup(); })" class="sbtn" id="deleteUserGroupBtn">${i18n.get('btn.delete')}</a>
			</ko:buttonPanel>
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">${i18n.get('usergroups.groupname')}</td>
						<td class="value">${group.name}</td>
					</tr>
				</tbody>
			</table>
			
			<h3>${i18n.get('usergroups.members.add')}</h3>
			
			<div id="add-errors"></div>
			
			<ko:propertyTable cssClass="new-member-form">
				<ko:propertyRow>
					<ko:propertyLabel value="Member user"></ko:propertyLabel>
					<ko:propertyValue>
						<input type="text" id="userLookup"></input>
					</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Member user group"></ko:propertyLabel>
					<ko:propertyValue>
						<input type="text" id="groupLookup"></input>
					</ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
			
			<ko:buttonPanel>
				<input type="button" value="${i18n.get('btn.save')}" class="sbtn" onClick="addMember()" />
			</ko:buttonPanel>
			
			<h3 style="margin-top: 2em">${i18n.get('usergroups.members')}</h3>
			<div id="member-list-container"></div>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Setting" importRMJS="true">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.table.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ref.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.datasource.js"></script>
	
		<style>
		
			.context-row {
				display: none;
			}
			
			tr.hidden-row {
				display: none;
			}
		
		</style>

		<script>
		
			function createLayoutLookup()
			{
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.Layout",
					properties: [
						{ name: "id" },
						{ name: "name" }
					]
				};
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: "Layouts",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var layoutLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "defaultLayoutId",
					selectedRecordId: "${setting.value}",
					afterSelect: setDefaultLayoutSettingValue
				});
				
				layoutLookup.render($("#default-layout-id"));
			}
		
			$(document).ready(function() {
				
				(function() {
					// jcr to query profiles
					var jcr = {
						baseTypeName: "kommet.basic.Profile",
						properties: [
							{ name: "id" },
							{ name: "name" }
						]
					};
					
					// options of the available items list
					var availableItemsOptions = {
						display: {
							properties: [
								{ name: "name", label: "Profile name", linkStyle: true }
							],
							idProperty: { name: "id" }
						},
						tableSearchOptions: {
							properties: [ { name: "name", operator: "ilike" } ]
						}
					};
					
					// create the lookup
					var profileLookup = km.js.ref.create({
						selectedRecordDisplayField: { name: "name" },
						jcr: jcr,
						availableItemsOptions: availableItemsOptions,
						inputName: "profileId",
						selectedRecordId: "${setting.hierarchy.profile.id}"
					});
					
					profileLookup.render($("#profileLookup"));
					
				})();
				
				(function() {
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
						selectedRecordId: "${setting.hierarchy.user.id}"
					});
					
					userLookup.render($("#userLookup"));
					
				})();
				
				(function() {
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
					var userGroupLookup = km.js.ref.create({
						selectedRecordDisplayField: { name: "name" },
						jcr: jcr,
						availableItemsOptions: availableItemsOptions,
						inputName: "userGroupId",
						selectedRecordId: "${setting.hierarchy.userGroup.id}"
					});
					
					userGroupLookup.render($("#userGroupLookup"));
					
				})();
			});
			
			$(document).ready(function() {
				
				var onContextChange = function(val) {
					
					// hide all context rows
					$(".context-row").hide();
					
					// show only the applicable context row
					$("#setting-form #" + val.replace(/\s/, "-") + "-context").show();
				};
				
				// select context
				if ("${setting.hierarchy.activeContextName}")
				{
					$("#activeContext").val("${setting.hierarchy.activeContextName}".toLowerCase());
					onContextChange("${setting.hierarchy.activeContextName}".toLowerCase());
				}
				
				$("#activeContext").change(function() { onContextChange($(this).val()); });
				
				// set selected key
				if ("${setting.key}")
				{
					$("select#settingKey").val("${setting.key}").change();
				}
				
				createLayoutLookup();
				initKeyChange();
				onKeyChange($("select#settingKey"));
			});
			
			function onKeyChange(select)
			{
				var originalValue = $("input#value").val();
				
				// if any of the predefined values are selected
				if (select.val())
				{
					$("tr.key-row").hide();
					$("#manualKey").val(null);
					$("input#value").val(null);
					
					$("#page-title").text(select.find("option:selected").text());
				}
				else
				{
					$("tr.key-row").show();
					$("#page-title").text("User setting");
				}
				
				if (select.val() === "km.sys.defaultlayout.id")
				{
					$("tr.value-row").hide();
					$("tr.default-layout-row").show();
				}
				else
				{
					$("tr.value-row").show();
					$("tr.default-layout-row").hide();
					$("input#value").val(originalValue);
				}
			}
			
			function initKeyChange()
			{
				$("select#settingKey").change(function() {
					
					onKeyChange($(this));
					
				});
			}
			
			function setDefaultLayoutSettingValue(layoutId)
			{
				console.log("SEL: " + layoutId);
				$("input#value").val(layoutId);
			}
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<form id="setting-form" method="post" action="${pageContext.request.contextPath}/km/usersettings/save">
				<input type="hidden" name="settingId" value="${setting.id}" />
				
				<ko:pageHeader id="page-title">
					<c:if test="${empty setting.id}">New setting</c:if>
					<c:if test="${not empty setting.id}">${setting.key}</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="predefinedKey" id="settingKey">
								<option value="">Generic setting</option>
								<option value="km.home.url">Default home URL</option>
								<option value="km.sys.defaultlayout.id">Default layout</option>
								<option value="km.sys.can.login">Is allowed to log in</option>
								<option value="km.sys.env.default.title">Default application title</option>
								<option value="km.sys.recorddetails.collections.display">Display collections on record details</option>
								<option value="km.sys.newtask.emailnotification">Send email if task assigned</option>
								<option value="km.sys.login.url">Login action URL</option>
								<option value="km.btns.section.class">Buttons section CSS class</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="key-row">
						<ko:propertyLabel value="Key" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="key" id="manualKey" value="${setting.key}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="value-row">
						<ko:propertyLabel value="Value"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="value" id="value" value="${setting.value}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="hidden-row default-layout-row">
						<ko:propertyLabel value="Default layout"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="default-layout-id" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Context"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="activeContext" id="activeContext">
								<option value="environment">Environment</option>
								<option value="profile">Profile</option>
								<option value="locale">Locale</option>
								<option value="user group">User Group</option>
								<option value="user">User</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="context-row" id="profile-context">
						<ko:propertyLabel value="Profile"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="profileLookup" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="context-row" id="user-context">
						<ko:propertyLabel value="User"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="userLookup" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="context-row" id="user-group-context">
						<ko:propertyLabel value="User Group"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="userGroupLookup" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="context-row" id="locale-context">
						<ko:propertyLabel value="Language" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="locale">
								<c:forEach items="${locales}" var="locale">
									<option value="${locale.name()}"<c:if test="${user.localeSetting.id == locale.id}"> selected</c:if>>${locale.language}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
				
				<ko:buttonPanel>
					<input type="submit" value="Save" class="sbtn" />
					<c:if test="${not empty setting.id}">
						<a href="${pageContext.request.contextPath}/km/usersettings/${setting.id}" class="sbtn">Cancel</a>
					</c:if>
					<c:if test="${empty setting.id}">
						<a href="${pageContext.request.contextPath}/km/usersettings/list" class="sbtn">Cancel</a>
					</c:if>
				</ko:buttonPanel>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
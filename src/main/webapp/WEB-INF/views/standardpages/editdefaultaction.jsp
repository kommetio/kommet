<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit standard action" importRMJS="true">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ui.js"></script>
	
		<script type="text/javascript">
		
			$(document).ready(function() {
	
				$("#controllerOption").change(function() {
					changeControllerOption($("#controllerOption").val());
				});
				
				$("#viewOption").change(function() {
					changeViewOption($("#viewOption").val());
				});
				
				$('input[name="usedAction"]:radio').change(function() {
					selectUsedAction($(this).filter(':checked').val());
				});
				
				km.js.ui.autoFormatName({
					target: $("input[name='actionName']")
				});
				
				km.js.ui.autoFormatName({
					target: $("input[name='newViewName']")
				});
				
				selectUsedAction("${usedAction}");
				changeViewOption($("#viewOption").val());
				changeControllerOption($("#controllerOption").val());
				
				if ("${controllerOption}" != null)
				{
					changeControllerOption("${controllerOption}");
				}
				
				$("#restoreDefaultBtn").click(function() {
					
					$("input[name='usedAction']").val("restoredefault");
					$("#actionForm").submit();
					
				});
				
				initActionLookup();
				initControllerLookup();
				initViewLookup();
				
				km.js.utils.bind($("#actionName"), $("#actionURL"), "", function(actionName) {
					
					if (!actionName)
					{
						return "";
					}
					
					// remove package name from action
					if (actionName.indexOf(".") >= 0)
					{
						actionName = actionName.substring(actionName.lastIndexOf(".") + 1, actionName.length);
					}
					
					return actionName.replace(/\s/g, "_").toLowerCase();
				});
				
				km.js.utils.bind($("#actionName"), $("#newViewName"), "", function(actionName) {
					
					if (!actionName)
					{
						return "";
					}
					
					var bareActionName = actionName.indexOf(".") >= 0 ? actionName.substring(actionName.lastIndexOf(".") + 1, actionName.length) : actionName;
					return km.js.utils.capitalize(bareActionName.replace(/\s/g, "_")) + "View";
				});
				
				// set focus on action name
				$("#actionName").focus();
			});
			
			function selectUsedAction(value)
			{
				if (value == "existing")
				{
					$(".new-page-section").hide();
					$("#existing-page-section").show();
				}
				else
				{
					$(".new-page-section").show();
					$("#existing-page-section").hide();
				}
			}
			
			function changeControllerOption (option)
			{
				$(".ctrl-opt").hide();
				$(".ctrl-opt-" + option).show();
			}
			
			function changeViewOption (option)
			{
				$(".view-opt").hide();
				$(".view-opt-" + option).show();
			}
			
			function initActionLookup()
			{
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.Action",
					properties: [
						{ name: "id" },
						{ name: "name" }
					],
					restrictions: [ { property_name: "isSystem", operator: "eq", args: [ "false" ]}]
				};
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: "Actions",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var actionLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "pageId",
					selectedRecordId: "${page.id}"
				});
				
				actionLookup.render($("#existingActionLookup"));
			}
			
			function initControllerLookup()
			{
				$.get("${pageContext.request.contextPath}/km/classes/controllers", function(data) {
					
					if (data.success)
					{
						createControllerLookup(data.data);
					}
					else
					{
						throw data.message;
					}
					
				}, "json");
			}
			
			function initViewLookup()
			{
				$.get("${pageContext.request.contextPath}/km/views/all", function(data) {
					
					if (data.success)
					{
						createViewLookup(data.data);
					}
					else
					{
						throw data.message;
					}
					
				}, "json");
			}
			
			function createControllerLookup(jsrc)
			{
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.Class",
					properties: [
						{ name: "id" },
						{ name: "name" },
						{ name: "packageName" }
					]
				};
				
				var ds = km.js.datasource.create({
					type: "collection",
					data: jsrc.records,
					jsti: jsrc.jsti
				});
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true },
							{ name: "packageName", label: "Package", linkStyle: true, format: function(val) {
								if (val)
								{
									if (val.indexOf(km.js.config.envPackagePrefix) === 0)
									{
										val.substring(km.js.config.envPackagePrefix.length);
									}
								}
								else
								{
									return "";
								}
							}}
						],
						idProperty: { name: "id" }
					},
					title: "Controllers",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var ctrlLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					datasource: ds,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "controllerId",
					selectedRecordId: "${page.controller.id}"
				});
				
				ctrlLookup.render($("#controller-id"));
			}
			
			function createViewLookup(jsrc)
			{
				var jcr = {
					baseTypeName: "kommet.basic.View",
					properties: [
						{ name: "id" },
						{ name: "name" },
						{ name: "packageName" }
					]
				};
				
				var ds = km.js.datasource.create({
					type: "collection",
					data: jsrc.records,
					jsti: jsrc.jsti
				});
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true },
							{ name: "packageName", label: "Package", linkStyle: true, format: function(val) {
								if (val)
								{
									if (val.indexOf(km.js.config.envPackagePrefix) === 0)
									{
										val.substring(km.js.config.envPackagePrefix.length);
									}
								}
								else
								{
									return "";
								}
							}}
						],
						idProperty: { name: "id" }
					},
					title: "Views",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var viewLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					datasource: ds,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "viewId",
					selectedRecordId: "${page.view.id}"
				});
				
				viewLookup.render($("#view-id"));
			}
		
		</script>
		
		<style>
		
			table#current-action {
				margin-bottom: 5em;
			}
			
			table.kdetails > tbody > tr > td.label {
				width: 15em;
			}
			
			tr.border-top-none > td {
				border-top: none;
			}
			
			tr.border-bottom-none > td {
				border-bottom: none;
			}
			
			td#restore-default-cell {
				padding-bottom: 1rem;
			}
			
			"km-select-view-cell .km-lookup {
				width: 25em;
			}
		
		</style>
	
		<div class="ibox">
		
			<ko:pageHeader>Standard action for object ${type.label}</ko:pageHeader>
			
			<ko:propertyTable id="current-action">
				<ko:propertyRow>
					<ko:propertyLabel value="Data type"></ko:propertyLabel>
					<ko:propertyValue>${type.label} (${type.qualifiedName})</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Action type"></ko:propertyLabel>
					<ko:propertyValue>${actionType}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Profile"></ko:propertyLabel>
					<ko:propertyValue>${profileName}</ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
			
			<form method="post" id="actionForm" action="${pageContext.request.contextPath}/km/standardactions/savedefaultaction">
				<input type="hidden" name="typeId" value="${type.KID}" />
				<input type="hidden" name="profileId" value="${profileId}" />
				<input type="hidden" name="actionType" value="${actionType}" />
				
				<kolmu:errors messages="${errorMsgs}" />
				<kolmu:messages messages="${actionMsgs}" />
				
				<ko:propertyTable>
					<ko:propertySection title="Used action">
						<ko:propertyRow>
							<ko:propertyLabel cssStyle="width:150px"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="radio" name="usedAction" value="existing"<c:if test="${usedAction != 'new'}"> checked</c:if>>Use an existing action<br>
								<input type="radio" name="usedAction" value="new"<c:if test="${usedAction == 'new'}"> checked</c:if>>Create a new action
							</ko:propertyValue>
						</ko:propertyRow>
					</ko:propertySection>
					<ko:propertySection title="Existing action to assign" id="existing-page-section">
						<ko:propertyRow cssClass="border-bottom-none">
							<ko:propertyLabel value="Action"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" id="existingActionLookup" />
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow cssClass="border-top-none">
							<ko:propertyLabel></ko:propertyLabel>
							<ko:propertyValue id="restore-default-cell">
								<a href="javascript:void();" class="sbtn" id="restoreDefaultBtn">Restore default</a>
							</ko:propertyValue>
						</ko:propertyRow>
					</ko:propertySection>
					<ko:propertySection title="New action" cssClass="new-page-section">
						<ko:propertyRow>
							<ko:propertyLabel value="Action name" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" id="actionName" name="actionName" value="${actionName}" />
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow>
							<ko:propertyLabel value="URL" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" id="actionURL" name="url" value="${url}" />
							</ko:propertyValue>
						</ko:propertyRow>
					</ko:propertySection>
					<ko:propertySection title="Controller" cssClass="new-page-section">
						<ko:propertyRow>
							<ko:propertyLabel value="Controller"></ko:propertyLabel>
							<ko:propertyValue>
								<select name="controllerOption" id="controllerOption">
									<option value="default" selected>Standard controller for ${type.label}</option>
									<option value="new"<c:if test="${controllerOption == 'new'}"> selected</c:if>>Generate new</option>
									<option value="existing"<c:if test="${controllerOption == 'existing'}"> selected</c:if>>Use existing</option>
								</select>
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow cssClass="ctrl-opt ctrl-opt-new" cssStyle="display:none">
							<ko:propertyLabel value="Controller name"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="newControllerName" value="${newControllerName}" />
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow cssClass="ctrl-opt ctrl-opt-existing" cssStyle="display:none">
							<ko:propertyLabel value="Controller"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" id="controller-id"></input>
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow cssClass="ctrl-opt  ctrl-opt-new ctrl-opt-existing" cssStyle="display:none">
							<ko:propertyLabel value="Controller method"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="controllerMethod" value="${controllerMethod}" />
							</ko:propertyValue>
						</ko:propertyRow>
					</ko:propertySection>
					<ko:propertySection title="View" cssClass="new-page-section">
						<ko:propertyRow>
							<ko:propertyLabel value="View"></ko:propertyLabel>
							<ko:propertyValue>
								<select name="viewOption" id="viewOption">
									<option value="default" selected>Use default</option>
									<option value="new"<c:if test="${viewOption != 'existing'}"> selected</c:if>>Generate new</option>
									<option value="existing"<c:if test="${viewOption == 'existing'}"> selected</c:if>>Use existing</option>
								</select>
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow cssClass="view-opt view-opt-new">
							<ko:propertyLabel value="New view name" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" id="newViewName" name="newViewName" value="${newViewName}" />
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow cssClass="view-opt view-opt-existing" cssStyle="display:none">
							<ko:propertyLabel value="Select View"></ko:propertyLabel>
							<ko:propertyValue id="km-select-view-cell">
								<input type="text" id="view-id"></input>
							</ko:propertyValue>
						</ko:propertyRow>
					</ko:propertySection>
					
					<ko:propertySection title="Apply action to">
						<ko:propertyRow>
							<ko:propertyLabel value="Apply to profiles"></ko:propertyLabel>
							<ko:propertyValue>
								<select name="profileScope">
									<option value="singleProfile">${profileName}</option>
									<option value="allProfiles">all profiles</option>
								</select>
							</ko:propertyValue>
						</ko:propertyRow>
					</ko:propertySection>
					
				</ko:propertyTable>
				
				<ko:buttonPanel>
					<input type="submit" value="Apply" id="savePageBtn" />
					<%-- on cancel go back to type details --%>
					<input type="button" class="sbtn" onclick="openUrl('${pageContext.request.contextPath}/km/type/${type.keyPrefix}#rm.tab.2')" value="Cancel" />
				</ko:buttonPanel>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${obj.label}" importRMJS="true">
	<jsp:body>
	
		<script>

			$(document).ready(function() {
				initTabs();
				
				km.js.ui.bool($("table#field-list > tbody > tr > td.field-required"));
				km.js.ui.bool($("table#type-properties > tbody > tr > td.bool-icon"));
				km.js.utils.openMenuItem("Data Types");
				
				if ("${obj.isDeclaredInCode()}" === "true")
				{
					km.js.ui.info("This type has been declared in code and is not editable through UI", "type-info");
				}
				
				initDeleteFieldBtn();
			});
			
			function initViewLookups()
			{
				$.get("${pageContext.request.contextPath}/km/views/all?customOnly=true", function(data) {
					
					if (data.success)
					{
						createViewLookup(data.data, $("#defaultListView"), "${defaultListViewId}", "km.sys.default.type.view.list");
						createViewLookup(data.data, $("#defaultCreateView"), "${defaultCreateViewId}", "km.sys.default.type.view.create");
						createViewLookup(data.data, $("#defaultEditView"), "${defaultEditViewId}", "km.sys.default.type.view.edit");
						createViewLookup(data.data, $("#defaultDetailsView"), "${defaultDetailsViewId}", "km.sys.default.type.view.details");
					}
					else
					{
						throw data.message;
					}
					
				}, "json");
			}
			
			function createViewLookup(jsrc, target, selectedRecordId, settingKey)
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
					inputName: settingKey,
					visibleInput: {
						cssClass: ""
					},
					selectedRecordId: selectedRecordId,
					afterSelect: (function(settingKey) {
						return function(recordId) {
							saveDefaultView(settingKey);
						}
					})(settingKey),
					afterClear: (function(settingKey) {
						return function(recordId) {
							saveDefaultView(settingKey);
						}
					})(settingKey)
				});
				
				viewLookup.render(target);
			}
			
			function initDeleteFieldBtn()
			{
				$("a.delete-field").each(function() {
					
					var fieldId = $(this).attr("id").substring("delete-field-".length);
					
					km.js.ui.confirm({
						target: $(this),
						question: "Are you sure?",
						callback: function() {
							$.post("${pageContext.request.contextPath}/km/field/delete", { id: fieldId, typePrefix: "${obj.keyPrefix}" }, function(data) {
								
								if (data.success)
								{
									km.js.ui.statusbar.show("Field has been deleted", 5000);
								}
								else
								{
									km.js.ui.statusbar.err(data.messages, 10000);
								}
								
							}, "json");
						}
					});
					
				});
			}
			
			function initTabs()
			{
				// create tabs
				var tabs = km.js.tabs.create({
					tabs: [
						{ content: $("#type-details"), label: "Overview" },
						{ content: $("#type-fields"), label: "Fields" },
						{ content: $("#type-std-actions"), label: "Actions and Views" },
						{ content: $("#type-buttons"), label: "Buttons" },
						{ content: $("#type-related-actions"), label: "Related actions" },
						{ content: $("#type-triggers"), label: "Triggers" },
						{ content: $("#type-vrs"), label: "Validation rules" },
						{ content: $("#type-ucs"), label: "Unique checks" },
						{ content: $("#type-srs"), label: "Sharing rules" }
					],
					originalContentHandling: "remove",
					afterRender: function() {
						loadStdActions();
						loadRelatedPages();
						loadTriggers();
						loadVRs();
						loadUniqueChecks();
						loadSharingRules();
						loadButtons();
					}
				});
				
				tabs.render(function(code) {
					$("#tab-container").html(code);
				});
				
				// open the active tab
				tabs.openActiveTab();
			}

			function loadTriggers()
			{
				$.get("${pageContext.request.contextPath}/km/triggers/${obj.keyPrefix}", function(data) {
					$("#triggers").html(data);
				});
			}
			
			function loadButtons()
			{
				var ds = km.js.datasource.create({
					type: "database"
				});
				
				var jcr = {
					baseTypeName: "kommet.basic.Button",
					properties: [
						{ name: "id" },
						{ name: "url" },
						{ name: "name" },
						{ name: "label" },
						{ name: "labelKey" }
					],
					restrictions: [
						{ property_name: "typeId", operator: "eq", args: [ "${obj.KID}" ]}
					]
				};
					
				var displayOptions = {
					properties: [
						{ name: "label", label: "Label", linkStyle: true, url: km.js.config.contextPath + "/km/buttons/{id}" },
						{ name: "labelKey", label: "Label Key", linkStyle: true }
					],
					idProperty: { name: "id" }
				};
				
				var tableOptions = {
					id: "type-buttons"
				}
				
				var buttonTable = km.js.table.create(ds, jcr, displayOptions, tableOptions);
				
				buttonTable.render(jcr, function(table) {
					$(".km-type-button-list").empty().append(table);
				});
			
			}
			
			function loadSharingRules()
			{
				$.get("${pageContext.request.contextPath}/km/typesharingrules/${obj.KID}", function(data) {
					$("#sharingrules").html(data);
				});
			}

			function loadStdActions()
			{
				$.get("${pageContext.request.contextPath}/km/standardactions/${obj.keyPrefix}", function(data) {
					$("#stdactions").html(data);
					initViewLookups();
					//initViews();
				});
			}
			
			function loadRelatedPages()
			{
				$.get("${pageContext.request.contextPath}/km/types/relatedpages/${obj.keyPrefix}", function(data) {
					$("#relatedpages").html(data);
				});
			}

			function loadVRs()
			{
				$.post("${pageContext.request.contextPath}/km/typevalidationrules?typeId=${obj.KID}", function(data) {
					$("#vrs").html(data);
					km.js.ui.bool($("#vrs table#vrList > tbody > tr > td.active-vr"));
				});
			}
			
			function loadUniqueChecks()
			{
				$.get("${pageContext.request.contextPath}/km/typeuniquechecks?typeId=${obj.KID}", function(data) {
					$("#ucs").html(data);
				});
			}
			
			function deleteType(forceCleanup)
			{
				km.js.ui.statusbar.show("Deleting type...");
				
				$.post("${pageContext.request.contextPath}/km/type/delete", { typeId: "${obj.KID}", forceCleanup: forceCleanup }, function(data) {
					if (data.status == "success")
					{
						openUrl("${pageContext.request.contextPath}/km/types/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}
		
		</script>
		
		<style>
		
			"km-type-buttons {
				margin-top: 1em;
			}
		
		</style>
		
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<div id="tab-container"></div>
	
		<div id="type-details">
			
			<div class="km-title">${obj.label}</div>
				<%--<ko:pageSubheader>Administration panel for object ${obj.label}</ko:pageSubheader>--%>
		
			<ko:buttonPanel>
				<c:if test="${canEdit == true}">
					<a href="${pageContext.request.contextPath}/km/types/edit/${obj.keyPrefix}" class="sbtn" id="editTypeBtn">Edit</a>
				</c:if>
				<a href="${pageContext.request.contextPath}/${obj.keyPrefix}" class="sbtn">Show records</a>
				<%--<a href="${pageContext.request.contextPath}/km/types/views/${obj.keyPrefix}" class="sbtn">Views</a>--%>
				<c:if test="${canEditPermissions == true}">
					<a href="${pageContext.request.contextPath}/km/typepermissions/${obj.keyPrefix}" class="sbtn">Permissions</a>
				</c:if>
				<c:if test="${canDelete == true}">
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this object?', 'warnPrompt', function() { deleteType(false); })" id="deleteTypeBtn" class="sbtn">Delete</a>
				</c:if>
				<c:if test="${canForceDelete == true}">
					<a href="javascript:;" onclick="ask('Are you sure you want to force delete this object?', 'warnPrompt', function() { deleteType(true); })" id="deleteTypeBtn" class="sbtn">Force Delete</a>
				</c:if>
			</ko:buttonPanel>
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<form method="post" action="${pageContext.request.contextPath}/km/type/save">
				<input type="hidden" name="kObjectId" value="${obj.id}" />
				<table id="type-properties" class="kdetails" style="margin: 30px 0 30px 0">
					<tbody>
						<tr>
							<td class="label">API name</td>
							<td class="value">${obj.apiName}</td>
							<td class="sep"></td>
							<td class="label">Package</td>
							<td class="value">${typePackage}</td>
						</tr>
						<tr>
							<td class="label">Label</td>
							<td class="value">${obj.label}</td>
							<td class="sep"></td>
							<td class="label">Plural label</td>
							<td class="value">${obj.pluralLabel}</td>
						</tr>
						<tr>
							<td class="label">Label Key</td>
							<td class="value">${obj.uchLabel}</td>
							<td class="sep"></td>
							<td class="label">Plural label key</td>
							<td class="value">${obj.uchPluralLabel}</td>
						</tr>
						<tr>
							<td class="label">Default field</td>
							<td class="value">${defaultField}</td>
							<td class="sep"></td>
							<td class="label">Sharing controlled by field</td>
							<td class="value">${sharingControlledByField}</td>
						</tr>
						<tr>
							<td class="label">Combine record and cascade sharings</td>
							<td class="value bool-icon">${obj.combineRecordAndCascadeSharing}</td>
							<td class="sep"></td>
							<td class="label">Description</td>
							<td class="value">${obj.description}</td>
						</tr>
					</tbody>
				</table>
			</form>
		
		</div>
		
		<div id="type-std-actions">
			<div class="section-title">Actions and views</div>
			<div id="stdactions"></div>
		</div>
		
		<div id="type-related-actions">
			<div class="section-title">Related actions</div>
			<div id="relatedpages"></div>
		</div>
		
		<div id="type-triggers">
			<div class="section-title">Triggers</div>
			
			<%--<div style="margin-bottom: 20px">
				<a href="${pageContext.request.contextPath}/km/triggers/new/${obj.keyPrefix}" class="sbtn" id="addTriggerBtn">Add trigger</a>
			</div>--%>
			
			<div id="triggers"></div>
		</div>
		
		<div id="type-vrs">
			<div class="section-title">Validation rules</div>
			
			<div style="margin-bottom: 20px">
				<a href="${pageContext.request.contextPath}/km/validationrules/new?typeId=${obj.KID}" class="sbtn" id="addVRBtn">Add validation rule</a>
			</div>
			
			<div id="vrs"></div>
		</div>
		
		<div id="type-ucs">
			<div class="section-title">Unique checks</div>
			
			<div style="margin-bottom: 20px">
				<a href="${pageContext.request.contextPath}/km/uniquechecks/new?typeId=${obj.KID}" class="sbtn" id="addVRBtn">Add unique check</a>
			</div>
			
			<div id="ucs"></div>
		</div>
		
		<div id="type-srs">
			<div class="section-title">Sharing rules</div>
			
			<div id="sharingrules"></div>
		</div>
		
		<div id="type-buttons">
			<div class="section-title">Buttons</div>
			
			<div>
				<a href="${pageContext.request.contextPath}/km/buttons/new?typeId=${obj.KID}" class="sbtn">Create button</a>
			</div>
			
			<div id="km-type-buttons">
				<div id="km-type-button-list">
			
				</div>
			</div>
		</div>
		
		<div id="type-fields">
			<div class="section-title">Custom fields</div>
			<c:if test="${canAddFields == true}">
				<a href="${pageContext.request.contextPath}/km/field/new?typeId=${obj.KID}" class="sbtn">New field</a>
			</c:if>
			
			<div id="type-info"></div>
			
			<table class="std-table" id="field-list" style="margin-top: 15px">
				<thead>
					<tr class="cols">
						<th>Label</th>
						<th>API Name</th>
						<th>Data type</th>
						<th>Required</th>
						<th>Action</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="field" items="${customFields}">
						<tr>
							<td><a href="${pageContext.request.contextPath}/km/field/${field.KID}">${field.label}</a></td>
							<td>${field.apiName}</td>
							<td>${field.dataType.name}
								<c:if test="${field.dataType.id == 0}">
								(${field.dataType.decimalPlaces} decimal places)
								</c:if>
								<c:if test="${field.dataType.id == 6}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.type.keyPrefix}">${field.dataType.type.label}</a>
								</c:if>
								<c:if test="${field.dataType.id == 8}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.inverseType.keyPrefix}">${field.dataType.inverseType.label}</a>
								</c:if>
								<c:if test="${field.dataType.id == 10}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.associatedType.keyPrefix}">${field.dataType.associatedType.label}</a>
								<c:if test="${field.dataType.linkingType.isAutoLinkingType() == false}">
								through <a href="${pageContext.request.contextPath}/km/type/${field.dataType.linkingType.keyPrefix}">${field.dataType.linkingType.label}</a>
								</c:if>
								</c:if>
							</td>
							<td class="field-required">${field.required}</td>
							<td><a href="javascript:;" id="delete-field-${field.KID}" class="delete-field"><kolmu:label key="btn.delete"></kolmu:label></a></td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
			
			<div class="section-title" style="margin: 2em 0 0.5em 0">System fields</div>
			
			<table class="std-table" id="field-list" style="margin-top: 15px">
				<thead>
					<tr class="cols">
						<th>Label</th>
						<th>API Name</th>
						<th>Data type</th>
						<th>Required</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="field" items="${systemFields}">
						<tr>
							<td><a href="${pageContext.request.contextPath}/km/field/${field.KID}">${field.label}</a></td>
							<td>${field.apiName}</td>
							<td>${field.dataType.name}
								<c:if test="${field.dataType.id == 0}">
								(${field.dataType.decimalPlaces} decimal places)
								</c:if>
								<c:if test="${field.dataType.id == 6}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.type.keyPrefix}">${field.dataType.type.label}</a>
								</c:if>
								<c:if test="${field.dataType.id == 8}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.inverseType.keyPrefix}">${field.dataType.inverseType.label}</a>
								</c:if>
								<c:if test="${field.dataType.id == 10}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.associatedType.keyPrefix}">${field.dataType.associatedType.label}</a>
								through <a href="${pageContext.request.contextPath}/km/type/${field.dataType.linkingType.keyPrefix}">${field.dataType.linkingType.label}</a>
								</c:if>
							</td>
							<td class="field-required">${field.required}</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${lib.name} - Library" importRMJS="true">
	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.devitems.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.utils.js"></script>
	
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<style>
		
			div#libitem-add-form {
				margin: 2em 0 2em 0;
			}
			
			input#save-url-btn {
				margin-left: 1em
			}
			
			div#lib-items {
				display: table;
				width: 100%;
				margin-top: 1rem;
			}
			
			div#lib-items > div {
				display: table-row
			}
			
			div#lib-items > div > div {
				display: table-cell;
				width: 100%;
			}
			
			div#lib-items > div.labels > div {
				font-size: 1.5rem
			}
			
			div#lib-items > div.labels > div {
				padding-bottom: 1em;
			}
			
			li.km-item-hidden {
				display: none !important;
			}
			
			div.lib-items-read-only ul.km-devitems-tree li.km-devitems-selected {
				background-color: inherit;
			}
    		
    		div.km-items-box {
    			margin-top: 1rem;
    		}
		
		</style>
		
		<script>
		
			var selectedItemIds = ${selectedItemIds};
			
			var availableLibItems = {
				items: null,
				jsti: null,
				types: null
			};
			
			function getSelectedItemIds()
			{
				return selectedItemIds;
			}
			
			function renderItems (items, jsti, types, selectedItemIds, isReadOnly, showUnselectedItems)
			{	
				var onClick = isReadOnly ? null : (function(items, jsti, showUnselectedItems) {
					return function(item) {
						var selectedItemIds = toggleItem(item.id);
					}
				})(items, jsti, showUnselectedItems);
				
				var filteredItems = null;
				var filtetedTypes = null;
				
				if (isReadOnly)
				{
					$("#lib-items").addClass("lib-items-read-only");
					
					filteredItems = {};
					filtetedTypes = [];
					
					// remove unselected items from item collection
					for (var typeId in items)
					{
						var typeItems = items[typeId];
						var newTypeItems = [];
						for (var i = 0; i < typeItems.length; i++)
						{
							var item = typeItems[i];
							if (selectedItemIds[item["id"]])
							{
								newTypeItems.push(item);
							}
						}
						
						filteredItems[typeId] = newTypeItems;
					}
					
					for (var i = 0; i < types.length; i++)
					{
						var type = types[i];
						if (selectedItemIds[type["id"]])
						{
							filtetedTypes.push(type);
						}
					}
				}
				else
				{
					filteredItems = items;
					filtetedTypes = types;
					$("#lib-items").removeClass("lib-items-read-only");
				}
				
				// render component tree
				km.js.devitems.create({
					items: filteredItems,
					jsti: jsti,
					types: filtetedTypes,
					group: "type",
					checkboxes: !isReadOnly,
					unselectedItemCssClass: showUnselectedItems ? null : "km-item-hidden",
					selectedItemIds: selectedItemIds,
					onClick: onClick
				}).render($("#dev-selected-items"));
			}
			
			function toggleItem(itemId)
			{
				if (selectedItemIds[itemId])
				{
					delete selectedItemIds[itemId];		
				}
				else
				{
					selectedItemIds[itemId] = {};
				}
				
				return selectedItemIds;
			}
		
			$(document).ready(function() {
				
				var itemIds = getSelectedItemIds();
				var itemCount = 0;
				for (var itemId in itemIds)
				{
					itemCount++;
				}
				
				$("#lib-item-count").text(itemCount);
				
				var items = getDevItems(function(items, jsti, types) {
					
					// store all available lib items for future uses
					availableLibItems.items = items;
					availableLibItems.jsti = jsti;
					availableLibItems.types = types;
					
					renderItems(items, jsti, types, getSelectedItemIds(), true, false);
				});	
			});
			
			function getDevItems(callback)
			{
				var container = { 
					itemsByType: {
						// empty
					},
					jsti: {},
					types: {}
				};
				
				var ds = km.js.datasource.create({
					type: "database"
				});
				
				var jcr = {};
				
				var parseResultsCallback = function(cont, typeId, notifier) {				
					return function(records, recordCount, jsti) {
						cont.itemsByType[typeId] = km.js.utils.addPropertyNamesToJSRC(records, jsti);
						cont.jsti = $.extend(true, {}, cont.jsti, jsti);
						notifier.reach("type" + typeId);
					}
				}
				
				var notifier = km.js.notifier.get();
				notifier.wait("type0");
				notifier.wait("type1");
				notifier.wait("type2");
				notifier.wait("type3");
				notifier.wait("type4");
				notifier.wait("type5");
				notifier.wait("type6");
				notifier.wait("type7");
				notifier.wait("type8");
				notifier.wait("type9");
				notifier.wait("type10");
				notifier.wait("type11");
				notifier.wait("type12");
				notifier.wait("type13");
				
				notifier.onComplete = (function(container) {
					return function() {
						callback(container.itemsByType, container.jsti, container.types);
					}
				})(container);
				
				var accessLevelCond = [ { property_name: "accessType", operator: "eq", args: [ 0 ] } ];
				
				// query classes
				jcr = {	baseTypeName: "kommet.basic.Class",	properties: [ { name: "id" }, { name: "name" }, { name: "packageName" }	], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 0, notifier));
				
				// query views
				jcr = {	baseTypeName: "kommet.basic.View",	properties: [ { name: "id" }, { name: "name" }, { name: "packageName" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 1, notifier));
				
				// query layouts
				jcr = {	baseTypeName: "kommet.basic.Layout", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 4, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.ValidationRule", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 5, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.UniqueCheck", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 6, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.App", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 7, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.ScheduledTask", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 8, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.Profile", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 9, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.UserGroup", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 10, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.WebResource", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 11, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.ViewResource", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 12, notifier));
				
				jcr = {	baseTypeName: "kommet.basic.Action", properties: [ { name: "id" }, { name: "name" } ], restrictions: accessLevelCond };
				ds.query(jcr, parseResultsCallback(container, 13, notifier));
				
				$.get(km.js.config.contextPath + "/km/customtypes", (function(notif, cont) {
					
					return function(data) {
						cont.types = data;
						notif.reach("type2");
						notif.reach("type3");
					}
					
				})(notifier, container), "json");
				
				return container;
			}
			
			function deleteLib()
			{
				$.post("${pageContext.request.contextPath}/km/libraries/delete", { id: "${lib.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/libraries/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}
			
			function deactivateLib()
			{
				$.post("${pageContext.request.contextPath}/km/libraries/deactivate", { id: "${lib.id}" }, function(data) {
					if (data.success === true)
					{
						km.js.utils.openURL(km.js.config.contextPath + "/km/libraries/${lib.id}");
					}
					else
					{
						km.js.ui.statusbar.show("Could not uninstall library");
					}
				}, "json");
			}
			
			function activateLib()
			{
				$.post("${pageContext.request.contextPath}/km/libraries/activate", { id: "${lib.id}" }, function(data) {
					if (data.success === true)
					{
						km.js.utils.openURL(km.js.config.contextPath + "/km/libraries/${lib.id}");
					}
					else
					{
						km.js.ui.statusbar.show("Could not activate library");
					}
				}, "json");
			}
			
			function saveItems()
			{
				var itemIdList = [];
				
				// find all checked items
				$("div.km-devitems-checkbox-wrapper > input").each((function(idList) {
					
					return function() {
						if ($(this).is(":checked"))
						{
							id = $(this).attr("id").substring("record_".length);
							itemIdList.push(id);
						}
					}
					
				})(itemIdList));
				
				$.post(km.js.config.contextPath + "/km/libitems/save", { itemIds: itemIdList.join(","), libId: "${lib.id}" }, (function(itemIds) {
					
					return function(data) {
						
						if (data.success)
						{
							km.js.ui.statusbar.show("Items have been saved", 10000);
							$("#lib-item-count").text(itemIds.length);
						}
						else
						{
							km.js.ui.statusbar.err(data.message, 10000);
						}
					}
					
				})(itemIdList), "json");
			}
			
			$(document).ready(function() {
				
				$("#save-url-btn").click(function() {
					$.post("${pageContext.request.contextPath}/km/libraries/additem", { id: $("#lib-item-id").val() }, function(data) {
						
						$("#new-app-url").val("");
						initLibItems();
						
					}, "json");
				})
			});
			
			function libItemsEdit()
			{
				$("#items-read-panel").hide();
				$("#items-edit-panel").show();
				
				// render editable item list
				renderItems(availableLibItems.items, availableLibItems.jsti, availableLibItems.types, selectedItemIds, false, true);
			}
			
			function libItemsRead()
			{
				$("#items-read-panel").show();
				$("#items-edit-panel").hide();
				
				// render editable item list
				renderItems(availableLibItems.items, availableLibItems.jsti, availableLibItems.types, selectedItemIds, true, false);
			}
		
		</script>
		
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="">
				
				<ko:pageHeader>${lib.name}</ko:pageHeader>
				
				<ko:buttonPanel>
					<c:if test="${isEditable == true}">
					<a href="${pageContext.request.contextPath}/km/libraries/edit/${lib.id}" class="sbtn">Edit</a>
					</c:if>
					<a href="${pageContext.request.contextPath}/km/lib/download/${lib.id}" class="sbtn">Export</a>						
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this library?', 'warnPrompt', function() { deleteLib(); })" id="deleteLibBtn" class="sbtn">Delete</a>
					
					<c:if test="${lib.source != 'Local'}">
						<c:if test="${lib.isEnabled == true}">
						<a href="javascript:;" onclick="ask('Are you sure you want to uninstall this library?', 'warnPrompt', function() { deactivateLib(); })" class="sbtn">Uninstall</a>
						</c:if>
						<c:if test="${lib.isEnabled == false}">
						<a href="javascript:;" onclick="ask('Are you sure you want to re-install this library?', 'warnPrompt', function() { activateLib(); })" class="sbtn">Install</a>
						</c:if>
					</c:if>
				</ko:buttonPanel>
				
				<div id="warnPrompt" style="margin-top:10px"></div>
				
				<ko:propertyTable>
					
					<ko:propertyRow>
						<ko:propertyLabel value="name"></ko:propertyLabel>
						<ko:propertyValue>${lib.name}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Provider"></ko:propertyLabel>
						<ko:propertyValue>${lib.provider}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Version"></ko:propertyLabel>
						<ko:propertyValue>${lib.version}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Source"></ko:propertyLabel>
						<ko:propertyValue>${lib.source}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Access Level"></ko:propertyLabel>
						<ko:propertyValue>${lib.accessLevel}</ko:propertyValue>
					</ko:propertyRow>
					<c:if test="${lib.source != 'Local'}">
						<ko:propertyRow>
							<ko:propertyLabel value="Status"></ko:propertyLabel>
							<ko:propertyValue>${lib.status}</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow>
							<ko:propertyLabel value="Is Enabled"></ko:propertyLabel>
							<ko:propertyValue>${lib.isEnabled}</ko:propertyValue>
						</ko:propertyRow>
					</c:if>
					<ko:propertyRow>
						<ko:propertyLabel value="Description"></ko:propertyLabel>
						<ko:propertyValue>${lib.description}</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>

			</form>
			
		</div>
			
		<div class="ibox km-items-box">
		
			<div class="km-title">Library items (<span id="lib-item-count"></span>)</div>
			
			<c:if test="${isEditable == true}">
				<div id="items-read-panel">
					<a href="javascript:;" class="sbtn" onclick="libItemsEdit()">Edit</a>
				</div>
				
				<div id="items-edit-panel" style="display: none">
					<a href="javascript:;" class="sbtn" onclick="saveItems()">Save</a>
					<a href="javascript:;" class="sbtn" onclick="libItemsRead()">Cancel edit</a>
				</div>
			</c:if>
		
			<div id="lib-items">
				<div>
					<div id="dev-selected-items"></div>
				</div>
			</div>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
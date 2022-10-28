<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Database objects">

	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.devitems.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.utils.js"></script>
	
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<style>
		
			div#km-component-tree {
				margin-top: 2rem;
			}
		
			div.km-second-ibox {
				margin-top: 1rem;
			}
		
			table#customTypeList {
				display: none;
			}
		
			table.type-list td.created-date {
				white-space: nowrap;
			}
			
			h3.type-header {
				margin: 2em 0 0 0;
				font-size: 1.2rem;
    			color: #545454;
			}
			
			div#km-obj-buttons > a.km-plain-btn {
				text-decoration: none;
			    color: #545454;
			    border: 1px solid #d6d6d6;
			    padding: 0.5rem;
			    border-radius: 0.2em;
			    padding: 0.6em 0.9em;
			    margin-right: 1em;
			}
		
		</style>
		
		<script>
		
		var availableLibItems = {
			items: null,
			jsti: null,
			types: null
		};
		
		function renderItems (items, jsti, types, selectedItemIds, isReadOnly)
		{	
			var showUnselectedItems = true;
			
			var filteredItems = null;
			var filtetedTypes = null;
			
			if (isReadOnly)
			{
				$(".km-component-tree").addClass("lib-items-read-only");
				
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
						newTypeItems.push(item);
					}
					
					filteredItems[typeId] = newTypeItems;
				}
				
				for (var i = 0; i < types.length; i++)
				{
					var type = types[i];
					filtetedTypes.push(type);
				}
			}
			else
			{
				filteredItems = items;
				filtetedTypes = types;
				$(".km-component-tree").removeClass("lib-items-read-only");
			}
			
			// render component tree
			km.js.devitems.create({
				items: filteredItems,
				jsti: jsti,
				types: filtetedTypes,
				group: "type",
				checkboxes: false,
				unselectedItemCssClass: null,
				selectedItemIds: {},
				onClick: openItem,
				ignoreSelection: true
			}).render($(".km-component-tree"));
		}
		
		function openItem(item)
		{
			console.log(JSON.stringify(item));
			location.href = km.js.config.contextPath + "/" + km.js.utils.getItemLink(item.id);
		}
		
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
		
		$(document).ready(function() {
			km.js.utils.openMenuItem("Objects");
			
			var items = getDevItems(function(items, jsti, types) {
				
				// store all available lib items for future uses
				availableLibItems.items = items;
				availableLibItems.jsti = jsti;
				availableLibItems.types = types;
				
				renderItems(items, jsti, types, {}, true);
			});
			
			$("a#objectListBtn").click(function() {
				$("#customTypeList").show();
				$(".km-component-tree").hide();
			});
			
			$("a#componentTreeBtn").click(function() {
				$("#customTypeList").hide();
				$(".km-component-tree").show();
			});
		});
		
		</script>
	
		<div class="ibox">
		
			<div class="km-title">Custom Objects</div>
	
			<div id="km-obj-buttons">
				<a href="${pageContext.request.contextPath}/km/types/new" class="sbtn" id="newTypeBtn">New object</a>
				<a href="javascript:;" id="objectListBtn" class="km-plain-btn">Object list</a>
				<a href="javascript:;" id="componentTreeBtn" class="km-plain-btn">Component tree</a>
			</div>
			
			<div id="km-component-tree"></div>
			
			<table id="customTypeList" class="std-table type-list" style="margin-top: 30px">
				<thead>
					<tr class="cols">
						<th>Label</th>
						<th>API Name</th>
						<th>Package</th>
						<th>Creation Date</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="type" items="${customTypes}">
						<tr>
							<td><a href="${pageContext.request.contextPath}/km/type/${type.keyPrefix}">${type.label}</a></td>
							<td>${type.apiName}</td>
							<td>${type['package']}</td>
							<td class="created-date"><km:dateTime value="${type.created}" format="dd-MM-yyyy HH:mm:ss" /></td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
			
		</div>
		
		<div class="ibox km-second-ibox">
			
			<div class="km-title">Standard Objects</div>
	
			<table id="standardTypeList" class="std-table type-list">
				<thead>
					<tr class="cols">
						<th>Label</th>
						<th>API Name</th>
						<th>Package</th>
						<th>Creation Date</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="type" items="${basicTypes}">
						<tr>
							<td><a href="${pageContext.request.contextPath}/km/type/${type.keyPrefix}">${type.label}</a></td>
							<td>${type.apiName}</td>
							<td>${type['package']}</td>
							<td>-</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
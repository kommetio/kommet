<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout titleKey="reports.edit.title">
	<jsp:body>
	
		<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/ztree/css/zTreeStyle/zTreeStyle.css" type="text/css">
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/ztree/js/jquery.ztree.core-3.5.js"></script>
		
		<style>
		
			#type-choice li {
				margin: 3px 0 3px 0;
			}
			
			#report-conf {
				width: 100%;
			}
			
			#field-choice-wrapper, #type-choice-wrapper {
				width: 47%;
				height: 500px;
				display: inline-block;
				border: 2px solid #E9E9E9;
				background: #f0f6e4;
				overflow: scroll;
			}
			
			#type-choice-wrapper ul.ztree > li {
				font-family: Arial, Verdana, Helvetica;
				font-size: 12px;
			}
			
			#field-choice-wrapper {
				margin-left: 20px;
			}
			
			table#field-choice {
				width: 100%;
				border-collapse: collapse;
			}
			
			table#field-choice > tbody > tr > td {
				font-size: 12px;
				padding: 5px;
				font-family: Arial, Verdana, Helvetica;
				border-bottom: 1px dotted #dadada;
				border-top: none;
				cursor: pointer;
			}
			
			table#field-choice > tbody > tr.active, table#field-choice > tbody > tr.activeProp {
				background-color: #fff;
			}
			
			table#field-choice > tbody > tr.activeProp { 
				border-top: 2px solid rgb(183, 195, 196);
			}
			
			table#field-choice > tbody > tr.active {
				border-bottom: 2px solid rgb(183, 195, 196);
			}
			
			table#field-choice > tbody > tr.active > td {
				border-top: none;
			}
			
			table#field-choice > tbody > tr.activeProp > td {
				border-bottom: 1px dotted #ddd;
			}
			
			table#field-choice > tbody > tr.activeProp > td:nth-child(1) {
				
			}
			
			table#field-choice > tbody > tr.inactive:hover {
				background-color: #fafafa;
			}
			
			table#field-choice > tbody > tr > td:nth-child(2) {
				width: 60px;
			}
			
			table#field-choice > tbody > tr > td:nth-child(2) > img {
				height: 15px;
				display: inline-block;
				margin-right: 3px;
			}
			
			table#field-choice > tbody > tr > td:nth-child(2) > img.arr {
				height: 13px;
				position: relative;
				top: -1px;
			}
			
			/* simulate table */
			div.prop-table {
				display: table;
				margin-left: 20px;
			}
			
			div.prop-table > div {
				display: table-row;
			}
			
			div.prop-table > div > div {
				display: table-cell;
			}
			
			#field-choice-wrapper .prop-table .label {
				color: rgb(87, 87, 87);
				font-weight: normal;
				text-align: left;
				padding: 5px 10px 5px 0;
				font-size: 11px;
			}
		
		</style>
		
		<script>
		
			function saveReport()
			{
				report.properties = [];
				report.groupings = [];
				report.baseTypeId = null;

				<%-- This method iterates over all HTML elements representing selected properties and creates a JCR object from them --%>
				var listSelectedFields = function() {
					return function() {

						var idPath = $(this).attr("id").substring(3);
						var ids = idPath.split(".");
						var typeId = ids[0];

						console.log("id = " + idPath);

						if (report.baseTypeId == null)
						{
							report.baseTypeId = typeId;
						}
						else if (report.baseTypeId != typeId)
						{
							showMsg("err", "${i18n.get('reports.reporttype.onlyonebasistypeallowed')}", "info", null, null, "${pageContext.request.contextPath}/rm");
							report.baseTypeId = -1;
						}

						var fieldDef = {
							// remove the type id from the beginning of ID path
							id: idPath.substring(idPath.indexOf('.') + 1)
						}

						var fullId = (typeId + "." + fieldDef.id);

						var aggr = $("#aggr-" + fullId.replace(/\./g, "\\.")).val();
						var alias = $("#alias-" + fullId.replace(/\./g, "\\.")).val()
						
						if (aggr !== "grouping")
						{
							if (aggr != "")
							{
								fieldDef.aggr = aggr;
							}
							if (alias != "")
							{
								fieldDef.alias = alias;
							}
							report.properties.push(fieldDef);
						}
						else
						{
							var grouping = {
								property_id: fieldDef.id
							}
							
							if (alias != "")
							{
								grouping.alias = alias;
							}
							report.groupings.push(grouping);
						}
					}
				}
				
				// get selected fields
				$("#field-choice > tbody > tr.sf").each(listSelectedFields());

				if (report.baseTypeId == -1)
				{
					console.log("Exiting with BT error");
					return;
				}
				
				$.post("${pageContext.request.contextPath}/km/reporttypes/save", { name: $("#reportName").val(), desc: $("#reportDesc").val(), reportTypeId: reportTypeId, serializedCriteria: JSON.stringify(report) }, function(data) {
					$("#err").html("");
					if (data.status == "success")
					{
						reportTypeId = data.data.reportTypeId;
						showMsg("err", "${i18n.get('reports.reporttype.saved')}", "info", null, null, "${pageContext.request.contextPath}/rm");

						<%-- show report run button --%>
						var runBtn = "<a class=\"sbtn\" href=\"${pageContext.request.contextPath}/km/reporttypes/run/" + reportTypeId + "\">${i18n.get('reports.run')}</a>";
						$("#report-btns > #run-btn-container").html(runBtn);
					}
					else
					{
						showMsg("err", data.messages, "error", null, null, "${pageContext.request.contextPath}/rm");
					}
				}, "json");
			}

			var report = {
				baseTypeId: null
			}

			var reportTypeId = "${reportTypeId}";

			function initReportType (jcr, typeNodes)
			{
				report.baseTypeId = jcr.baseTypeId;

				getPropertyPath = function(propIds, fieldNodes)
				{
					var currNode = null;
					
					// find subfields of all fields
					for (var i = 0; i < propIds.length; i++)
					{
						var propId = propIds[i];

						// search all field nodes for subfields of the current field
						for (var k = 0; k < fieldNodes.length; k++)
						{
							if (propId == fieldNodes[k].id)
							{
								currNode = fieldNodes[k];
								break;
							}
						}

						fieldNodes = currNode.children;
					}

					return currNode.path;
				}

				var fields = null;
				for (var i = 0; i < typeNodes.length; i++)
				{
					if (typeNodes[i].id == report.baseTypeId)
					{
						fields = typeNodes[i].children;
						break;
					}
				}

				if (fields == null)
				{
					console.log("error = fields not found");
				}

				// init selected properties
				for (var i = 0; i < jcr.properties.length; i++)
				{
					var prop = jcr.properties[i];

					// read property path from type nodes
					var path = getPropertyPath(prop.id.split("."), fields);
					
					addSelectedFieldToPanel((report.baseTypeId + "." + prop.id).split("."), path, prop.aggr, prop.alias);
				}

				// init groupings
				for (var i = 0; i < jcr.groupings.length; i++)
				{
					var group = jcr.groupings[i];

					// read property path from type nodes
					var path = getPropertyPath(group.property_id.split("."), fields);
					
					addSelectedFieldToPanel((report.baseTypeId + "." + group.property_id).split("."), path, "grouping", group.alias);
				}
			}

			function toggleFieldProps (fieldId)
			{
				var escapedFieldId = fieldId.replace(/\./g, "\\.");
				var isVisible = $("table#field-choice > tbody > tr#sfp-" + escapedFieldId).hasClass("active"); 
				
				$("table#field-choice > tbody > tr.sfp").hide();
				$("table#field-choice > tbody > tr.sfp").removeClass("active");
				$("table#field-choice > tbody > tr.sfp").addClass("inactive");
				$("table#field-choice > tbody > tr.sf").removeClass("activeProp");
				$("table#field-choice > tbody > tr.sf").addClass("inactive");

				if (!isVisible)
				{
					$("table#field-choice > tbody > tr#sfp-" + escapedFieldId).show().addClass("active").removeClass("inactive");
					$("table#field-choice > tbody > tr#sf-" + escapedFieldId).addClass("activeProp").removeClass("inactive");
				}
			}

			function removeField(fieldId)
			{
				fieldId = fieldId.replace(/\./g, "\\.");
				$("#field-choice > tbody > tr#sf-" + fieldId).remove();
				$("#field-choice > tbody > tr#sfp-" + fieldId).remove();
			}

			function moveFieldUp (id)
			{
				fieldId = id.replace(/\./g, "\\.");

				var nameRow = $("#field-choice > tbody > tr#sf-" + fieldId);
				var propRow = $("#field-choice > tbody > tr#sfp-" + fieldId);

				var prevRow = nameRow.prevAll(".sf:first");

				if (typeof(prevRow.attr("id")) != "undefined")
				{ 
					prevRow.before(nameRow);
					prevRow.before(propRow);
	
					//nameRow.remove();
					//propRow.remove();
				}
			}

			function moveFieldDown (id)
			{
				fieldId = id.replace(/\./g, "\\.");

				var nameRow = $("#field-choice > tbody > tr#sf-" + fieldId);
				var propRow = $("#field-choice > tbody > tr#sfp-" + fieldId);

				var nextRow = propRow.nextAll(".sfp:first");
				
				if (typeof(nextRow.attr("id")) != "undefined")
				{
					// place row under the next row - it is important to call this
					// first on propRow, then on nameRow
					nextRow.after(propRow);
					nextRow.after(nameRow);
	
					//nameRow.remove();
					//propRow.remove();
				}
			}

			function addSelectedFieldToPanel (idPath, path, selectedAggr, alias)
			{
				getSelectedFieldItem = function(idPath, path, alias) 
				{
					var itemId = idPath.join(".");

					var code = "<tr class=\"sf inactive\" id=\"sf-" + itemId + "\"><td onclick=\"toggleFieldProps('" + itemId + "')\">" + path + "</td>";

					code += "<td>";
					code += "<img onclick=\"removeField('" + itemId + "')\" src=\"${pageContext.request.contextPath}/resources/images/ex.png\">";
					code += "<img onclick=\"moveFieldUp('" + itemId + "')\" class=\"arr\" src=\"${pageContext.request.contextPath}/resources/images/uparrowg.png\">";
					code += "<img onclick=\"moveFieldDown('" + itemId + "')\" class=\"arr\" src=\"${pageContext.request.contextPath}/resources/images/downarrowg.png\">";
					code += "</td>";
					code += "</tr>";

					// add row for field properties
					code += "<tr class=\"sfp\" id=\"sfp-" + itemId + "\" style=\"display:none\"><td colspan=\"2\">";

					code += "<div class=\"prop-table\">";
					code += "<div>";
					code += "<div class=\"label\">${i18n.get('reports.field.alias')}:</div>";
					code += "<div><input class=\"std-input\" type=\"text\" id=\"alias-" + itemId + "\" value=\"" + (alias ? alias : "") + "\" placeholder=\"" + path + "\"></input></div>";
					code += "</div><div>";

					code += "<div class=\"label\">${i18n.get('reports.aggr.label')}:</div>";
					code += "<div>";
					code += "<select class=\"std-input\" id=\"aggr-" + itemId + "\">"
					code += "<option value=\"\">${i18n.get('reports.aggr.plainvalue.name')}</option>";
					code += "<option value=\"grouping\">${i18n.get('reports.aggr.grouping.name')}</option>";
					code += "<option value=\"sum\">${i18n.get('reports.aggr.sum.name')}</option>";
					code += "<option value=\"count\">${i18n.get('reports.aggr.count.name')}</option>";
					code += "<option value=\"avg\">${i18n.get('reports.aggr.avg.name')}</option>";
					code += "<option value=\"max\">${i18n.get('reports.aggr.max.name')}</option>";
					code += "<option value=\"min\">${i18n.get('reports.aggr.min.name')}</option>";
					code += "</select>";
					code += "</div>";
					
					code += "</div></div>";
					code += "</td></tr>";
					
					return code;	
				}
				
				$("#field-choice > tbody").append(getSelectedFieldItem(idPath, path, alias));
				
				if (selectedAggr)
				{
					// preselect aggregation
					$("#aggr-" + idPath.join("\\.")).val(selectedAggr);
				}
			}

			$(document).ready(function() {

				$.get("${pageContext.request.contextPath}/km/reporttypes/types/data", function(data) {
					if (data.status == "success") {

						// build type structure for ztree
						var types = [];

						createTypeNode = function(type, types) {

							var node = {
								name: type.label,
								id: type.id,
								children: [],
								isRef: true,
								baseType: type.id
							}

							for (var i = 0; i < type.fields.length; i++)
							{
								node.children.push(createFieldNode(type.fields[i], types, type.label, [ type.id ], type.id, 0));
							}

							return node;
						}

						createFieldNode = function(field, types, path, idPath, baseType, depth) {

							var newIdPath = [ field.id ];
							newIdPath = idPath.concat(newIdPath);
							
							var node = {
								name: field.label,
								id: field.id,
								path: path + " > " + field.label,
								idPath: newIdPath,
								isRef: false,
								baseType: baseType
							}

							if (field.typePrefix)
							{
								node.isRef = true;
								if (depth < 2)
								{
									if (types[field.typePrefix])
									{
										node.children = [];
										var childObj = types[field.typePrefix]; 
											
										for (var i = 0; i < childObj.fields.length; i++)
										{
											node.children.push(createFieldNode(childObj.fields[i], types, node.path, node.idPath, depth + 1));
										}
									}
								}
							}

							return node;
						}

						for (var typePrefix in data.data)
						{
							types.push(createTypeNode(data.data[typePrefix], data.data));
						}

						var treeSettings = {
							callback: {
								onClick: selectField
							}
						};

						$.fn.zTree.init($("#type-choice"), treeSettings, types);
						
						function selectField (event, treeId, treeNode)
						{		
							if (treeNode.isRef)
							{
								return;
							}
							addSelectedFieldToPanel(treeNode.idPath, treeNode.path, null, null);	
						}

						if (reportTypeId != "")
						{
							initReportType(${jcr}, types);
						}
					}
					else
					{
						showMsg("err", data.messages, "error", null, null, "${pageContext.request.contextPath}/rm");
					}
				}, "json");
				
			});		
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<ko:pageHeader>
				<c:if test="${reportTypeId != null}">${reportTypeName}</c:if>
				<c:if test="${reportTypeId == null}"><kolmu:label key="reports.new.title" i18n="${i18n}" /></c:if>
			</ko:pageHeader>
			
			<div id="err"></div>
			
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">${i18n.get('reports.reportype.name')}</td>
						<td class="value"><input type="text" id="reportName" value="${reportTypeName}"></input></td>
					</tr>
					<tr>
						<td class="label">${i18n.get('reports.reportype.desc')}</td>
						<td class="value"><textarea id="reportDesc">${reportTypeDesc}</textarea></td>
					</tr>
				</tbody>
			</table>
	
			<div id="report-btns">
				<input type="button" value="${i18n.get('btn.save')}" class="sbtn" onclick="saveReport()" />
				<a href="${pageContext.request.contextPath}/km/reporttypes/list" class="sbtn">${i18n.get('btn.cancel')}</a>
				<span id="run-btn-container"></span>
			</div>
			<div style="margin-bottom: 40px"></div>
			
			<h3>${i18n.get('reports.field.choice')}</h3>

			<div id="report-conf">
				<div id="type-choice-wrapper">
					<ul id="type-choice" class="ztree"></ul>
				</div>
				<div id="field-choice-wrapper">
					<table id="field-choice">
						<tbody></tbody>
					</table>
				</div>
			</div>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
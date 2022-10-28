<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="DAL console" importRMJS="true">

	<jsp:body>
	
		<style>
		
			textarea.query {
				width: 100%;
  				height: 8em;
  				box-sizing: border-box;
				font-family: Consolas, Courier, Arial, Verdana;
				box-shadow: 0 0 0.2em rgba(0, 0, 0, 0.1);
				background-color: #F9F9F9;
				border: 1px solid #ccc;
				border-radius: 0.2em;
				padding: 0.5em;
			}
			
			div#result-raw {
				border: 1px solid #ccc;
				border-radius: 2px;
				padding: 10px;
				font-family: Courier, Arial, Verdana;
				background-color: #F6F3F3;
				display: none;
				font-size: 1.2em;
			}
			
			div#result-tree ul {
				list-style-type: square;
			}
			
			div#result-tree li {
				display: none
			}
			
			ul#result-switch {
				list-style-type: none;
				margin: 20px 0 20px 0;
				padding: 0;
			}
			
			ul#result-switch > li {
				display: inline-block;
				padding: 5px;
				border-radius: 2px;
				background-color: #f6f6f6;
				border: 1px solid #ccc;
				cursor: pointer;
			}
			
			ul#result-switch > li:hover {
				background-color: #fff;
			}
			
			div#query-builder-line {
				margin: 1em 0 1em 0;
				display: table;
			}
			
			div#query-builder-line > div {
				display: table-cell;
			}
			
			div#query-builder-line > div:FIRST-CHILD {
				padding-right: 1em;
			}
		
		</style>
		
		<script type="text/javascript">
		
			$(document).ready(function() {
				$(".show-on-result").hide();
				km.js.utils.openMenuItem("Query database");
				
				showTypeLookup();
			});
			
			function showTypeLookup()
			{
				window.dalconsole = {
					availableTypes: []
				};
				
				window.dalconsole.availableTypes = ${availableTypes};
				
				var lookupOptions = {
					target: $("#query-type"),
					inputName: "stub",
					types: window.dalconsole.availableTypes,
					visibleInput: {
						cssClass: "std-input"
					},
					afterSelect: function(selectedTypeId) {
						
						var selectedType = null;
						
						for (var i = 0; i < window.dalconsole.availableTypes.length; i++)
						{
							if (window.dalconsole.availableTypes[i].id == selectedTypeId)
							{
								selectedType = window.dalconsole.availableTypes[i];
								break;
							}
						}
						
						var fields = [];
						
						for (var i = 0; i < selectedType.fields.length; i++)
						{
							var field = selectedType.fields[i];
							fields.push(field.apiName + (!field.isPrimitive ? ".id" : ""));
						}
						
						var query = "SELECT " + fields.join(", ") + " FROM " + selectedType.qualifiedName;
						$("#query").val(query);
						
					}
				}
				
				km.js.ui.typeLookup(lookupOptions);
			}

			function executeQuery()
			{
				if (!$("#query").val())
				{
					showMsg("query-error", "Please enter the query to run", "error", null, null, km.js.config.contextPath);
					return;
				}
				
				$("#query-error").empty().hide();
				$("#result-tree").empty().hide();
				$("#result-table").empty().hide();
				$(".show-on-result").hide();
				$("#result-raw").html('Running query...');
				
				$.ajax({ // ajax call starts
					url: '${pageContext.request.contextPath}/km/rest/dal',
					data: { q: $('#query').val(), env: '${envId}' },
					dataType: 'json',
					success: function(data) {
			        	$("#result-table").show();
			        	$(".show-on-result").show();
							
						if (data instanceof Array)
						{
							$('#result-raw').html('<pre>' + JSON.stringify(data, null, "\t") + '</pre>');
							$("#result-tree").empty().append(getJsonTree(data));
							$("#result-table").empty().append(data.length ? getDataTable(data) : $("<div></div>").text("No records found"));
						}
						else
						{
							$('#result-raw').html(data.error);
						} 
					},
					error: function(xhr, status, error) {
						var respJson = JSON.parse(xhr.responseText);
						showMsg("query-error", respJson.message, "error", null, null, km.js.config.contextPath);
					}
				});
			}
			
			function openResults(i)
			{
				$(".query-res").hide();
				$(".query-res-" + i).show();
			}
			
			function getDataTable(data)
			{
				if (data.length === 0)
				{
					return;
				}
				
				// get column names from the first record
				var firstRecord = data[0];
				
				var properties = [];
				var header = $("<tr></tr>");
				for (var prop in firstRecord)
				{
					properties.push(prop);
					header.append($("<th></th>").text(prop));
				}
				
				var table = $("<table></table>").addClass("std-table");
				table.append($("<thead></thead>").append(header));
				
				var body = $("<tbody></tbody>");
				for (var i = 0; i < data.length; i++)
				{
					var rec = data[i];
					var row = $("<tr></tr>");
					for (var k = 0; k < properties.length; k++)
					{
						row.append($("<td></td>").text(rec[properties[k]]));
					}
					
					body.append(row);
				}
				
				table.append(body);
				return table;
			}
			
			function getJsonTree(obj)
			{
				var item = $("<ul></ul>");
				
				var name = $("<a></a>").html("item").click(function() {
					$(this).siblings("li").toggle();
				});
				
				//item.append(name);
				
				if (typeof(obj) === "object")
				{
					for (prop in obj)
					{
						var val = obj[prop];
						var li = $("<li></li>").append(getJsonTree(val));
						item.append(li);
					}	
				}
				else if ($.isArray(obj))
				{
					console.log("arr");
					for (var i = 0; i < obj.length; i++)
					{
						item.append($("<li></li>").append(getJsonTree(obj[i])));
					}
				}
				else
				{
					var li = $("<li></li>").append(obj);
					item.append(li);
				}
				
				return item;
			}
		
		</script>
	
	
			<km:breadcrumbs isAlwaysVisible="true"/>
	
			<div class="ibox">
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
					
			<ko:pageHeader>Database query tool</ko:pageHeader>
			
			<div id="query-error"></div>
			
			<div id="query-builder-line">
				<div>Query object:</div>
				<div><input id="query-type"></input></div>
			</div>
			
			<textarea class="query" id="query" name="query" spellcheck="false"></textarea>
			<p>
			<input type="button" class="sbtn" onclick="executeQuery()" value="Run query"></input>
			</p>

			<ul id="result-switch" class="show-on-result">
				<li onClick="openResults(0)">Raw results</li>
				<li onClick="openResults(2)">Result table</li>
			</ul>
				
			<div class="query-res query-res-0" id="result-raw"></div>
			<div class="query-res query-res-1" id="result-tree"></div>
			<div class="query-res query-res-2" id="result-table"></div>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
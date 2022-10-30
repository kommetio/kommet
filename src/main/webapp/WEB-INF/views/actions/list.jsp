<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Actions" importRMJS="true">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<script>
		
		$(document).ready(function() {
			km.js.utils.openMenuItem("Actions");
		});
		
		</script>
		
		<style>
		
			div#search-panel {
				margin: 25px 0 10px 0;			
			}
			
			input#keyword {
				width: 300px;
			}
			
			input#showSystemActions {
				margin-left: 20px;
			}			
		
		</style>
	
		<div class="ibox">
	
			<ko:pageHeader>Actions</ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/actions/new" class="sbtn">New action</a>
			<input type="submit" class="sbtn" onClick="searchActions()" value="Search" />
			
			<div id="search-panel">
				<input type="text" id="keyword" class="std-input" placeholder="search text"></input>
				<input type="checkbox" id="showSystemActions" onclick="searchActions()">Show system actions
			</div>
	
			<table id="action-list" class="std-table" style="margin-top: 30px">
				<thead>
					<tr class="cols">
						<th>Name</th>
						<th>URL</th>
						<th>Type</th>
						<th>Controller/Method</th>
						<th>Creation Date</th>
					</tr>
				</thead>
				<tbody>
				</tbody>
			</table>
		
		</div>
		
		<script>
		
			function searchActions()
			{	
				$.get("${pageContext.request.contextPath}/km/actions/search", { keyword: $("#keyword").val(), showSystemActions: $("#showSystemActions").is(":checked") }, function(data) {
						
					var renderRow = function (action) {
						
						var row = $("<tr></tr>");
						
						if (action.isGeneric === false)
						{
							row.append($("<td></td>").append($("<a></a>").attr("href", km.js.config.contextPath + "/km/actions/" + action.id.id).text(action.interpretedName ? action.interpretedName : "-")));
							row.append($("<td></td>").append($("<a></a>").attr("href", km.js.config.contextPath + "/km/actions/" + action.id.id).text(action.url)));
							row.append($("<td></td>").text("Registered"));
						}
						else
						{
							row.append($("<td></td>").text("-"));
							row.append($("<td></td>").append($("<a></a>").attr("href", km.js.config.contextPath + "/km/classes/" + action.controllerId.id).text(action.url)));
							row.append($("<td></td>").text(action.isRest ? "REST/Generic" : "Regular/Generic"));
						}
						row.append($("<td></td>").append($("<a></a>").attr("href", km.js.config.contextPath + "/km/classes/" + action.controllerId.id).text(action.controllerName + "." + action.actionMethod)));
						row.append($("<td></td>").text(km.js.utils.formatDate(new Date(action.createdDate))));
						
						$("table#action-list > tbody").append(row);
					}
					
					if (data.success === true)
					{
						$("table#action-list > tbody").empty();
						
						for (var i = 0; i < data.data.length; i++)
						{
							renderRow(data.data[i]);
						}
					}
					else
					{
						km.js.ui.statusbar.show("Error returning actions");
					}
					
				}, "json");
			}

			/*function applyFilter()
			{
				filter = "";
				if ($('#showSystemActions').is(':checked'))
				{
					filter += ".system-action";	
				}
				else
				{
					filter += ".non-system-action";
				}

				if ($("#keyword").val() != '')
				{
					filter += ":contains(" + $("#keyword").val() + ")";
				}

				$("#action-list > tbody > tr").hide();
				$("#action-list > tbody > tr" + filter).show();
			}*/

			$("#keyword").keypress(function(e) {
				if(e.which == 13)
				{
					searchActions();
				}
			});

			$(document).ready(function() {
				searchActions();
				$("#keyword").focus();
			});
		
		</script>
	
	</jsp:body>
	
</ko:homeLayout>
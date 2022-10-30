<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${app.name} - App" importRMJS="true">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
		
		<style>
		
			div#app-url-add-form {
				margin: 2em 0 2em 0;
			}
			
			input#save-url-btn {
				margin-left: 1em
			}
		
		</style>
		
		<script>
			
			function deleteApp()
			{
				$.post("${pageContext.request.contextPath}/km/apps/delete", { id: "${app.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/apps/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}
			
			function initAppUrls()
			{
				var ds = km.js.datasource.create({
					type: "database"
				});
				
				var jcr = {
					baseTypeName: "kommet.basic.AppUrl",
					properties: [
						{ name: "id" },
						{ name: "url" }
					]
				};
				
				var deleteCallback = function (record, recordId, cell) {
					var btn = $("<a></a>").attr("href", "javascript:;").html("Delete");
					
					km.js.ui.confirm({
						target: btn,
						question: "Are you sure?",
						callback: function() {
							$.post("${pageContext.request.contextPath}/km/appurls/delete", { id: recordId }, function(data) {
								initAppUrls();
							}, "json");
						}
					});
					
					cell.empty().append(btn);
				};
				
				var displayOptions = {
					properties: [
						{ name: "url", label: "Domain URL", linkStyle: false },
						{ name: "id", label: "Delete", content: deleteCallback }
					],
					idProperty: { name: "id" }
				};
				
				var tableOptions = {
					id: "apps"
				}
				
				var appUrlTable = km.js.table.create(ds, jcr, displayOptions, tableOptions);
				
				appUrlTable.render(jcr, function(table) {
					$("#app-url-list").empty().append(table);
				});
			}
			
			$(document).ready(function() {
				initAppUrls();
				
				$("#save-url-btn").click(function() {
					$.post("${pageContext.request.contextPath}/km/appurls/save", { url: $("#new-app-url").val(), appId: "${app.id}" }, function(data) {
						
						$("#new-app-url").val("");
						initAppUrls();
						
					}, "json");
				});
				
				km.js.utils.openMenuItem("Apps");
			});
		
		</script>
		
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="">
				
				<ko:pageHeader>${app.name}</ko:pageHeader>
				
				<ko:buttonPanel>
					<a href="${pageContext.request.contextPath}/km/apps/edit/${app.id}" class="sbtn">Edit</a>						
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this app?', 'warnPrompt', function() { deleteApp(); })" id="deleteAppBtn" class="sbtn">Delete</a>
				</ko:buttonPanel>
				
				<div id="warnPrompt" style="margin-top:10px"></div>
				
				<ko:propertyTable>
					
					<ko:propertyRow>
						<ko:propertyLabel value="Label"></ko:propertyLabel>
						<ko:propertyValue>${app.label}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Name"></ko:propertyLabel>
						<ko:propertyValue>${app.name}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Type"></ko:propertyLabel>
						<ko:propertyValue>${app.type}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Landing URL"></ko:propertyLabel>
						<ko:propertyValue>${app.landingUrl}</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>

			</form>
			
			<h3 style="margin-top: 2em">Mapped URLs</h3>
			
			
			<div id="app-url-add-form">
			
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Add new URL"></ko:propertyLabel>
						<ko:propertyValue><input type="text" id="new-app-url"></input><input type="button" class="sbtn" value="Save" id="save-url-btn"></ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
			
			</div>
			
			<div id="app-url-list"></div>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
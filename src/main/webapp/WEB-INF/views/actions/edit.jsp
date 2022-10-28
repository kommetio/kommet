<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit action">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				km.js.ui.autoFormatName({
					target: $("input[name='name']")
				});
				
				initTooltips();
				initControllerMethod("${page.controller.id}", "${page.controllerMethod}");
			});
			
			function initControllerMethod(controllerId, methodName)
			{
				if (!controllerId)
				{
					$("#controllerMethodCell").empty().hide();
				}
				
				var controllers = ${controllerActionMethodsJSON};
				
				var select = $("<select></select>").attr("name", "controllerMethod").attr("id", "controllerMethod");
				
				var hasMethods = false;
				
				for (var i = 0; i < controllers.length; i++)
				{
					var cls = controllers[i];
					if (cls.id === controllerId)
					{
						for (var k = 0; k < cls.methods.length; k++)
						{
							var method = cls.methods[k];
							hasMethods = true;
							
							var option = $("<option></option>").attr("value", method).text(method);
							
							if (method === methodName)
							{
								option.attr("selected", "selected")
							}
							
							select.append(option);
						}
					}
				}
				
				if (!hasMethods)
				{
					$("#controllerMethodCell").empty().append($("<span>No action methods available in selected class</span>")).show()
				}
				else
				{
					$("#controllerMethodCell").empty().append(select).show();
				}
			}
			
			function initTooltips()
			{
				km.js.ui.tooltip({
					afterTarget: $("input[name='name']"),
					text: "The name that uniquely identifies this action, e.g. CustomerDetail or com.myapp.OrderList"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='url']"),
					text: "URL at which the action will be available"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='isPublic']"),
					text: "A public action is available to unauthenticated users. Non-public actions are only available to logged in users."
				});
				
				/*km.js.ui.tooltip({
					afterTarget: $("input[id='cls-id']"),
					text: "Controller class that will be called to prepare data for this action"
				});*/
			}
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script>
		
			function createClassLookup()
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
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true },
							{ name: "packageName", label: "Package", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: "Classes",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var clsLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "controllerId",
					selectedRecordId: "${page.controller.id}",
					afterSelect: function(classId) { initControllerMethod(classId); }
				});
				
				clsLookup.render($("#cls-id"));
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
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true },
							{ name: "packageName", label: "Package", linkStyle: true }
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
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "viewId",
					selectedRecordId: "${page.view.id}"
				});
				
				viewLookup.render($("#view-id"));
			}
			
			$(document).ready(function() {
				createClassLookup();
				createViewLookup();
				km.js.utils.openMenuItem("Actions");
			});
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<ko:pageHeader>
				<c:if test="${page.id != null}">Edit action</c:if>
				<c:if test="${page.id == null}">New action</c:if>
			</ko:pageHeader>
			
			<form method="post" action="${pageContext.request.contextPath}/km/actions/save">
				<input type="hidden" name="pageId" value="${page.id}" />
				
				<ko:buttonPanel>
					<input type="submit" value="Save" />
					<a class="sbtn" href="${pageContext.request.contextPath}/km/actions/list">Cancel</a>
				</ko:buttonPanel>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${page.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="URL" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="url" value="${page.url}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Controller" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<%--<select name="controllerId">
								<option value="">-- Select --</option>
								<c:forEach items="${kollFiles}" var="file">
									<option value="${file.id}"<c:if test="${file.id == page.controller.id}"> selected</c:if>>${file['packageName']}<c:if test="${not empty file['packageName']}">.</c:if>${file.name}</option>
								</c:forEach>
							</select>--%>
							<input type="text" id="cls-id">
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Controller method" required="true"></ko:propertyLabel>
						<ko:propertyValue id="controllerMethodCell">
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="View" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="view-id">
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Is Public" required="false"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" value="true" name="isPublic" <c:if test="${page.isPublic == true}"> checked</c:if> />
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
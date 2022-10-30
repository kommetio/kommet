<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}">
	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ui.js"></script>
		
		<style>
		
			div.km-lookup {
				width: 25em;
			}
			
			.action-choice {
				display: none;
			}
		
		</style>
	
		<script>
		
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
					inputName: "actionId",
					inputId: "actionId",
					selectedRecordId: "${button.action.id}"
				});
				
				actionLookup.render($("#referencedAction"));
			}
			
			function bindButtonName()
			{
				km.js.utils.bind($("#label"), $("#name"), "", function(label) {
					
					if (!label)
					{
						return "";
					}
					
					return "kommet.buttons." + km.js.utils.capitalize(label.replace(/\s/g, "_").toLowerCase());
				});
			}
		
			$(document).ready(function() {
				
				km.js.ui.typeLookup({
					
					selectedRecordId: "${type.KID}",
					inputName: "typeId",
					inputId: "typeId",
					target: $("#buttonType")
					
				});
				
				bindButtonName();
				
				initActionLookup();
				
				updateActionType("${actionType}");
				
				$("#actionType").change(function() {
					
					updateActionType($(this).val());
					
				});
				
				$("#saveBtn").click(function() {
					
					$("#error-container").empty();
					
					var payload = {
						buttonId: $("#buttonId").val(),
						name: $("#name").val(),
						label: $("#label").val(),
						labelKey: $("#labelKey").val(),
						url: $("#url").val(),
						onClick: $("#onClick").val(),
						typeId: $("input[name=typeId]").val(),
						actionType: $("#actionType").val(),
						actionId: $("#actionId").val(),
						displayCondition: $("#displayCondition").val()
					};
					
					$.post(km.js.config.contextPath + "/km/buttons/save", payload, function(resp) {
						
						if (resp.success)
						{
							$("#buttonId").val(resp.data.buttonId);
							km.js.utils.openURL(km.js.config.contextPath + "/km/type/${type.keyPrefix}#rm.tab.3")
						}
						else
						{
							km.js.ui.error(resp.messages, $("#error-container"));
						}
						
					}, "json");
					
				});
				
			});
			
			function updateActionType(type)
			{
				$(".action-choice").hide();
				$(".action-choice-" + type).show();
				$("#actionType").val(type);
			}
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true"/>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
			
			<div id="error-container"></div>
		
			<form method="post" action="${pageContext.request.contextPath}/km/buttons/save" id="buttonForm">
				<input type="hidden" name="buttonId" id="buttonId" value="${button.id}" />
				
				<ko:pageHeader>${pageTitle}</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Label" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="label" id="label" value="${button.label}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Label Key" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="labelKey" id="labelKey" value="${button.labelKey}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" id="name" value="${button.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Action" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="actionType" id="actionType">
								<option>-- select action type --</option>
								<option value="action">Action</option>
								<option value="url">URL</option>
								<option value="onClick">On Click Javascript</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="action-choice action-choice-url">
						<ko:propertyLabel value="URL" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="url" id="url" value="${button.url}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="action-choice action-choice-action">
						<ko:propertyLabel value="Action" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="referencedAction" value="${button.action}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="action-choice action-choice-onClick">
						<ko:propertyLabel value="On Click event" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="onClick" id="onClick" value="${button.onClick}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Type"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="buttonType" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Display condition"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="displayCondition" value="${button.displayCondition}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="button" value="Save" id="saveBtn" class="sbtn" /></ko:buttonCell>
						<c:if test="${not empty button.id}">
							<a href="${pageContext.request.contextPath}/km/buttons/${button.id}" class="sbtn">Cancel</a>
						</c:if>
						<c:if test="${empty button.id}">
							<a href="${pageContext.request.contextPath}/km/type/${type.keyPrefix}#rm.tab.3" class="sbtn">Cancel</a>
						</c:if>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
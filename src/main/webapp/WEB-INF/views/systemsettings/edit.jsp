<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="kommet.systemsettings.SystemSettingKey" %>

<ko:homeLayout title="System settings">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.View",
					properties: [
						{ name: "name" },
						{ name: "packageName" },
						{ name: "id" }
					]
				};
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true },
							{ name: "packageName", label: "Package Name", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: "Views",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" }, { name: "packageName", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var errorViewLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "defaultErrorViewId",
					selectedRecordId: "${setting_DEFAULT_ERROR_VIEW_ID}"
				});
				
				errorViewLookup.render($("#error-view-lookup"));
			});
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<ko:pageHeader>System settings</ko:pageHeader>
			
			<form method="POST" action="${pageContext.request.contextPath}/km/systemsettings/save">
				<input type="submit" value="Save" class="sbtn" />
				<a href="${pageContext.request.contextPath}/km/systemsettings/details" class="sbtn">Cancel</a>
			
				<table class="kdetails" style="margin: 30px 0 30px 0">
					<tbody>
						<tr>
							<td class="label">Default environment locale</td>
							<td class="value">
								<select name="setting_DEFAULT_ENV_LOCALE">
									<option value="EN_US"<c:if test="${setting_DEFAULT_ENV_LOCALE == 'EN_US'}"> selected</c:if>>English (United States)</option>
									<option value="PL_PL"<c:if test="${setting_DEFAULT_ENV_LOCALE == 'PL_PL'}"> selected</c:if>>Polski (Polska)</option>
								</select>
							</td>
							<td class="sep"></td>
							<td class="label">Ignore non existing field labels</td>
							<td class="value">
								<select name="setting_IGNORE_NON_EXISTING_FIELD_LABELS">
									<option value="true"<c:if test="${setting_IGNORE_NON_EXISTING_FIELD_LABELS == 'true'}"> selected</c:if>>Yes</option>
									<option value="false"<c:if test="${setting_IGNORE_NON_EXISTING_FIELD_LABELS == 'false'}"> selected</c:if>>No</option>
								</select>
							</td>
						</tr>
						<tr>
							<td class="label">Minimum password length</td>
							<td class="value">
								<input type="text" name="setting_MIN_PASSWORD_LENGTH" value="${setting_MIN_PASSWORD_LENGTH}" >
							</td>
							<td class="sep"></td>
							<td class="label">Default error view</td>
							<td class="value">
								<input type="text" id="error-view-lookup">
							</td>
						</tr>
					</tbody>
				</table>
			</form>
		
		</div>
	
	</jsp:body>
</ko:homeLayout>
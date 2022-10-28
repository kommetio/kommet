<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit user" importRMJS="true">
	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ui.js"></script>

		<script>
		
			$(document).ready(function() {
				
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.Profile",
					properties: [
						{ name: "name" },
						{ name: "id" },
						{ name: "label" }
					]
				};
				
				// options of the available items list
				var availableItemsOptions = {
					options: {
						id: "profile-search"
					},
					display: {
						properties: [
							{ name: "label", label: "Label", linkStyle: true },
							{ name: "name", label: "Name", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: "Profiles",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" }, { name: "label", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var profileLookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "profileId",
					selectedRecordId: "${user.profile.id}"
				});
				
				profileLookup.render($("#profileLookup"));
			});
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<form method="post" action="${pageContext.request.contextPath}/km/user/save">
				<input type="hidden" name="userId" value="${user.id}" />
				
				<ko:pageHeader>
					<c:if test="${empty user.id}">New user</c:if>
					<c:if test="${not empty user.id}">Edit user</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="User name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="userName" value="${user.userName}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="E-mail" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="email" value="${user.email}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Password"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="password" name="password" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Repeat password"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="password" name="repeatedPassword" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Profile"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="profileLookup" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Time zone" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="timezone" value="${user.timezone}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Language" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="locale">
								<c:forEach items="${locales}" var="locale">
									<option value="${locale.name()}"<c:if test="${user.localeSetting.id == locale.id}"> selected</c:if>>${locale.language}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Default layout"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="layoutId">
								<option value="">-- <kolmu:label key="select.option.none" /> --</option>
								<c:forEach var="layout" items="${layouts}">
									<option value="${layout.id}" <c:if test="${layout.id == userSettings.layout.id}"> selected</c:if>>${layout.name}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
				
				<ko:buttonPanel>
					<input type="submit" value="Save" class="sbtn" />
					<c:if test="${empty user.id}">
						<a href="${pageContext.request.contextPath}/km/users/list" class="sbtn">Cancel</a>
					</c:if>
					<c:if test="${not empty user.id}">
						<a href="${pageContext.request.contextPath}/km/user/${user.id}" class="sbtn">Cancel</a>
					</c:if>
				</ko:buttonPanel>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
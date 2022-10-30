<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${setting.key}">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true"/>
	
		<script>
		
			function deleteSetting()
			{
				$.post("${pageContext.request.contextPath}/km/usersettings/delete", { id: "${setting.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/usersettings/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}
		
		</script>
	
		<div class="ibox">
			
			<ko:pageHeader>${pageTitle}</ko:pageHeader>	
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
			
			<%-- pages can be edited only if they are not standard actions for some type, i.e. their type property is null --%>
			<c:if test="${empty page.typeId}">
				<ko:buttonPanel>
					<a href="${pageContext.request.contextPath}/km/usersettings/edit/${setting.id}" class="sbtn">Edit</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this setting?', 'warnPrompt', function() { deleteSetting(); })" id="deleteSettingBtn" class="sbtn">Delete</a>
				</ko:buttonPanel>
			</c:if>
			<div id="warnPrompt" style="margin: 10px 0 20px 0"></div>
			
			<ko:propertyTable>
				<c:if test="${empty settingName}">
				<ko:propertyRow>
					<ko:propertyLabel value="Key"></ko:propertyLabel>
					<ko:propertyValue>${setting.key}</ko:propertyValue>
				</ko:propertyRow>
				</c:if>
				<c:if test="${not empty settingName}">
				<ko:propertyRow>
					<ko:propertyLabel value="Name"></ko:propertyLabel>
					<ko:propertyValue>${settingName}</ko:propertyValue>
				</ko:propertyRow>
				</c:if>
				<ko:propertyRow>
					<ko:propertyLabel value="Value"></ko:propertyLabel>
					<ko:propertyValue>${settingValue}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Context"></ko:propertyLabel>
					<ko:propertyValue>${setting.hierarchy.activeContextName}</ko:propertyValue>
				</ko:propertyRow>
				<c:if test="${setting.hierarchy.activeContextName == 'profile'}">
					<ko:propertyRow>
						<ko:propertyLabel value="Profile"></ko:propertyLabel>
						<ko:propertyValue>${setting.hierarchy.profile.name}</ko:propertyValue>
					</ko:propertyRow>
				</c:if>
				<c:if test="${setting.hierarchy.activeContextName == 'locale'}">
					<ko:propertyRow>
						<ko:propertyLabel value="Locale"></ko:propertyLabel>
						<ko:propertyValue>${setting.hierarchy.locale.language}</ko:propertyValue>
					</ko:propertyRow>
				</c:if>
				<c:if test="${setting.hierarchy.activeContextName == 'user'}">
					<ko:propertyRow>
						<ko:propertyLabel value="User"></ko:propertyLabel>
						<ko:propertyValue>${setting.hierarchy.user.userName}</ko:propertyValue>
					</ko:propertyRow>
				</c:if>
				<c:if test="${setting.hierarchy.activeContextName == 'user group'}">
					<ko:propertyRow>
						<ko:propertyLabel value="User Group"></ko:propertyLabel>
						<ko:propertyValue>${setting.hierarchy.userGroup.name}</ko:propertyValue>
					</ko:propertyRow>
				</c:if>
			</ko:propertyTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
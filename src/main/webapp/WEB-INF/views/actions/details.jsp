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

<ko:homeLayout title="${page.interpretedName}" importRMJS="true">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script type="text/javascript">
		
			function deleteAction()
			{
				$.post("${pageContext.request.contextPath}/km/actions/delete", { pageId: "${page.id}" }, function(data) {
					if (data.status == "success")
					{
						openUrl("${pageContext.request.contextPath}/km/actions/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}
			
			$(document).ready(function() {
				km.js.ui.bool($("#action-details span#public-action-check"));
			});

		</script>
	
		<div class="ibox">
			
			<ko:pageHeader>${page.interpretedName} action</ko:pageHeader>	
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
			
			<%-- pages can be edited only if they are not standard action for some type, i.e. their type property is null --%>
			<c:if test="${empty page.typeId}">
				<ko:buttonPanel>
					<a href="${pageContext.request.contextPath}/km/actions/edit/${page.id}" class="sbtn">Edit</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this action?', 'warnPrompt', function() { deleteAction(); })" id="deleteActionBtn" class="sbtn">Delete</a>
				</ko:buttonPanel>
			</c:if>
			<div id="warnPrompt" style="margin: 10px 0 20px 0"></div>
			
			<ko:propertyTable id="action-details">
				<ko:propertyRow>
					<ko:propertyLabel value="Name"></ko:propertyLabel>
					<ko:propertyValue>${page.interpretedName}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="URL"></ko:propertyLabel>
					<ko:propertyValue>${page.url}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Controller"></ko:propertyLabel>
					<ko:propertyValue><a href="${pageContext.request.contextPath}/km/classes/${controllerId}">${controllerName}</a></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Controller method"></ko:propertyLabel>
					<ko:propertyValue>${page.controllerMethod}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="View"></ko:propertyLabel>
					<ko:propertyValue><a href="${pageContext.request.contextPath}/km/views/${page.view.id}">${page.view.interpretedName}</a></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Is Public"></ko:propertyLabel>
					<ko:propertyValue><span id="public-action-check">${page.isPublic}</span></ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
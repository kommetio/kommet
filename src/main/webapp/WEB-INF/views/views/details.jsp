<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${view.interpretedName}">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script type="text/javascript">
		
			function deleteView()
			{
				$.post("${pageContext.request.contextPath}/km/views/delete", { viewId : "${view.id}" } , function(data) {
					// delete warn message
					$("#warnPrompt").html("");
					
					if (data.status == "success")
					{
						// redirect to view list
						openUrl("${pageContext.request.contextPath}/km/views/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error", null, null);
					}
				}, "json")
			}
			
			$(document).ready(function() {
				km.js.ui.bool($("table.view-props > tbody > tr > td.bool-icon"));
			});
		
		</script>
	
		<div class="ibox">
		
			<ko:pageHeader>${view.interpretedName} view</ko:pageHeader>
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<%-- views can be edited only if they are not standard actions for some type, i.e. their type property is null --%>
			<ko:buttonPanel>
				<%--<c:if test="${isTypeDetails == true}">
					<a href="${pageContext.request.contextPath}/km/views/editor/${view.id}" class="sbtn">Open in editor</a>
				</c:if>--%>
				<c:if test="${empty view.typeId}">
					<a href="${pageContext.request.contextPath}/km/ide/${view.id}" class="sbtn" id="openIdeBtn">Open in IDE</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this view?', 'warnPrompt', function() { deleteView(); })" class="sbtn" id="deleteViewBtn">Delete</a>
				</c:if>
			</ko:buttonPanel>
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails view-props" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Name</td>
						<td class="value">${view.interpretedName}</td>
						<td class="sep"></td>
						<td class="label">Package</td>
						<td class="value">${view['packageName']}</td>
					</tr>
					<tr>
						<td class="label">Is System</td>
						<td class="value bool-icon">${view.isSystem}</td>
						<td class="sep"></td>
						<td class="label"></td>
						<td class="value"></td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
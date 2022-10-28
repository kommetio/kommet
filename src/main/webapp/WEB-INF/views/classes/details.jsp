<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Class details">
	<jsp:body>
	
		<script>
		
		$(document).ready(function() {
			km.js.utils.openMenuItem("Classes");
		});
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
			
			<ko:pageHeader>${file.name}</ko:pageHeader>
		
			<c:if test="${file.isSystem == false}">
				<a href="javascript:;" onclick="ask('Are you sure you want to delete this class?', 'warnPrompt', function() { deleteClass(); })" class="sbtn">Delete</a>
				<a href="${pageContext.request.contextPath}/km/ide/${file.id}" class="sbtn">Open in IDE</a>
				<a href="${pageContext.request.contextPath}/km/classes/compile/${file.id}" class="sbtn">Compile</a>
			</c:if>
			
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Name</td>
						<td class="value">${file.name}</td>
						<td class="sep"></td>
						<td class="label">Package</td>
						<td class="value">${packageName}</td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
		<script>
		
			function deleteClass()
			{
				$.post("${pageContext.request.contextPath}/km/rest/classes/delete", { id: "${file.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/classes/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}
		
		</script>
		
	</jsp:body>
</ko:homeLayout>
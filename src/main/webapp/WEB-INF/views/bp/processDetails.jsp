<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${process.label}">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
			
			<ko:pageHeader>${process.label}</ko:pageHeader>
		
			<a href="javascript:;" onclick="ask('Are you sure you want to delete this process?', 'warnPrompt', function() { deleteProcess(); })" class="sbtn">Delete</a>
			<a href="${pageContext.request.contextPath}/km/bp/builder/${process.id}" class="sbtn">Open in builder</a>
			
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Label</td>
						<td class="value">${process.label}</td>
						<td class="sep"></td>
						<td class="label">Name</td>
						<td class="value">${process.name}</td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
		<script>
		
			function deleteProcess()
			{
				$.post("${pageContext.request.contextPath}/km/bp/deleteprocess", { id: "${process.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/bp/processes/list");
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
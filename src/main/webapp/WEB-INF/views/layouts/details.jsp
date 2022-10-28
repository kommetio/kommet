<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Layout details">
	<jsp:body>
	
		<script type="text/javascript">
		
			function deleteLayout()
			{
				$.post("${pageContext.request.contextPath}/km/layouts/delete", { id : "${layout.id}" } , function(data) {
					// delete warn message
					$("#warnPrompt").html("");
					
					if (data.success === true)
					{
						// redirect to view list
						openUrl("${pageContext.request.contextPath}/km/layouts/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error", null, null);
					}
				}, "json")
			}
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<ko:pageHeader>Layout details</ko:pageHeader>
			<ko:buttonPanel>
				<a href="${pageContext.request.contextPath}/km/ide/${layout.id}" class="sbtn" id="openIdeBtn">Open in IDE</a>
				<c:if test="${canEdit == true}">
					<a href="${pageContext.request.contextPath}/km/layouts/edit/${layout.id}" class="sbtn">Edit</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this layout?', 'warnPrompt', function() { deleteLayout(); })" class="sbtn" id="deleteLayoutBtn">Delete</a>
				</c:if>
			</ko:buttonPanel>
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Name</td>
						<td class="value">${layout.name}</td>
						<td class="sep"></td>
						<td colspan="2"></td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
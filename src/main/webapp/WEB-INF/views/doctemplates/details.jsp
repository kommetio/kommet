<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Template details">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<style>
			
			div.content {
				padding: 10px;
				background-color: #F7F5F5;
				border: 1px solid #ddd;
				border-radius: 2px;
				margin: 10px 0 10px 0;
			}
		
		</style>
	
		<script type="text/javascript">
		
			function deleteTemplate()
			{
				$.post("${pageContext.request.contextPath}/km/doctemplates/delete", { id : "${template.id}" } , function(data) {
					// delete warn message
					$("#warnPrompt").html("");
					
					if (data.status == "success")
					{
						// redirect to view list
						openUrl("${pageContext.request.contextPath}/km/doctemplates");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error", null, null);
					}
				}, "json")
			}
		
		</script>
	
		<div class="ibox">
		
			<ko:pageHeader>${template.name}</ko:pageHeader>
			<ko:buttonPanel>
				<a href="${pageContext.request.contextPath}/km/doctemplates/edit/${template.id}" class="sbtn">Edit</a>
				<a href="javascript:;" onclick="ask('Are you sure you want to delete this document template?', 'warnPrompt', function() { deleteTemplate(); })" class="sbtn" id="deleteTemplateBtn">Delete</a>
			</ko:buttonPanel>
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Name</td>
						<td class="value">${template.name}</td>
					</tr>
					<tr>
						<td class="label">Content</td>
						<td class="value"></td>
					</tr>
					<tr>
						<td colspan="2">
							<div class="content">${content}</div>
						</td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
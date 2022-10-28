<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Text Label details">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script type="text/javascript">
		
			function deleteLabel()
			{
				$.post("${pageContext.request.contextPath}/km/textlabels/delete", { id : "${label.id}" } , function(data) {
					// delete warn message
					$("#warnPrompt").html("");
					
					if (data.status == "success")
					{
						// redirect to view list
						openUrl("${pageContext.request.contextPath}/km/textlabels/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error", null, null);
					}
				}, "json")
			}
		
		</script>
	
		<div class="ibox">
		
			<ko:pageHeader>${label.key}</ko:pageHeader>
			<ko:buttonPanel>
				<a href="${pageContext.request.contextPath}/km/textlabels/edit/${label.id}" class="sbtn">Edit</a>
				<a href="javascript:;" onclick="ask('Are you sure you want to delete this text label?', 'warnPrompt', function() { deleteLabel(); })" class="sbtn" id="deleteLabelBtn">Delete</a>
			</ko:buttonPanel>
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Key</td>
						<td class="value">${label.key}</td>
					</tr>
					<tr>
						<td class="label">Value</td>
						<td class="value">${label.value}</td>
					</tr>
					<tr>
						<td class="label">Locale</td>
						<td class="value">${label.locale}</td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
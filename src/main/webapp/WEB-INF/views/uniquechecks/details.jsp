<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Unique check details">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script type="text/javascript">
		
			function deleteCheck()
			{
				$.post("${pageContext.request.contextPath}/km/uniquechecks/delete", { id : "${uc.id}" } , function(data) {
					
					// delete warn message
					$("#warnPrompt").html("");
					
					if (data.success === true)
					{
						// redirect to view list
						openUrl("${pageContext.request.contextPath}/km/type/" + data.data.keyPrefix + "#rm.tab.6");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error", null, null);
					}
				}, "json");
			}
		
		</script>
		
		<style>
	
			ul"km-uc-field-list {
				list-style: none;
				padding: 0;
				margin: 0;
			}
			
			ul"km-uc-field-list > li {
				display: inline-block;
				border-radius: 2px;
				padding: 0.3em;
				border: 1px solid #ccc;
				background-color: #eee;
			}
			
		</style>
	
		<div class="ibox">
		
			<ko:pageHeader>${uc.name}</ko:pageHeader>
			<ko:buttonPanel>
				<c:if test="${isSystem == false}">
					<a href="${pageContext.request.contextPath}/km/uniquechecks/edit/${uc.id}" class="sbtn">Edit</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this unique check?', 'warnPrompt', function() { deleteCheck(); })" class="sbtn" id="deleteCheckBtn">Delete</a>
				</c:if>
			</ko:buttonPanel>
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Name</td>
						<td class="value">${uc.name}</td>
					</tr>
					<tr>
						<td class="label">Fields</td>
						<td class="value">
							<ul id="km-uc-field-list">
							<c:forEach var="fieldId" items="${uc.parsedFieldIds}">
								<li>${fieldsById[fieldId].apiName}</li>
							</c:forEach>
							</ul> 
						</td>
					</tr>
					<tr>
						<td class="label">Created date</td>
						<td class="value"><km:dateTime value="${uc.createdDate}" format="dd-MM-yyyy HH:mm:ss" /></td>
					</tr>
					<tr>
						<td class="label">Created by</td>
						<td class="value"><a href="${pageContext.request.contextPath}/km/user/${createdBy.id}">${createdBy.userName}</a></td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
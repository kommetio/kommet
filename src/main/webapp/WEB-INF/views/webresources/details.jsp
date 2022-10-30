<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${resourceName}" importRMJS="true">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script type="text/javascript">
		
			function deleteResource()
			{
				$.post("${pageContext.request.contextPath}/km/webresources/delete", { resourceId: "${resource.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/webresources/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}

			$(document).ready(function() {
				km.js.ui.bool($("table#wr-props #isPublicField"));
			});

		</script>
		
		<style>
		
			img.km-thumb {
				width: 20%;
				margin: 2rem 0;
				border: 1px solid #ccc;
				border-radius: 0.2em;
			}
		
		</style>
	
		<div class="ibox">
			
				<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
				<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
				
				<ko:pageHeader>${resourceName}</ko:pageHeader>
				<ko:buttonPanel>
					<a href="${pageContext.request.contextPath}/km/webresources/edit/${resource.id}" class="sbtn"><kolmu:label key="btn.edit" /></a>
					<a href="${pageContext.request.contextPath}/km/download/${resource.file.id}" class="sbtn">${downloadLabel}</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this web resource?', 'warnPrompt', function() { deleteResource(); })" id="deleteResourceBtn" class="sbtn"><kolmu:label key="btn.delete" /></a>
				</ko:buttonPanel>
				
				<div id="warnPrompt" style="margin: 10px 0 20px 0"></div>
				
				<img src="${pageContext.request.contextPath}/km/download/${resource.file.id}" class="km-thumb"></img>
				
				<ko:propertyTable id="wr-props">
					<ko:propertyRow>
						<ko:propertyLabel value="Name"></ko:propertyLabel>
						<ko:propertyValue>${resourceName}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Is Public"></ko:propertyLabel>
						<ko:propertyValue id="isPublicField">${resource.isPublic}</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${resource.name}">
	<jsp:body>
	
		<script type="text/javascript">
		
			function deleteResource()
			{
				$.post("${pageContext.request.contextPath}/km/viewresources/delete", { resourceId: "${resource.id}" }, function(data) {
					if (data.success === true)
					{
						openUrl("${pageContext.request.contextPath}/km/viewresources/list");
					}
					else
					{
						showMsg("warnPrompt", data.messages, "error");
					}
				}, "json");
			}

		</script>
	
		<div class="ibox">
			
				<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
				<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
				
				<ko:pageHeader>${resource.name}</ko:pageHeader>
				<ko:buttonPanel>
					<a href="${pageContext.request.contextPath}/km/viewresources/edit/${resource.id}" class="sbtn"><kolmu:label key="btn.edit" /></a>
					<a href="${pageContext.request.contextPath}/km/ide/${resource.id}" class="sbtn">Open in IDE</a>
					<a href="javascript:;" onclick="ask('Are you sure you want to delete this view resource?', 'warnPrompt', function() { deleteResource(); })" id="deleteResourceBtn" class="sbtn"><kolmu:label key="btn.delete" /></a>
				</ko:buttonPanel>
				
				<div id="warnPrompt" style="margin: 10px 0 20px 0"></div>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name"></ko:propertyLabel>
						<ko:propertyValue>${resource.name}</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="MIME type"></ko:propertyLabel>
						<ko:propertyValue>${resource.mimeType}</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
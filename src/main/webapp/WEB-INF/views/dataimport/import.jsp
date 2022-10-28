<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Import data">
	<jsp:body>
	
		<script>
		
			function importData()
			{	
				// create iframe
				var iframe = $('<iframe name="uploadiframe" id="uploadiframe" style="display: none" />');
				$("body").append(iframe);
	
				// submit upload form to the iframe
				$("#uploadForm").attr("target", "uploadiframe");
				$("#uploadForm").submit();
	
				// wait for the iframe to load
				$("#uploadiframe").load(function () {
	                iframeContents = $("#uploadiframe")[0].contentWindow.document.body.innerHTML;
	
	                var data = JSON.parse(iframeContents);
	                console.log("DATA: " + JSON.stringify(data));
	
					if (data.success === true)
					{
						km.js.ui.statusbar.show("Upload complete");   
					}
					else
					{
						km.js.ui.statusbar.show(data.messages);
					}
	            });
			}
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<ko:pageHeader>Import data</ko:pageHeader>
			
			<form:form id="uploadForm" enctype="multipart/form-data" modelAttribute="uploadItem" method="post" action="${pageContext.request.contextPath}/km/dataimport/upload">
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="files.file.label"></ko:propertyLabel>
						<ko:propertyValue>
							<form:input path="fileData" type="file" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Row handler"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="rowHandler">
								<c:forEach var="handler" items="${rowHandlers}">
									<option value="${handler}">${handler}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
				<ko:buttonPanel>
					<input type="button" onclick="importData()" class="sbtn" value="Import" />
				</ko:buttonPanel>
				
			</form:form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
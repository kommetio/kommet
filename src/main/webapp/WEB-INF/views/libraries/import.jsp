<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}" importRMJS="true">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<script>
		
			function upload ()
			{	
				km.js.ui.statusbar.show("Importing library...");
				
				// create iframe
				var iframe = $('<iframe name="uploadiframe" id="uploadiframe" style="display: none" />');
				$("body").append(iframe);
	
				// submit upload form to the iframe
				$("#uploadForm").attr("target", "uploadiframe");
				$("#uploadForm").submit();
	
				$("#uploadStatus").html("Uploading...");
	
				// wait for the iframe to load
				$("#uploadiframe").load(function () {
	                iframeContents = $("#uploadiframe")[0].contentWindow.document.body.innerHTML;
	                
	                function strip(html)
	                {
	                   var tmp = document.createElement("DIV");
	                   tmp.innerHTML = html;
	                   return tmp.textContent || tmp.innerText || "";
	                }
	
	                // iframe contents is HTML, so we need to extract json from it
	                var data = JSON.parse(strip(iframeContents));
	
					if (data.success === true)
					{
						// on success, redirect to library details
						km.js.utils.openURL(km.js.config.contextPath + "/km/libraries/" + data.data.libraryId)
					}
					else
					{
						km.js.ui.statusbar.show("Library deployment failed. See status logs for details");
						showLibErrors(data);
					}
	            });
			}
			
			function showLibErrors (data)
			{
				var errors = $("<ul><ul>");
				
				for (var i = 0; i < data.messages.length; i++)
				{
					errors.append($("<li></li>").text(data.messages[i]));
				}
				
				$("div#errors").empty().append($("<div>Deployment errors</div>").addClass("deploy-err-header")).append(errors);
				$("div#errors").show();
			}
		
		</script>
		
		<style>
		
			input#saveBtn {
				margin-top: 2em;
			}
			
			div#errors {
				border: 1px solid #ccc;
			    margin-top: 2rem;
			    border-radius: 2px;
			    background-color: #fff8a2;
			    padding: 2em;
			}
			
			div.deploy-err-header {
				font-size: 1rem;
				font-weight: bold;
			}
		
		</style>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form:form id="uploadForm" enctype="multipart/form-data" modelAttribute="uploadItem" method="post" action="${pageContext.request.contextPath}/km/lib/doimport">
				
				<input type="hidden" name="libId" value="${lib.id}" />
				
				<ko:pageHeader>${pageTitle}</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Library ZIP file" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<form:input path="fileData" type="file" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Package prefix" required="false"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="packagePrefix" /> 
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
				
				<input type="button" id="saveBtn" onclick="upload()" class="sbtn" value="Import" />
				
				<div id="errors" style="display:none"></div>

			</form:form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
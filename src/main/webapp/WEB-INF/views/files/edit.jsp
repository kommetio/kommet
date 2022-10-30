<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:userLayout layoutPath="${layoutPath}" titleKey="files.upload.pagetitle">
	<jsp:body>
	
		<style>
		
			.uploaded {
				color: rgb(19, 111, 19);
			}
			
			.uploading {
				color: #aaa;
			}
			
			<%-- let file window fill the whole iframe --%>
			div.file-ibox {
				height: 100%;
    			box-sizing: border-box;
			}
			
			<%-- since this page will be placed within a pop-up, it does not inherit the 0.75em from
				the page main container and we need to set the font-size here --%>
			<%--div.file-ibox {
				font-size: 0.85rem
			}--%>
		
		</style>
	
		<script>

			function upload()
			{
				// TODO add method to check upload progress
				
				// create iframe
				var iframe = $('<iframe name="uploadiframe" id="uploadiframe" style="display: none" />');
				$("body").append(iframe);

				// submit upload form to the iframe
				$("#uploadForm").attr("target", "uploadiframe");
				$("#uploadForm").submit();

				$("#uploadStatus").html("Uploading...");
				$("#uploadStatus").removeClass("uploaded");
				$("#uploadStatus").addClass("uploading");

				hideError();

				// wait for the iframe to load
				$("#uploadiframe").load(function () {
	                iframeContents = $("#uploadiframe")[0].contentWindow.document.body.innerHTML;

	                var data = JSON.parse(iframeContents);

					if (data.status == "success")
					{
		                //$("#uploadStatus").html("Upload complete");
		                //$("#uploadStatus").removeClass("uploading");
						//$("#uploadStatus").addClass("uploaded");
						showActionMsg(km.js.config.i18n["files.upload.complete"]);
		                
		                <c:if test="${empty file.id}">
		                $("#propSystemFileName").val(data.fileName);
		                
						<%-- if it's a new file, a name has been generated for it --%>
						$("#uploadFileName").val(data.originalFileName);
	
						<%-- if it's a new file, it can be saved only after it has been successfully uploaded --%>
						$("#saveBtn").show();
						$("#nameRow").show();
						$("#commentRow").show();
						</c:if>
					}
					else if (data.status == "error")
					{
						//$("#uploadStatus").html("Upload failed");
						showError("Upload failed: " + data.messages);
					}
					else
					{
						//$("#uploadStatus").html("Upload failed");
						showError("Download failed for unknown reasons");
					}	
	            });
			}

			function showError(msg)
			{
				showMsg("msgs", msg, "error", "margin-bottom:30px", null, "${pageContext.request.contextPath}");
			}

			function showActionMsg(msg)
			{
				showMsg("msgs", msg, "info", null, null, "${pageContext.request.contextPath}");
			}

			function hideError()
			{
				$("#msgs").html("").hide();
			}

			function saveFile()
			{
				// rewrite fields from the upload form to the hidden property form
				$("#propFileName").val($("#uploadFileName").val());
				<c:if test="${not empty recordId}">
				$("#propComment").val($("#comment").val());
				</c:if>
				hideError();

				$.post('${pageContext.request.contextPath}/km/files/save', $("#propsForm").serialize(), function(res) {
					if (res.result == "success")
					{
						if (res.widgetId)
						{
							// refresh association panel of files - this has to be done before
							// closeRialog is called because the latter destroys the iframe
							parent.window['refreshAssociationPanel_' + res.widgetId]();
							
							// close dialog
							parent.window.$.closeRialog();
						}
						else if (res.parentDialog)
						{
							parent.window[res.parentDialog].close();
						}
						else
						{
							location.href = '${pageContext.request.contextPath}/km/files/' + res.fileId + "?recordId=" + res.recordId;
						}
					}
					else
					{
						showError(res.message);
					}
				},
				"json");
			}
		
		</script>

		<div class="ibox file-ibox">
		
			<c:if test="${not empty recordId && widgetId == null && parentDialog == null}">
				<div class="upper-links">
					<a href="${pageContext.request.contextPath}/km/${recordId}"><kolmu:label key="back.to.record.list" /> ${recordTypeLabel}</a>
				</div>
			</c:if>
			<c:if test="${empty recordId && widgetId == null && parentDialog == null}">
				<div style="width:100%; text-align:left; margin-bottom: 20px">
					<a href="${pageContext.request.contextPath}/km/files/list"><kolmu:label key="files.back.to.list" /></a>
				</div>
			</c:if>
			
			<form id="propsForm" method="post" action="${pageContext.request.contextPath}/km/files/save">
				<input type="hidden" id="propSystemFileName" name="systemFileName" />
				<input type="hidden" id="propFileName" name="fileName" />
				<input type="hidden" id="propComment" name="comment" />
				<input type="hidden" name="fileId" value="${file.id}" />
				<input type="hidden" name="recordId" value="${recordId}" />
				<input type="hidden" name="assocFieldIds" value="${assocFieldIds}" />
				<input type="hidden" name="widgetId" value="${widgetId}" />
				<input type="hidden" name="parentDialog" value="${parentDialog}" />
			</form>
		
			<form:form id="uploadForm" enctype="multipart/form-data" modelAttribute="uploadItem" method="post" action="${pageContext.request.contextPath}/km/files/upload">
			
				<input type="hidden" name="fileId" value="${file.id}" />
				<input type="hidden" name="revisionId" value="${file.latestRevision.id}" />
				<input type="hidden" id="fileName" name="fileName" value="${file.latestRevision.path}" />
				
				<ko:pageHeader>
					<c:if test="${empty file.id}"><kolmu:label key="files.upload.header" /></c:if>
					<c:if test="${not empty file.id}"><kolmu:label key="files.edit.header" /></c:if>
				</ko:pageHeader>
				
				<div id="msgs" style="display:none"></div>
				
				<ko:propertyTable>
					<ko:propertyRow id="nameRow" cssStyle="display:none">
						<ko:propertyLabel valueKey="files.name"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="uploadFileName" value="${file.name}" /> 
						</ko:propertyValue>
					</ko:propertyRow>
					<c:if test="${not empty recordId}">
						<ko:propertyRow id="commentRow" cssStyle="display:none">
							<ko:propertyLabel valueKey="files.comment"></ko:propertyLabel>
							<ko:propertyValue>
								<textarea id="comment"></textarea>
							</ko:propertyValue>
						</ko:propertyRow>
					</c:if>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="files.file.label"></ko:propertyLabel>
						<ko:propertyValue>
							<form:input path="fileData" type="file" onchange="upload()" />
						</ko:propertyValue>
					</ko:propertyRow>
					<%--<ko:propertyRow cssStyle="display:none" id="status-row">
						<ko:propertyLabel value=""></ko:propertyLabel>
						<ko:propertyValue>
							<div id="uploadStatus" style="font-weight: bold;"></div>
						</ko:propertyValue>
					</ko:propertyRow>
					--%>
				</ko:propertyTable>
				<ko:buttonPanel>
					<input type="button" id="saveBtn" onclick="saveFile()" class="sbtn" <c:if test="${empty file.id}">style="display:none" </c:if>value="<kolmu:label key="btn.save" />" />
				</ko:buttonPanel>
			</form:form>

		</div>
		
	</jsp:body>
</ko:userLayout>
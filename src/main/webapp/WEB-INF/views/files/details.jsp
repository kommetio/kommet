<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:userLayout layoutPath="${layoutPath}" title="${file.name}" importRMJS="true">
	<jsp:body>
	
		<script>

			function openBrowseDialog()
			{
				$("#fileItem").click();
			}

			function upload()
			{
				// TODO add method to check upload progress
				
				// create iframe
				var iframe = $('<iframe name="uploadiframe" id="uploadiframe" style="display: none" />');
				$("body").append(iframe);

				// submit upload form to the iframe
				$("#uploadForm").attr("target", "uploadiframe");
				$("#uploadForm").submit();

				showMsg("uploadStatus", "Uploading...", "info", "margin-top: 20px");

				$("#uploadStatus").hide();

				// wait for the iframe to load
				$("#uploadiframe").load(function () {
	                iframeContents = $("#uploadiframe")[0].contentWindow.document.body.innerHTML;

	                var data = JSON.parse(iframeContents);

					if (data.status == "success")
					{
						//
						showMsg("uploadStatus", km.js.config.i18n["files.upload.complete"], "info", "margin-top: 20px");
		                
		                $("#propSystemFileName").val(data.fileName);
						//$("#saveBtn").show();
					}
					else if (data.status == "error")
					{
						showMsg("uploadStatus", "Upload failed: " + data.message, "error", "margin-top: 20px");
					}
					else
					{
						showMsg("uploadStatus", "Upload failed for unknown reasons", "error", "margin-top: 20px");
					}	
	            });
			}

			function saveFile()
			{
				// rewrite fields from the upload form to the hidden property form
				$("#propFileName").val($("#uploadFileName").val());
				$("#uploadStatus").hide();

				$.post('${pageContext.request.contextPath}/km/files/save', $("#propsForm").serialize(), function(res) {
					if (res.result == "success")
					{
						location.href = '${pageContext.request.contextPath}/files/' + res.fileId + "?recordId=" + res.recordId;
					}
					else
					{
						showError(res.message);
					}
				},
				"json");
			}

			$(document).ready(function() {
				$("#fileItem").change(function(e) {
					upload();
				});
				
				// create tabs
				var tabs = km.js.tabs.create({
					tabs: [
						{ content: $("#file-details"), label: "File details" },
						{ content: $("#file-revisions"), label: "Revisions", beforeOpen: function() { console.log("Before open tab"); } }
					]
				});
				
				tabs.render(function(code) {
					$("#tab-container").html(code);
				});
				
				// open the first tab
				tabs.open(0);
			});
			
		
		</script>
		
		<div id="tab-container"></div>

		<div id="file-details">
		
			<c:if test="${not empty recordId}">
				<div class="upper-links" style="margin-bottom: 15px">
					<a href="${pageContext.request.contextPath}/km/${recordId}"><kolmu:label key="back.to.record.list" /> ${recordTypeLabel}</a>
				</div>
			</c:if>
			<c:if test="${empty recordId}">
				<div class="upper-links" style="margin-bottom: 15px">
					<a href="${pageContext.request.contextPath}/km/files/list"><kolmu:label key="files.back.to.list" /></a>
				</div>
			</c:if>
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
				
			<ko:pageHeader>${file.name}</ko:pageHeader>
			<div id="errors" class="msg-tag action-errors" style="display:none"></div>
			
			<a href="${pageContext.request.contextPath}/km/download/${file.id}" class="sbtn">${downloadLabel}</a>
			<a href="javascript:;" onclick="openBrowseDialog()" class="sbtn">${uploadLabel}</a>
			
			<form:form id="uploadForm" enctype="multipart/form-data" modelAttribute="uploadItem" method="post" action="${pageContext.request.contextPath}/km/files/upload" cssStyle="display:none">
				<form:input id="fileItem" path="fileData" type="file" />
				<input type="hidden" name="revisionId" value="${file.latestRevision.id}" />
				<input type="hidden" id="fileName" name="fileName" value="${file.latestRevision.path}" />
			</form:form>
			
			<div>
				<div id="uploadStatus"></div>
			</div>
			
			<ko:propertyTable cssStyle="margin-top: 30px">
				<ko:propertyRow>
					<ko:propertyLabel valueKey="files.name"></ko:propertyLabel>
					<ko:propertyValue>${file.name}</ko:propertyValue>
				</ko:propertyRow>
				<c:if test="${not empty recordId}">
					<ko:propertyRow>
						<ko:propertyLabel valueKey="files.comment"></ko:propertyLabel>
						<ko:propertyValue><div class="inactive-textbox">${fileObjAssignment.comment}</div></ko:propertyValue>
					</ko:propertyRow>
				</c:if>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="label.createdDate"></ko:propertyLabel>
					<ko:propertyValue>
						<km:dateTime value="${file.createdDate}" />
					</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="label.lastModifiedDate"></ko:propertyLabel>
					<ko:propertyValue>
						<km:dateTime value="${file.lastModifiedDate}" />
					</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="files.size"></ko:propertyLabel>
					<ko:propertyValue>${file.latestRevision.size} <kolmu:label key="files.bytes" /></ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>

		</div>
		
		<div id="file-revisions">
			<ko:pageHeader>File revisions</ko:pageHeader>
			
			<km:dataTable var="revisionTable" query="select id, name, revisionNumber, createdDate from FileRevision where file.id = '${file.id}'" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
				</km:dataTableSearch>
				<km:dataTableColumn url="${pageContext.request.contextPath}/km/filerevisions/{id}" name="name" label="Revision Name" sortable="true" linkStyle="true" />
				<km:dataTableColumn name="revisionNumber" label="Revision Number" sortable="true" linkStyle="true" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" linkStyle="false" />
				<%-- <km:dataTableOption name="afterRender" value="function(table) { $('#file-revisions').append(table.generatedCode); }" /> --%>
			</km:dataTable>
		</div>
		
	</jsp:body>
</ko:userLayout>
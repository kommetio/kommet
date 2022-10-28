<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Files" importRMJS="true">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
	
			<ko:pageHeader>Files</ko:pageHeader>
			
			<km:dataTable query="select id, name, createdDate from File" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="Upload" onClick="uploadFile" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/files/{id}" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
		
		<script>
		
			var uploadDialog = km.js.ui.dialog.create({
				id: "file-upload",
				size: {
					width: "800px",
					height: "600px"
				},
				url: "${pageContext.request.contextPath}/km/files/new?parentDialog=uploadDialog"
			});
			
			function uploadFile()
			{
				uploadDialog.show();
			}
		
		</script>
	
	</jsp:body>
	
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Views">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				
				initViews();
				
			});
			
			function initViews()
			{
				km.js.db.query("select id, name, isSystem, packageName from View where typeId = '${type.KID}'", function(records) {

					console.log("Fetched: " + JSON.stringify(records));
					
					for (var i = 0; i < records.length; i++)
					{
						var box = $("<div></div>").addClass("km-view-box");
					}
					
				});
			}
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<div class="section-title">Data views for object ${type.label}</div>
			
			<div id="view-list">
			
				<km:dataTable query="select id, name, createdDate, isSystem, packageName from View where typeId = '${type.KID}'" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
					<km:dataTableSearchField name="keetleCode" label="Code" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New view" url="${pageContext.request.contextPath}/km/tasks/new" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/tasks/{id}" />
				<km:dataTableColumn name="packageName" label="Package" sortable="true" linkStyle="false" />
				<km:dataTableColumn name="createdDate" label="Created" sortable="true" />
			</km:dataTable>
			
			</div>
		
		</div>
		
	</jsp:body>
	
</ko:homeLayout>
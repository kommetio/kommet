<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout titleKey="usergroups.list.title">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
			<ko:pageHeader>${i18n.get('usergroups.list.title')}</ko:pageHeader>
			
			<km:dataTable query="select id, name, createdDate from UserGroup where accessType = 0" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" labelKey="btn.new" url="${pageContext.request.contextPath}/km/usergroups/new" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/usergroups/{id}" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" linkStyle="false" />
			</km:dataTable>
			
			<%--<div id="userGroups" style="margin-top: 20px;"></div>--%>
		</div>
		
		<%--<script>

			$(document).ready(function() {
				loadUserGroups();
			});

			linkFieldFormat = function(obj) {
				return "<a href=\"${pageContext.request.contextPath}/km/usergroups/" + obj.id + "\">" + obj.name + "</a>";
			}

			function loadUserGroups()
			{
				$.get("${pageContext.request.contextPath}/km/usergroups/list/data", function(data) {
					
					if (data.status == "success")
					{
						var options = {
							id: "userGroupList",
							columns: [ 
								{ title: "${i18n.get('usergroups.groupname')}", property: "name", formatObjectValue: linkFieldFormat },
								{ title: "${i18n.get('label.createdDate')}", property: "createdDate" },
							],
							cssClasses: [ "std-table" ],
							cellFocus: false,
							display: "table"
						}
						
						var groupTable = tables.create(options);
						groupTable.render($("#userGroups"), data.data);	
					}
					else
					{
						showMsg ("userGroups", data.messages, "error", null, null, "${pageContext.request.contextPath}/rm");
					}
					
				}, "json");
			}
		
		</script>--%>
	
	</jsp:body>
	
</ko:homeLayout>
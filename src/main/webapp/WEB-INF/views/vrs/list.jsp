<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Validation Rules">

	<jsp:body>
	
		<script>
		
			function typeName(typeId)
			{
				return km.js.config.types[typeId].qualifiedName;
			}
		
		</script>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
	
			<ko:pageHeader>Validation Rules</ko:pageHeader>
			
			<km:dataTable query="select id, name, typeId from kommet.basic.ValidationRule where accessType = 0" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
				</km:dataTableSearch>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/validationrules/{id}" />
				<km:dataTableColumn name="typeId" label="Object" sortable="false" linkStyle="false" formatFunction="typeName" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
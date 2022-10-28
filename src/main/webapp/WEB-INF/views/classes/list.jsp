<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Classes">

	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.table.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.tablesearch.js"></script>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
			<div class="km-title-wrapper">
				<div>
					<img src="${pageContext.request.contextPath}/resources/images/classes.png"></img>
				</div>
				<div>
					Classes
				</div>
			</div>
			
			<km:dataTable query="select id, name, packageName createdDate from Class where accessType = 0" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="name" />
					<km:dataTableSearchField name="packageName" />
					<km:dataTableSearchField name="kollCode" label="Code" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" labelKey="btn.new" url="${pageContext.request.contextPath}/km/classes/new" />
				</km:buttons>
				<km:dataTableColumn name="name" label="Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/classes/{id}" />
				<km:dataTableColumn name="packageName" label="Package Name" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/classes/{id}" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" linkStyle="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
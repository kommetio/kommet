<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Text Labels" importRMJS="true">

	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
	
			<ko:pageHeader>Text Labels</ko:pageHeader>
			
			<km:dataTable query="select id, key, value, locale, createdDate from TextLabel" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="key" />
					<km:dataTableSearchField name="value" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" url="${pageContext.request.contextPath}/km/textlabels/new" />
				</km:buttons>
				<km:dataTableColumn name="key" label="Key" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/textlabels/{id}" />
				<km:dataTableColumn name="value" label="Value" sortable="true" linkStyle="false" />
				<km:dataTableColumn name="locale" label="Locale" sortable="true" linkStyle="false" />
				<km:dataTableColumn name="createdDate" label="Created Date" sortable="true" linkStyle="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
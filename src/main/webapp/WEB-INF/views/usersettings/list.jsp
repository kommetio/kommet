<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout titleKey="usersettings.list.title" importRMJS="true">

	<jsp:body>
	
		<script>
		
			function getSettingName(key)
			{
				var interpretedName = km.js.config.i18n["usersetting.key." + key]; 
				return interpretedName ? interpretedName : key;
			}
		
		</script>
	
		<km:breadcrumbs isAlwaysVisible="true"/>
	
		<div class="ibox">
	
			<ko:pageHeader><kolmu:label key="usersettings.list.title" /></ko:pageHeader>
			
			<km:dataTable query="select id, key, value, createdDate, hierarchy.activeContextName from SettingValue" paginationActive="true" pageSize="25">
				<km:dataTableSearch>
					<km:dataTableSearchField name="key" />
					<km:dataTableSearchField name="value" />
				</km:dataTableSearch>
				<km:buttons>
					<km:button label="New" url="${pageContext.request.contextPath}/km/usersettings/new" />
				</km:buttons>
				<km:dataTableColumn name="key" labelKey="usersettings.key" formatFunction="getSettingName" sortable="true" linkStyle="true" url="${pageContext.request.contextPath}/km/usersettings/{id}" />
				<km:dataTableColumn name="value" labelKey="usersettings.value" sortable="true" link="false" />
				<km:dataTableColumn name="hierarchy.activeContextName" labelKey="usersettings.activecontext" sortable="true" link="false" />
				<km:dataTableColumn name="createdDate" labelKey="label.createdDate" sortable="true" link="false" />
			</km:dataTable>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
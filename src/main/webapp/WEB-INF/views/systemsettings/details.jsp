<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="kommet.systemsettings.SystemSettingKey" %>

<ko:homeLayout title="System settings">
	<jsp:body>
	
		<div class="ibox">
		
			<ko:pageHeader>System settings</ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/systemsettings/edit" class="sbtn">Edit</a>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Default environment locale</td>
						<td class="value">${setting_DEFAULT_ENV_LOCALE}</td>
						<td class="sep"></td>
						<td class="label">Ignore non existing field labels</td>
						<td class="value">${setting_IGNORE_NON_EXISTING_FIELD_LABELS}</td>
					</tr>
					<tr>
						<td class="label">Minimum password length</td>
						<td class="value">${setting_MIN_PASSWORD_LENGTH}</td>
						<td class="sep"></td>
						<td class="label">Default error view</td>
						<td class="value">${setting_DEFAULT_ERROR_VIEW_ID}</td>
					</tr>
				</tbody>
			</table>
		
		</div>
	
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Layout settings">
	<jsp:body>
	
		<div class="ibox">
		
			<ko:pageHeader>Layout settings</ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/layouts/editsettings" class="sbtn">Edit</a>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Default layout</td>
						<td class="value">${defaultLayout}</td>
						<td class="sep"></td>
						<td colspan="2"></td>
					</tr>
				</tbody>
			</table>
		
		</div>
	
	</jsp:body>
</ko:homeLayout>
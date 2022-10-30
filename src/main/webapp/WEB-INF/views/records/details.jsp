<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Object Details">
	<jsp:body>
	
		<div class="ibox">
		
			<%-- <a href="${pageContext.request.contextPath}/km/objects/edit/${obj.KID}" class="sbtn">Edit</a>--%>
		
			<form method="post" action="${pageContext.request.contextPath}/km/object/save">
				<input type="hidden" name="kObjectId" value="${record.id}" />
				<table class="kdetails" style="margin: 30px 0 30px 0">
					<tbody>
						<tr>
							<td class="label">Label</td>
							<td class="value">${obj.label}</td>
							<td class="sep"></td>
							<td class="label">Plural label</td>
							<td class="value">${obj.pluralLabel}</td>
						</tr>
						<tr>
							<td class="label">API name</td>
							<td class="value">${obj.apiName}</td>
							<td class="sep"></td>
							<td colspan="2"></td>
						</tr>
					</tbody>
				</table>
			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
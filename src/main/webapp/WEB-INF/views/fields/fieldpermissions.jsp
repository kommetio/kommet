<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Field Permissions">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<div style="width:100%; text-align:left; margin-bottom: 20px">
				<a href="${pageContext.request.contextPath}/km/field/${field.KID}">Back to ${field.label}</a>
			</div>
		
			<div class="section-title">Profile access to field ${field.label}</div>
		
			<form method="post" action="${pageContext.request.contextPath}/km/fieldpermissions/save">
				<input type="hidden" name="fieldId" value="${field.KID}" />
				<table class="kdetails" style="margin: 30px 0 30px 0">
					<tbody>
						<c:forEach items="${permissions}" var="perm">
							<tr>
								<td class="label">${perm.profile.name}</td>
								<td class="value">
									<input type="checkbox" name="perm_${perm.profile.id}_read" value="true" <c:if test="${perm.read == true}">checked="checked"</c:if> <c:if test="${canEdit != true}">disabled="disabled"</c:if>/>read
									<input type="checkbox" name="perm_${perm.profile.id}_edit" value="true" <c:if test="${perm.edit == true}">checked="checked"</c:if> <c:if test="${canEdit != true}">disabled="disabled"</c:if>/>edit
								</td>
							</tr>
						</c:forEach>
					</tbody>
				</table>
				
				<c:if test="${canEdit == true}">
				<input type="submit" value="Save" />
				<input type="button" class="sbtn" onclick="openUrl('${pageContext.request.contextPath}/km/field/${field.KID}')" value="Cancel" />
				</c:if>
			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
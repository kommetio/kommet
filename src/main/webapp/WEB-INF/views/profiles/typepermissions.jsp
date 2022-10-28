<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Object Permissions">
	<jsp:body>
	
		<style>
		
			a.field-link {
				margin: 0 40px 0 20px;
			}
		
		</style>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<div style="width:100%; text-align:left; margin-bottom: 20px">
				<a href="${pageContext.request.contextPath}/km/profile/${profile.id}">Back to profile ${profile.name}</a>
			</div>
			<ko:pageHeader>Type permissions for profile ${profile.name}</ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/profiles/edittypepermissions/${profile.id}" class="sbtn">Edit</a>
		
			<input type="hidden" name="profileId" value="${profile.id}" />
			<table class="kdetails" style="margin: 30px 0 30px 0" id="type-permissions">
				<tbody>
					<c:forEach items="${typePermissions}" var="perm">
						<tr>
							<td class="label">${perm.type.label}</td>
							<td class="value">
								<a class="field-link" href="${pageContext.request.contextPath}/km/profiles/fieldpermissions?typeId=${perm.type.KID}&profileId=${profile.id}">fields</a>
							</td>
							<td class="value">
								<input type="checkbox" disabled name="perm_${perm.type.KID}_read" value="true" class="read" <c:if test="${perm.permission.read == true}">checked="checked"</c:if>/>read
							</td>
							<td class="value">
								<input type="checkbox" disabled name="perm_${perm.type.KID}_edit" value="true" class="edit" <c:if test="${perm.permission.edit == true}">checked="checked"</c:if>/>edit
							</td>
							<td class="value">
								<input type="checkbox" disabled name="perm_${perm.type.KID}_delete" value="true" class="delete" <c:if test="${perm.permission.delete == true}">checked="checked"</c:if>/>delete
							</td>
							<td class="value">
								<input type="checkbox" disabled name="perm_${perm.type.KID}_create" value="true" class="create" <c:if test="${perm.permission.create == true}">checked="checked"</c:if>/>create
							</td>
							<td class="value">
								<input type="checkbox" disabled name="perm_${perm.type.KID}_readAll" value="true" class="readAll" <c:if test="${perm.permission.readAll == true}">checked="checked"</c:if>/>read all
							</td>
							<td class="value">
								<input type="checkbox" disabled name="perm_${perm.type.KID}_editAll" value="true" class="editAll" <c:if test="${perm.permission.editAll == true}">checked="checked"</c:if>/>edit all
							</td>
							<td class="value">
								<input type="checkbox" disabled name="perm_${perm.type.KID}_deleteAll" value="true" class="deleteAll" <c:if test="${perm.permission.deleteAll == true}">checked="checked"</c:if>/>delete all
							</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
			
			<form action="${pageContext.request.contextPath}/km/profile/clonepermissions" method="post">
				<input type="submit" value="Clone from profile" class="sbtn" />
				<select name="sourceProfileId">
					<c:forEach var="p" items="${profiles}">
						<c:if test="${p.id != profile.id}">
							<option value="${p.id}">${p.name}</option>
						</c:if>
					</c:forEach>
				</select>
				<input type="hidden" name="destProfileId" value="${profile.id}" />
			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
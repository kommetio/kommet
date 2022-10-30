<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Object Permissions">
	<jsp:body>
	
		<style>
		
			a.field-link {
				margin: 0 40px 0 20px;
			}
			
			#profile-perm th.cbc, #profile-perm td.cbc {
				padding: 5px;
				min-width: 80px;
			}
		
		</style>
	
		<script type="text/javascript">
		
		function toggleAll(id, cssClass)
		{
			var checkAll = $('#' + id).prop('checked');
			
			$('input.' + cssClass).each(function(index) {
				$(this).prop('checked', checkAll);
			});
		}
		
		</script>
	
		<div class="ibox">
		
			<div style="width:100%; text-align:left; margin-bottom: 20px">
				<a href="${pageContext.request.contextPath}/km/profile/${profile.id}">Back to profile ${profile.name}</a>
			</div>
			<ko:pageHeader>Object permissions for profile ${profile.name}</ko:pageHeader>
		
			<form method="post" action="${pageContext.request.contextPath}/km/profiles/typepermissions/save">
				
				<input type="submit" class="sbtn" value="Save">
				<a href="${pageContext.request.contextPath}/km/profiles/typepermissions/${profile.id}" class="sbtn">Cancel</a>
				
				<input type="hidden" name="profileId" value="${profile.id}" />
				<table id="profile-perm" class="kdetails" style="margin: 30px 0 30px 0" id="type-permissions">
					<thead>
						<tr>
							<th></th>
							<th></th>
							<th class="cbc"><input type="checkbox" id="select-all-read" onclick="toggleAll('select-all-read', 'read')" /></th>
							<th class="cbc"><input type="checkbox" id="select-all-edit" onclick="toggleAll('select-all-edit', 'edit')" /></th>
							<th class="cbc"><input type="checkbox" id="select-all-delete" onclick="toggleAll('select-all-delete', 'delete')" /></th>
							<th class="cbc"><input type="checkbox" id="select-all-create" onclick="toggleAll('select-all-create', 'create')" /></th>
							<th class="cbc"><input type="checkbox" id="select-all-readAll" onclick="toggleAll('select-all-readAll', 'editAll')" /></th>
							<th class="cbc"><input type="checkbox" id="select-all-editAll" onclick="toggleAll('select-all-editAll', 'readAll')" /></th>
							<th class="cbc"><input type="checkbox" id="select-all-deleteAll" onclick="toggleAll('select-all-deleteAll', 'deleteAll')" /></th>
						</tr>
					</thead>
					<tbody>
						<c:forEach items="${typePermissions}" var="perm">
							<tr>
							<td class="label">${perm.type.label}</td>
							<td class="value">
								<a class="field-link" href="${pageContext.request.contextPath}/km/profiles/fieldpermissions?typeId=${perm.type.KID}&profileId=${profile.id}">fields</a>
							</td>
							<td class="cbc">
								<input type="checkbox" name="perm_${perm.type.KID}_read" value="true" class="read" <c:if test="${perm.permission.read == true}">checked="checked"</c:if>/>read
							</td>
							<td class="cbc">
								<input type="checkbox" name="perm_${perm.type.KID}_edit" value="true" class="edit" <c:if test="${perm.permission.edit == true}">checked="checked"</c:if>/>edit
							</td>
							<td class="cbc">
								<input type="checkbox" name="perm_${perm.type.KID}_delete" value="true" class="delete" <c:if test="${perm.permission.delete == true}">checked="checked"</c:if>/>delete
							</td>
							<td class="cbc">
								<input type="checkbox" name="perm_${perm.type.KID}_create" value="true" class="create" <c:if test="${perm.permission.create == true}">checked="checked"</c:if>/>create
							</td>
							<td class="cbc">
								<input type="checkbox" name="perm_${perm.type.KID}_readAll" value="true" class="readAll" <c:if test="${perm.permission.readAll == true}">checked="checked"</c:if>/>read all
							</td>
							<td class="cbc">
								<input type="checkbox" name="perm_${perm.type.KID}_editAll" value="true" class="editAll" <c:if test="${perm.permission.editAll == true}">checked="checked"</c:if>/>edit all
							</td>
							<td class="cbc">
								<input type="checkbox" name="perm_${perm.type.KID}_deleteAll" value="true" class="deleteAll" <c:if test="${perm.permission.deleteAll == true}">checked="checked"</c:if>/>delete all
							</td>
						</tr>
						</c:forEach>
					</tbody>
				</table>
				
				<input type="submit" value="Save" />
				<a href="${pageContext.request.contextPath}/km/profiles/typepermissions/${profile.id}" class="sbtn">Cancel</a>
			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
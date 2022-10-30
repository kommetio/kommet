<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Permissions">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				
				if ("${afterSave}" === "true")
				{
					km.js.ui.statusbar.show("Permissions saved successfully", 5000);
				}
				
				$("#applyForFieldsBtn").click(function() {
					$("#applyForFields").val("true");
				});
				
			});
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<div class="section-title">Profile access to object ${object.label}</div>
		
			<form method="post" action="${pageContext.request.contextPath}/km/typepermissions/save">
				<input type="hidden" name="objectId" value="${object.KID}" />
				<input type="hidden" name="applyForFields" id="applyForFields" />
				<table class="kdetails" style="margin: 30px 0 30px 0">
					<tbody>
						<c:forEach items="${permissions}" var="perm">
							<tr>
								<td class="label">${perm.profile.label}</td>
								<td class="value">
									<input type="checkbox" name="perm_${perm.profile.id}_read" value="true" <c:if test="${perm.read == true}">checked="checked"</c:if>/>read
									<input type="checkbox" name="perm_${perm.profile.id}_edit" value="true" <c:if test="${perm.edit == true}">checked="checked"</c:if>/>edit
									<input type="checkbox" name="perm_${perm.profile.id}_delete" value="true" <c:if test="${perm.delete == true}">checked="checked"</c:if>/>delete
									<input type="checkbox" name="perm_${perm.profile.id}_create" value="true" <c:if test="${perm.create == true}">checked="checked"</c:if>/>create
									<input type="checkbox" name="perm_${perm.profile.id}_readAll" value="true" <c:if test="${perm.readAll == true}">checked="checked"</c:if>/>read all
									<input type="checkbox" name="perm_${perm.profile.id}_editAll" value="true" <c:if test="${perm.editAll == true}">checked="checked"</c:if>/>edit all
									<input type="checkbox" name="perm_${perm.profile.id}_deleteAll" value="true" <c:if test="${perm.deleteAll == true}">checked="checked"</c:if>/>delete all
								</td>
							</tr>
						</c:forEach>
					</tbody>
				</table>
				
				<a href="${pageContext.request.contextPath}/km/type/${object.keyPrefix}" class="sbtn km-back-btn">Back to ${object.label} Definition</a>
				<input type="submit" value="Apply" />
				<input type="submit" value="Apply for object and fields" id="applyForFieldsBtn" />
				<a href="${pageContext.request.contextPath}/km/type/${object.keyPrefix}" class="sbtn">Cancel</a>
			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
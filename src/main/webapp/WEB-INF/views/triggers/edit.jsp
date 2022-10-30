<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit trigger">
	<jsp:body>
	
		<style>
		
			ul#triggerPlacements {
				list-style: none;
				padding: 0;
			}
		
		</style>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<ko:pageHeader>Edit trigger for type ${type.label}</ko:pageHeader>
			
			<form method="post" action="${pageContext.request.contextPath}/km/triggers/save">
				<input type="hidden" name="typeId" value="${type.KID}" />
				<input type="hidden" name="typeTriggerId" value="${trigger.id}" />
				
				<ko:buttonPanel><input type="submit" value="Save" /></ko:buttonPanel>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Class"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="kollFileId">
								<c:forEach var="file" items="${kollFiles}">
									<option value="${file.id}">${file.name}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<%--<ko:propertyRow>
						<ko:propertyLabel value="Placement"></ko:propertyLabel>
						<ko:propertyValue>
							<ul id="triggerPlacements">
								<li><input type="checkbox" name="isBeforeInsert" value="true" <c:if test="${trigger.isBeforeInsert == true}"> checked</c:if>/>Before Insert</li>
								<li><input type="checkbox" name="isBeforeUpdate" value="true" <c:if test="${trigger.isBeforeUpdate == true}"> checked</c:if>/>Before Update</li>
								<li><input type="checkbox" name="isBeforeDelete" value="true" <c:if test="${trigger.isBeforeDelete == true}"> checked</c:if>/>Before Delete</li>
								<li><input type="checkbox" name="isAfterInsert" value="true" <c:if test="${trigger.isAfterInsert == true}"> checked</c:if>/>After Insert</li>
								<li><input type="checkbox" name="isAfterUpdate" value="true" <c:if test="${trigger.isAfterUpdate == true}"> checked</c:if>/>After Update</li>
							</ul>
						</ko:propertyValue>
					</ko:propertyRow>--%>
					<ko:propertyRow>
						<ko:propertyLabel value="Is Active"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="isActive" value="true" <c:if test="${trigger.isActive == true}"> checked</c:if> />
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
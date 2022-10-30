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
		
		<%--
		<script>

			function unregisterTrigger()
			{
				$.post("${pageContext.request.contextPath}/km/trigger/unregister/${typeTrigger.id}", function(data) {

					if (data.status == "success")
					{
						showMsg("warn-prompt", "Trigger unregistered", "info");
						$("#trigger-props").remove();
					}
					else
					{
						showMsg("warn-prompt", data.messages, "error");
					}
					
				}, "json");
			}
		
		</script>
		--%>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
			<div id="warn-prompt"></div>
		
			<ko:pageHeader>Trigger ${typeTrigger.triggerFile.name}</ko:pageHeader>
				
			<ko:propertyTable id="trigger-props">
				<ko:propertyRow>
					<ko:propertyLabel value="Class"></ko:propertyLabel>
					<ko:propertyValue><a href="${pageContext.request.contextPath}/km/classes/${typeTrigger.triggerFile.id}">${typeTrigger.triggerFile.qualifiedName}</a></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Placement"></ko:propertyLabel>
					<ko:propertyValue>
						<ul id="triggerPlacements">
							<li><input type="checkbox" disabled name="isBeforeInsert" value="true" <c:if test="${typeTrigger.isBeforeInsert == true}"> checked</c:if>/>Before Insert</li>
							<li><input type="checkbox" disabled name="isBeforeUpdate" value="true" <c:if test="${typeTrigger.isBeforeUpdate == true}"> checked</c:if>/>Before Update</li>
							<li><input type="checkbox" disabled name="isBeforeDelete" value="true" <c:if test="${typeTrigger.isBeforeDelete == true}"> checked</c:if>/>Before Delete</li>
							<li><input type="checkbox" disabled name="isAfterInsert" value="true" <c:if test="${typeTrigger.isAfterInsert == true}"> checked</c:if>/>After Insert</li>
							<li><input type="checkbox" disabled name="isAfterUpdate" value="true" <c:if test="${typeTrigger.isAfterUpdate == true}"> checked</c:if>/>After Update</li>
							<li><input type="checkbox" disabled name="isAfterUpdate" value="true" <c:if test="${typeTrigger.isAfterDelete == true}"> checked</c:if>/>After Delete</li>
						</ul>
					</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Is Active"></ko:propertyLabel>
					<ko:propertyValue>
						<input type="checkbox" disabled name="isActive" value="true" <c:if test="${typeTrigger.isActive == true}"> checked</c:if> />
					</ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
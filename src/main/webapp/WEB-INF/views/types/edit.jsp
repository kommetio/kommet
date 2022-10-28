<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${title}">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				
				km.js.utils.bind($("#typeName"), $("#typeTitle"), "${title}");
				km.js.utils.openMenuItem("Data Types");
				
				km.js.ui.autoFormatName({
					target: $("input[name='apiName']")
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='apiName']"),
					text: "Name that will be used to identify the object in Java code. Must start with a capital letter."
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='packageName']"),
					text: "Package of the Java type representing this object. E.g. com.office.types, myapp.objects"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='label']"),
					text: "Name of the object that will be displayed to users"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='pluralLabel']"),
					text: "Plural name of the object that will be displayed to users"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='combineRecordAndCascadeSharing']"),
					text: "If this option is selected and the object has property sharingControlledByParent set, the sharing settings specific to this record are combined with the sharings inherited from parent records"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='labelKey']"),
					text: "Key of the user setting whose value will be used as a label for this object"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='pluralLabelKey']"),
					text: "Key of the user setting whose value will be used as a plural label for this object"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("select[id='defaultField']"),
					text: "The main field that will be used to identify records of this object on record views, lists etc."
				});
				
			});
		
		</script>
			
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
			
			<div id="typeTitle" class="km-title">${title}</div>
		
			<form method="post" action="${pageContext.request.contextPath}/km/type/save">
				<input type="hidden" name="kObjectId" value="${type.KID}" />
				
				<ko:propertyTable>
					
					<ko:propertySection title="Object properties">
				
						<ko:propertyRow>
							<ko:propertyLabel value="API name" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="apiName" value="${type.apiName}" id="typeName" />
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow>
							<ko:propertyLabel value="Package" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="packageName" value="${type['package']}" />
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow>
							<ko:propertyLabel value="Label" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="label" value="${type.label}"/>
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow>
							<ko:propertyLabel value="Plural label" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="pluralLabel" value="${type.pluralLabel}" />
							</ko:propertyValue>
						</ko:propertyRow>
						<c:if test="${not empty type.KID}">
							<ko:propertyRow>
								<ko:propertyLabel value="Default field"></ko:propertyLabel>
								<ko:propertyValue>
									<select name="defaultField" id="defaultField">
										<c:forEach var="field" items="${type.fields}">
											<option value="${field.KID}"<c:if test="${type.defaultFieldId == field.KID}"> selected</c:if>>${field.label}</option>
										</c:forEach>
									</select>
								</ko:propertyValue>
							</ko:propertyRow>
						</c:if>
					
					</ko:propertySection>
					
					<ko:propertySection title="Additional options">
					
						<ko:propertyRow>
							<ko:propertyLabel value="Label Key"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="labelKey" value="${type.uchLabel}"/>
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow>
							<ko:propertyLabel value="Plural label key"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="pluralLabelKey" value="${type.uchPluralLabel}" />
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow>
							<ko:propertyLabel value="Description"></ko:propertyLabel>
							<ko:propertyValue>
								<textarea name="description">${type.description}</textarea>
							</ko:propertyValue>
						</ko:propertyRow>
						<c:if test="${not empty type.KID}">
							<ko:propertyRow>
								<ko:propertyLabel value="Sharing controlled by field"></ko:propertyLabel>
								<ko:propertyValue>
									<select name="sharingControlledByField" id="sharingControlledByField">
										<option value=""></option>
										<c:forEach var="field" items="${type.fields}">
											<c:if test="${(field.dataTypeId == 4 || field.dataTypeId == 6) && field.apiName != 'id'}">
											<option value="${field.KID}"<c:if test="${type.sharingControlledByFieldId == field.KID}"> selected</c:if>>${field.label}</option>
											</c:if>
										</c:forEach>
									</select>
								</ko:propertyValue>
							</ko:propertyRow>
						</c:if>
						<ko:propertyRow>
							<ko:propertyLabel value="Combine record and cascade sharing" required="false"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="checkbox" value="true" name="combineRecordAndCascadeSharing" <c:if test="${type.combineRecordAndCascadeSharing == true}"> checked</c:if> />
							</ko:propertyValue>
						</ko:propertyRow>
					
					</ko:propertySection>
					
				</ko:propertyTable>
				
				<ko:buttonPanel>
					<input type="submit" value="Save" id="saveTypeBtn" />
					<input type="button" class="sbtn" onclick="openUrl('${pageContext.request.contextPath}/km/types/list')" value="Cancel" />
				</ko:buttonPanel>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${field.label} field" importRMJS="true">
	<jsp:body>
	
		<style>
		
			ul#enum-values {
				list-style-type: none;
  				padding: 0;
  				margin: 0;
			}
			
			ul#enum-values > li {
				padding: 4px;
				border: 1px solid #ddd;
				border-radius: 2px;
				margin-bottom: 5px;
				background-color: rgb(249, 255, 197);
			}
		
		</style>
		
		<script>
		
		$(document).ready(function() {
			km.js.ui.bool($("table#field-details td.bool-icon"));
			km.js.utils.openMenuItem("Objects");
		});
		
		function deleteField()
		{
			$.post("${pageContext.request.contextPath}/km/field/delete", { id: "${field.KID}", typePrefix: "${field.type.keyPrefix}" }, function(data) {
				
				if (data.success)
				{
					// redirect to type details
					km.js.utils.openURL(km.js.config.contextPath + "/km/type/${field.type.keyPrefix}");
				}
				else
				{
					km.js.ui.statusbar.err(data.message);
				}
				
			}, "json");
		}
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox" >
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
			
			<ko:pageHeader>${field.label}
				<ko:pageSubheader>Details of field ${field.label}</ko:pageSubheader>
			</ko:pageHeader>
			
			<a href="${pageContext.request.contextPath}/km/type/${field.type.keyPrefix}" class="sbtn">Back to ${field.type.label} Definition</a>
			
			<c:if test="${canEdit == true}">
				<a href="${pageContext.request.contextPath}/km/field/edit/${field.KID}" class="sbtn">Edit</a>
				<a href="javascript:;" onclick="ask('Are you sure you want to delete this field?', 'warnPrompt', function() { deleteField(); })" id="deleteFieldBtn" class="sbtn">Delete</a>
			</c:if>
			<a href="${pageContext.request.contextPath}/km/fieldpermissions/${field.KID}" class="sbtn">Permissions</a>
			
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<form method="post" action="${pageContext.request.contextPath}/km/field/save">
				<input type="hidden" name="fieldId" value="${field.KID}" />
				<table class="kdetails" id="field-details" style="margin: 30px 0 30px 0">
					<tbody>
						<tr>
							<td class="label">API name</td>
							<td class="value">${field.apiName}</td>
							<td class="sep"></td>
							<td class="label">Label</td>
							<td class="value">${field.label}</td>
						</tr>
						<tr>
							<td class="label">Label Key</td>
							<td class="value">${field.uchLabel}</td>
							<td class="sep"></td>
							<td class="label"></td>
							<td class="value"></td>
						</tr>
						<tr>
							<td class="label">Object</td>
							<td class="value">${field.type.label}</td>
							<td class="sep"></td>
							<td class="label">Data Type</td>
							<td class="value">${field.dataType.name}
								<c:if test="${field.dataType.id == 0}">
								(${field.dataType.decimalPlaces} decimal places)
								</c:if>
								<c:if test="${field.dataType.id == 6}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.type.keyPrefix}">${field.dataType.type.label}</a>
								</c:if>
								<c:if test="${field.dataType.id == 8}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.inverseType.keyPrefix}">${field.dataType.inverseType.label}</a>
								</c:if>
								<c:if test="${field.dataType.id == 10}">
								to <a href="${pageContext.request.contextPath}/km/type/${field.dataType.associatedType.keyPrefix}">${field.dataType.associatedType.label}</a>
								<c:if test="${field.dataType.linkingType.isAutoLinkingType() == false}">
								through <a href="${pageContext.request.contextPath}/km/type/${field.dataType.linkingType.keyPrefix}">${field.dataType.linkingType.label}</a>
								</c:if>
								</c:if>
							</td>
						</tr>
						<c:if test="${field.dataType.id == 0}">
						<tr>
							<td class="label">Java representation</td>
							<td class="value">${field.dataType.javaType}</td>
							<td class="sep"></td>
							<td class="label"></td>
							<td class="value"></td>
						</tr>
						</c:if>
						<c:if test="${field.dataType.id == 8 || field.dataType.id == 10}">
							<td class="label">Display collection as</td>
							<td class="value">${collectionDisplay}</td>
							<td class="sep"></td>
							<td class="label"></td>
							<td class="value"></td>
						</c:if>
						<c:if test="${field.dataType.id == 1}">
						<tr>
							<td class="label">Display as</td>
							<td class="value">
								<c:if test="${field.dataType.isLong() == true}">
								text area
								</c:if>
								<c:if test="${field.dataType.isLong() == false}">
								single line input
								</c:if>
							</td>
							<td class="sep"></td>
							<td class="label">Is Formatted</td>
							<td class="value bool-icon">${field.dataType.isFormatted()}</td>
						</tr>
						</c:if>
						<c:if test="${field.dataType.id == 14}">
						<tr>
							<td class="label">AutoNumber format</td>
							<td class="value">${field.dataType.format}</td>
							<td class="sep"></td>
							<td class="label"></td>
							<td class="value bool-icon"></td>
						</tr>
						</c:if>
						<tr>
							<td class="label">Required</td>
							<td class="value bool-icon">${isRequired}</td>
							<td class="sep"></td>
							<td class="label">Unique</td>
							<td class="value bool-icon">${isUnique}</td>
						</tr>
						<tr>
							<td class="label">Cascade delete</td>
							<td class="value bool-icon">${isCascadeDelete}</td>
							<td class="sep"></td>
							<td class="label">Track history</td>
							<td class="value bool-icon">${field.trackHistory}</td>
						</tr>
						<tr>
							<td class="label">Default value</td>
							<td class="value"><c:out value="${field.defaultValue}" /></td>
							<td class="sep"></td>
							<c:if test="${field.dataType.name == 'Enumeration'}">
							<td class="label">Validate enumeration</td>
							<td class="value">${field.dataType.validateValues}</td>
							</c:if>
							<c:if test="${field.dataType.name != 'Enumeration'}">
							<td class="label"></td>
							<td class="value"></td>
							</c:if>
						</tr>
						<tr>
							<td class="label">Description</td>
							<td class="value" colspan="3">${field.description}</td>
						</tr>
					</tbody>
				</table>
				
				<c:if test="${field.dataType.name == 'Enumeration'}">
					<h3>Enumeration values</h3>
					
					<div>
						<ul id="enum-values">
							<c:forEach var="val" items="${enumValues}">
							<li>${val}</li>
							</c:forEach>
						</ul>
					</div>
									
				</c:if>
				
			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
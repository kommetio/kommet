<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-lightness/jquery-ui-1.10.3.custom.min.css" />

<style>
	
	ul"km-uc-field-list {
		list-style: none;
		padding: 0;
		margin: 0;
	}
	
	ul"km-uc-field-list > li {
		display: inline-block;
		border-radius: 2px;
		padding: 0.3em;
		border: 1px solid #ccc;
		background-color: #eee;
	}
	
</style>

<table class="std-table" id="ucList">
	<thead>
		<tr class="cols">
			<th>Name</th>
			<th>Fields</th>
			<th>Created Date</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${ucs}" var="uc">
			<c:if test="${uc.isSystem != true}">
				<tr>
					<td><a href="${pageContext.request.contextPath}/km/uniquechecks/${uc.id}">${uc.name}</a></td>
					<td>
						<ul id="km-uc-field-list">
						<c:forEach var="fieldId" items="${uc.parsedFieldIds}">
							<li>${fieldsById[fieldId].apiName}</li>
						</c:forEach>
						</ul> 
					</td>
					<td><km:dateTime value="${uc.createdDate}" format="dd-MM-yyyy HH:mm:ss" /></td>
				</tr>
			</c:if>
		</c:forEach>
	</tbody>
</table>
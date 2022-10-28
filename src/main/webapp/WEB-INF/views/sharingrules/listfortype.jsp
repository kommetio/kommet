<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-lightness/jquery-ui-1.10.3.custom.min.css" />

<table class="std-table" id="srList">
	<thead>
		<tr class="cols">
			<th>Name</th>
			<th>Shared with</th>
			<th>Created</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${sharingRules}" var="sr">
			<tr>
				<td><a href="${pageContext.request.contextPath}/km/classes/${sr.file.id}">${sr.name}</a></td>
				<td>${sr.sharedWith}</td>
				<td><km:dateTime value="${sr.createdDate}" format="dd-MM-yyyy HH:mm:ss" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
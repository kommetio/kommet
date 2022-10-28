<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-lightness/jquery-ui-1.10.3.custom.min.css" />

<table class="std-table" id="vrList">
	<thead>
		<tr class="cols">
			<th>Name</th>
			<th>Is Active</th>
			<th>Created Date</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${vrs}" var="vr">
			<tr>
				<td><a href="${pageContext.request.contextPath}/km/validationrules/${vr.id}">${vr.name}</a></td>
				<td class="active-vr">${vr.active}</td>
				<td><km:dateTime value="${vr.createdDate}" format="dd-MM-yyyy HH:mm:ss" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
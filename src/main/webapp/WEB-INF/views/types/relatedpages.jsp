<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-lightness/jquery-ui-1.10.3.custom.min.css" />

<table class="std-table" id="relatedPageList">
	<thead>
		<tr class="cols">
			<th>Name</th>
			<th>View</th>
			<th>Controller method</th>
			<th>Created Date</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${pages}" var="page">
			<tr>
				<td><ko:pageDetailsLink name="${page.name}" id="${page.id}"/></td>
				<td><ko:viewDetailsLink name="${page.view.interpretedName}" id="${page.view.id}" /></td>
				<td>${page.controllerMethod}</td>
				<td><km:dateTime value="${page.createdDate}" format="dd-MM-yyyy HH:mm:ss" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
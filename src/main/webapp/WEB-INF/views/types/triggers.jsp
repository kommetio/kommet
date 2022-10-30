<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-lightness/jquery-ui-1.10.3.custom.min.css" />

<style>

	div.km-trigger-btns {
		margin: 1em 0;
	}
	
</style>

<div class="km-trigger-btns">
	<a href="${pageContext.request.contextPath}/km/ide?template=trigger&typeId=${typeId}" class="sbtn">Create trigger</a>
</div>

<table class="std-table" id="triggerList">
	<thead>
		<tr class="cols">
			<th>Class name</th>
			<th>Before insert</th>
			<th>Before update</th>
			<th>Before delete</th>
			<th>After delete</th>
			<th>After insert</th>
			<th>After update</th>
			<th>Is active</th>
			<th>Created</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${triggers}" var="trigger">
			<tr>
				<td><a href="${pageContext.request.contextPath}/km/trigger/${trigger.id}">${trigger.triggerFile.name}</a></td>
				<td>${trigger.isBeforeInsert}</td>
				<td>${trigger.isBeforeUpdate}</td>
				<td>${trigger.isBeforeDelete}</td>
				<td>${trigger.isAfterDelete}</td>
				<td>${trigger.isAfterInsert}</td>
				<td>${trigger.isAfterUpdate}</td>
				<td>${trigger.isActive}</td>
				<td><km:dateTime value="${trigger.createdDate}" format="dd-MM-yyyy HH:mm:ss" /></td>
			</tr>
		</c:forEach>
	</tbody>
</table>
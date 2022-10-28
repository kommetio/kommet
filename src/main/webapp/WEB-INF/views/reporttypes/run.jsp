<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${reportName}">

	<jsp:body>
	
		<div class="ibox">
			<ko:pageHeader>${reportName}</ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/reporttypes/${reportTypeId}" class="sbtn">${i18n.get('reports.btn.report.type.details')}</a>
			<c:if test="${canEdit == true}"><a href="${pageContext.request.contextPath}/km/reporttypes/edit/${reportTypeId}" class="sbtn">${i18n.get('btn.edit')}</a></c:if>
			
			<table class="std-table" style="margin-top: 20px;">
			
				<thead>
					<c:forEach var="group" items="${groupings}">
						<th>${group.value}</th>
					</c:forEach>
					<c:forEach var="field" items="${displayedColumns}">
						<th>${field.value}</th>
					</c:forEach>
				</thead>
				<tbody>
					<c:forEach var="rec" items="${records}">
						<tr>
							<c:forEach var="group" items="${groupings}">
								<td>${rec.getField(groupingsToSelectedProps.get(group.key))}</td>
							</c:forEach>
							<c:if test="${fn:length(groupings) > 0}">
								<c:forEach var="field" items="${displayedColumns}">
									<td>${rec.getAggregateValue(field.key)}</td>
								</c:forEach>
							</c:if>
							<c:if test="${fn:length(groupings) == 0}">
								<c:forEach var="field" items="${displayedColumns}">
									<td>${rec.getValue(field.key)}</td>
								</c:forEach>
							</c:if>
						</tr>
					</c:forEach>
				</tbody>
			
			</table>
			
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
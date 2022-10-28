<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Objects">

	<jsp:body>
	
		<div class="ibox">
	
			<a href="${pageContext.request.contextPath}/km/new/${obj.keyPrefix}" class="sbtn">New</a>
	
			<table class="std-table" style="margin-top: 30px">
				<thead>
					<tr class="cols">
						<c:forEach var="field" items="${listedFields}">
							<th>${field.label}</th>
						</c:forEach>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="record" items="${records}">
						<tr>
							<c:forEach var="field" items="${listedFields}">
								<td>${wgfn:propertyValue(field.apiName, record)}</td>
							</c:forEach>
							<%--<td><a href="${pageContext.request.contextPath}/km/${record.id}">${record.id}</a></td>
							 --%>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
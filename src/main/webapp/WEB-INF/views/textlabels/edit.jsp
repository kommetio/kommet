<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit text label">
	<jsp:body>
		
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/textlabels/save">
				<input type="hidden" name="labelId" value="${label.id}" />
				
				<ko:pageHeader>
					<c:if test="${empty template.id}">New text label</c:if>
					<c:if test="${not empty template.id}">Edit text label</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Key"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="key" value="${label.key}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Value"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="value" value="${label.value}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Locale"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="locale">
								<option value="">All</option>
								<option value="EN_US"<c:if test="${label.locale == 'EN_US'}"> selected</c:if>>Engligh (United States)</option>
								<option value="PL_PL"<c:if test="${label.locale == 'PL_PL'}"> selected</c:if>>Polski</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell>
							<input type="submit" value="Save" />
							<c:if test="${not empty label.id}">
								<a href="${pageContext.request.contextPath}/km/textlabels/${label.id}" class="sbtn">Cancel</a>
							</c:if>
							<c:if test="${empty label.id}">
								<a href="${pageContext.request.contextPath}/km/textlabels/list" class="sbtn">Cancel</a>
							</c:if>
						</ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
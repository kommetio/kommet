<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout titleKey="usergroup.edit.title">
	<jsp:body>

		<script>
		
			$(document).ready(function() {
				km.js.ui.autoFormatName({
					target: $("input[name='name']")
				});
			});
		
		</script>
		
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/usergroups/save">
				<input type="hidden" name="groupId" value="${group.id}" />
				
				<ko:pageHeader>
					<c:if test="${empty group.id}">${i18n.get('usergroup.new.title')}</c:if>
					<c:if test="${not empty group.id}">${i18n.get('usergroup.edit.title')}</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${group.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell>
							<input type="submit" value="${i18n.get('btn.save')}" />
							<c:if test="${not empty group.id}">
								<a href="${pageContext.request.contextPath}/km/usergroups/${group.id}" class="sbtn">${i18n.get('btn.cancel')}</a>
							</c:if>
							<c:if test="${empty group.id}">
								<a href="${pageContext.request.contextPath}/km/usergroups/list" class="sbtn">${i18n.get('btn.cancel')}</a>
							</c:if>
						</ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
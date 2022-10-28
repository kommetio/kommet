<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${profile.name}">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				km.js.ui.autoFormatName({
					target: $("input[name='name']")
				});
			});
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<form method="post" action="${pageContext.request.contextPath}/km/profile/save">
				<input type="hidden" name="profileId" value="${profile.id}" />
				
				<ko:pageHeader>
					<c:if test="${empty profile.id}"><kolmu:label key="profile.new.title" /></c:if>
					<c:if test="${not empty profile.id}">${profile.name}</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="profile.name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${profile.name}" />
						</ko:propertyValue>
						<ko:propertyLabel valueKey="profile.label" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="label" value="${profile.label}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="profile.landing.url" required="false"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="landingURL" value="${profileSettings.landingURL}" />
						</ko:propertyValue>
						<ko:propertyLabel></ko:propertyLabel>
						<ko:propertyValue>
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
				
				<ko:buttonPanel>
					<input type="submit" value="<kolmu:label key="btn.save" />" />
					<a class="sbtn" href="${pageContext.request.contextPath}/km/profiles/list"><kolmu:label key="btn.cancel" /></a>
				</ko:buttonPanel>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
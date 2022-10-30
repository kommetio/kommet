<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<tags:guestLayout>

	<jsp:body>
	
		<div class="ibox" style="margin: 50px 200px 30px 200px; <c:if test="${showForm == false}">height:100px;</c:if>">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			
			<c:if test="${showForm == true}">
				<h3>${i18n.get('auth.restore.pwd.title')} ${user.userName}</h3>
			
				<form method="post" action="${pageContext.request.contextPath}/km/users/dorestorepassword">
					<input type="hidden" name="hash" value="${hash}" />
					<input type="hidden" name="envId" value="${envId}" />
					<ko:propertyTable>
						<ko:propertyRow>
							<ko:propertyLabel value="${i18n.get('user.password')}" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="password" name="newPassword" />
							</ko:propertyValue>
						</ko:propertyRow>
						<ko:propertyRow>
							<ko:propertyLabel value="${i18n.get('user.repeatPassword')}" required="true"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="password" name="newPasswordRepeated" />
							</ko:propertyValue>
						</ko:propertyRow>
					</ko:propertyTable>
					
					<ko:buttonPanel>
						<input type="submit" value="${i18n.get('btn.save')}" class="sbtn" />
					</ko:buttonPanel>
	
				</form>
			</c:if>
		
		</div>
		
	</jsp:body>
	
</tags:guestLayout>
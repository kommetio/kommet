<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<tags:guestLayout>

	<jsp:body>
	
		<style>
		
			div#km-activate {
				width: 30rem;
				margin: 0 auto;
				text-align: center;
			}
			
			div.line {
				padding: 0.5rem 0;
			}
			
			div.line > input[type=password] {
				width: 20rem;
			}
		
		</style>
		
		<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
		
		<c:if test="${showForm == true}">
			<div id="km-activate">
				<div class="km-title">${i18n.get('auth.activate.user.title')} ${user.userName}</div>
			
				<form method="post" action="${pageContext.request.contextPath}/km/users/doactivate">
					<input type="hidden" name="hash" value="${hash}" />
					<input type="hidden" name="envId" value="${envId}" />
					<div class="line">
						<input type="password" name="newPassword" placeholder="Choose your password" class="km-input" />
					</div>
					<div class="line">
						<input type="password" name="newPasswordRepeated" placeholder="Confirm password" class="km-input" />
					</div>
					<div class="line">
						<input type="submit" value="Activate account" class="sbtn" />
					</div>
	
				</form>
			</div>
		</c:if>
		
	</jsp:body>
	
</tags:guestLayout>
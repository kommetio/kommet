<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<tags:guestLayout>

	<jsp:body>
	
		<style>
	
			div#forgot-pwd {
				margin: 50px 200px 30px 200px;
				font-size: 0.75em;
			}
		
		</style>
	
		<div class="ibox" id="forgot-pwd">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<h3>${i18n.get('auth.forgot.password.title')}</h3>
			${i18n.get('auth.forgot.password.msg')}
		
			<p style="margin-bottom:10px">&nbsp;</p>
			
			<form method="post" action="${pageContext.request.contextPath}/km/users/sendpasswordlink">
				<input type="hidden" name="envId" value="${envId}" />
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="${i18n.get('user.email')}" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="email" />
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
				
				<ko:buttonPanel>
					<input type="submit" value="${i18n.get('btn.send')}" class="sbtn" />
					<a href="${pageContext.request.contextPath}/km/login?env=${envId}" class="sbtn">${i18n.get('btn.back.to.login')}</a>
				</ko:buttonPanel>

			</form>
		
		</div>
		
	</jsp:body>
	
</tags:guestLayout>
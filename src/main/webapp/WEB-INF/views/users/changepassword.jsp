<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:userLayout layoutPath="${layoutPath}" titleKey="user.changingpasswordfor">
	<jsp:body>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" cssClass="std-msgs" />
			<kolmu:messages messages="${actionMsgs}" cssClass="std-msgs" />
		
			<div style="width:100%; text-align:left; margin-bottom: 20px">
				<a href="${pageContext.request.contextPath}/km/me"><kolmu:label key="user.backtomyprofile" /></a>
			</div>
		
			<ko:pageHeader><kolmu:label key="user.changingpasswordfor" /> ${user.userName}</ko:pageHeader>
		
			<form method="post" action="${pageContext.request.contextPath}/km/dochangepassword">
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="user.oldpassword" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="password" name="oldPassword" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="user.password" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="password" name="newPassword" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="user.repeatPassword" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="password" name="newPasswordRepeated" />
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
				
				<ko:buttonPanel>
					<input type="submit" value="${i18n.get('btn.save')}" class="sbtn" />
					<a class="sbtn" href="${pageContext.request.contextPath}/km/me">${i18n.get('btn.cancel')}</a>
				</ko:buttonPanel>

			</form>
		
		</div>
		
	</jsp:body>
</ko:userLayout>
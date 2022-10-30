<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:userLayout layoutPath="${layoutPath}" titleKey="mail.newmessage.title">

	<jsp:body>
	
		<style>
		
			.new-msg input[type="text"], .new-msg textarea {
				width: 80%;
				font-size: 12px;
			}
			
			.new-msg textarea {
				height: 200px;
			}
		
		</style>
	
		<div class="box">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
				
			<ko:pageHeader><kolmu:label key="mail.newmessage.title" /></ko:pageHeader>
			
			<form action="${pageContext.request.contextPath}/km/mail/send" method="POST">
				
				<ko:propertyTable cssClass="new-msg">
					<ko:propertyRow>
						<ko:propertyLabel valueKey="mail.subject"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="subject" value="${subject}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="mail.to"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="recipients" value="${recipients}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel valueKey="mail.content"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="content">${content}</textarea>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="<kolmu:label key="mail.send" />" /></ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>
				
 			</form>
			
		</div>
	
	</jsp:body>
	
</ko:userLayout>
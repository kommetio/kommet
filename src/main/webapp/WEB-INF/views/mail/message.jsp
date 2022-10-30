<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:userLayout layoutPath="${layoutPath}" title="${subject}">

	<jsp:body>
	
		<style>
		
			div#email-text > pre {
				font-size: 12px;
				border: 1px solid #ccc;
				background-color: #F8F8F8;
				padding: 10px;
				margin-top: 20px;
				border-radius: 2px;
				font-family: Arial, Verdana;
				line-height: 150%;
			}
		
		</style>
	
		<div class="box">
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
				
			<ko:pageHeader>${subject}</ko:pageHeader>
				
			<ko:propertyTable>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="mail.subject"></ko:propertyLabel>
					<ko:propertyValue>${email.subject}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel valueKey="mail.to"></ko:propertyLabel>
					<ko:propertyValue>${email.recipients}</ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
			
			<div id="email-text"><pre>${email.plainTextBody}</pre></div>
			
		</div>
	
	</jsp:body>
	
</ko:userLayout>
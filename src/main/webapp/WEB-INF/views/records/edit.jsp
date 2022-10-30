<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="New object">
	<jsp:body>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}" />
			<kolmu:messages messages="${actionMsgs}" />
		
			<form method="post" action="${pageContext.request.contextPath}/km/record/save">
				<input type="hidden" name="recordId" value="${record.id}" />
				<input type="hidden" name="keyPrefix" value="${obj.keyPrefix}" />
				
				<ko:propertyTable>
					
					<c:forEach var="field" items="${editableFields}">
						<ko:propertyRow>
							<ko:propertyLabel value="${field.label}"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="text" name="field_${field.apiName}"<c:if test="${field.required == true}"> class="req"</c:if>/>
							</ko:propertyValue>
						</ko:propertyRow>
					</c:forEach>
				
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
			
		</div>
		
	</jsp:body>
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit layout settings">
	<jsp:body>
	
		<div class="ibox">
		
			<ko:pageHeader>Layout settings</ko:pageHeader>
		
			<form method="post" action="${pageContext.request.contextPath}/km/layouts/savesettings">
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Default layout" />
						<ko:propertyValue><input type="text" name="defaultLayout" value="${defaultLayout}" /></ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Save" /></ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>
			</form>
		</div>
	
	</jsp:body>
</ko:homeLayout>
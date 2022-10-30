<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="New type">

	<jsp:body>
	
		<div class="box">
			<div class="head1">New type</div>
			<div class="grey-hint">Create new type to store your data</div>
			
			<form action="${pageContext.request.contextPath}/km/objects/docreate" method="POST">
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="API name"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="apiName" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Package"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="package" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Label"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="label" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Plural label"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="pluralLabel" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell><input type="submit" value="Create" /></ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>
				
 			</form>
			
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
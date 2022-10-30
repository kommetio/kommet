<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Error Log">
	<jsp:body>
	
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<style>
		
			.err-details > pre {
				font-family: Arial, Verdana;
			}
		
		</style>
		
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
				
			<ko:pageHeader>Error Log</ko:pageHeader>
			
			<ko:propertyTable>
				<ko:propertyRow>
					<ko:propertyLabel value="Message"></ko:propertyLabel>
					<ko:propertyValue>${log.message}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Date"></ko:propertyLabel>
					<ko:propertyValue><km:dateTime value="${log.createdDate}" format="dd-MM-yyyy HH:mm:ss" /></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Severity"></ko:propertyLabel>
					<ko:propertyValue>${log.severity}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Class"></ko:propertyLabel>
					<ko:propertyValue>${log.codeClass}, ${log.codeLine}</ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Details"></ko:propertyLabel>
					<ko:propertyValue id="err-details"><pre>${log.details}</pre></ko:propertyValue>
				</ko:propertyRow>
			</ko:propertyTable>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
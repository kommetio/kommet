<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Generate field ID">

	<jsp:body>
	
		<style>
		
			textarea.query {
				width: 500px;
				height: 100px;
				font-family: Courier, Arial, Verdana;
			}
			
			div#result {
				border: 1px solid #ccc;
				border-radius: 2px;
				padding: 10px;
				font-family: Courier, Arial, Verdana;
				background-color: #F6F3F3;
				display: none;
			}
		
		</style>
		
		<script>
		
			function generateFieldId()
			{
				$.get("${pageContext.request.contextPath}/km/dogeneratefieldid", function(data) {
					$("#newid").val(data.data.id);
				}, "json");
			}
		
		</script>
	
			<div class="ibox">
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
					
			<ko:pageHeader>Generate field ID</ko:pageHeader>
			
			<input type="button" class="sbtn" onclick="generateFieldId()" value="Generate"></input>
			<input type="text" id="newid" disabled="disabled"></input>
		
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
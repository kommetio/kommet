<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:processBuilderLayout title="View editor">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				loadPreview("${view.id}");
			});
			
			function loadPreview(viewId)
			{
				$.get(km.js.config.contextPath + "/km/views/preview", { viewId: viewId }, function(data) {
					
					$("#preview").html(data);
					
				});
			}
		
		
		</script>
	
		<div>
		
			<div id="toolbar">
			
			</div>
			
			<div id="preview">
			
				
			
			</div>
		
		</div>
	
	</jsp:body>
</ko:processBuilderLayout>
	

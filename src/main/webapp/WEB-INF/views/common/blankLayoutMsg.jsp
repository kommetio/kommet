<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="false" %>

<ko:blankLayout title="Message">

	<style>
	
		#exception {
			margin-top: 30px;
		}
		
		#exception ul.stacktrace {
			display: none;
		}
	
	</style>

	<div class="ibox" style="height: 100px">
		<kolmu:errors messages="${errorMsgs}" />
		<kolmu:messages messages="${actionMsgs}" />
		
		<c:if test="${exception != null}">
			<div id="exception">
				<div class="msg">${exception.message}</div>
				<ul class="stacktrace">
					
				</ul>
			</div>
		</c:if>
	</div>
	
	<script>
		
		$(document).ready(function() {
			
			$("#exception .msg").click(function() {
				$("#exception .stacktrace").toggle();
			});
			
		});
	
	</script>
	
</ko:blankLayout>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page session="false" %>

<ko:errorLayout title="Message">

	<style>
		
		#exception ul.stacktrace {
			display: none;
		}
		
		div.msg {
			font-size: 0.75rem;
			margin: 1rem 0 2rem 0;
		}
	
	</style>

	<div class="ibox">
		<kolmu:errors messages="${errorMsgs}" />
		<kolmu:messages messages="${actionMsgs}" />
		
		<div class="msg">${exception.message}</div>
		
		<a href="${pageContext.request.contextPath}/km/me">Back to home page</a>
		
		<c:if test="${exception != null}">
			<div id="exception">
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
	
</ko:errorLayout>
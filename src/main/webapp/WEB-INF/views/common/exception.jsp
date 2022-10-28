<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isErrorPage="true" session="false" %>

<ko:homeLayout>
	<div class="ibox">
		<p style="margin-bottom:30px">${exception.message}</p>
		<p><a href="javascript:;" onclick='$("#stacktrace").toggle()'>Show stack trace</a></p>
		<div id="stacktrace" style="display:none">
			<c:forEach items="${exception.stackTrace}" var="elem">
				${elem.className}.${elem.methodName} (${elem.lineNumber})<br/>
			</c:forEach>
		</div>
	</div>
</ko:homeLayout>
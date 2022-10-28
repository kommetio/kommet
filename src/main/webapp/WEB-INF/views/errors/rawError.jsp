<%@ page isErrorPage="true" import="java.io.*" contentType="text/plain" %>

<link href="${pageContext.request.contextPath}/resources/layout.css" rel="stylesheet" type="text/css" />

<div class="ibox">
	Message:
	<%=exception.getMessage()%>
	
	StackTrace:
	<%
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		exception.printStackTrace(printWriter);
		out.println(stringWriter);
		printWriter.close();
		stringWriter.close();
	%>
</div>
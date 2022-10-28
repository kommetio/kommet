<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="File revision">
	<jsp:body>
	
		<div class="ibox">
		
			<ko:buttonPanel>
				<a href="${pageContext.request.contextPath}/km/download/${rev.file.id}" class="sbtn"><kolmu:label key="files.download" /></a>
			</ko:buttonPanel>
		
			<ko:pageHeader>${rev.name}</ko:pageHeader>
			<div id="warnPrompt" style="margin-top:10px"></div>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label">Name</td>
						<td class="value">${rev.name}</td>
					</tr>
					<tr>
						<td class="label">Revision number</td>
						<td class="value">${rev.revisionNumber}</td>
					</tr>
					<tr>
						<td class="label">File</td>
						<td class="value"><a href="${pageContext.request.contextPath}/km/files/${rev.file.id}">${rev.file.name}</a></td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
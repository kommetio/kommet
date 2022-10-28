<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout titleKey="comments.details.title">
	<jsp:body>
	
		<div class="ibox">
		
			<ko:pageHeader><kolmu:label key="comments.details.title" /></ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/comments/edit/${user.id}" class="sbtn"><kolmu:label key="btn.edit" /></a>
		
			<table class="kdetails" style="margin: 30px 0 30px 0">
				<tbody>
					<tr>
						<td class="label"><kolmu:label key="comments.text" /></td>
						<td class="value">${comment.text}</td>
						<td class="sep"></td>
						<td colspan="2"></td>
					</tr>
				</tbody>
			</table>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
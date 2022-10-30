<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout titleKey="mail.sentMessages.title">

	<jsp:body>
		
		<style>
		
			div#search-panel {
				margin-top: 25px;			
			}
			
			input#keyword {
				width: 300px;
			}			
		
		</style>
	
		<div class="ibox">
	
			<ko:pageHeader><kolmu:label key="mail.sentMessages.title" /></ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/mail/create" class="sbtn"><kolmu:label key="mail.composenew" /></a>
			
			<div id="search-panel">
				<input type="text" id="keyword" class="std-input" placeholder='<kolmu:label key="mail.searchtext" />'></input>
			</div>
	
			<table id="action-list" class="std-table" style="margin-top: 30px">
				<thead>
					<tr class="cols">
						<th><kolmu:label key="mail.subject" /></th>
						<th><kolmu:label key="mail.to" /></th>
						<th><kolmu:label key="mail.senddate" /></th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="email" items="${emails}">
						<tr>
							<td><a href="${pageContext.request.contextPath}/km/mail/message/${email.id}">${email.subject}</a></td>
							<td><a href="${pageContext.request.contextPath}/km/mail/message/${email.id}">${email.recipients}</a></td>
							<td><km:dateTime value="${email.sendDate}" format="dd-MM-yyyy HH:mm:ss" /></td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		
		</div>
		
		<script>

			function applyFilter()
			{
				filter = "";

				if ($("#keyword").val() != '')
				{
					filter += ":contains(" + $("#keyword").val() + ")";
				}

				$("#action-list > tbody > tr").hide();
				$("#action-list > tbody > tr" + filter).show();
			}

			$("#keyword").keyup(function() {
				applyFilter();
			});

			$(document).ready(function() {
				applyFilter();
				$("#keyword").focus();
			});
		
		</script>
	
	</jsp:body>
	
</ko:homeLayout>
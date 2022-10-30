<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${reportType.name}">

	<jsp:body>
	
		<script>

		function deleteReportType()
		{
			$.post("${pageContext.request.contextPath}/km/reporttypes/delete", { id: "${reportType.id}" }, function(data) {
				if (data.status == "success")
				{
					openUrl("${pageContext.request.contextPath}/km/reporttypes/list");
				}
				else
				{
					showMsg("warnPrompt", data.messages, "error");
				}
			}, "json");
		}
		
		</script>
	
		<div class="ibox">
			<ko:pageHeader>${reportType.name}</ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/reporttypes/edit/${reportType.id}" class="sbtn">${i18n.get('btn.edit')}</a>
			<a href="${pageContext.request.contextPath}/km/reporttypes/run/${reportType.id}" class="sbtn">${i18n.get('reports.run')}</a>
			<a href="javascript:;" onclick="ask('${i18n.get('reports.delete.ask')}?', 'warnPrompt', function() { deleteReportType(); })" id="deleteReportTypeBtn" class="sbtn">${i18n.get('btn.delete')}</a>
			<div id="warnPrompt" style="margin-top:20px"></div>
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
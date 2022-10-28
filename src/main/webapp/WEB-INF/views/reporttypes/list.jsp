<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout titleKey="reports.reporttypes.title">

	<jsp:body>
	
		<div class="ibox">
			<ko:pageHeader>${i18n.get('reports.reporttypes.title')}</ko:pageHeader>
			<a href="${pageContext.request.contextPath}/km/reporttypes/new" class="sbtn">${i18n.get('btn.new')}</a>
			<div id="reportTypes" style="margin-top: 20px;"></div>
		</div>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/tables.js"></script>
		<link href="${pageContext.request.contextPath}/resources/tables.css" rel="stylesheet" type="text/css" />
		
		<script>

			$(document).ready(function() {
				loadReportTypes();
			});

			linkFieldFormat = function(obj) {
				return "<a href=\"${pageContext.request.contextPath}/km/reporttypes/" + obj.id + "\">" + obj.name + "</a>";
			}

			function loadReportTypes()
			{
				$.get("${pageContext.request.contextPath}/km/reporttypes/list/data", function(data) {
					
					if (data.status == "success")
					{
						var options = {
							id: "reportTypeList",
							columns: [ 
								{ title: "${i18n.get('reports.reportype.name')}", property: "name", formatObjectValue: linkFieldFormat },
								{ title: "${i18n.get('label.createdDate')}", property: "createdDate" },
							],
							cssClasses: [ "std-table" ],
							cellFocus: false,
							display: "table"
						}
						
						var reportTable = tables.create(options);
						reportTable.render($("#reportTypes"), data.data);	
					}
					else
					{
						showMsg ("reportTypes", data.messages, "error", null, null, "${pageContext.request.contextPath}/rm");
					}
					
				}, "json");
			}
		
		</script>
	
	</jsp:body>
	
</ko:homeLayout>
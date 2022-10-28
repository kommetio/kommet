<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ attribute name="layoutPath" required="true" rtexprvalue="true" %>
<%@ attribute name="title" required="false" rtexprvalue="true" %>
<%@ attribute name="titleKey" required="false" rtexprvalue="true" %>
<%@ attribute name="importRMJS" required="false" rtexprvalue="false" %>
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	
		<c:if test="${title != null}">
			<title><c:out value="${title}" /> - Kommet</title>
		</c:if>
		<c:if test="${title == null}">
			<c:if test="${titleKey == null}">
				<title>Kommet</title>
			</c:if>
			<c:if test="${titleKey != null}">
				<title>Kommet - <kolmu:label key="${titleKey}" /></title>
			</c:if>
		</c:if>
	
		<link href="${pageContext.request.contextPath}/resources/layout.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/themes/std/styles.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/tag-styles.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/header.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.core.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/km.config.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-1.9.1.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-ui-1.10.3.custom.min.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery.visualize.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/common.js"></script>
		
		<c:if test="${importRMJS == 'true'}">
		<link href="${pageContext.request.contextPath}/resources/km/css/km.all.min.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.all.min.js"></script>
		</c:if>
		
		<%-- date/time picker js and styles --%>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-timepicker-addon.min.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-sliderAccess.js"></script>
		<link href="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-timepicker-addon.min.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/css/smoothness/jquery-ui-1.10.3.custom.min.css" rel="stylesheet" type="text/css" />
		
		<script>
			
			$(document).ready(function () {
			
				if (km.js.config.authData.user.userName === "root")
				{
					return;
				}
			
				$.getJSON("//freegeoip.net/json/?callback=?", function (data) {
					
					window.rmtraffic = {
						order: 0,
						ip4: data.ip,
						pageViewHash: Math.floor((Math.random() * 10000000000000) + 1),
						logCallback: function() {
						
							$.get("https://kommet.io/traffic/log", {
								viewHash: window.rmtraffic.pageViewHash,
								url: location.href,
								order: window.rmtraffic.order,
								env: km.js.config.envId,
								ip4: window.rmtraffic.ip4,
								browser: navigator.userAgent,
								sessionId: km.js.config.sessionId,
								userName: km.js.config.authData.user.userName,
								userId: km.js.config.authData.user.id,
								referrer: document.referrer
							}, function(resp) {
								console.log("Ping: " + resp);
							});
							window.rmtraffic.order++;
							
							setTimeout(window.rmtraffic.logCallback, 10000);
						}
					}
					
					// schedule pings
					window.rmtraffic.logCallback();
					
				});
			});
			
		</script>
		
	</head>
	
	<c:if test="${not empty layoutPath}">
		<jsp:include page="../userlayouts/${layoutPath}_before.jsp" />
	</c:if>
	<jsp:doBody />
	<c:if test="${not empty layoutPath}">
		<jsp:include page="../userlayouts/${layoutPath}_after.jsp" />
	</c:if>
	
</html>

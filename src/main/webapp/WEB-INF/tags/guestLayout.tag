<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ attribute name="title" required="false" rtexprvalue="true" %>
<%@ attribute name="bodyCssStyle" required="false" rtexprvalue="false" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<meta name="description" content="Kommet programming platform" />
	
	<c:if test="${title != null}">
		<title><c:out value="${title}" /> - Kommet</title>
	</c:if>
	<c:if test="${title == null}">
		<title>Kommet</title>
	</c:if>
	
	<link rel="icon" type="image/gif" href="${pageContext.request.contextPath}/resources/images/favicon.ico" />
	<link href="${pageContext.request.contextPath}/resources/layout.css" rel="stylesheet" type="text/css" />
	<link href="${pageContext.request.contextPath}/resources/header.css" rel="stylesheet" type="text/css" />
	<link href="${pageContext.request.contextPath}/resources/themes/std/styles.css" rel="stylesheet" type="text/css" />
	<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery.visualize.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/common.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.core.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/js/km.config.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.all.min.js"></script>
	<link href="${pageContext.request.contextPath}/resources/km/css/km.all.min.css" rel="stylesheet" type="text/css" />
	
</head>

<style>
	
	div.km-bl-table {
		background: #395872;
	}
	
	div.main-content {
		background: #fff;
		padding: 3rem;
	}

</style>

<body<c:if test="${bodyCssStyle != null}"> style="${bodyCssStyle}"</c:if>>
	
	<div id="content">
		<div class="header-bar">
			<div class="km-bl-table">
				<div class="logo-cell">
					<div class="km-square"><div></div></div>
				</div>
				<div class="brand-cell" id="brand-cell">
					<span class="brand">kommet</span>
				</div>
			</div>
 		</div>
   		
  		<div class="main-content">
			<jsp:doBody />
		</div>
	</div>
		
		<div id="footer">
			Raimme � 2016
			<span id="footerLinks">
				<c:forEach items="${links}" var="link">
					&nbsp;&nbsp;<c:out value="${link.code}" escapeXml="false" />
				</c:forEach>
			</span>
		</div>
		
	</body>
</html>
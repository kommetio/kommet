<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ attribute name="title" required="false" rtexprvalue="true" %>
<%@ attribute name="titleKey" required="false" rtexprvalue="true" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<meta name="description" content="Kommet programming platform" />
		
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
		
		<link rel="icon" type="image/gif" href="${pageContext.request.contextPath}/resources/images/favicon.ico" />
		<link href="${pageContext.request.contextPath}/resources/layout.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/tag-styles.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/header.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.core.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/km.config.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-1.9.1.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-ui-1.10.3.custom.min.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery.visualize.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/common.js"></script>
		
		<link href="${pageContext.request.contextPath}/resources/km/css/km.all.min.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/themes/std/styles.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.all.min.js"></script>
	</head>
	
	<jsp:doBody />

</html>
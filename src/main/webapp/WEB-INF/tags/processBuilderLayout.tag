<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ attribute name="title" required="false" rtexprvalue="true" %>
<%@ attribute name="titleKey" required="false" rtexprvalue="true" %>
<%@ attribute name="bodyCssStyle" required="false" rtexprvalue="false" %>
<%@ attribute name="importRMJS" required="false" rtexprvalue="false" %>
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
		<link href="${pageContext.request.contextPath}/resources/km/css/km.all.min.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/themes/std/styles.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.core.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/km.config.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-1.9.1.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-ui-1.10.3.custom.min.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery.visualize.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/common.js"></script>
		
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/km.config.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.all.min.js"></script>
		
		<style>
		
			img#ricon {
				cursor: pointer;
			}
			
			#bp-maincontent {
				padding: 0;
			}
			
			table.layout-table > tbody > tr > td {
				border: none;
			}
			
			<style>
	
			div#layout-table {
				display: table;
				width: 100%;
				height: 100%;
			}
			
			div#layout-table > div {
				display: table-row;
			}
			
			div#layout-table > div > div {
				display: table-cell;
			}
			
			div.header-bar {
				height: 30px;
				display: table;
				background-color: #4c6c86;
			}
			
			.km-square > div {
				background-color: #4c6c86;
			}
		
		</style>
		
		
	</head>
	<body style="background-color: rgb(236, 236, 236); <c:if test="${bodyCssStyle != null}">; ${bodyCssStyle}</c:if>">
	
		<div id="content">
			<table class="layout-table">
				<tbody>
					<tr>
						<td colspan="3">
							<div class="header-bar header-bar-bg">
								<div class="km-bl-cell">
									<div class="km-bl-table">
										<div class="logo-cell">
											<div class="km-square"><div></div></div>
										</div>
										<div class="brand-cell" id="brand-cell">
											<span class="brand">kommet</span>
										</div>
									</div>
								</div>
							</div>
						</td>
					</tr>
					<tr id="mainbox">
						<td class="main-content" id="bp-maincontent">
							<jsp:doBody />
						</td>
					</tr>
				</tbody>
			</table>
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
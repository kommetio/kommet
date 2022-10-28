<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ attribute name="title" required="false" rtexprvalue="true" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		
		<c:if test="${title != null}">
			<title><c:out value="${title}" /> - Raimme</title>
		</c:if>
		<c:if test="${title == null}">
			<title>Raimme</title>
		</c:if>
		
		<link rel="icon" type="image/gif" href="${pageContext.request.contextPath}/resources/images/favicon.ico" />
		<link href="${pageContext.request.contextPath}/resources/layout.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/tag-styles.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/header.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/km/css/km.all.min.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/themes/std/styles.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/css/smoothness/jquery-ui-1.10.3.custom.min.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/codemirror/lib/codemirror.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/codemirror/addon/fold/foldgutter.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/codemirror/addon/hint/show-hint.css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/ztree/css/demo.css" type="text/css" rel="stylesheet" type="text/css" />
		<link href="${pageContext.request.contextPath}/resources/ztree/css/zTreeStyle/zTreeStyle.css" rel="stylesheet" type="text/css" />
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-1.9.1.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-ui-1.10.3.custom.min.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery.visualize.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/common.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/codemirror/lib/codemirror.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/mode/clike/clike.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/mode/xml/xml.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/matchbrackets.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/fold/foldcode.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/fold/foldgutter.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/fold/brace-fold.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/hint/show-hint.js"></script>
		
		<%-- ide search --%>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/search/search.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/search/searchcursor.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/search/jump-to-line.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/search/matchesonscrollbar.js"></script>
		<link href="${pageContext.request.contextPath}/resources/codemirror/addon/search/matchesonscrollbar.css" rel="stylesheet" type="text/css" />
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/search/match-highlighter.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/scroll/annotatescrollbar.js"></script>
		<script src="${pageContext.request.contextPath}/resources/codemirror/addon/dialog/dialog.js"></script>
		<link href="${pageContext.request.contextPath}/resources/codemirror/addon/dialog/dialog.css" rel="stylesheet" type="text/css" />
		
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/ztree/js/jquery.ztree.core-3.5.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/ztree/js/jquery.ztree.exhide-3.5.min.js"></script>
		
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.core.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/km.config.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.all.min.js"></script>
		
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
			
			html, body {
				height: 100%;
				width: 100%;
			}
			
			div#layout-table > div > div.header-bar {
				height: 30px;
				display: table;
				background-color: #4c6c86;
			}
			
			.km-square > div {
				background-color: #4c6c86;
			}
		
		</style>
			
			
	</head>
	
	<body>
		
		<div id="layout-table">
			<div>
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
			</div>
			<div>
				<div>
				<jsp:doBody />
				</div>
			</div>
		</div>
		
	</body>
</html>
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
		<link href="${pageContext.request.contextPath}/resources/css/smoothness/jquery-ui-1.10.3.custom.min.css" rel="stylesheet" type="text/css" />
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
		<script type="text/javascript" src="${pageContext.request.contextPath}/js/km.config.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.all.min.js"></script>
		
		<%-- date/time picker js and styles --%>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-timepicker-addon.min.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-sliderAccess.js"></script>
		<link href="${pageContext.request.contextPath}/resources/datetimepicker/jquery-ui-timepicker-addon.min.css" rel="stylesheet" type="text/css" />
		
		<script async src="https://use.fontawesome.com/5abe83ecce.js"></script>
		
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

		
		<script>
		
			$(document).ready(function() {
				
				km.js.utils.renderProfile();
				
				adjustSizes();
				
				var openCallback = function() {
					$(".km-rp-cell").css("top", $("#skm-layout-topbar").outerHeight());
					$(".km-rp-cell").toggle("slide", { direction: "right" });
				};
				
				var rightPanel = km.js.rightpanel.create({
					icon: $(".km-notif-icon-wrapper"),
					openCallback: openCallback
				});
				
				rightPanel.render(function(code) {
					var panel = $("<div></div>").addClass("km-rp-cell").append(code);
					
					// adjust panel height to main box	
					panel.css("height", Math.max($(".km-layout-main").outerHeight(), $(".km-layout-menu").outerHeight()));
					
					$("div#km-layout-mainbox").append(panel);
				});
				
				var brandName = km.js.config.userSettings['km.sys.env.default.title'];
				if (brandName)
				{
					$("#brand-cell > span.brand").text(brandName);
				}
				
				$(".km-notif-icon").click(openCallback);
				
				<%-- clone the menu item, because sliding the original menu would affect its properties and it would be difficult to go back to the original display --%>
				var clonedMenu = $(".km-layout-menu").clone();
				clonedMenu.hide();
				clonedMenu.attr("id", "km-layout-menu-clone");
				$(document.body).append(clonedMenu);
				
				$(".km-burger-icon").click(function() {
					
					<%-- disabled body scrolling of body element, because we want to scroll the menu, which is position: fixed --%>
					$(document.body).toggleClass("scroll-disabled");
					
					var menu =  $(".km-layout-menu-clone");
					menu.css("position", "fixed");
					menu.css("top", $("#km-layout-topbar").outerHeight());
					menu.css("height", $("#content").outerHeight() - $("#km-layout-topbar").outerHeight());
					menu.css("overflow", "auto");
					menu.toggle("slide", { direction: "left", complete: function() {
						//$(this).addClass("km-menu-visible");
					}});
				});
				
				<%-- km.js.ui.ripple($("div#left-menu")); --%>
			});
			
			function adjustSizes()
			{
				var leftHeight = $(".km-left-container").outerHeight();
				if (leftHeight > $("body").outerHeight())
				{
					$("body").css("height", leftHeight + "px");
					$(".km-left-container").css("position", "relative");
				}
			}
		
		</script>
		
	</head>
	<body style="<c:if test="${bodyCssStyle != null}">; ${bodyCssStyle}</c:if>">
	
		<div id="content">
			<div id="km-layout-mainbox">
				<div class="km-layout-row">
					<div id="km-layout-menu" class="km-left-menu">
						<div class="km-left-container">
							<div class="km-bl-table">
								<img src="${pageContext.request.contextPath}/resources/images/km-logo-white.png" class="km-home-logo"></img>
								<!--<div class="logo-cell">
									<div class="km-square"><div></div></div>
								</div>
								<div class="brand-cell" id="brand-cell">
									<span class="brand">kommet</span>
								</div>-->
							</div>
							<div class="km-profile-box">
								<div class="km-profile-pic"></div>
								<div class="km-profile-name"></div>
							</div>
							<tags:leftmenu />
						</div>
					</div>
					<div id="km-layout-main">
						<div id="km-layout-topbar" class="km-rd-col">
							<div class="header-bar header-bar-bg">
								<div class="km-top-middle">
									<div class="km-search-wrapper">
										<img src="${pageContext.request.contextPath}/resources/images/mglass.png"></img>
										<input type="text" class="km-input km-top-search" placeholder="Search"></input>
									</div>
								</div>
								<div id="km-top-icons">
									<div>
										<img src="${pageContext.request.contextPath}/resources/images/burger-black.png" id="km-burger-icon"></img>
									</div>
									<div id="km-notif-icon-wrapper">
										<img src="${pageContext.request.contextPath}/resources/images/bell-black.png" id="km-notif-icon"></img>
									</div>
								</div>
				   			</div>
						</div>
						<div class="km-layout-body">
						<jsp:doBody />
						</div>
					</div>
				</div>
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
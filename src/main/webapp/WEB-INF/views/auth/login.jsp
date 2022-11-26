<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<tags:blankLayout>

	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ui.js"></script>
		<link href="${pageContext.request.contextPath}/resources/themes/std/styles.css" rel="stylesheet" type="text/css" />
	
		<style>
		
			div.km-center-container {
				margin: 0 auto;
				margin-top: 5rem;
				width: 100%;
				text-align: middle;
			}
			
			td.remember-me {
				color: #dadada;
			}
			
			.auth-forms {
				opacity: 0.95;
			    border: 0.1em solid #ccc;
			    border-radius: 0.3em;
			    background-color: rgba(255, 255, 255, 0.17);
			    box-shadow: 0 0 0.8em rgba(0, 0, 0, 0.1);
			    padding: initial;
			    margin: 0 auto;
			    margin-top: 5em;
			}
			
			div.km-logo-container {
				margin: 0 auto;
				display: table;
			}
			
			div.km-logo-container > div {
				display: table-cell;
				vertical-align: middle;
				font-size: 2.5rem;
				text-align: left;
				color: #fff;
				text-shadow: 1px 1px 1px #1b528a;
			}
			
			div.km-logo {
				width: 3rem;
			}
			
			div.km-logo > img {
				height: 4rem;
				position: relative;
    			left: -10px;
			}
		
			table.auth-forms td.btn-cell {
				padding-top: 1em;
			}
			
			body {
				background-color: #2f5678;
			}
			
			.auth-forms a.sbtn {
				font-size: 0.8rem;
				display: block;
				margin: 0 0 0.5rem 0;
				padding: 0.7em;
				box-sizing: border-box;
			    background: rgb(47 86 120);
			    border: 1px solid #1f4354;
				box-shadow: none;
				text-shadow: none;
				color: #f1f1f1;
			}
			
			.auth-forms input[type="text"], .auth-forms input[type="password"] {
			    border: 0.1em solid #fff;
			    padding: 0.7em;
			    background-color: #f2f2f2;
			    border-radius: 0.2em;
			    margin-bottom: 0.5rem;
			    box-sizing: border-box;
			    width: 100%;
			}
			
			.auth-forms {
				width: 20rem;
				padding: 1.5rem;
			}
			
			#auth-form-content {
				width: 100%;
			}
			
			@media only screen and (min-device-width : 320px) and (max-device-width : 480px) {
			
				body {
					background: none;
				}
				
				.auth-forms {
					border: 0;
					box-shadow: none;
					box-sizing: border-box;
					margin-top: 0;
				}
				
				div.km-center-container {
					margin-top: 0;
				}
				
				div.km-logo-container {
					background: #395872;
    				width: 100%;
				}
				
				div.km-logo-container > div {
					padding: 1rem;
				}
			
			}
		
		</style>
		
		<script>
		
			$(document).ready(function() {
				$("input#username").focus().attr("autocapitalize", "none");
				
				$("#loginBtn").click(function() {
					
					km.js.ui.statusbar.hide();
					
					var waitIcon = km.js.ui.buttonWait({
						button: $(this),
						text: "Logging in"
					});
					
					//km.js.ui.statusbar.show(km.js.config.i18n ? km.js.config.i18n["auth.login.inprogress"] : "Logging in...");
					
					$.post(km.js.config.contextPath + "/km/auth/dologin", $("#login-form").serialize(), (function(waitIcon) {
						
						return function(data) {
							
							km.js.ui.buttonWaitStop(waitIcon);
							
							if (data.success)
							{
								km.js.utils.openURL(km.js.config.contextPath + "/" + (data.data.url ? data.data.url : ""));
							}
							else
							{
								km.js.ui.statusbar.err(data.messages ? data.messages : data.message);
							}
						}
						
					})(waitIcon), "json");
					
				});
				
				$(document.body).keypress(function(e) {
					if (e.keyCode == 13) {
						$("#loginBtn").click();
					}
				});
				
			});
		
		</script>
	
		<div class="km-center-container">
			<div class="km-logo-container">
				<div class="km-logo">
					<img src="${pageContext.request.contextPath}/resources/images/km-logo-white.png"></img>
				</div>
			</div>
			<div class="auth-forms">
				<div>
					<kolmu:errors messages="${errorMsgs}" />
				</div>
				<div>
					<kolmu:messages messages="${actionMsgs}" />
				</div>
				<div>
						<%--<h3><kolmu:label key="login.title" i18n="${i18n}" /></h3>--%>
						<form id="login-form">
						
							<div id="login-errors"></div>
							
							<input type="hidden" name="url" value="${url}" />
							<input type="hidden" name="locale" value="EN_US" />
							<table id="auth-form-content">
								<tr>
									<td>
										<input type="text" name="username" id="username" placeholder="<kolmu:label key="user.username" i18n="${i18n}" />" />
									</td>
								</tr>
								<tr>
									<td>
										<input type="password" name="password" placeholder="<kolmu:label key="user.password" i18n="${i18n}" />" />
									</td>
								</tr>
								<c:if test="${showEnv == true}">
									<tr>
										<td>
											<input type="text" name="envId" placeholder="<kolmu:label key="login.env" i18n="${i18n}" />" value="${envId}" />
										</td>
									</tr>
								</c:if>
								<c:if test="${showEnv != true}">
									<input type="hidden" name="envId" value="${envId}" />
								</c:if>
								<tr>
									<td class="btn-cell">
										<a id="loginBtn" href="javascript:;" class="sbtn"><kolmu:label key="login.btn" i18n="${i18n}" /></a>
										<c:if test="${not empty envId}">
											<a class="sbtn" href="${pageContext.request.contextPath}/km/users/forgottenpassword?envId=${envId}"><kolmu:label key="login.forgotpwd.link" i18n="${i18n}" /></a>
										</c:if>
									</td>
								</tr>
								<tr>
									<td class="btn-cell remember-me">
										<input type="checkbox" name="rememberMeDays" value="14"></input> <kolmu:label key="auth.rememberme" i18n="${i18n}" />
									</td>
								</tr>
							</table>
						</form>
					</div>
							
				</div>
		</div>
		
		<!--[if lt IE 9]>
		<script>
			$(".auth-forms").parent().append("<div class=\"ibox\" style=\"margin:50px\"><div id=\"browser-err\"></div>&nbsp;</div>");
			$(".auth-forms").remove();
			showMsg ("browser-err", "<h3>${browserMsgTitle}</h3>${browserMsgText}", "error", "padding-bottom: 20px", null, "${pageContext.request.contextPath}/rm");
		</script>
		<![endif]-->
		
	</jsp:body>
	
</tags:blankLayout>
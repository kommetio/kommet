<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="auth" uri="/WEB-INF/tld/kolmu-auth-tags.tld" %>

<%@ attribute name="items" required="false" rtexprvalue="true" %>

<script>

	$(document).ready(function() {
		collapseMenu();
		initMenu();
	});

	function collapseMenu()
	{
		$("#left-menu > ul > li > ul").hide();
	}

	function initMenu()
	{
		$("#left-menu > ul > li > a").click(function() {
			$(this).closest("li").find("ul").toggle();
		});
	}

</script>

<div id="left-menu">
	<ul>
  		<auth:check profile="System Administrator">
			<li>
				<a target="_self" href="${pageContext.request.contextPath}/km/setup" class="menu-item">Setup</a>
			</li>
		</auth:check>
  	</ul>
</div>
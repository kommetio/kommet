<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page %>

<tags:homeLayout title="Administration Panel">

	<jsp:body>
	
		<style>
	
			#ops {
				margin: 40px 0 70px 0;
			}
			
			ul#ops {
				list-style: none;
				padding: 0;
			}
			
			ul#ops > li {
				padding: 15px 0 15px 0;
			}
		
		</style>
	
		<script>
	
			function reloadConfig()
			{
				$.get("${pageContext.request.contextPath}/km/reloadconfig", function(data) {
	
					// clear all error messages
					$("#ops .result").html("");
					
					if (data.status == "success")
					{
						$("#configreload .result").html("Done");
					}
					else
					{
						$("#configreload .result").html("Error");
					}
				}, "json");
			}

			function clearEnvCache()
			{
				$.get("${pageContext.request.contextPath}/km/envs/clearcache", function(data) {
	
					// clear all error messages
					$("#ops .result").html("");
					
					if (data.status == "success")
					{
						$("#clearenv .result").html("Done");
					}
					else
					{
						$("#clearenv .result").html("Error");
					}
				}, "json");
			}

			function clearTextLabels()
			{
				$.get("${pageContext.request.contextPath}/km/textlabels/clearcache", function(data) {
	
					// clear all error messages
					$("#ops .result").html("");
					
					if (data.status == "success")
					{
						$("#clearlabels .result").html("Done");
					}
					else
					{
						$("#clearlabels .result").html("Error");
					}
				}, "json");
			}
		
		</script>
	
		<div class="ibox">
			<div class="head1">Administration Panel</div>
			<div class="grey-hint">Manage your environment from here</div>
			
			<ul id="ops">
				<li id="configreload">
					<div class="action">
						<a href="javascript:;" onclick="reloadConfig()" class="sbtn">Reload configuration</a>
						<span class="result"></span>
					</div>
				</li>
				<li id="clearenv">
					<div class="action">
						<a href="javascript:;" onclick="clearEnvCache()" class="sbtn">Clear environment cache</a>
						<span class="result"></span>
					</div>
				</li>
				<li id="clearlabels">
					<div class="action">
						<a href="javascript:;" onclick="clearTextLabels()" class="sbtn">Clear text label cache</a>
						<span class="result"></span>
					</div>
				</li>
			</ul>
			
		</div>
	
	</jsp:body>
	
</tags:homeLayout>
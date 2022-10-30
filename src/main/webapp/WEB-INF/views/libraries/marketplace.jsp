<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="App Marketplace" importRMJS="true">

	<jsp:body>
	
		<style>
		
			input#km-search {
				width: 16rem;
    			margin-bottom: 3rem;
			}
			
			div.km-mrkt-subtitle {
				margin-bottom: 1rem;
			}
		
		</style>
	
		<script>
		
			$(document).ready(function() {
				
				km.js.marketplace.render({
					target: $(".km-marketplace-wrapper")
				});
				
				$(".km-search").keyup(function() {
				
					km.js.marketplace.render({
						target: $(".km-marketplace-wrapper"),
						keyword: $(this).val()
					});
					
				});
				
			});
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
			<ko:pageHeader><i class="fa fa-cube"></i>App Marketplace</ko:pageHeader>
			<div class="km-mrkt-subtitle">Use built-in templates, apps and libraries to build an app rapidly from existing components</div>
			<input type="text" class="km-input" id="km-search" placeholder="Search for apps"></input>
			
			<div id="km-marketplace-wrapper"></div>
			
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
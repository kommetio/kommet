<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<tags:homeLayout title="New environment">

	<jsp:body>
	
		<div class="box">
			<div class="head1">Create your environment</div>
			<div class="grey-hint">Create an environment, a space where your apps will reside</div>
			
			<form action="${pageContext.request.contextPath}/km/env/docreate" method="POST">
				<div>
					<div>Environment name:</div>
					<div><input type="text" name="name" /></div>
				</div>
				<input type="submit" value="Create" />
 			</form>
			
		</div>
	
	</jsp:body>
	
</tags:homeLayout>
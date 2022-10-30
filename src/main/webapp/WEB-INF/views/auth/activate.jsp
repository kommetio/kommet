<%@ taglib prefix="faq" uri="/WEB-INF/tld/faq-tags.tld" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ page session="false" %>

<tags:homeLayout>

	<jsp:body>
	
		<faq:errors messages="${errorMsgs}" />
		<faq:messages messages="${actionMsgs}" />
		
		<%-- <div class="box">
			Thanks for joining! Your account is now activated. If you have a moment, please tell us a bit about yourself so that other users can get to know you.
			
			<form method="post" action="/profile/save">
				<input type="text" name="displayName" placeholder="Community name" />
				<textarea name="about" placeholder="A few words about yourself"></textarea>
				<input type="submit" class="sbtn" value="Save" />
			</form> 
		</div>--%>
		
	</jsp:body>
	
</tags:homeLayout>
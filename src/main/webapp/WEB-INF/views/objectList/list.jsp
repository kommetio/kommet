<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<km:viewWrapper>
	<km:view name="stub-value">
		<%-- this page is embedded in a dialog, so we need to scale the font --%>
		<div class="km-font-scale">
			<km:objectListConfig config="${config}" />
		</div>
	</km:view>
</km:viewWrapper>
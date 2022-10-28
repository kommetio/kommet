<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}">
	<jsp:body>
	
		<script>
		
			$(document).ready(function() {
				
				$("#saveBtn").click(function() {
					saveDictionary();
				});
				
			});
			
			function saveDictionary()
			{
				km.js.ui.statusbar.show("Saving...");
				
				// serialize items
				/*var items = [];
				for (var i = 0; i < window.dictionary.items.length; i++)
				{
					var item = window.dictionary.items[i];
					items.push(item.id + "," + item.name + "," + item.key + "," + i);
				}*/
				
				var payload = {
					name: $("#name").val(),
					//items: items.join(";"),
					id: $("#dictionaryId").val()
				};
				
				$.post(km.js.config.contextPath + "/km/dictionaries/save", payload, function(result) {
					
					if (result.success)
					{
						if (!result.data.dictionaryId)
						{
							throw "Dictionary save but its ID not passed";	
						}
						
						km.js.utils.openURL(km.js.config.contextPath + "/km/dictionaries/" + result.data.dictionaryId);
					}
					else
					{
						// show errors
						km.js.ui.statusbar.err(result.message ? result.message : result.messages);
					}
					
				}, "json");
			}
		
		</script>
	
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<input type="hidden" id="dictionaryId" value="${dictionary.id}" />
			
			<ko:pageHeader>${pageTitle}</ko:pageHeader>
			
			<ko:propertyTable>
				<ko:propertyRow>
					<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
					<ko:propertyValue>
						<input type="text" name="name" id="name" value="${dictionary.name}" />
					</ko:propertyValue>
				</ko:propertyRow>
				<ko:buttonRow>
					<ko:buttonCell><input type="button" value="Save" id="saveBtn" class="sbtn" /></ko:buttonCell>
					<c:if test="${not empty dictionary.id}">
						<a href="${pageContext.request.contextPath}/km/dictionaries/${dictionary.id}" class="sbtn">Cancel</a>
					</c:if>
					<c:if test="${empty dictionary.id}">
						<a href="${pageContext.request.contextPath}/km/dictionaries/list" class="sbtn">Cancel</a>
					</c:if>
				</ko:buttonRow>
			</ko:propertyTable>
			
			<div id="item-list"></div>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
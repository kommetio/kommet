<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Edit validation rule">
	<jsp:body>
	
		<script>
			
			$(document).ready(function() {
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='name']"),
					text: "Name must start with a capital letter (e.g. CheckAge) or be a qualified name (e.g. kommet.CheckPrice)."
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='errorMsg']"),
					text: "Message to be displayed when the above condition is not met."
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='errorMsgLabel']"),
					text: "Key of a text label that defines the errror message to be displayed when the above condition is not met."
				});
				
				km.js.ui.autoFormatName({
					target: $("input[name='name']")
				});
				
			});
			
			function toggleRelHints()
			{
				$("#relHints").toggle();
			}
		
		</script>
		
		<style>
		
			div.rel-syntax ul {
				list-style-type: none;
				padding: 0;
				margin: 0;
				margin-top: 0.5em;
			}
			
			div.rel-syntax ul > li {
				margin-bottom: 0.7rem;
			}
			
			div.rel-syntax .op {
				padding: 0.1em 0.5em;
			    width: 2.5rem;
			    text-align: center;
			    border: 1px solid #ccc;
			    background-color: #fff2b4;
			    border-radius: 2px;
			    margin-right: 0.5rem;
			    display: inline-block;
			}
		
		</style>
		
		<div class="ibox">
		
			<kolmu:errors messages="${errorMsgs}"/>
			<kolmu:messages messages="${actionMsgs}"/>
		
			<form method="post" action="${pageContext.request.contextPath}/km/validationrules/save">
				<input type="hidden" name="typeId" value="${typeId}" />
				<input type="hidden" name="ruleId" value="${vr.id}" />
				
				<ko:pageHeader>
					<c:if test="${empty vr.id}">New validation rule</c:if>
					<c:if test="${not empty vr.id}">${vr.name}</c:if>
				</ko:pageHeader>
				
				<ko:propertyTable>
					<ko:propertyRow>
						<ko:propertyLabel value="Name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="name" value="${vr.name}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="REL evaluation" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="code">${vr.code}</textarea>
							<a href="javascript:;" onClick="toggleRelHints()">REL syntax</a>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="relHints" cssStyle="display: none">
						<ko:propertyLabel></ko:propertyLabel>
						<ko:propertyValue>
							<div class="rel-syntax">
							Operators:
							<ul>
								<li><span class="op">=</span> equals</li>
								<li><span class="op">!=</span> not equals</li>
								<li><span class="op">&gt;</span> greater than</li>
								<li><span class="op">&gt;=</span> greater or equal</li>
								<li><span class="op">&lt;</span>  lesser than</li>
								<li><span class="op">&lt;=</span> lesser or equal</li>
								<li><span class="op">"string"</span> string literal</li>
							</ul>
							</div>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Error message"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="errorMsg" value="${vr.errorMessage}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Error message text label"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="errorMsgLabel" value="${vr.errorMessageLabel}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Is Active"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="isActive" value="true"<c:if test="${vr.active == true}"> checked</c:if>></input>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:buttonRow>
						<ko:buttonCell>
							<input type="submit" value="Save" />
							<c:if test="${not empty label.id}">
								<a href="${pageContext.request.contextPath}/km/validationrules/${label.id}" class="sbtn">Cancel</a>
							</c:if>
							<c:if test="${empty label.id}">
								<a href="${pageContext.request.contextPath}/km/type/${keyPrefix}/#rm.tab.5" class="sbtn">Cancel</a>
							</c:if>
						</ko:buttonCell>
					</ko:buttonRow>
				</ko:propertyTable>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
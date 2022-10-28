<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="wgfn" uri="/WEB-INF/tld/wg-functions.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Data grid">

	<jsp:body>
	
		<style>
		
			div.km-layout-body {
				background: #fff;
				min-height: 100vh;
				padding: 2rem 1rem;
			}
		
		</style>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.db.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.grid.js"></script>
	
		<script>
		
			$(document).ready(function() {
				
				initGrid();
				
			});
			
			function initGrid()
			{
				km.js.db.query("select ${fieldList} from ${type.qualifiedName} order by createdDate asc", function(data, count, jsti) {
					
					data = km.js.utils.addPropertyNamesToJSRC(data, jsti);
					
					var fields = [];
					
					// find all fields for this type in JSTI
					for (var fieldId in jsti.fields)
					{
						var field = jsti.fields[fieldId];
						
						if (field.typeId === "${type.KID}")
						{
							fields.push({
								name: field.apiName,
								label: field.label,
								dataType: field.dataType
							});
						}
					}
					
					var grid = km.js.grid.create({
						
						records: data,
						fields: fields,
						emptyColumnCount: 10,
						emptyRowCount: 30,
						typeName: "${type.qualifiedName}"
						
					});
					
					grid.show($(".km-grid-container"));
					
				});
			}
		
		</script>
		
		<div class="km-title">${type.pluralLabel}</div>
		
		<div id="km-grid-container"></div>
	
	</jsp:body>
	
</ko:homeLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="auth" uri="/WEB-INF/tld/kolmu-auth-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Data Model Diagram" importRMJS="true">
	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/raphael.js"></script>
	
		<style>
		
			div#diagram {
				position: relative;
				height: 100%;
				width: 100%;
				font-size: 1rem;
			}
			
			div.gridbg {
 				background-color: transparent;
    			background-image: linear-gradient(0deg, transparent 24%, rgba(1, 84, 144, .05) 25%, rgba(1, 84, 144, .05) 26%, transparent 27%, transparent 74%, rgba(1, 84, 144, .05) 75%, rgba(1, 84, 144, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(1, 84, 144, .05) 25%, rgba(1, 84, 144, .05) 26%, transparent 27%, transparent 74%, rgba(1, 84, 144, .05) 75%, rgba(1, 84, 144, .05) 76%, transparent 77%, transparent);
  				height:100%;
  				background-size:50px 50px;
			}
			
			div#diagram-wrapper {
				background-image: linear-gradient(#ffffff, #dedede);
				border: 1px solid #e6e6e6;
				height: 800px;
				overflow: auto;
			}
			
			div.type-box {
				border: 1px solid #ddd;
				border-radius: 0.2em;
				width: 10em;
				height: 18em;
				background-color: #fff;
				cursor: move;
				overflow: hidden;
			}
			
			div.type-box:hover {
				box-shadow: 0 0 1em rgb(187, 187, 187);
			}
			
			div.type-box div.type-name {
				font-weight: bold;
				font-size: 0.9rem;
				box-sizing: border-box;
				width: 100%;
				padding: 0.5em;
				color: #525252;
				text-align: center;
				background-color: #fff7bf;
				border-bottom: 1px solid #ccc;
			}
			
			div.type-box div.type-name > span {
				cursor: pointer;
			}
			
			svg {
				width: 100%;
				height: 100%
			}
			
			ul.field-list {
				list-style-type: none;
				padding: 0;
				margin: 0;
			}
			
			ul.field-list > li {
				font-size: 0.75em;
				padding: 0.5em;
				border-bottom: 1px solid #eee;
			}
			
			ul.field-list > li > span {
				cursor: pointer;
			}
			
			div.field-container {
				overflow: auto;
				height: 100%;
				width: 100%;
			}
			
			div.diagram-header {
				padding: 1em;
				margin-bottom: 1em;
			}
			
			div.cbwrapper {
				display: inline-block;
    			height: 100%;
    			vertical-align: middle;
    			margin-right: 1em;
			}
			
		</style>
		
		<script>
		
			$(document).ready(function() {
				loadTypes(true);
				
				$("#onlyCustomTypes").change(function() {
					loadTypes($(this).is(":checked"));
				});
			});
			
			window.typeBindings = ${typeBindings};
			window.displayedTypeIds = {};
			
			function loadTypes(onlyCustomTypes)
			{	
				var types = ${types};
				
				window.displayedTypeIds = {};
				$("#diagram").empty();
				var paper = Raphael(document.getElementById('diagram'), 1000, 1000);
				
				var prevBox = null;
				var box = null;
				var order = 0;
				
				for (var i = 0; i < types.length; i++)
				{
					var type = types[i];
					
					if (onlyCustomTypes === true && type.isBasic === true)
					{
						// skip basic type
						continue;
					}
					
					window.displayedTypeIds[type.id] = type.id;
					
					renderType(paper, type, order++);
				}
				
				paintLines(paper, displayedTypeIds);
			}
			
			function join (paper, box1Id, box2Id, label)
			{
				var box1 = $("#typebox-" + box1Id);
				var box2 = $("#typebox-" + box2Id);
				
				var offsetX1 = box1.position().left;
				var offsetY1 = box1.position().top;
				var width1 = box1.width();
				var height1 = box1.height();
				var centerX1 = Math.ceil(offsetX1 + width1 / 2);
				var centerY1 = Math.ceil(offsetY1 + height1 / 2);
				
				var offsetX2 = box2.position().left;
				var offsetY2 = box2.position().top;
				var width2 = box2.width();
				var height2 = box2.height();
				var centerX2 = Math.ceil(offsetX2 + width2 / 2);
				var centerY2 = Math.ceil(offsetY2 + height2 / 2);
				
				var path = curve(paper, parseInt(centerX1), parseInt(centerY1), parseInt(centerX2), parseInt(centerY2));
				
				if (label)
				{
					labelPath(path, label);
				}
			}
			
			function renderType (paper, type, i)
			{
				var box = $("<div></div>").addClass("type-box");
				box.attr("id", "typebox-" + type.id);
				
				var name = $("<div></div>").addClass("type-name");
				
				var typeLabel = $("<span></span>").text(type.label);
				
				typeLabel.click((function(typePrefix) {
					
					return function() {
						km.js.utils.openURL(km.js.config.contextPath + "/km/type/" + typePrefix);
					}
					
				})(type.keyPrefix));
				
				name.append(typeLabel);
				
				box.append(name);
				
				var fields = $("<ul></ul>").addClass("field-list");
				
				for (var k = 0; k < type.fields.length; k++)
				{
					var field = type.fields[k];
					
					var li = $("<li></li>");
					
					var fieldName = $("<span></span>").text(field.label);
					
					fieldName.click((function(fieldId) {
						
						return function() {
							km.js.utils.openURL(km.js.config.contextPath + "/km/field/" + fieldId);
						}
						
					})(field.id));
					
					li.append(fieldName);
					
					fields.append(li);
				}
				
				box.append($("<div></div>").append(fields).addClass("field-container"));
				
				box.draggable({
				
					drag: (function() {
						
						return function() {
							paintLines(paper);
						}
						
					})(paper)
					
				});
				
				box.resizable();
				
				
				$("#diagram").append(box);
				
				var boxHeight = 300;
				var boxesPerLines = 7;
				
				box.css("position", "absolute");
				box.css("top", 20 + Math.ceil((i + 1)/boxesPerLines - 1) * boxHeight);
				box.css("left", 200 * (i % boxesPerLines) + 40);
				
				return box;
			}
			
			function paintLines(paper)
			{
				paper.clear();
				
				for (var i = 0; i < window.typeBindings.length; i++)
				{
					var binding = window.typeBindings[i];
					
					// skip system field bindings
					if (binding.field === "createdBy" || binding.field === "lastModifiedBy")
					{
						continue;
					}
					
					if (window.displayedTypeIds[binding.firstTypeId] && window.displayedTypeIds[binding.secondTypeId])
					{
						join(paper, binding.firstTypeId, binding.secondTypeId, binding.desc);
					}
				}
			}
			
			function curve (paper, startX, startY, endX, endY)
			{
				var cx1 = Math.abs(startX - endX)/2;
				var cy1 = Math.abs(startY - endY)/2;
				var cx2 = Math.abs(startX - endX)/2;
				var cy2 = Math.abs(startY - endY)/2;
				
				// curse
				//var path = paper.path("M" + startX + " " + startY + " C " + cx1 + " " + cy1 + " " + cx2 + " " + cy2 + " " + endX + " " + endY).attr({"stroke": "#999", "stroke-width": 2});
				var path = paper.path("M" + startX + " " + startY + " L " + endX + " " + endY).attr({"stroke": "#999", "stroke-width": 2, 'arrow-end': 'classic-wide-long', 'arrow-start': 'classic-wide-long'});
				//path.attr({ 'arrow-end': 'classic-wide-long', 'arrow-start': 'classic-wide-long' });
				
				return path;
			}
			
			function labelPath (path, text, textattr)
			{
			    if ( textattr == undefined )
			    {
			        textattr = { 'font-size': 12, fill: '#000', stroke: 'none', 'font-family': 'Arial,Helvetica,sans-serif', 'font-weight': 400 };
			    }
			    var bbox = path.getBBox();
			    var textObj = path.paper.text( bbox.x + bbox.width / 2, bbox.y + bbox.height / 2 + 10, text ).attr( textattr );
			    return textObj;
			}
		
		</script>
		
		<div class="ibox diagram-header">
		
			<div class="header-row">
				<div class="cbwrapper"><input type="checkbox" id="onlyCustomTypes" value="true" checked="checked"></div>Only custom objects
			</div>
		
		</div>
	
		<div id="diagram-wrapper">
			<div id="diagram" class="gridbg">
				<svg id="diagramsvg">
					
				</svg>
			</div>
		</div>
	
	</jsp:body>
</ko:homeLayout>
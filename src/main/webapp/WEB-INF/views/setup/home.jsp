<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page %>

<tags:homeLayout title="Environment setup">

	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.marketplace.js"></script>
	
		<style>
		
			input#km-search {
				width: 16rem;
    			margin-bottom: 3rem;
			}
		
			div.km-setup-tiles {
				margin-top: 4em;
			}
		
			div.km-setup-tiles > div {
				width: 10em;
				border-radius: 0.3em;
				padding: 2em;
				text-align: center;
				display: table-cell;
				margin-right: 2em;
				vertical-align: middle;
			}
			
			div.km-setup-tiles > div.action-box:hover {
				box-shadow: 0 0 0.8rem rgba(100, 100, 100, 0.32);
			}
			
			div.km-setup-tiles div.icon {
				margin-bottom: 2em;
			}
		
			div.km-setup-tiles > div img {
				height: 4rem;
			}
			
			div.km-setup-tiles a.title {
				font-size: 1rem;
				cursor: pointer;
				text-decoration: none;
				color: #5a5a5a;
			}
			
			div.action-box {
				box-shadow: 0 0 0.8em rgba(0, 0, 0, 0.1);
				border: 1px solid #dfdfdf;
				cursor: pointer;
			}
			
			div.action-arrow {
				opacity: 0.7;
			}
			
			div.km-db-list {
	
			}
			
			div.km-db-list > div {
				padding: 0.3em 0.5em 0.3em 0;
			}
			
			div.km-db-list a {
				width: 20rem;
				overflow: hidden;
				text-decoration: none;
				color: #444;
			}
			
			div.km-db-list a:hover {
				text-decoration: underline;
			}
			
			div.km-db-section {
				display: table-cell;
    			width: 30rem;
			}
			
			div.km-dashboard {
				margin-bottom: 1rem;
			}
			
			div.km-db-list a.km-db-all-link {
				color: #004fff;
				text-decoration: underline;
				margin-top: 1.5rem;
			}
			
			div.km-section-btns {
				margin-bottom: 2rem;
			}
			
			textarea.km-dal-query {
				font-family: Consolas;
				background: transparent;
			    font-size: 0.85rem;
			    padding: 0;
			    color: #fff;
			    border: none;
			    width: 100%;
			    box-sizing: border-box;
			    outline: none;
			    line-height: 150%;
			    height: 1000rem;
			}
			
			div.km-dal-input {
				display: table;
				width: 100%;
				background: transparent;
				border-radius: 0.2em;
			    color: #fff;
			    padding: 1rem;
			    box-sizing: border-box;
			}
			
			div.km-dal-input > div {
				display: table-cell;
				vertical-align: top;
				font-family: Consolas;
			}
			
			div#km-dal-code {
				padding: 1rem;
				overflow-y: scroll;
				overflow-x: auto;
				background: #333;
				border: 1px solid #666;
				border-radius: 0.2em;
				height: 5rem;
			}
			
			div.km-dal-btns {
				position: absolute;
				top: 0;
				right: 3rem;
				padding: 1em;
			}
			
			div.km-dal-btns > a {
				color: #fff;
				margin: 0 0.5em;
			}
			
			div.km-dal-top-wrapper {
				position: relative;
			}
			
			div.km-db-console {
				margin-bottom: 1rem;
			}
			
			div.km-dal-type-wrapper {
				margin-bottom: 1.5rem;
			}
			
			div.km-dal-type-wrapper div.km-lookup {
				display: inline-block;
				margin-left: 1rem;
			}
			
			div.km-dal-type-wrapper div.km-lookup > input[type=text] {
				width: 20rem;
			}
			
			div.km-marketplace-box {
				margin-bottom: 1rem;
			}
			
			div#km-marketplace-wrapper {
				margin-top: 2rem;
			}
			
			div.km-mrkt-subtitle {
				margin-bottom: 1rem;
			}
		
		</style>
		
		<script>
			
			var lib = {};
			
			setup = {
					
				typeLookup: function(options) {
					
					// fetch all user types
					var call = $.post(km.js.config.contextPath + "/km/rest/jsti", { allUserTypes: true }, "json");
					
					call.done((function(options) {
						
						return function(jsti) {
							
							options.types = [];
							options.jsti = jsti;
							
							// convert to array
							for (var typeId in jsti.types)
							{
								options.types.push(jsti.types[typeId]);
							}
							
							var lookupOptions = {
								target: options.target,
								inputName: "stub",
								types: options.types,
								visibleInput: {
									cssClass: "std-input"
								},
								afterSelect: (function(options) {
									
									return function(selectedTypeId) {
							
										var selectedType = null;
										
										for (var typeId in options.jsti.types)
										{
											if (typeId == selectedTypeId)
											{
												selectedType = options.jsti.types[typeId];
												break;
											}
										}
										
										var fields = [];
										
										for (var fieldId in options.jsti.fields)
										{
											var field = options.jsti.fields[fieldId];
											if (field.typeId === selectedType.id)
											{
												var isPrimitive = field.dataType.id !== km.js.datatypes.object_reference.id && field.dataType.id !== km.js.datatypes.association.id && field.dataType.id !== km.js.datatypes.inverse_collection.id;
												fields.push(field.apiName + (!isPrimitive ? ".id" : ""));
											}
										}
										
										var query = "Querying type: SELECT " + fields.join(", ") + " FROM " + selectedType.qualifiedName;
										options.queryInput.val(query);
									}
									
								})(options)
							}
							
							km.js.ui.typeLookup(lookupOptions);
						}
					})(options));
					
				},
					
				cleanRecords: function(records, jsti) {
					
					records = km.js.utils.addPropertyNamesToJSRC(records, jsti);
					
					var newRecords = [];
					
					// remove PIRs from records
					for (var i = 0; i < records.length; i++)
					{
						var rec = records[i];
						for (var key in rec)
						{
							// remove PIRs - their property name is a field ID starting with 003
							if (key.indexOf("003") === 0)
							{
								delete rec[key];
							}
						}
					}
					
					return records;
				},
					
				runQuery: function(options) {
					
					var onFailure = (function(options) {
						
						return function(xhr) {
							
							var msgs = JSON.parse(xhr.responseText).messages;
							
							// display errors
							options.target.val(options.target.val() + "\n>> " + msgs.join("\n") + "\n\nEnter database query: ");
						}
						
					})(options);
					
					km.js.db.query(options.query, (function(options) {
							
						return function(records, count, jsti) {
							
							var prompt = "Enter database query: ";
								
							if ($.isArray(records))
							{
								records = setup.cleanRecords(records, jsti);
								
								options.target.val(JSON.stringify(records, null, "\t") + "\n\n" + prompt);
								
								$("div#km-dal-code").animate({
									height: "30rem"
								});
								
								//$("#result-tree").empty().append(getJsonTree(data));
								//$("#result-table").empty().append(data.length ? getDataTable(data) : $("<div></div>").text("No records found"));
							}
							else
							{
								options.target.html(data.error);
							}
						}
						
					})(options), onFailure, "json");
					
				},
					
				dalConsole: function(options) {
					
					var code = $("<div></div>").attr("id", "km-dal-code");
					
					var textWrapper = $("<div></div>").addClass("km-dal-input");
					//textWrapper.append($("<div>Enter database query &gt;</div>"));
					var textarea = $("<textarea autocomplete=\"off\" autocorrect=\"off\" autocapitalize=\"off\" spellcheck=\"false\"></textarea>").addClass("km-dal-query").val("select id, userName, email, profile.name from User limit 3");
					textarea.val("Enter database query: select id, userName, email from User limit 3");
					textWrapper.append($("<div></div>").append(textarea));
					
					textarea.focus(function() {
						
						$("div#km-dal-code").animate({
							height: "10rem"
						});
						
					});
					
					textarea.keypress(function(e) {
						
						if (e.which === 13)
						{
							var query = $(this).val();
							query = query.substring(query.toLowerCase().lastIndexOf("select "));
							
							// execute query
							setup.runQuery({
								query: query,
								target: $(this)
							});
						}
						
					});
					
					code.append(textWrapper);
					
					var buttons = $("<div></div>").addClass("km-dal-btns");
					
					var clearBtn = $("<a></a>").attr("href", "javascript:;").text("clear").click((function(textarea) {
						return function() {
							textarea.val("Enter database query: ");
						}
					})(textarea));
					buttons.append(clearBtn);
					
					var tipsBtn = $("<a></a>").attr("href", "javascript:;").text("manual").click((function(textarea) {
						return function() {
							var tip = "Raimme uses DAL (Data Query Language), an extension to SQL.\n\n";
							tip += "Sample queries:\n";
							tip += "\nQuery object ID: select id from <OBJECT API NAME>";
							tip += "\nQuery fields of type kommet.Customer: select id. firstName, lastName from kommet.Customer";
							tip += "\nQuery object reference relationship: select id, email, profile.name, profile.label from User";
							tip += "\nQuery collection: select id, items.id, items.price from rm.shop.Order";
							tip += "\nCount records: select count(id) from User";
							textarea.val(tip);
							
							$("div#km-dal-code").animate({
								height: "30rem"
							});
						}
					})(textarea));
					buttons.append(tipsBtn);
					
					var topWrapper = $("<div></div>").addClass("km-dal-top-wrapper");
					topWrapper.append(code).append(buttons);
					
					if (options.target instanceof jQuery)
					{
						options.target.empty().append(topWrapper);
					}
					
				},
					
				dashboard: function(options) {
					
					$.get(km.js.config.contextPath + "/km/setup/stats/items", (function(options) {
						
						return function(result) {
							
							var sectionButtons = function(options) {
								var code = $("<div></div>").addClass("km-section-btns");
								var newBtn = $("<a></a>").attr("href", km.js.config.contextPath + "/km/" + options.newLink).text(options.newLabel).addClass("sbtn");
								code.append(newBtn);
								return code;
							};
							
							var header = function(label, icon) {
								var title = $("<div><div>").addClass("km-title");
								var icon = $("<i></i>").addClass("fa").addClass(icon);
								title.append(icon).append(label);
								return title;
							}
							
							var code = $("<div></div>").addClass("ibox km-dashboard");
							
							// type section
							var typeSection = $("<div></div>").addClass("km-db-section");
							typeSection.append(header("Objects", "fa-database"));
							typeSection.append(sectionButtons({
								newLink: "types/new",
								newLabel: "Create object"
							}));
							var typeList = $("<div></div>").addClass("km-db-list");
							
							var types = result.data.data.types;
							
							for (var i = 0; i < types.length; i++)
							{
								var type = types[i];
								var box = $("<div></div>");
								box.append($("<a></a>").attr("href", km.js.config.contextPath + "/km/type/" + type.keyPrefix).text(type.label));
								typeList.append(box);
							}
							
							var box = $("<div></div>");
							box.append($("<a></a>").attr("href", km.js.config.contextPath + "/km/types/list").text("all objects").addClass("km-db-all-link"));
							typeList.append(box);
							
							typeSection.append(typeList);
							code.append(typeSection);
							
							// class section
							var classSection = $("<div></div>").addClass("km-db-section");
							classSection.append(header("Classes", "fa-code"));
							classSection.append(sectionButtons({
								newLink: "classes/new",
								newLabel: "Create class"
							}));
							var clsList = $("<div></div>").addClass("km-db-list");
							
							var classes = result.data.data.classes;
							
							for (var i = 0; i < classes.length; i++)
							{
								var cls = classes[i];
								var box = $("<div></div>");
								box.append($("<a></a>").attr("href", km.js.config.contextPath + "/km/classes/" + cls.id).text(cls.name));
								clsList.append(box);
							}
							
							var box = $("<div></div>");
							box.append($("<a></a>").attr("href", "/km/classes/list").text("all classes").addClass("km-db-all-link"));
							clsList.append(box);
							
							classSection.append(clsList);
							code.append(classSection);
							
							// view section
							var viewSection = $("<div></div>").addClass("km-db-section");
							viewSection.append(header("Views", "fa-laptop"));
							viewSection.append(sectionButtons({
								newLink: "views/new",
								newLabel: "Create view"
							}));
							var viewList = $("<div></div>").addClass("km-db-list");
							
							var views = result.data.data.views;
							
							for (var i = 0; i < views.length; i++)
							{
								var view = views[i];
								var box = $("<div></div>");
								box.append($("<a></a>").attr("href", km.js.config.contextPath + "/km/views/" + view.id).text(view.name));
								viewList.append(box);
							}
							
							var box = $("<div></div>");
							box.append($("<a></a>").attr("href", "/km/views/list").text("all views").addClass("km-db-all-link"));
							viewList.append(box);
							
							viewSection.append(viewList);
							code.append(viewSection);
							
							// action section
							var actionSection = $("<div></div>").addClass("km-db-section");
							actionSection.append(header("Web & REST actions", "fa-flash"));
							actionSection.append(sectionButtons({
								newLink: "actions/new",
								newLabel: "Create action"
							}));
							var actionList = $("<div></div>").addClass("km-db-list");
							
							var actions = result.data.data.actions;
							
							for (var i = 0; i < actions.length; i++)
							{
								var action = actions[i];
								var box = $("<div></div>");
								box.append($("<a></a>").attr("href", km.js.config.contextPath + "/km/actions/" + action.id).text(action.name));
								actionList.append(box);
							}
							
							var box = $("<div></div>");
							box.append($("<a></a>").attr("href", "/km/actions/list").text("all actions").addClass("km-db-all-link"));
							actionList.append(box);
							
							actionSection.append(actionList);
							code.append(actionSection);
							
							options.target.empty().append(code);
						}
						
						
					})(options), "json");
					
				}
					
			}
			
			$(document).ready(function() {
				
				$("#action-create-model").click(function() {
					km.js.utils.openURL(km.js.config.contextPath + "/km/types/list");
				});
				
				$("#action-customize-views").click(function() {
					km.js.utils.openURL(km.js.config.contextPath + "/km/type/1cc#rm.tab.2");
				});
				
				$("#action-custom-logic").click(function() {
					km.js.utils.openURL(km.js.config.contextPath + "/km/ide");
				});
				
				setup.dashboard({
					target: $("#km-dashboard-wrapper")
				});
				
				setup.dalConsole({
					target: $("#km-dalconsole-wrapper")
				});
					
				km.js.marketplace.render({
					target: $("#km-marketplace-wrapper"),
					limit: 10
				});
				
				$(".km-search").keyup(function() {
					km.js.marketplace.render({
						target: $("#km-marketplace-wrapper"),
						keyword: $(this).val()
					});
				});
				
				setup.typeLookup({
					target: $("input#km-dal-type"),
					queryInput: $("textarea.km-dal-query")
				});
				
			});
		
		</script>
		
		<div class="ibox km-marketplace-box">
			<div class="km-title"><i class="fa fa-cube"></i>App marketplace</div>
			<div class="km-mrkt-subtitle">Use built-in templates, apps and libraries to build an app rapidly from existing components</div>
			<input type="text" class="km-input" id="km-search" placeholder="Search for apps"></input>
			<div id="km-marketplace-wrapper"></div>
		</div>
		
		<div class="ibox km-db-console">
			<div class="km-title"><i class="fa fa-ticket"></i>Database query console</div>
			<div class="km-dal-type-wrapper">
				Query type: <input type="text" id="km-dal-type"></input>
			</div>
			<div id="km-dalconsole-wrapper"></div>
		</div>
		<div id="km-dashboard-wrapper"></div>
	
		<%--<div class="ibox">
			<div class="km-title">Build an app in a few steps</div>
			
			<div class="km-setup-tiles">
			
				<div class="action-box" id="action-create-model">
					
					<div class="icon"><img src="${pageContext.request.contextPath}/resources/images/dbicon.png"></div>
					<a href="${pageContext.request.contextPath}/km/types/list" class="title">Create data model</a>
				
				</div>
				
				<div class="action-arrow">
					<img src="${pageContext.request.contextPath}/resources/images/rightarrow.png">
				</div>
				
				<div class="action-box" id="action-customize-views">
					
					<div class="icon"><img src="${pageContext.request.contextPath}/resources/images/webicon.png"></div>
					<a class="title">Customize views</a>
				
				</div>
				
				<div class="action-arrow">
					<img src="${pageContext.request.contextPath}/resources/images/rightarrow.png">
				</div>
				
				<div class="action-box" id="action-custom-logic">
					
					<div class="icon"><img src="${pageContext.request.contextPath}/resources/images/customize.png"></div>
					<a class="title">Build custom logic</a>
				
				</div>
			
			</div>
			
		</div>--%>
	
	</jsp:body>
	
</tags:homeLayout>
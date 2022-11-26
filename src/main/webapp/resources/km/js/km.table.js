/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.table = {
	
	create: function(ds, jcr, display, settings) {
	
		var defaultSettings = {
			cssClass: "km-table-std",
			wrapperCssClass: null,
			contextPath: km.js.config.contextPath,
			
			// number of rows to be included in the draggable helper
			dragRowCount: 10,
			
			// page size, if pagination is used
			pageSize: 20,
			
			// callback executed after the "render" method is finished
			afterRender: null,
			
			// callback executed after a column is removed
			afterRemoveColumn: null,
			
			// formats to which items in this table can be exported
			// right now only "csv" is supported
			// e.g. exportFormats: [ "csv" ]
			exportFormats: [],
			buttonPanel: null,
			
			pagination: {
				active: false,
				currentPage: 0,
				pageCount: null
			}
		}
		
		var options = $.extend({}, defaultSettings, settings);
		
		// if ID property of the selected row is not defined, set it to the ID property of the record
		if (!display.idProperty)
		{
			display.idProperty = { name: "id" };
		}
		
		if (options.title)
		{
			if (options.title.placement === "buttons-left")
			{
				if (options.buttonPanel)
				{
					// let button panel handle the title
					options.buttonPanel.title = options.title;
				}
				/*else
				{
					warn "Cannot render title next to the buttons because button panel options are null. Define button panel options or set title.placement = 'top'";
				}*/
			}
		}
		
		var tableObj = {
			datasource: null,
			jcr: null,
			
			// title: { text: "some title", cssClass: "km-title", placement: "buttons-left" }
			title: options.title,
			
			// current data state
			data: null,
			
			// JQuery item containing the generated code
			generatedCode: null,
			
			// display configuration - column names, grouping, formatting
			display: null,
			settings: null,
			activeDragColumnIndex: null,
			sortPropertyId: null,
			sortOrder: null,
			unpagedDataCount: null,
			id: options.id,
			buttonPanel: options.buttonPanel,
			baseType: null,
			
			// pagination definition
			pagination: options.pagination,
			
			/**
			 * Renders the table and puts its content either into a container, or into a callback function.
			 * @param jcr - JCR criteria
			 * @param destination - JQuery element of callback 
			 */
			render: function(jcr, destination) {
				
				var onFetchData = function(table, jcr) {
					return function(data, unpagedDataCount, jsti) {
						
						var isJsonDatasource = table.datasource.type === "json";
						
						// jsti is required for all datasource types excep json
						if (!jsti && !isJsonDatasource)
						{
							throw "JSTI not passed to datasource.query callback";
						}
						
						if (!isJsonDatasource)
						{
							table.baseType = km.js.utils.getTypeFromJSTI(jcr, jsti);
						
							// display options may contain only property names, so before we can use it
							// we need to populate property IDs on it
							table.populateIdsOnTableOptions(jsti, table.baseType);
						}
						
						// generate jquery table code
						table.getCode(data, unpagedDataCount);
						
						// replace footer placeholder with actual footer code
						table.rerenderFooter();
						table.initHeaderFilters();
						table.draggableColumns();
						table.updateHeaders();
						table.updateButtonPanel();
						
						if (typeof(destination) === "function")
						{
							destination(table.generatedCode, table)
						}
						else
						{
							destination.html(table.generatedCode);
						}
						
						if (typeof(table.settings.afterRender) === "function")
						{
							table.settings.afterRender(table);
						}
						
						// notify resizeEvent about this table having loaded
						// so that listeners potentially interested in it are notified
						if (km.js.scope.resizeEvent)
						{
							km.js.scope.resizeEvent.notify();
						}
						
						table.data = [].concat(data);
						table.unpagedDataCount = unpagedDataCount;
					}
				}
				
				var actualJCR = km.js.utils.isEmpty(jcr) ? this.jcr : jcr;
				
				if (km.js.utils.isEmpty(actualJCR))
				{
					console.error("JCR is empty");
				}
				
				this.datasource.query(actualJCR, onFetchData(this, actualJCR));
			},
			
			rerenderRows: function(data, unpagedDataCount, settings) {
				
				if (km.js.utils.isEmpty(this.generatedCode))
				{
					throw "Cannot call rerenderRows when no table code has been generated";
				}
				
				this.generatedCode.find("div.kmjstb").html(this.renderRows(data, unpagedDataCount));
				
				if (settings && typeof(settings.onComplete) === "function")
				{
					settings.onComplete(this);
				}
			},
			
			/**
			 * @private
			 */
			populateIdsOnTableOptions: function(jsti, baseType) {
				
				if (!this.display)
				{
					return;
				}
				
				if (this.display.properties && this.display.properties.length > 0)
				{
					km.js.utils.populateIdsOnProperties(this.display.properties, baseType, jsti);
				}
				
				// populate ID property PIR basing on its name - if not already populated
				if (this.display.idProperty)
				{
					if (!this.display.idProperty.id)
					{
						if (this.display.idProperty.name)
						{
							this.display.idProperty.id = km.js.utils.nestedPropertyToPir(this.display.idProperty.name, baseType, jsti);
						}
						else
						{
							throw "Neither ID property name nor its ID defined in table options";
						}
					}
				}
				else
				{
					throw "ID property not defined in km.js.table options";
				}
			},
			
			updateButtonPanel: function() {
				
				if (km.js.utils.isEmpty(this.generatedCode))
				{
					throw "Cannot call updateHeaders when no table code has been generated";
				}
				
				if (!this.buttonPanel || !this.buttonPanel.buttons || this.buttonPanel.buttons.length === 0)
				{
					return;
				}
				
				if (typeof(this.buttonPanel.render) !== "function")
				{
					throw "Button panel defined on km.table does not have function render(). It may not have been created using km.buttonpanel.create()";
				}
				
				// render the button panel into the btn panel container
				this.buttonPanel.render(this.generatedCode.find(".km-btn-panel-container"));
			},
			
			updateHeaders: function() {
				
				if (km.js.utils.isEmpty(this.generatedCode))
				{
					throw "Cannot call updateHeaders when no table code has been generated";
				}
				
				// switch grouping icons in headers according to JCR
				
				if (!km.js.utils.isEmpty(jcr.groupings))
				{
					for (var i = 0; i < jcr.groupings.length; i++)
					{
						var propId = jcr.groupings[i].property_id;
						this.generatedCode.find("#" + this.settings.id + "-group-btn-" + propId.replace(/\./g, "-")).attr("src", this.imagePath + "linkh.png");  
					}
				}
				
				for (var i = 0; i < jcr.properties.length; i++)
				{
					var prop = jcr.properties[i];
					var aggrSelect = this.generatedCode.find("#" + this.settings.id + " .kmjsth select#aggr-" + prop.id.replace(/\./g, "-"));
					
					var dataTypeId = null;
					
					if (this.datasource.type !== "json")
					{
						dataTypeId = this.datasource.jsti.fields[km.js.utils.lastProperty(prop.id)].dataType.id;
					}
					else
					{
						// for json collections, assume all fields are of data type text
						// in the future this could be extended
						dataTypeId = km.js.datatypes.text.id;
					}
					
					var aggrFunctions = this.aggrFunctionsForDataType(dataTypeId);
					
					var code = "";
					for (var k = 0; k < aggrFunctions.length; k++)
					{
						code += "<option value=\"" + aggrFunctions[k].name + "\">" + aggrFunctions[k].label + "</option>";
					}
					
					aggrSelect.html(code);
					
					if (!km.js.utils.isEmpty(prop.aggr))
					{
						aggrSelect.val(prop.aggr).show();
					}
				}
			},
			
			container: function() {
				return $("#km-table-container-" + this.settings.id);
			},
			
			/**
			 * Generates basic code of the table.
			 * @private
			 */
			getCode: function(data, unpagedDataCount) {
			
				var code = $("<div id=\"km-table-container-" + this.settings.id + "\"></div>").addClass("km-table-container");
				
				if (this.title && this.title.placement === "top")
				{
					// render title
					var title = $("<div></div>").text(this.title.text).addClass("km-record-table-title");
					
					if (this.title.cssClass)
					{
						title.addClass(this.title.cssClass);
					}
					
					code.append(title);
				}
				
				code.append($("<div class=\"km-btn-panel-container\"></div>"));
				
				var tableWrapper = $("<div id=\"km-table-wrapper-" + this.settings.id + "\" class=\"km-table-wrapper\"></div>");
				
				if (this.settings.wrapperCssClass)
				{
					tableWrapper.addClass(this.settings.wrapperCssClass);
				}
				
				code.append(tableWrapper);
				
				var table = $("<div class=\"kmjstable " + this.settings.cssClass + "\" id=\"" + this.settings.id + "\"></div>");
				table.append(this.renderHeaders());
				
				// append body to the table
				table.append($("<div class=\"kmjstb\"></div>"));
				
				code.append(table);
				
				// add footer container
				code.append($("<div class=\"kmjstf\"></div>"));
				
				this.generatedCode = code;
				this.generatedCode.find(".kmjstb").html(this.renderRows(data, unpagedDataCount));
				return this.generatedCode;
			},

			rerenderFooter: function() {
				
				if (km.js.utils.isEmpty(this.generatedCode))
				{
					throw "Cannot call rerenderFooter when no table code has been generated";
				}
				
				//$(".km-table-wrapper-" + this.settings.id + " .kmjstf").replaceWith(this.renderFooter());
				this.generatedCode.find(".kmjstf").replaceWith(this.renderFooter());
			},
			
			renderFooter: function() {
			
				var elem = $("<div class=\"kmjstf\"></div>");
				var footerRow = $("<div class=\"kmjstr\"></div>");
				
				var pageCounterMax = this.pagination.pageCount > 15 ? 15 : this.pagination.pageCount;
				
				// if pagination is on, render pagination row
				if (this.pagination.active)
				{
					var pagination = $("<div id=\"pagination-" + this.settings.id + "\" class=\"km-pagination\"></div>");
					
					pagination.append($("<a href=\"javascript:;\" class=\"km-first km-page-no\"></a>").html("&lt;&lt;"));
					
					// add previous button
					pagination.append($("<a href=\"javascript:;\" class=\"km-prev km-page-no\"></a>").html("&lt;"));
					
					var pageNums = $("<div class=\"km-page-no-no-ellipsis\"></div>");
					
					for (var i = 0; i < pageCounterMax; i++)
					{
						pageNums.append($("<a href=\"javascript:;\" class=\"km-page-no km-page-" + i + "\">" + (i + 1) + "</a>"));
					}
					
					pagination.append(pageNums);
					
					if (this.pagination.pageCount > pageCounterMax)
					{
						var pageNums2 = $("<div class=\"km-page-no-ellipsis\"></div>");
						for (var i = pageCounterMax; i < (this.pagination.pageCount - 1); i++)
						{
							pageNums2.append($("<a href=\"javascript:;\" class=\"km-page-no km-page-" + i + "\">" + (i + 1) + "</a>"));
						}
						
						pagination.append(pageNums2);
					}
					
					var pageNums3 = $("<div class=\"km-page-no-no-ellipsis\">");
					
					if (this.pagination.pageCount > pageCounterMax)
					{
						// add last page number
						pageNums3.append($("<a href=\"javascript:;\" class=\"km-page-no km-page-" + (this.pagination.pageCount - 1) + "\">" + this.pagination.pageCount + "</a>"));
					}
					
					pagination.append(pageNums3);
					
					// add next button
					pagination.append($("<a href=\"javascript:;\" class=\"km-next km-page-no\"></a>").html("&gt;"));
				}
				
				footerRow.append(pagination);
				
				var exportCSV = false;
				var exportXLSX = false;
				
				// add export buttons
				if (!km.js.utils.isEmpty(this.settings.exportFormats) && this.settings.exportFormats.length > 0)
				{
					var exportBtnRow = $("<div class=\"kmjstr\"></div>");
					var exportBtns = $("<div class=\"km-export-btns\"></div>");
					for (var i = 0; i < this.settings.exportFormats.length; i++)
					{
						if (this.settings.exportFormats[i] === "csv")
						{
							exportCSV = true;
							exportBtns.append($("<input type=\"button\" class=\"sbtn km-export-csv\" id=\"" + this.id + "-csv\" value=\"CSV\">"));
							exportBtns.append($("<form id=\"" + this.settings.id + "-csv-form\" method=\"post\" style=\"display:none\"><input type=\"hidden\" name=\"jcr\"></form>"));
						}
						else if (this.settings.exportFormats[i] === "xlsx")
						{
							exportXLSX = true;
							exportBtns.append($("<input type=\"button\" class=\"sbtn km-export-xlsx\" id=\"" + this.id + "-xlsx\" value=\"Excel\">"));
							exportBtns.append($("<form id=\"" + this.settings.id + "-xlsx-form\" method=\"post\" style=\"display:none\"><input type=\"hidden\" name=\"jcr\"></form>"));
						}
						else
						{
							throw "Unsupported export format " + this.options.exportFormats[i];
						}
					}

					exportBtnRow.append(exportBtns);
					footerRow.append(exportBtns);
				}
				
				elem.append(footerRow);
				
				var prevCallback = function(table) {
					return function() {
						table.prevPage();
					}
				};
				
				var nextCallback = function(table) {
					return function() {
						table.nextPage();
					}
				};
				
				var goToPageCallback = function(table, pageNo) {
					return function() {
						table.goToPage(pageNo);
					}
				};
				
				if (exportCSV === true)
				{
					elem.find(".km-export-csv").click((function(table) {
						return function() {
							table.exportToCsv();
						}
					})(this));
				}
				
				if (exportXLSX === true)
				{
					elem.find(".km-export-xlsx").click((function(table) {
						return function() {
							table.exportToXlsx();
						}
					})(this));
				}
				
				elem.find(".km-prev").click(prevCallback(this));
				elem.find(".km-next").click(nextCallback(this));
				elem.find(".km-first").click(goToPageCallback(this, 0));
				elem.find(".km-last").click(goToPageCallback(this, this.pagination.pageCount - 1));
				
				for (var i = 0; i < pageCounterMax; i++)
				{
					elem.find(".km-page-" + i).click(goToPageCallback(this, i));
				}
				
				if (this.pagination.pageCount > pageCounterMax)
				{
					elem.find(".km-page-" + (this.pagination.pageCount - 1)).click(goToPageCallback(this, (this.pagination.pageCount - 1)));
				}
				
				return elem;
			},
			
			renderHeaders: function() {
			
				var code = "<div class=\"kmjsth\">";
				// start row
				code += "<div class=\"kmjstr\">";
				
				// create table header
				for (var i = 0; i < this.display.properties.length; i++)
				{
					code += "<div class=\"kmjstd th-" + this.display.properties[i].id.replace(/\./g, "-") + "\">" + this.display.properties[i].label + "</div>";
				}
				
				// end row and thead
				code += "</div></div>";
				
				return $(code);
			},
			
			aggrFunctionsForDataType: function(dataTypeId) {
			
				var commonAggr = [
					{ name: "count", label: "zliczanie" }
				];
				
				if (dataTypeId === 0)
				{
					commonAggr.push({ name: "sum", label: "suma" });
					commonAggr.push({ name: "avg", label: "srednia" });
					commonAggr.push({ name: "min", label: "minimum" });
					commonAggr.push({ name: "max", label: "maksimum" });
				}
				
				return commonAggr;
			},

			/**
			 * Returns the default comparison operator to be used with field of the given data type.
			 * @param dataTypeId - numeric ID of the data type
			 */
			defaultRestrictionOperatorForType: function(dataTypeId) {
				if (dataTypeId === km.js.datatypes.bool.id || dataTypeId  === km.js.datatypes.number.id)
				{
					return "eq";
				}
				else
				{
					return "ilike";
					//console.log("No default operator defined for data type ID " + dataTypeId);
				}
			},
			
			waitModeOn: function() {
				$("#" + this.settings.id + " > .kmjstb").css({'filter':'alpha(opacity=40)', 'zoom':'1', 'opacity':'0.4'});
			},
			
			waitModeOff: function() {
				$("#" + this.settings.id + " > .kmjstb").css({'filter':'alpha(opacity=100)', 'zoom':'1', 'opacity':'1.0'});
			},
			
			renderRows: function(filteredData, unpagedDataCount) {
				
				if (!filteredData)
				{
					return "";
				}
				
				if (this.pagination.active)
				{
					this.pagination.pageCount = Math.ceil(unpagedDataCount / this.pagination.pageSize);
				}
				
				// This function takes a string containing pseudo-REL expressions
				// e.g. details?id={id} and substitutes parameters in curly brackets with record field values
				var interpreteRecordFields = function(s, recordId, record) {
					return s.replace(/{(.*?)}/g, function(matchedSubstring, group) {
						
						if (group === "id")
						{
							return recordId;
						}
						else
						{
							return km.js.utils.propVal(record, group);
							//throw "Cannot interprete property " + group + ". Only property \"id\" is supported.";
						}
					});
				};
				
				var rows = [];
				
				// tells if property names have been initialized on records
				var isPropNamesInitialized = false;
				
				if (!this.datasource.type === "database")
				{
					if (this.datasource.jsti)
					{
						filteredData = km.js.utils.addPropertyNamesToJSRC(filteredData, this.datasource.jsti);
						isPropNamesInitialized = true;
					}
					else
					{
						throw "JSTI not defined on database datasource";
					}
				}
				
				// tells if we need to call km.js.utils.addPropertyNamesToJSRC on records to initialize property names
				var isAssigningPropNamesNeeded = false;
				
				for (var i = 0; i < this.display.properties.length; i++)
				{
					var prop = this.display.properties[i];
					
					// check if this property URL contains property name references that need to be interpreted
					if (prop.url && /{(.*?)}/.test(prop.url) && !isPropNamesInitialized)
					{
						if (this.datasource.type === "database" && this.datasource.jsti)
						{
							filteredData = km.js.utils.addPropertyNamesToJSRC(filteredData, this.datasource.jsti);
							isPropNamesInitialized = true;
						}
						else
						{
							// we just show a warning instead of error, because it is possible that the property names are already set if they were received
							// from a non-database DS (is this correct? or are they never set in this case?)
							console.warn("Requested initializing properties on record set, but it was impossible due to missing JSTI");
						}
					}
				}
				
				if (filteredData.length)
				{
					for (var i = 0; i < filteredData.length; i++)
					{
						var rec = filteredData[i];
						
						// start row
						var rowCode = "<div class=\"kmjstr\"";
						var idValue = null;
						
						// add row ID
						if (!km.js.utils.isEmpty(this.display.idProperty.id))
						{
							idValue = rec[this.display.idProperty.id];
							if (!idValue)
							{
								throw "ID property " + this.display.idProperty.id + " not retrieved in query";
							}
							rowCode += " id=\"" + idValue + "\"";
						}
						
						rowCode += "></div>";
						
						var elem = $(rowCode);
						
						for (var k = 0; k < this.display.properties.length; k++)
						{
							var prop = this.display.properties[k];
							var val = prop.id ? km.js.utils.propVal(rec, prop.id) : rec;
							
							// apply custom formatting to field value if defined
							if (typeof(prop.format) === "function")
							{
								val = prop.format(val, rec);
							}
							
							if (val == null)
							{
								val = "";
							}
							cellCode = "<div class=\"kmjstd kmjstd-" + prop.id.replace(/\./g, "-") + "";
							
							if (prop.linkStyle === true)
							{
								// style this cell's content as a link
								cellCode += " kmjstda";
							}
							
							cellCode += "\"></div>";
							var cell = $(cellCode);
							
							// for mobile devices, append hidden label
							var mobileLabel = $("<div></div>").addClass("km-prop-mobile-label").text(prop.label);
							cell.append(mobileLabel);
							
							var cellContent = null;
							
							if (!km.js.utils.isEmpty(prop.url))
							{
								// if URL is specified, render this property as link
								cellContent = $("<a href=\"" + interpreteRecordFields(prop.url, rec[this.display.idProperty.id], rec) + "\">" + val + "</a>");
							}
							else if (val instanceof jQuery)
							{
								cellContent = val;
							}
							else
							{
								cellContent = val;
							}
							
							cell.append(cellContent);
							
							if (prop.content && typeof(prop.content) === "function")
							{
								// call a callback on this cell with the value that is either the record
								// or the record field's value
								prop.content(rec, idValue, cell);
							}
							
							elem.append(cell);
						}
						
						rows.push(elem);
					}
				}
				else
				{
					// no results
					var row = $("<div></div>").addClass("kmjstr km-table-no-results");
					var cell = $("<div></div>").addClass("kmjstd").text(km.js.config.i18n["msg.noresults"]);
					row.append(cell);
					
					for (var k = 1; k < this.display.properties.length; k++)
					{
						row.append($("<div></div>").addClass("kmjstd"));
					}
					
					rows.push(row);
				}
				
				// append onClick events
				for (var i = 0; i < this.display.properties.length; i++)
				{
					var prop = this.display.properties[i];
					if (typeof(prop.onClick) === "function")
					{
						var clickCallback = function(func) {
							return function() {
								var rowId = $(this).closest(".kmjstr").attr("id");
								func(rowId);
							}
						}
						
						for (var k = 0; k < rows.length; k++)
						{
							rows[k].find(".kmjstd-" + prop.id.replace(/\./g, "-")).click(clickCallback(prop.onClick));
						}
					}
				}
				
				return rows;
			},
			
			// switches view to the specified page, does all the rerendering
			// @param pageIndex - zero-based
			goToPage: function(pageIndex) {
				this.pagination.currentPage = pageIndex;
				this.jcr.offset = this.pagination.pageSize * pageIndex;
				this.update();
			},
			
			prevPage: function() {
				if (this.pagination.currentPage > 0) {
					this.goToPage(this.pagination.currentPage - 1);
				}
			},
			
			nextPage: function() {
				if (this.pagination.currentPage < (this.pagination.pageCount - 1)) {
					this.goToPage(this.pagination.currentPage + 1);
				}
			},
			
			// this is the main function to be called in order to refresh the table
			// after JCR or data has changed
			update: function(onComplete) {
				
				var filteredData = [].concat(this.data);
				
				// callback function to be called when data is fetched from datasource
				var onFetchData = function(table, onComplete) {
					return function(data, unpagedDataCount) {
						table.rerenderRows(data, unpagedDataCount);
						table.rerenderFooter();
						table.updatePaginationDisplay();
						
						table.data = [].concat(data);
						table.unpagedDataCount = unpagedDataCount;
						
						if (typeof(onComplete) === "function")
						{
							onComplete(table);
						}
					}	
				}
				
				// if jcr is specified, filter datasource using this jcr to get data to display
				if (!km.js.utils.isEmpty(jcr))
				{
					this.datasource.query(jcr, onFetchData(this, onComplete));
				}
				else
				{
					(onFetchData(this))(filteredData, this.unpagedDataCount)
				}
			},
			
			updatePaginationDisplay: function() {
				
				if (km.js.utils.isEmpty(this.generatedCode))
				{
					throw "Cannot call updatePaginationDisplay when no table code has been generated";
				}
				
				this.generatedCode.find("#pagination-" + this.settings.id + " .km-page-no").removeClass("km-page-no-hl")
				this.generatedCode.find("#pagination-" + this.settings.id + " .km-page-" + this.pagination.currentPage).addClass("km-page-no-hl")
				
				if (this.pagination.currentPage == 0)
				{
					this.generatedCode.find("#pagination-" + this.settings.id + " .km-page-0").addClass("km-page-no-hl");
					this.generatedCode.find("#pagination-" + this.settings.id + " .km-prev").addClass("km-page-no-hl");
					this.generatedCode.find("#pagination-" + this.settings.id + " .km-first").addClass("km-page-no-hl");
				}
				
				if (this.pagination.currentPage == (this.pagination.pageCount - 1))
				{
					this.generatedCode.find("#pagination-" + this.settings.id + " .km-page-" + this.pagination.currentPage).addClass("km-page-no-hl");
					this.generatedCode.find("#pagination-" + this.settings.id + " .km-next").addClass("km-page-no-hl");
					this.generatedCode.find("#pagination-" + this.settings.id + " .km-last").addClass("km-page-no-hl");
				}
			},
			
			setPageSize: function(size) {
				this.pagination.pageSize = size;
				this.jcr.offset = this.pagination.pageSize * pageIndex;
				this.update();
				
				// rerender footer because page numbers will change when page size changes
				this.rerenderFooter();
			},
			
			// Move column at index sourceIndex to index targetIndex. Index arguments are indices of the column in the table, zero-based.
			moveColumn: function(sourceIndex, targetIndex) {
			
				if (sourceIndex === targetIndex)
				{
					return;
				}
			
				// for every row in the table, move cells within row
				$("#" + this.settings.id + " .kmjstr").each(function() {
				
					// find cells to move
					var sourceCell = $(this).children().eq(sourceIndex);
					var targetCell = $(this).children().eq(targetIndex);
					
					if (targetIndex > sourceIndex)
					{
						targetCell.after(sourceCell);
						// now move the property in display properties
					}
					else
					{
						targetCell.before(sourceCell);
					}
				});
				
				var sourceProp = this.display.properties[sourceIndex];
				km.js.utils.remove(sourceIndex, this.display.properties);
				km.js.utils.insert(targetIndex, this.display.properties, sourceProp);
			},
			
			// This function turns on dragging of column headers
			draggableColumns: function() {
				
				if (km.js.utils.isEmpty(this.generatedCode))
				{
					throw "Cannot call draggableColumns when no table code has been generated";
				}
				
				// use closure to give internal event handlers access to the table ("this") object
				var func = function(table) {
					
					for (var i = 0; i < table.display.properties.length; i++)
					{
						var prop = table.display.properties[i];
						
						if (prop.draggable !== true)
						{
							continue;
						}
						
						var dashedPropId = prop.id.replace(/\./g, "-");
						var headerCell = table.generatedCode.find(".kmjsth > .kmjstr > .th-" + dashedPropId);
						
						headerCell.draggable({
							
							// used cloned helper while dragging
							helper: "clone",
							
							start: function(event, ui) {
								
								var index = $(this).index();
								
								// keep track of the index of the column that is dragged
								// so that we can move it when it is dropped
								table.activeDragColumnIndex = index;
								
								var col = $("<div id=\"drag-col-" + table.settings.id + "\"></div>");
								
								// build div containing all cells of the column
								var i = 0;
								table.generatedCode.find(".kmjstr .kmjstd:nth-child(" + (index + 1) + ")").each(function() {
									if (i < table.settings.dragRowCount)
									{
										col.append($(this).clone());
										i++;
									}
									else
									{
										return false;
									}	
								});
								
								$(ui.helper).html(col);
								$(ui.helper).css("border", "1px dotted #666");
								$(ui.helper).css("background-color", "#eee").css("color", "#555").css("opacity", "0.9");
							},
							
							cursor: "move",
							
							stop: function() {
								// when dragging stops, just hide the helper
								var col = $("#drag-col-" + table.settings.id);
								col.remove();
							}
						});
					}
					
					table.generatedCode.find(".kmjsth > .kmjstr > .kmjstd").droppable({
						drop: function(event, ui) {
						
							// only react to table cells being dropped
							// ignore other elements
							if ($(ui.draggable).hasClass("kmjstd")){
								// when column is dropped, move it to where it was dropped
								table.moveColumn(table.activeDragColumnIndex, $(this).index());
								table.activeDragColumnIndex = null;
							}
						},
						tolerance: "pointer"
					});
				};
				
				return func(this);
				
			},
			
			exportToXlsx: function() {
				
				var table = this;
				
				$("#" + table.settings.id + "-xlsx-form").attr("action", km.js.config.sysContextPath + "/rest/jsds/query?format=xlsx&exportFileName=" + (table.settings.exportFileName ? table.settings.exportFileName : ""));
				
				// jcr.properties may contain more properties than are displayed by the table, 
				// but we want to include only the displayed properties in the CSV
				// TODO preserve only properties from display in the table.jcr criteria
				var xlsxJCR = $.extend({}, table.jcr);
				
				// TODO this is incorrect because it discards all limit and offset settings
				// on the JCR. What it should do is to discard only limit and offset settings
				// added during table pagination, but keep those that may have existed in the
				// original datasource.
				xlsxJCR.offset = null;
				xlsxJCR.limit = null;
				
				xlsxJCR.properties = [];
				for (var i = 0; i < table.display.properties.length; i++)
				{
					var found = null;
					for (var k = 0; i < table.jcr.properties.length; k++)
					{
						if (table.jcr.properties[k].id === table.display.properties[i].id)
						{
							found = table.jcr.properties[k];
							break;
						}
					}
					
					if (found)
					{
						xlsxJCR.properties.push(found);
					}
				}
				
				$("#" + table.settings.id + "-xlsx-form > input[name='jcr']").val(JSON.stringify(xlsxJCR));
				$("#" + table.settings.id + "-xlsx-form").submit();
			},
			
			exportToCsv: function() {
				
				var table = this;
	
				$("#" + table.settings.id + "-csv-form").attr("action", km.js.config.sysContextPath + "/rest/jsds/query?format=csv&exportFileName=" + (table.settings.exportFileName ? table.settings.exportFileName : ""));
				
				// jcr.properties may contain more properties than are displayed by the table, 
				// but we want to include only the displayed properties in the CSV
				// TODO preserve only properties from display in the table.jcr criteria
				var csvJCR = $.extend({}, table.jcr);
				
				// TODO this is incorrect because it discards all limit and offset settings
				// on the JCR. What it should do is to discard only limit and offset settings
				// added during table pagination, but keep those that may have existed in the
				// original datasource.
				csvJCR.offset = null;
				csvJCR.limit = null;
				
				csvJCR.properties = [];
				for (var i = 0; i < table.display.properties.length; i++)
				{
					var found = null;
					for (var k = 0; k < table.jcr.properties.length; k++)
					{
						if (table.jcr.properties[k].id === table.display.properties[i].id)
						{
							found = table.jcr.properties[k];
							break;
						}
					}
					
					if (found)
					{
						csvJCR.properties.push(found);
					}
				}
				
				//$("#" + table.settings.id + "-csv-form > input[name='jcr']").val(JSON.stringify(table.jcr));
				$("#" + table.settings.id + "-csv-form > input[name='jcr']").val(JSON.stringify(csvJCR));
				$("#" + table.settings.id + "-csv-form").submit();
			},
			
			/**
			 * Calls a sort action on a column identified by the given PIR.
			 */
			sort: function (pir) {
				
				var dashedProp = pir.replace(/\./, "-");
				
				// find header cell for this property
				var headerCell = this.container().find(".th-" + dashedProp);
				
				this.waitModeOn();
				var sortDirection = this.sortOrder === "asc" ? "desc" : "asc";
				
				// set all sort buttons in the table to default
				headerCell.closest(".kmjsth").find(".kmjstd .sort-btn").attr("src", this.imagePath + "sort.png");
				
				// set this columns sort button
				headerCell.find(".sort-btn").attr("src", this.imagePath + "sort" + sortDirection + ".png");
				this.sortOrder = sortDirection;
				
				this.jcr.orderings = [ { property_id: pir, direction: sortDirection } ];
				
				var onFetchData = function(table) {
					return function(data, unpagedDataCount) {
						table.rerenderRows(data, unpagedDataCount, {
							onComplete: function(table) { table.waitModeOff(); }
						});
						
						table.data = [].concat(data);
						table.unpagedDataCount = unpagedDataCount;
					}	
				}
				
				// filter datasource using this jcr to get data to display
				this.datasource.query(jcr, onFetchData(this));
	
			},
			
			initHeaderFilters: function() {
				
				var func = function(table) {
					
					if (km.js.utils.isEmpty(table.generatedCode))
					{
						throw "Cannot call initHeaderFilters when no table code has been generated";
					}
				
					for (var i = 0; i < table.display.properties.length; i++)
					{
						var prop = table.display.properties[i];
						var dashedPropId = prop.id.replace(/\./g, "-");
						
						var dataType = null;
						
						if (table.datasource.type === "database")
						{
							dataType = table.datasource.jsti.fields[km.js.utils.lastProperty(prop.id)].dataType.id;
							
							// note that dataType can be 0 (i.e. number), so simply checking the condition (!dataType) is not enough, because for dataType == 0, the condition is met
							if (!dataType && dataType !== 0)
							{
								throw "Data type not retrieved for property " + JSON.stringify(prop);
							}
						}
						
						// find header for this property
						var headerCell = table.generatedCode.find(".kmjsth > .kmjstr > .th-" + dashedPropId);
						
						var label = prop.label;
						var propId = prop.id;
						
						var hcode = "<span class=\"km-th-label\">" + label + "</span>";
						
						if (prop.filterable === true)
						{
							hcode += "<div class=\"filter-val\"></div>";
						}
						
						hcode += "<div class=\"km-label-btns\">";
						
						if (prop.groupable === true)
						{
							hcode += "<div class=\"km-header-btn\"><img class=\"group-btn\" id=\"" + table.settings.id + "-group-btn-" + dashedPropId + "\" src=\"" + table.imagePath + "link.png\"></div>";
						}
						
						if (prop.filterable === true)
						{
							hcode += "<div class=\"km-header-btn\"><img class=\"filter-btn\" id=\"" + table.settings.id + "-filter-btn-" + dashedPropId + "\" src=\"" + table.imagePath + "filter.png\"></div>";
						}
						
						if (prop.sortable === true)
						{
							// when querying a database data source, you cannot sort by formula fields
							if (table.datasource.type === "database" && dataType === km.js.datatypes.formula.id)
							{
								console.warn("Sorting by formula field " + prop.id + " skipped");
								prop.sortable = false;
							}
							else
							{
								hcode += "<div class=\"km-header-btn\"><img class=\"sort-btn\" id=\"" + table.settings.id + "-sort-btn-" + dashedPropId + "\" src=\"" + table.imagePath + "sort.png\"></div>";
							}
						}
						
						if (prop.removable === true)
						{
							hcode += "<div class=\"km-header-btn\"><img class=\"del-btn\" id=\"" + table.settings.id + "-del-btn-" + dashedPropId + "\" src=\"" + table.imagePath + "exb.png\"></div>";
						}
						
						hcode += "<select class=\"aggr-select\" id=\"aggr-" + dashedPropId + "\">";
						
						hcode += "<option value=\"count\">zliczanie</option>";
						hcode += "<option value=\"sum\">suma</option>";
						hcode += "<option value=\"avg\">srednia</option>";
						hcode += "<option value=\"min\">minimum</option>";
						hcode += "<option value=\"max\">maksimum</option>";
						hcode += "</select>";
						
						hcode += "</div>";
						
						if (prop.filterable === true)
						{
							hcode += "<div><input class=\"km-js-th-input th-input-" + dashedPropId + "\" type=\"text\" placeholder=\"filtr\"></div>";
						}
						
						headerCell.html(hcode);
						
						if (prop.filterable === true)
						{
							// attach event for showing input when label is clicked
							headerCell.find("img.filter-btn").each(function() {
							
								$(this).click(function() {
								
									var filterInput = $(this).closest(".kmjstd").find("input.km-js-th-input");
									
									if (filterInput.is(":visible"))
									{
										filterInput.hide();
									}
									else
									{
										$(this).closest(".kmjstr").find(".kmjstd input.km-js-th-input").hide();
										$(this).closest(".kmjstd").find(".filter-val").hide();
										filterInput.show();
									}
								});
							});
						}
						
						if (prop.sortable === true)
						{
							// attach a sort event to the column's label
							headerCell.find(".km-th-label").click((function(table, prop) {
								return function() {	table.sort(prop.id); }
							})(table, prop));
							
							// attach event for sorting
							headerCell.find("img.sort-btn").each(function() {
							
								$(this).click(function() {
								
									table.waitModeOn();
									var propId = $(this).attr("id").substring((table.settings.id + "-sort-btn-").length);
									// property in the class name may contain dashes (if nested)
									propId = propId.replace(/\-/g, ".");
									
									var sortDirection = table.sortOrder === "asc" ? "desc" : "asc";
									$(this).closest(".kmjsth").find(".kmjstd .sort-btn").attr("src", table.imagePath + "sort.png");
									$(this).attr("src", table.imagePath + "sort" + sortDirection + ".png");
									table.sortOrder = sortDirection;
									
									table.jcr.orderings = [ { property_id: propId, direction: sortDirection } ];
									
									var onFetchData = function(table) {
										return function(data, unpagedDataCount) {
											table.rerenderRows(data, unpagedDataCount, {
												onComplete: function(table) { table.waitModeOff(); }
											});
											
											table.data = [].concat(data);
											table.unpagedDataCount = unpagedDataCount;
										}	
									}
									
									// filter datasource using this jcr to get data to display
									table.datasource.query(table.jcr, onFetchData(table));
								});
							});
						}
						
						// attach event for removing column if removal is defined for this column
						if (prop.removable === true)
						{
							headerCell.find("img.del-btn").each(function() {
							
								$(this).click(function() {
								
									var propId = $(this).attr("id").substring((table.settings.id + "-del-btn-").length);
									
									// check if this property is grouped
									var isGrouped = table.isGrouped(table.datasource.jcr, propId);
									
									// property in the class name may contain dashes (if nested)
									propId = propId.replace(/\-/g, ".");
									
									table.removeColumn(propId, true);
									
									// if the property was grouped, removing it affects other columns
									// so we need to rerender the whole table
									if (isGrouped)
									{
										table.update();
									}
								});
							});
						}
						
						// attach event for selecting aggregate function
						headerCell.find(".aggr-select").each(function() {
							
							$(this).change(function() {
								
								var propId = $(this).attr("id").substring("aggr-".length);
								table.setAggr(jcr, propId, $("#aggr-" + propId).val());
								
								// rerender table
								table.update();
							});
							
						});
						
						if (prop.groupable === true)
						{
							headerCell.find(".group-btn").each(function() {
								$(this).click(function() {
									
									// find property ID for which grouping was called
									var propId = $(this).attr("id").substring($(this).attr("id").indexOf("-group-btn") + "-group-btn".length + 1);
									// property in the class name may contain dashes (if nested)
									propId = propId.replace(/\-/g, ".");
									
									if (table.isGrouped(jcr, propId))
									{
										// remove grouping
										$(this).attr("src", table.imagePath + "link.png");
										table.setGrouping(table.jcr, null);
										
										// hide aggr selection
										var row = $(this).closest(".kmjstr");
										row.find("select.aggr-select").hide();
									}
									else
									{
										var row = $(this).closest(".kmjstr");
										row.find("img.group-btn").attr("src", table.imagePath + "link.png");
										$(this).attr("src", table.imagePath + "linkh.png");
										
										table.setGrouping(table.jcr, propId);
										
										// show aggregate function choice
										row.find("select.aggr-select").show();
										row.find("select#aggr-" + propId).hide();
									}
									
									// rerender table
									table.update();
								});
							});
						}
						
						if (prop.filterable === true)
						{
							// attach event for hiding input
							headerCell.find("input.km-js-th-input").each(function() {
							
								// hide input when it loses focus
								$(this).focusout(function() {
									$(this).hide();
								});
								
								// add live search
								$(this).keyup(function(e) {
								
									var classes = $(this).attr("class").split(/\s+/);
									var propId = null;
									for (var i = 0; i < classes.length; i++)
									{
										if (classes[i].indexOf("th-input-") == 0)
										{
											propId = classes[i].substring("th-input-".length);
										}
									}
									
									// property in the class name may contain dashes (if nested)
									propId = propId.replace(/\-/g, ".");
									
									if (km.js.utils.isEmpty($(this).val()))
									{
										table.removeRestrictions(table.jcr, propId);
										
										// change filter button to unhighlighted
										$(this).closest(".kmjstd").find(".filter-btn").attr("src", table.imagePath + "filter.png");
									}
									else
									{
										table.addRestriction (table.jcr, propId, $(this).val());
										
										// we need to go back to the first page, otherwise we could end up on
										// and non-existing page if the amount of results decreases
										table.goToPage(0);
										
										// change filter button to highlighted
										$(this).closest(".kmjstd").find(".filter-btn").attr("src", table.imagePath + "filterh.png");
									}
									
									// rerender table
									table.update();
								});
							
							});
						
						}
						
					}
				
				}
				
				return func(this);
			},
			
			setAggr: function(jcr, propId, aggr) {
			
				for (var i = 0; i < jcr.properties.length; i++)
				{
					var prop = jcr.properties[i];
					if (prop.id == propId)
					{
						prop.aggr = aggr;
						return;
					}
				}	
			},
			
			isGrouped: function(jcr, propId) {
				
				if (jcr.groupings == null || jcr.groupings.length == 0)
				{
					return false;
				}
				
				for (var i = 0; i < jcr.groupings.length; i++)
				{
					if (jcr.groupings[i].property_id == propId)
					{
						return true;
					}
				}
				
				return false;
			},
			
			/**
			 * Sets or unsets grouping on a the table.
			 * @param jcr - jcr on which grouping is to be set
			 * @param propId - PIR of the property, which should be grouped. If null, all groupings will be removed from the JCR
			 */
			setGrouping: function(jcr, propId) {
				
				// remove all groupings
				jcr.groupings = [];
				
				if (propId != null)
				{
					jcr.groupings.push({
						property_id: propId
					});
					
					// add aggregate function on all other fields, and remove aggregate function on the grouped field
					for (var i = 0; i < jcr.properties.length; i++)
					{
						var prop = jcr.properties[i];
						
						// if an aggregate function was already defined for this property,
						// use it, otherwise use the default aggregate function "count"
						var oldAggr = prop.aggr;
						prop.aggr = prop.id === propId ? null : (km.js.utils.isEmpty(oldAggr) ? "count" : oldAggr);
					}
				}
				else
				{
					// if propId is null, then all groupings are removed, which also means all aggregate functions will be removed
					for (var i = 0; i < jcr.properties.length; i++)
					{
						jcr.properties[i].aggr = null;
					}
				}
			},
			
			removeRestrictions: function(jcr, propId) {
				
				var newRestrictions = [];
				
				for (var i = 0; i < jcr.restrictions.length; i++)
				{
					if (jcr.restrictions[i].property_id != propId)
					{
						newRestrictions.push(jcr.restrictions[i])
					}
				}
				
				jcr.restrictions = newRestrictions;
			},
			
			/**
			 * Get restriction from user input
			 */
			parseRestriction: function(input) {
			
				var restr = { operator: null, args: [] };
				
				if (km.js.utils.isEmpty(input))
				{
					return restr;
				}
			
				input = input.trim();
				
				var words = input.split(/\s+/);
				if (words.length == 1)
				{
					restr.args.push(input);
					return restr;
				}
				
				getRestrictionName = function(op) {
					if (op == ">")
					{
						return "gt";
					}
					else if (op == ">=")
					{
						return "ge";
					}
					else if (op == "<")
					{
						return "lt";
					}
					else if (op == "<=")
					{
						return "le";
					}
					else if (op == "=")
					{
						return "eq";
					}
					else
					{
						return null;
					}
				}
				
				var operatorName = getRestrictionName(words[0]);
				
				// check if input starts with operator
				if (operatorName != null)
				{
					restr.operator = operatorName;
					
					// treat all other words as an arg for the operator
					restr.args.push(input.substring(words[0].length));
				}
				else
				{
					// treat the whole input as restriction arg
					restr.args.push(input);
				}
				
				// return constructed restriction
				return restr;
			},

			removeColumn: function(propId, isRemoveFromConfig) {
				this.removeColumnFromView(propId);
				
				if (!isRemoveFromConfig)
				{
					return;
				}
				
				// remove property from display config
				for (var i = 0; i < this.display.properties.length; i++)
				{
					if (this.display.properties[i].id === propId)
					{
						this.display.properties.splice(i, 1);
						break;
					}
				}
				
				//console.log(JSON.stringify(this.jcr));
				
				// remove property from JCR properties and groupings, but not from JCR restrictions
				var groupings = this.jcr.groupings;
				
				// tells whether the removed property was used in groupings
				var isGrouped = false;
				
				if (!km.js.utils.isEmpty(groupings))
				{
					for (var i = 0; i < groupings.length; i++)
					{
						if (groupings[i].property_id === propId)
						{
							groupings.splice(i, 1);
							isGrouped = true;
							break;
						}
					}
				}
				
				// remove property from JCR properties and groupings, but not from JCR restrictions
				var jcrProps = this.jcr.properties;
				for (var i = 0; i < jcrProps.length; i++)
				{
					if (jcrProps[i].id === propId)
					{
						jcrProps.splice(i, 1);
						break;
					}
					else if (isGrouped)
					{
						// if a property has been removed by which a grouping was made,
						// we will want to remove aggregate function from all other properties
						jcrProps[i].aggr = null;
						console.log("rem for " + jcrProps[i].id);
					}
				}
				
				if (typeof(this.settings.afterRemoveColumn) === "function")
				{
					this.settings.afterRemoveColumn(propId);
				}
			},
			
			// for every cell in a column related to the given property, execute a callback function
			everyCell: function(propId, callback) {
			
				var dashedPropId = propId.replace(/\./g, "-");
				var th = $("#" + this.settings.id + " .th-" + dashedPropId);
				var index = th.index();
				
				if (index < 0)
				{
					return;
				}
				
				var removeFunc = function(table, callback) {
					$("#" + table.settings.id + " .kmjstr").each(function() {
						callback($(this).children().eq(index));
					});
				}
				
				removeFunc(this, callback);
			},
			
			// Remove column from display configuration and from JCR
			removeColumnFromView: function(propId) {
				this.everyCell(propId, function(cell) { cell.remove() });
			},
			
			hideColumn: function(propId) {
				this.everyCell(propId, function(cell) { cell.fadeOut(400) });
			},
			
			showColumn: function(propId) {
				this.everyCell(propId, function(cell) { cell.fadeIn(400) });
			},
			
			setButtonPanel: function(btnPanel) {
				this.buttonPanel = btnPanel;
				
				// rewrite title options
				this.buttonPanel.options.title = this.title;
			},
			
			addRestriction: function(jcr, propId, val) {
			
				// find restrictions for this property
				var restriction = null;
				var restrictionFound = false;
				
				if (!km.js.utils.isEmpty(jcr.restrictions))
				{
					for (var i = 0; i < jcr.restrictions.length; i++)
					{
						if (jcr.restrictions[i].property_id == propId)
						{
							restriction = jcr.restrictions[i];
							restrictionFound = true;
						}
					}
				}
				else
				{
					jcr.restrictions = [];
				}
			
				// if restriction for this property does not exist, create it
				if (restriction == null)
				{
					restriction = { property_id: propId };
				}
				
				// parse the user input to construct a restriction
				var parsedRestriction = this.parseRestriction(val);
				
				var operator = parsedRestriction.operator;
				
				// get the default restriction operator for the data type of this property
				var fieldData = km.js.jsti.fieldData(this.settings.jsti, km.js.utils.lastProperty(propId));
				
				if (km.js.utils.isEmpty(fieldData))
				{
					throw "JSTI does not contain information about field " + propId;
				}
				
				// user's input may or may not contain an operator. E.g. "> 2" does contain an operator, but "john*" does not
				// if no operator is defined, we will use the default operator corresponding to the field's data type
				if (operator == null)
				{	
					operator = this.defaultRestrictionOperatorForType(fieldData.dataType.id);
					
					if (operator.toLowerCase() === "like" || operator.toLowerCase() === "ilike")
					{
						var newArgs = [];
						for (var i = 0; i < parsedRestriction.args.length; i++)
						{
							newArgs.push("%" + parsedRestriction.args[i] + "%");
						}
					}
					
					parsedRestriction.args = newArgs;
					
					console.log("Using default operator " + operator + " for dt " + fieldData.dataType.id);
					//operator = "ilike";
				}
				
				restriction.operator = operator;
				restriction.args = parsedRestriction.args;
				
				// user input args are always strings, but we want to parse them to their
				// real data type
				if (fieldData.dataType.id === km.js.datatypes.number.id)
				{
					var parsedArgs = [];
					for (var i = 0; i < restriction.args.length; i++)
					{
						parsedArgs.push(parseFloat(restriction.args[i]));
					}
					
					restriction.args = parsedArgs;
				}
				else if (fieldData.dataType.id === km.js.datatypes.bool.id)
				{
					var parsedArgs = [];
					for (var i = 0; i < restriction.args.length; i++)
					{
						parsedArgs.push(restriction.args[i] === "true");
					}
					
					restriction.args = parsedArgs;
				}
				
				if (!restrictionFound)
				{
					jcr.restrictions.push(restriction);
				}
			}
		}
		
		tableObj.datasource = ds;
		tableObj.jcr = jcr;
		tableObj.data = ds.data;
		tableObj.display = display;
		tableObj.settings = options;
		tableObj.pagination.pageSize = km.js.utils.isEmpty(settings.pageSize) ? tableObj.pagination.pageSize : settings.pageSize;
		
		if (tableObj.settings.paginationActive === true || tableObj.settings.paginationActive === false)
		{
			tableObj.pagination.active = tableObj.settings.paginationActive;
		}
		
		// for JSON datasources, the IDs of display properties are undefined, because these are simple js object
		// however, the code of km.table operates on property IDs, so we need to rewrite them for unified format for processing
		if (tableObj.datasource.type === "json")
		{
			tableObj = this.adjustJSONDatasource(tableObj);
		}
		
		if (tableObj.pagination.active === true)
		{
			tableObj.jcr.offset = 0;
			tableObj.jcr.limit = tableObj.pagination.pageSize;
		}
		
		tableObj.imagePath = tableObj.settings.contextPath + "/resources/images/";
		return tableObj;
	},
	
	/**
	 * Rewrites property names to property IDs
	 */
	adjustJSONDatasource: function (tableObj) {
		
		var newJCRProps = [];
		for (var i = 0; i < tableObj.jcr.properties.length; i++)
		{
			var prop = tableObj.jcr.properties[i];
			prop.id = prop.name;
			newJCRProps.push(prop);
		}
		
		tableObj.jcr.properties = newJCRProps;
		
		var newDisplayProps = [];
		for (var i = 0; i < tableObj.display.properties.length; i++)
		{
			var prop = tableObj.display.properties[i];
			prop.id = prop.name;
			newDisplayProps.push(prop);
		}
		
		tableObj.display.properties = newDisplayProps;
		
		if (tableObj.jcr.groupings)
		{
			// rewrite JCR groupings
			var newGroupingProps = [];
			for (var i = 0; i < tableObj.jcr.groupings.length; i++)
			{
				var prop = tableObj.jcr.groupings[i];
				prop.id = prop.name;
				newGroupingProps.push(prop);
			}
			
			tableObj.jcr.groupings = newGroupingProps;
		}
		
		if (tableObj.selectedRecordDisplayField)
		{
			tableObj.selectedRecordDisplayField.id = tableObj.selectedRecordDisplayField.name;
		}
		
		if (tableObj.display.idProperty)
		{
			tableObj.display.idProperty.id = tableObj.display.idProperty.name;
		}
		
		if (!tableObj.display.defaultProperty || !tableObj.display.defaultProperty.name)
		{
			throw "Option display.defaultProperty.name needs to be specified when using datasource of type \"json\"";
		}
		
		return tableObj;
		
	}
};
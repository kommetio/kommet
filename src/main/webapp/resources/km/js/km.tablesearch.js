/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.tablesearch = {

	create: function(settings) {
	
		var defaultSettings = {
			columns: 2,
			searchLabel: "Search"
		}
		
		var options = $.extend({}, defaultSettings, settings);
		
		var panel = {
				
			// km.js.table object associated with this search panel
			table: options.table,
			
			// number of columns
			columns: options.columns,
			
			cssClass: options.cssClass,
			
			properties: options.properties,
			
			searchPropertyValues: {},
			
			searchLabel: options.searchLabel,
			
			jsti: options.jsti,
			
			originalRestrictionCount: null,
		
			render: function(callback) {
				
				if (!this.properties || this.properties.length == 0)
				{
					throw "No properties specified in the table search panel";
				}
				
				if (!this.table)
				{
					throw "No table associated with the table search panel"; 
				}
				
				if (!this.table.jcr)
				{
					throw "Table object associated with the search panel has no JCR";
				}
				
				if (!this.table.datasource)
				{
					throw "Data source not set on the associated table";
				}
				
				if (!this.jsti)
				{
					throw "JSTI not set on table search";
				}
				
				var tableSearchDiv = $("<div></div>").addClass("km-table-search").attr("id", "table-search-" + this.table.id);
				
				if (this.cssClass)
				{
					tableSearchDiv.addClass(this.cssClass);
				}
				
				var searchTable = $("<div></div>").addClass("km-table-search-table");
				
				var baseType = this.table.baseType;
				
				if (!baseType)
				{
					if (!this.jsti)
					{
						throw "Base type on table not set and it cannot be retrieved because JSTI on table search is not set";
					}
					else
					{
						baseType = this.jsti.types[this.table.jcr.baseTypeId];
					}
				}
				
				
				km.js.utils.populateIdsOnProperties(this.properties, baseType, this.jsti);
				var row = null;
				
				for (var i = 0; i < this.properties.length; i++)
				{
					if (i % this.columns == 0)
					{
						searchTable.append(row);
						// start new row
						row = $("<div></div>").addClass("km-table-search-row");
					}
					
					var prop = this.properties[i];
					var field = this.jsti.fields[km.js.utils.lastProperty(prop.id)];
					
					var label = prop.label ? prop.label : field.label;
					
					row.append($("<div class=\"km-table-search-cell km-table-search-label\">" + label + "</div>"));
					
					var editCell = $("<div></div>").addClass("km-table-search-cell");
					editCell.append(this.getSearchInput(prop, field.dataType));
					row.append(editCell);
				}
				
				var emptyCells = this.properties.length % this.columns;
				
				// if number of properties modulo number of columns is not zero, we need to render some empty columns
				if (emptyCells !== 0)
				{
					for (var i = 0; i < emptyCells; i++)
					{
						row.append($("<div class=\"km-table-search-cell km-table-search-label\"></div>"));
						var editCell = $("<div></div>").addClass("km-table-search-cell");
						row.append(editCell);
					}
				}
				
				searchTable.append(row);
				
				// add search button to the table button panel or create a new button panel
				var btnPanel = this.table.buttonPanel;
				if (!btnPanel)
				{
					btnPanel = km.js.buttonpanel.create({ id: this.table.id + "-btn-panel" });
				}
				
				var searchBtn = {
					label: this.searchLabel,
					onClick: (function(tableSearch) { return function() { tableSearch.search(); } })(this)
				}
				
				btnPanel.addButton(searchBtn);
				this.table.setButtonPanel(btnPanel);
				
				// append search table to div
				tableSearchDiv.append(searchTable);
				
				// intercept "enter" key press on search panel and trigger search
				tableSearchDiv.keypress((function(tableSearch) {
					
					return function(e) {
						if (e.keyCode == 13) {
							tableSearch.search();
							return false;
						}
					}
					
				})(this));
				
				if (typeof(callback) === "function")
				{
					// invoke callback function with the generated code as parameter
					callback(tableSearchDiv);
				}
				else
				{
					// append the table search as first child in the table container
					this.table.container().prepend(tableSearchDiv);
				}
				
				return tableSearchDiv;
			},
			
			// This method can be called externally to invoke the search action on the
			// table search and the associated table
			search: function() {
				var jcr = this.table.jcr;
				
				if (!jcr)
				{
					throw "Associated table has null JCR";
				}
				
				// start with removing all restrictions with flag "table_search_restriction"
				if (this.table.jcr.restrictions)
				{
					for (var i = 0; i < this.table.jcr.restrictions.length; i++)
					{
						if (this.table.jcr.restrictions[i].table_search_restriction === true)
						{
							km.js.utils.remove(i, this.table.jcr.restrictions);
						}
					}
				}
				
				// add conditions to table JCR
				for (var i = 0; i < this.properties.length; i++)
				{
					var prop = this.properties[i];
					var searchVal = this.searchPropertyValues[prop.id];
					
					if (searchVal)
					{
						var arg = searchVal;
						if (prop.operator === "ilike" || prop.operator === "like")
						{
							arg = "%" + arg + "%";
						}
						
						// create new restriction
						var restriction = {
							property_id: prop.id,
							operator: prop.operator,
							args: [ arg ],
							// additional flag telling that this restriction has been added by the table search
							table_search_restriction: true
						};
						
						// add restriction to JCR
						if (!this.table.jcr.restrictions)
						{
							this.table.jcr.restrictions = [];
						}
						
						this.table.jcr.restrictions.push(restriction);
					}
				}
				
				this.table.goToPage(0);
				
				// update associated table
				this.table.update();
			},
			
			getSearchInput: function(prop, dataType) {
				
				var input = $("<input></input>");
				
				input.keydown((function(propertyValues, propId) {
					
					return function() {
						propertyValues[propId] = $(this).val();
					}
					
				})(this.searchPropertyValues, prop.id));
				
				if (!dataType)
				{
					throw "Data type for property" + prop.id + " not found in JSTI";
				}
				
				if (dataType.id === km.js.datatypes.number.id || dataType.id === km.js.datatypes.text.id || dataType.id === km.js.datatypes.enumeration.id || dataType.id === km.js.datatypes.autonumber.id)
				{
					input.attr("type", "text").attr("id", "search-" + prop.id.replace(/\./, "-"));
					return input;
				}
				else if (dataType.id === km.js.datatypes.bool.id)
				{
					input.attr("type", "checkbox").attr("id", "search-" + prop.id.replace(/\./, "-"));
					return input;
				}
				else if (dataType.id === km.js.datatypes.rid.id)
				{
					input.attr("type", "text").attr("id", "search-" + prop.id.replace(/\./, "-"));
					return input;
				}
				else
				{
					throw "Unsupported data type ID " + dataType.id + " while rendering input field";
				}
			}
				
		}
		
		return panel;
	}
}
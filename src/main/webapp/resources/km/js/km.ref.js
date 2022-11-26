/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.ref = {

	/**
	 * Create a look-up object
	 * @param options
	 * @returns
	 */
	create: function(options) {
		
		var applyDefaultOptions = function(options) {
			
			// apply default datasource
			if (!options.datasource)
			{
				options.datasource = km.js.datasource.create({
					type: "database"
				});
			}
			
			if (!options.id)
			{
				options.id = "km-ref-" + km.js.utils.random(1000000);
			}
			
			if (options.datasource.type === "json")
			{
				if (options.availableItemsOptions && options.availableItemsOptions.display && options.availableItemsOptions.display.idProperty && !options.availableItemsOptions.display.idProperty.id)
				{
					options.availableItemsOptions.display.idProperty.id = options.availableItemsOptions.display.idProperty.name;
				}
			}
			
			return options;
			
		};
		
		// apply default options
		options = applyDefaultOptions(options);
		
		// create and return look-up object
		var ref = {
		
			// recent params used in the most recent call to function render()
			recentRenderParams: null,
			
			inputName: options.inputName,
			inputId: options.inputId,
			visibleInput: options.visibleInput,
			
			// the ID of the currently selected record
			selectedRecordId: options.selectedRecordId,
			
			// the value of the field selectedRecordDisplayFieldId for the currently selected record
			// identified by selectedRecordId
			selectedRecordDisplayName: options.selectedRecordDisplayName,
			
			// PIR of the field to be displayed from the selected record
			selectedRecordDisplayField: options.selectedRecordDisplayField,
			
			availableItemsOptions: options.availableItemsOptions,
			
			// datasource for available items
			datasource: options.datasource,
			
			// jcr to extract data for the available items table
			jcr: options.jcr,
			
			dialog: null,
			
			// id: "lookup-" + km.js.utils.random(10000000),
			id: options.id,
			
			dialogOptions: options.availableItemsDialogOptions,
			
			// callback method called when an item is selected on the list
			// and the new value is applied to the input
			afterSelect: options.afterSelect,
			
			// callback function called after the value of the lookup is cleared
			// the ID of the cleared record is passed to the function
			afterClear: options.afterClear,
			
			// view or edit mode
			mode: options.mode ? options.mode : "edit",
			
			// true or false
			editable: options.editable,
		
			render: function(target) {
				
				// if a record has been selected but we don't have the value of its display field
				// we will want to fetch this value and display it when it's retrieved
				if (this.selectedRecordId && !this.selectedRecordDisplayName)
				{
					if (!this.selectedRecordDisplayField)
					{
						throw "Display field not defined. Specify option selectedRecordDisplayField in configuration of the km.ref component.";
					}
					else if (!this.selectedRecordDisplayField.id && !this.selectedRecordDisplayField.name)
					{
						throw "Neither selectedRecordDisplayField ID nor its name specified";
					}
					
					// prepare jcr to fetch the value
					var displayValueJCR = {
						baseTypeId: this.jcr.baseTypeId,
						baseTypeName: this.jcr.baseTypeName,
						properties: [
						   { id: this.selectedRecordDisplayField.id, name: this.selectedRecordDisplayField.name }
						],
						restrictions: [ { 
								operator: "eq",
								property_id: this.availableItemsOptions.display.idProperty.id,
								property_name: this.availableItemsOptions.display.idProperty.name,
								args: [ this.selectedRecordId ]
							}
						]
					}
					
					// fetch value
					this.datasource.query(displayValueJCR, (function(lookup) {
						return function(records, recordCount, jsti) {
							
							// if selected record display field ID is not specified, get it
							// from JSTI basing on the name
							if (!lookup.selectedRecordDisplayField.id)
							{
								if (lookup.datasource.type !== "json")
								{
									var baseType = km.js.utils.getTypeFromJSTI(lookup.jcr, jsti);
									if (!baseType)
									{
										throw "Base type not retrieved";
									}
									
									lookup.selectedRecordDisplayField.id = km.js.utils.nestedPropertyToPir(lookup.selectedRecordDisplayField.name, baseType, jsti);
									
									if (!lookup.selectedRecordDisplayField.id)
									{
										throw "Could not get ID for select record display field with name '" + lookup.selectedRecordDisplayField.name + "' on type " + baseType.qualifiedName;
									}
								}
								else
								{
									if (!lookup.availableItemsOptions.display.defaultProperty.name)
									{
										throw "Option display.defaultProperty.name name not defined on km.js.table using datasource of type \"json\"";
									}
									
									lookup.selectedRecordDisplayField.id = lookup.availableItemsOptions.display.defaultProperty.name;
								}
							}
							
							if (records.length === 0)
							{
								throw "Selected record with ID " + lookup.selectedRecordId + " not found";
							}
							else
							{
								lookup.setDisplayValue(records[0][lookup.selectedRecordDisplayField.id]);
							}
						}
					})(this))
				}
			
				this.recentRenderParams = target;
				
				if (!this.id)
				{
					throw "Object reference ID is not defined";
				}
				
				// create dialog
				if (!this.dialog)
				{
					var defaultDialogOptions = {
						size: {
							fitToContent: true
						},
						// put the contents of the dialog in an iframe,
						// even though it is defined as HTML, not as url src attribute
						iframe: {
							addDefaultStyles: true,
							cssClass: "km-std-iframe",
							// css class to be set on the iframe's content's body
							cssBodyClass: "km-std-iframe-body",
							allowAdjustBodyWidth: true
						},
						id: this.id + "-items-dialog",
						// the iframe will be responsible for scrolling, so we hide
						// the scrollbar of the dialog window
						//style: "overflow:hidden"
						style: "background-color: #fff"
					};
					var dialogOptions = $.extend({}, defaultDialogOptions, this.dialogOptions);
					this.dialog = km.js.ui.dialog.create(dialogOptions);
				}
				
				var code = $("<div class=\"km-lookup\" id=\"km-lookup-" + this.id + "\"></div>");
				
				if (!this.inputName)
				{
					throw "Reference input name must not be empty. Specify inputName option in lookup configuration";
				}
				
				if (this.mode === "view")
				{
					code.addClass("km-readonly-lookup");
					
					if (this.editable === true)
					{
						code.click((function(lookup) {
							
							return function(e) {
								lookup.makeEditable();
							}
							
							
						})(this));
					}
				}
				
				var hiddenInput = $("<input type=\"hidden\" class=\"km-lookup-val\" name=\"" + this.inputName + "\"></input>");
				
				if (this.selectedRecordId)
				{
					hiddenInput.val(this.selectedRecordId);
				}
				
				if (this.inputId)
				{
					hiddenInput.attr("id", this.inputId);
				}
				
				code.append(hiddenInput);
				
				var input = $("<input class=\"km-lookup-display-name\" type=\"text\" readonly=\"true\"></input>");
				
				if (this.visibleInput && this.visibleInput.cssClass)
				{
					input.addClass(this.visibleInput.cssClass);
				}
				
				// append an event that opens the list of available items when input is clicked
				input.click((function(lookup) {
					return function () {
						
						// open available items only if the lookup is editable
						// if it is not editable, the first click on it should make it editable
						// and only the second click should open the available items list
						if (lookup.isEditable())
						{
							console.log("Opening available items");
							lookup.openAvailableItems();
						}
					}
				})(this));
				
				code.append(input);
				
				if (this.mode === "view" && this.editable === true)
				{
					input.blur((function(lookup) {
						return function () {
							lookup.makeIneditable();
						}
					})(this));
				}
				
			
				// add the clear button to delete the value of the field
				var clearBtn = $("<img class=\"km-lookup-del\" src=\"" + km.js.config.imagePath + "/ex.png\">");
				clearBtn.click((function(lookup) {
					return function() {
						lookup.clear();
					}
				})(this));
				
				code.append(clearBtn);
				
				// return result
				if (target)
				{	
					if (typeof(target) === "function")
					{
						// call callback
						target(code, this);
					}
					else if (target instanceof jQuery)
					{
						console.log("Target " + target.size() + ", target desc: " + target + " (" + JSON.stringify(target) + ")");
						target.replaceWith(code);
					}
					else
					{
						throw "Unsupported argument type " + typeof(target) + " while calling ref.render()";
					}
					
					if (this.selectedRecordDisplayName)
					{
						this.setDisplayValue(this.selectedRecordDisplayName);
					}
				}
					
				return code;
			},
			
			isEditable: function() {
				return !this.container().hasClass("km-readonly-lookup");
			},
			
			/**
			 * @ public
			 */
			makeEditable: function() {
				this.container().removeClass("km-readonly-lookup");
			},
			
			makeIneditable: function() {
				this.container().addClass("km-readonly-lookup");
			},
			
			/**
			 * Clears the lookup value
			 * @public
			 */
			clear: function() {
				this.setDisplayValue(null);
				this.container().find("input.km-lookup-val").val(null);
				
				if (typeof(this.afterClear) === "function")
				{
					// call an after clear callback, passing the ID of the record that is cleared
					this.afterClear(this.selectedRecordId);
				}
				
				this.selectedRecordId = null;
			},
			
			/**
			 * Refresh the lookup.
			 * @public
			 */
			refresh: function() {
				this.render(this.recentRenderParams);
			},
			
			openAvailableItems: function() {
				
				var defaultTableOptions = {
					// ait stands for available items table
					id: this.id + "-ait",
					options: {
						pageSize: 10,
						paginationActive: true
					}
				};
			
				// options of the available items km.js.table that override those specified by the user
				var overridingTableOptions = {
					
				};
				
				// attach an on click event to every displayed property
				for (var i = 0; i < this.availableItemsOptions.display.properties.length; i++)
				{
					var prop = this.availableItemsOptions.display.properties[i];
					prop.onClick = (function(lookup) {
						return function(recordId) {
							lookup.select(recordId);
						}
					})(this);
				}
			
				// first create a list of available items
				var availableItemsOptions = $.extend({}, defaultTableOptions, this.availableItemsOptions, overridingTableOptions);
				
				// create the table of available items
				var availableItemsTable = km.js.table.create(this.datasource, this.jcr, availableItemsOptions.display, availableItemsOptions.options);
				
				var openTableInDialog = function(lookup) {
					return function(tableCode, table) {
						
						var tableSearch = null;
						
						var isJsonDatasource = table.datasource.type === "json";
						
						// we will need JSTI to extract data about the searched type
						if (!table.datasource.jsti && !isJsonDatasource)
						{
							throw "JSTI not available on datasource related to available items table";
						}
						
						var baseType = null;
						
						if (!isJsonDatasource)
						{
							baseType = km.js.utils.getTypeFromJSTI(lookup.jcr, table.datasource.jsti);
						
							if (!baseType)
							{
								throw "Type information not found in JSTI for type KID " + lookup.jcr.baseTypeId;
							}
						}
						
						// check if table search is defined for available items table
						if (!km.js.utils.isEmpty(lookup.availableItemsOptions.tableSearchOptions))
						{
							console.log("Adding table search for type " + JSON.stringify(baseType));
							
							var defaultOptions = {
								properties: [ { id: baseType.defaultFieldId, operator: "ilike" } ],
								cssClass: "km-table-search-std"
							};
							
							// some options for the table search will be inherited from the km.js.lookup
							var overridingOptions = {
								table: table,
								jsti: table.datasource.jsti
							};
							
							// combine user-defined and inherited options, but let inherited options override those from user
							var finalTableSearchOptions = $.extend({}, defaultOptions, lookup.availableItemsOptions.tableSearchOptions, overridingOptions);
							
							if (!isJsonDatasource)
							{
								km.js.utils.populateIdsOnProperties(finalTableSearchOptions.properties, baseType, table.datasource.jsti);
							}
							
							// instantiate table search
							tableSearch = km.js.tablesearch.create(finalTableSearchOptions);
						}
						
						var wrappedTableCode = $("<div></div>");
						var title = lookup.availableItemsOptions.title ? lookup.availableItemsOptions.title : baseType.pluralLabel;
						
						// add title to the rendered table code
						wrappedTableCode.append(km.js.ui.header(title, null, "margin-bottom: 20px"));
						
						// if table search is defined, prepend it to the table code
						if (tableSearch)
						{
							tableCode = tableSearch.render().add(tableCode);
						}
						
						wrappedTableCode.append(tableCode);
						lookup.dialog.show(wrappedTableCode);
					}
				}
				
				// render the table, and once rendering is finished, put it inside a dialog
				availableItemsTable.render(null, openTableInDialog(this));
			},
			
			/**
			 * @public
			 */
			container: function() {
				var cont = $("div[id='km-lookup-" + this.id + "']");
				if (cont.length > 1)
				{
					throw "Ambiguous container, found " + cont.length + " such elements instead of one";
				}
				return cont;
			},
			
			/**
			 * @public
			 */
			select: function(recordId) {
				
				this.dialog.hide();
				this.selectedRecordId = recordId;
				
				// erase the old display name, so that the km.js.ref knows it has to find a new one
				// corresponding to the newly selected this.selectedRecordId
				this.selectedRecordDisplayName = null;
				
				if (this.mode === "view" && this.editable === true)
				{
					// make the lookup readonly again
					this.container().addClass("km-readonly-lookup");
				}
				
				if (typeof(this.recentRenderParams) === "function")
				{
					this.render(this.recentRenderParams);
					
					if (typeof(this.afterSelect) === "function")
					{
						this.afterSelect(this.selectedRecordId);
					}
				}
				else if (this.recentRenderParams instanceof jQuery)
				{
					this.render(this.container());
					
					if (typeof(this.afterSelect) === "function")
					{
						this.afterSelect(this.selectedRecordId);
					}
				}
				else
				{
					throw "Unsupported recent invocation param type: " + typeof(this.recentRenderParams);
				}
			},
			
			/**
			 * @private
			 */
			setDisplayValue: function(val) {
				this.container().find("input.km-lookup-display-name").val(val);
				this.selectedRecordDisplayName = val;
			}
		
		};
		
		// register reference in the scope
		km.js.scope.refs[km.js.utils.normalizeId(ref.id)] = ref;
		
		// return reference object
		return ref;
	}
	
	/*selectRefItem: function (lookupId, id, name, event) {
		
		var ref = km.js.scope.refs[km.js.utils.normalizeId(lookupId)];
		
		console.log("REF: " + ref);
		
		if (!ref)
		{
			throw "Ref field with ID " + lookupId + " not found";
		}
		
		if (name)
		{
			ref.setDisplayValue(name);
		}
		
		ref.select(id);
	}*/

}
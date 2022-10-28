/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.rel = {
	
	create: function(settings) {
	
		var defaultSettings = {
			removeBtn: true,
			addBtn: true
		}
	
		var options = $.extend({}, defaultSettings, settings);
		
		var relationField = options.jsti.fields[options.fieldId];
		var associatedType = null;
		var valueGetterFunction = null;
		var baseType = options.jsti.types[options.typeId];
		
		if (relationField.dataType.id === km.js.datatypes.inverse_collection.id)
		{
			if (km.js.utils.isEmpty(relationField.dataType.inverseTypeId))
			{
				throw "Inverse type ID not specifed";
			}
			associatedType = options.jsti.types[relationField.dataType.inverseTypeId];
		}
		else if (relationField.dataType.id === km.js.datatypes.association.id)
		{
			if (km.js.utils.isEmpty(relationField.dataType.associatedTypeId))
			{
				throw "Associated type ID not specifed";
			}
			associatedType = options.jsti.types[relationField.dataType.associatedTypeId];
		}
		
		if (!associatedType)
		{
			throw "Associated type not defined for relation field " + JSON.stringify(relationField);
		}
		
		// to every selected item property prepend the id of the relationship
		var queriedProps = [];
		for (var i = 0; i < options.selectedItemProperties.length; i++)
		{
			queriedProps.push({
				id: relationField.id + "." + options.selectedItemProperties[i].id 
			});
		}
		
		valueGetterFunction = function(onComplete) {
			var ds = km.js.datasource.create({ type: "database" });
			var jcr = {
				baseTypeId: options.typeId,
				properties: queriedProps,
				restrictions: [
				   { property_id: baseType.idFieldId, operator: "eq", args: [ options.recordId ]}
				]
			}
			
			// when results are fetched, one record of the base type will be returned containing a
			// collection of related objects, so this needs to be parsed before returning
			var parseResultsCallback = function(onComplete) {
				
				return function(data) {
					data.records = data.length > 0 ? data[0][relationField.id] : [];
					onComplete(data);
				}
				
			}
			
			//console.log("Created value getter for REL, querying: " + JSON.stringify(jcr));
			ds.query(jcr, parseResultsCallback(onComplete));
		}
		
		// always query the ID field
		queriedProps.push({
			id: relationField.id + "." + associatedType.idFieldId
		});
		
		if (km.js.utils.isEmpty(options.availableItemsOptions))
		{
			options.availableItemsOptions = {};
		}
		
		if (km.js.utils.isEmpty(options.availableItemsOptions.properties))
		{
			// if no fields have been defined to be displayed on the available items list
			// just display the default field
			var defaultField = options.jsti.fields[associatedType.defaultFieldId];
			if (km.js.utils.isEmpty(defaultField))
			{
				throw "Default field with ID " + associatedType.defaultFieldId + " not found";
			}
			
			options.availableItemsOptions.properties = [{ id: defaultField.id, label: defaultField.label }];
		}
		
		// function called to get items which can be assigned to the association
		var getAssignableItems = options.getAssignableItems;
		
		if (typeof(getAssignableItems) !== "function")
		{
			// generate function that will get the assignable items
			// basing on the relationship
			getAssignableItems = function(onComplete) {
				
				// construct datasource
				var ds = km.js.datasource.create({ type: "database" });
				
				// JCR to query all records of the associated type
				var jcr = { baseTypeId: associatedType.id, properties: [] };
				
				// always query the id property
				jcr.properties.push({ id: associatedType.idFieldId });
				
				for (var i = 0; i < options.availableItemsOptions.properties.length; i++)
				{
					jcr.properties.push({ id: options.availableItemsOptions.properties[i].id });
				}
				
				// query items from datasource and call a user-defined callback
				ds.query(jcr, onComplete);
			}
		}
	
		// create an instance of the rel object that will be returned to the user
		var rel = {
				
			// base type on which the relationship field is defined
			baseType: baseType,
			
			// JSTI representation of the association property, e.g. "pigeon.children"
			relationField: relationField,
			
			// JSTI representation of the type of collection records
			associatedType: associatedType,
			
			// unique ID of the km.js.rel instance
			id: options.id,
			
			// km.js.itemlist component for displaying assigned items
			itemList: null,
			
			// function to be called when we want to fetch the selected values for this relationship
			getSelectedValues: valueGetterFunction,
			
			// km.ui.dialog object representing the dialog displaying the list of available items
			availableItemsDialog: null,
			
			// configuration of the available items table
			availableItemsOptions: {
				properties: null,
				
				// title to be displayed above the available table options table
				// if null, no title will be displayed
				title: null,
				
				// Options of a table search appended to the available items table.
				// If null, no table search will be appended to the table. 
				tableSearchOptions: null
			},
			
			options: options,
			
			/**
			 * Universal function to be called when we want the rel to be updated.
			 */
			update: function() {
				
				var onCodePrepare = function(relObj) {
					return function(elem) {
						// replace the rel component with its updated version
						$("#" + relObj.id).replaceWith(elem);
					}
				}
				
				this.render(onCodePrepare(this));
			},
			
			render: function(onRelRenderComplete) {	
				
				var addBtnClickCallback = function(relObj) {
					
					return function(panel) {
						
						var onDataLoad = function() {
							return function(data, dataLength, jsti) {
								console.log("Fetched assignable data");
								relObj.openSelectDialog(data, jsti);
							}
						}
						
						getAssignableItems(onDataLoad(this));
					}
					
				}
				
				var deleteItemCallback = function(relObj) {
					return function(item, itemId) {
						console.log("Deleting " + itemId);
						km.js.data.unassociate(relObj.relationField.id, relObj.options.recordId, itemId);
					}
				}
			

				var itemListOptions = {
					id: "item-list-" + this.id,
					properties: options.selectedItemProperties,
					idField: associatedType.idFieldId,
					onItemClick: function(record) {
						km.js.utils.openURL(km.js.config.contextPath + "/" + record[associatedType.idFieldId])
					},
					afterItemDelete: deleteItemCallback(this),
					removeBtn: options.removeBtn,
					addBtn: options.addBtn,
					addBtnClick: addBtnClickCallback(this),
	  
					getSelectedValues: valueGetterFunction
				}
				
				// function called when selected values have been fetched
				var createItemList = function(onComplete) {
					
					return function(data) {
						
						// create item list
						this.itemList = km.js.itemlist.create(itemListOptions);
						
						// render item list
						this.itemList.render((function(onComplete) {
							return function(elem, itemList) {
								itemListElem = elem;
								
								if (typeof(onComplete) === "function")
								{
									onComplete(elem);
								}
								
								itemList.update();
							}
						})(onComplete));	
					}
				}
			
				// get selected values for this relationship, and then create the item list
				this.getSelectedValues(createItemList(onRelRenderComplete)); 
			},
			
			/**
			 * Function called when the select dialog is opened.
			 * This method prepares the item table and then displays it in a dialog window.
			 * @param data - JSRC data containing the assignable records
			 */
			openSelectDialog: function(data, jsti) {
				
				// create dialog
				var dialogOptions = {
					id: "av-dialog-" + this.id,
					width: "800px",
					height: "500px",
					cssClass: "km-rel-av-dialog",
					iframe: {
						addDefaultStyles: true,
						cssClass: "km-std-iframe",
						// css class to be set on the iframe's content's body
						cssBodyClass: "km-std-iframe-body",
						allowAdjustBodyWidth: true
					}
				}
				
				if (this.availableItemsDialog == null)
				{
					// create dialog
					this.availableItemsDialog = km.js.ui.dialog.create(dialogOptions);
				}
				
				// create a callback that will display the dialog once available items table is created
				var dialogShow = function(dialogObj, title) {
					return function(tableCode, table) {
						
						var wrappedTableCode = $("<div style=\"margin: 0 auto;\"></div>");
						
						// if title is defined for the available items list, add it to the rendered table code
						if (options.availableItemsOptions.title) {
							wrappedTableCode.append(km.js.ui.header(options.availableItemsOptions.title, null, "margin-bottom: 20px"));
						}
						
						wrappedTableCode.append(tableCode);
						dialogObj.show(wrappedTableCode);
					}
				}
				
				// create table of available items
				var availableItemTable = this.getAvailableItemTable(data, jsti, dialogShow(this.availableItemsDialog, this.title));
			},
			
			/**
			 * @private
			 * Create an instance of type km.js.table for displaying available items.
			 * On complete, call the onComplete callback with the instantiated table
			 * as parameter.
			 * @param data - array of record objects
			 * @param jsti - JSTI
			 * @param onComplete - callback function to be called when data is fetched from datasource
			 */
			getAvailableItemTable: function(data, jsti, onComplete) {
				
				// create an offline datasource from the available items
				var offlineDS = km.js.datasource.create({
					type: "collection",
					data: data,
					jsti: jsti
				});
				
				if (km.js.utils.isEmpty(options.availableItemsOptions.properties))
				{
					throw "Properties for available items not defined";
				}
				
				var jcr = {
					baseTypeId: this.associatedType.id,
					properties: [].concat(this.options.availableItemsOptions.properties)
				}
				
				// always add the id field to the queried properties, even if it is already there
				jcr.properties.push({ id: this.associatedType.idFieldId });
				
				var availableItemsTableDisplay = {
					properties: [],
					idProperty: { id: this.associatedType.idFieldId }
				}
				
				// add all properties
				for (var i = 0; i < this.options.availableItemsOptions.properties.length; i++)
				{
					availableItemsTableDisplay.properties.push({
						id: this.options.availableItemsOptions.properties[i].id,
						label: this.options.availableItemsOptions.properties[i].label,
						sortable: this.options.availableItemsOptions.properties[i].sortable,
						//filterable: this.options.availableItemsOptions.properties[i].filterable,
						linkStyle: true,
						onClick: (function(relObj) { 
							return function(id) { relObj.onItemSelect(id); }
						})(this)
					});
				}
					
				// instantiate km.js.table
				var availableItemsTable = km.js.table.create(offlineDS, jcr, availableItemsTableDisplay, { 
					id: "available-items-" + this.id,
					jsti: jsti,
					pageSize: 10,
					paginationActive: true,
					cssClass: "km-table-std km-rel-av-items",
					wrapperCssClass: "km-rel-av-items-wrapper",
					buttonPanel: this.options.availableItemsOptions.buttonPanel
				});
				
				var tableSearch = null;
				
				// table search
				if (!km.js.utils.isEmpty(options.availableItemsOptions.tableSearchOptions))
				{
					var defaultOptions = {
						properties: [ { id: this.associatedType.defaultFieldId, operator: "ilike" } ],
						cssClass: "km-table-search-std"
					};
					
					// some options for the table search will be inherited from the km.js.rel
					var overridingOptions = {
						table: availableItemsTable,
						jsti: jsti
					};
					
					// combine user-defined and inherited options, but let inherited options override those from user
					var finalTableSearchOptions = $.extend({}, defaultOptions, options.availableItemsOptions.tableSearchOptions, overridingOptions);
					
					// instantiate table search
					tableSearch = km.js.tablesearch.create(finalTableSearchOptions);
				}
				
				// onComplete is a callback to be called with generated table code as parameter
				// but before we call it, we may want to modify the code by e.g. append table search to it
				var onTableDataFetch = function (tableSearchObj, onCompleteCallback) { 
					return function(tableCode, table) {
						
						if (tableSearchObj)
						{
							tableCode = tableSearchObj.render().add(tableCode);
						}
						
						onCompleteCallback(tableCode, table);
					}
				}
				
				// render the table and pass the rendered code into the onComplete method
				availableItemsTable.render(null, onTableDataFetch(tableSearch, onComplete));
			},
			
			/**
			 * Function called when an item is selected from the available items list
			 */
			onItemSelect: function(selectedRecordId) {
				this.availableItemsDialog.hide();
				
				var onAssociationCreated = function(relObj) {
					return function(linkingRecordId) {
						// refresh the whole itemlist
						relObj.update();
					}
				}
				
				var onAssociationFailed = function(data) {
					// show error if something went wrong
					km.js.ui.statusbar.err(data.message ? data.message : data.messages, 10000);
				}
				
				km.js.data.associate(this.relationField.id, this.options.recordId, selectedRecordId, onAssociationCreated(this), onAssociationFailed);
			}
		}
		
		// register rel in the scope
		km.js.scope.rels[km.js.utils.normalizeId(rel.id)] = rel;
		
		return rel;
	}
}
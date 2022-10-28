/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.devitems = {
	
	create: function(settings) {
		
		var defaultSettings = {
				
			// all items to be displayed, structured by package
			// sample item structure:
			// {
			//    { 
            //  	     "type0": [ {}, {} ],
			//	
			//	  }
			// }
			items: null,
			
			// group can have value either "type" or "package"
			group: "package",
			
			checkboxes: false,
			
			checkboxCallback: null
		}
		
		var mergedSettings = $.extend({}, defaultSettings, settings);
		
		var view = {
		
			options: mergedSettings,
			
			container: null,
				
			render: function(target, overriddenOptions) {
				
				var actualOptions = overriddenOptions ? $.extend({}, this.options, overriddenOptions) : this.options;
				
				this.container = $("<div></div>").addClass("km-dev-items-container");
				this.container.append(this.getBtnPanel(actualOptions));
				
				if (!actualOptions.items)
				{
					throw "Item collection has not been provided";
				}
				
				if (actualOptions.group === "type")
				{
					var tree = $("<ul></ul>").addClass("km-devitems-tree");
					
					var types = actualOptions.types;
					//console.log("types: " + JSON.stringify(types));
					
					var typeAndFieldCount = 0;
					
					// add types and fields
					if (types && $.isArray(types))
					{
						var typeGroup = $("<ul></ul>");
						var itemTree = {};
						
						for (var i = 0; i < types.length; i++)
						{
							var type = types[i];
							var name = type.qualifiedName.split(".");
							var isSelected = actualOptions.selectedItemIds && km.js.utils.isObject(actualOptions.selectedItemIds[type["id"]]); 
							
							if (isSelected || actualOptions.ignoreSelection)
							{
								typeAndFieldCount++;
							}
							
							itemTree = this.addItemToTree(itemTree, name, name, type["id"], isSelected);
							
							// for each field add a separate item prefixed with the type's name
							for (var k = 0; k < type.fields.length; k++)
							{
								var field = type.fields[k];
								
								// skip system fields
								if (field.isSystem)
								{
									continue;
								}
								
								var isFieldSelected = actualOptions.selectedItemIds && actualOptions.selectedItemIds[field["id"]];
								if (isFieldSelected || actualOptions.ignoreSelection)
								{
									typeAndFieldCount++;
								}
								
								var fieldName = (type.qualifiedName + "._" + field.apiName).split(".");
								
								itemTree = this.addItemToTree(itemTree, fieldName, fieldName, field["id"], isFieldSelected, {
									field: field
								});
							}
						}
						
						// traverse item tree
						typeGroup = this.appendItemTree(typeGroup, itemTree, this, 1, actualOptions, false);
						
						var typeNameLink = $("<a></a>").text("Types and fields (" + typeAndFieldCount + ")").addClass("km-devitems-type");
						typeNameLink.click(function() {
							$(this).closest("div.km-devitems-typegroup-name").siblings("ul").children("li").toggle();
						});
						
						var typeNameWrapper = $("<div></div>").addClass("km-devitems-typegroup-name");
						var folderIcon = $("<span></span>").addClass("km-devitems-typegroup-icon").append($("<i></i>").addClass("fa fa-folder km-devitem-icon"));
						typeNameWrapper.append(folderIcon).append(typeNameLink);
						var typeGroupLi = $("<li></li>").append(typeNameWrapper).append(typeGroup);
						
						tree.append(typeGroupLi);
					}
					
					for (typeId in actualOptions.items)
					{
						var componentType = km.js.config.componentTypes[typeId];
						
						if (!componentType)
						{
							throw "Component type with ID " + typeId + " not found";
						}
						
						var typeGroup = $("<ul></ul>");
						
						var items = actualOptions.items[typeId];
						
						if (items)
						{
							if ($.isArray(items))
							{
								var itemTree = {};
								
								// get item tree from list
								for (var i = 0; i < items.length; i++)
								{
									var item = items[i];
									
									var itemNameParts = [];
									var itemId = null;
									for (var itemProp in item)
									{
										// the records might be enriched with string property names
										// but we only want property IDs, which all start with field prefix "003"
										if (itemProp.indexOf("003") !== 0)
										{
											continue;
										}
										
										var field = actualOptions.jsti.fields[itemProp];
										
										// if this is an ID field
										if (field.apiName === "id")
										{
											itemId = item[itemProp];
										}
										else
										{
											var namePart = item[itemProp];
											itemNameParts.push(namePart);
										}
									}
									
									var name = itemNameParts.join(".").split(".");
									
									var isSelected = actualOptions.selectedItemIds && actualOptions.selectedItemIds[itemId]; 
									
									itemTree = this.addItemToTree(itemTree, name, name, itemId, isSelected);
								}
								
								// traverse item tree
								typeGroup = this.appendItemTree(typeGroup, itemTree, this, 1, actualOptions, false);
							}
							else
							{
								throw "Items for component type ID " + typeId + " are not represented in an array";
							}
						}
						
						var ctn = componentType.name.toLowerCase().replace(/\_/g, " ");
						
						var typeNameLink = $("<a></a>").text(ctn + " (" + items.length + ")").addClass("km-devitems-type");
						typeNameLink.click(function() {
							$(this).closest("div.km-devitems-typegroup-name").siblings("ul").children("li").toggle();
						});
						
						var typeNameWrapper = $("<div></div>").addClass("km-devitems-typegroup-name");
						var folderIcon = $("<span></span>").addClass("km-devitems-typegroup-icon").append($("<i></i>").addClass("fa fa-folder km-devitem-icon"));
						typeNameWrapper.append(folderIcon).append(typeNameLink);
						var typeGroupLi = $("<li></li>").append(typeNameWrapper).append(typeGroup);
						
						tree.append(typeGroupLi);
					}
					
					this.container.append(tree);
				}
				else if (actualOptions.group === "package")
				{
					throw "Grouping by package not supported";
				}
				else
				{
					throw "Unsupported dev item grouping '" + actualOptions.group + "'";
				}
				
				this.container.append(tree);
				
				if (typeof(target) === "function")
				{
					// invoke callback
					target(this.container, this);
				}
				else if (target instanceof jQuery)
				{
					target.empty().append(this.container);
				}
				
				this.collapse();
			},
			
			collapse: function() {
				this.container.find("li.km-devitems-enditem-li").hide();
				this.container.find("li.km-devitems-package-li").hide();
			},
			
			addItemToTree: function(tree, partialNameParts, fullNameParts, itemId, isSelected, options) {
				
				if (partialNameParts.length > 1)
				{
					var subTree = tree[partialNameParts[0]]; 
					if (!subTree)
					{
						tree[partialNameParts[0]] = {};
						subTree = tree[partialNameParts[0]];
					}
					
					partialNameParts.splice(0, 1);
					subTree = this.addItemToTree(subTree, partialNameParts, fullNameParts, itemId, isSelected, options);
				}
				else
				{
					if (tree[partialNameParts[0]])
					{
						throw "The same item " + partialNameParts[0] + " added twice";
					}
					
					tree[partialNameParts[0]] = {
						name: partialNameParts[0],
						qualifiedName: fullNameParts.join("."),
						id: itemId,
						isSelected: isSelected,
						options: options
					};
				}
				
				return tree;
			},
			
			appendItemTree: function (parentList, items, view, level, opts, isFields) {
				
				for (var itemPart in items)
				{	
					var item = items[itemPart];
					
					if (isFields && !km.js.utils.isObject(item))
					{
						continue;
					}
					
					// if it's a field, remove the initial underscore
					var label = isFields ? itemPart.substring(1) : itemPart;
					
					var li = $("<li></li>");
					var liText = $("<span></span>").text(label).addClass("km-devitems-item-name");
					
					var itemWrapper = $("<div></div>").css("padding-left", (2 * level) + "em");
					var itemLink = $("<a></a>").append(liText);
					
					// if item has an ID, it is an end item
					if (item.id)
					{
						itemLink.addClass("km-devitems-enditem");
						li.addClass("km-devitems-enditem-li").attr("id", "km-devitems-enditem-li-" + item.id);
						
						// add hidden input with full type name
						li.append($("<input></input>").val(item.qualifiedName).attr("name", "qualifiedName").attr("type", "hidden"));
						
						if (typeof(view.options.itemClick) === "function")
						{
							itemLink.click((function(callback, thisItem) {
								
								return function() {
									callback(thisItem);
								}
								
							}(view.options.itemClick, item)));
						}
						
						if (opts.checkboxes === true)
						{
							// render checkbox next to the item name
							var checkbox = $("<input></input>").attr("type", "checkbox").attr("id", "record_" + item.id); 
							 
							if (item.isSelected)
							{
								checkbox.attr("checked", "checked");
							}
							
							if (typeof(opts.onCheck) === "function")
							{
								checkbox.click((function(thisItem) {
									
									return function() {
										opts.onCheck(thisItem);
									}
									
								})(item));
							}
							
							itemWrapper.append($("<div></div>").addClass("km-devitems-checkbox-wrapper").append(checkbox));
						}
						
						if (typeof(opts.onClick) === "function")
						{
							itemLink.click((function(thisItem, thisLi) {
								
								return function() {
									opts.onClick(thisItem);
									thisLi.toggleClass("km-devitems-selected");
								}
								
							})(item, li));
						}
						
						var iconWrapper = $("<div></div>").addClass("km-devitems-icon-wrapper");
						var icon1 = $("<i></i>").addClass("fa fa-file km-devitem-icon");
						iconWrapper.append(icon1);
						itemWrapper.append(iconWrapper);
						
						itemWrapper.append(itemLink);
						
						if (isFields)
						{
							var fieldDef = item.options.field;
							if (!fieldDef)
							{
								throw "Field definition not set";
							}
							
							var fieldInfo = $("<div></div>").addClass("km-field-info");
							fieldInfo.text("(" + fieldDef.datatype.name + ")");
							itemWrapper.append(fieldInfo);
						}
						
						li.append(itemWrapper);
						
						if (item.isSelected)
						{
							li.addClass("km-devitems-selected")
						}
						else if (opts.unselectedItemCssClass)
						{
							li.addClass(opts.unselectedItemCssClass);
						}
						
						// even though it's an end item, it might still have children
						// if it's a type (i.e. its ID starts with prefix "002"
						if (item.id.indexOf("002") === 0)
						{
							li.addClass("km-devitems-type-li");
							
							itemLink.click((function() {
								return function() {
									$(this).closest(".km-devitems-type-li").children("ul").children("li").toggle();
								}
							}()));
							
							// append field list
							var subitems = $("<ul></ul>");
							this.appendItemTree(subitems, item, view, level + 1, opts, true);
							li.append(subitems);
						}
					}
					else
					{
						itemLink.addClass("km-devitems-package");
						li.addClass("km-devitems-package-li");
						li.append(itemLink);
						
						itemLink.click((function() {
							return function() {
								$(this).closest(".km-devitems-package-li").children("ul").children("li").toggle();
							}
						}()));
						
						var icon = $("<i></i>").addClass("fa fa-list km-devitem-icon");
						
						icon.click((function() {
							return function() {
								$(this).closest(".km-devitems-package-li").children("ul").children("li").toggle();
							}
						}()));
						
						itemWrapper.append($("<div></div>").addClass("km-devitems-icon-wrapper").append(icon));
						itemWrapper.append(itemLink);
						li.append(itemWrapper);
						
						var subitems = $("<ul></ul>");
						this.appendItemTree(subitems, item, view, level + 1, opts, false);
						li.append(subitems);
					}
					
					parentList.append(li);
				}
				
				return parentList;
			},
			
			getBtnPanel: function(opts) {
				
				var panel = $("<div></div>").addClass("km-devitems-btns");
				
				/*var groupByTypeBtn = $("<a>group by type</a>").click((function(view) {
					
					return function() {
						view.render(view.target, { group: "type" });
					}
					
				})(this));
				
				panel.append(groupByTypeBtn);
				panel.append($("<span>|</span>").addClass("km-devitems-btn-sep"));
				
				var groupByPackageBtn = $("<a>group by package</a>").click((function(view) {
					
					return function() {
						view.render(view.target, { group: "package" });
					}
					
				})(this));
				
				panel.append(groupByPackageBtn);*/
				
				return panel;
			}
			
			
			
		}
		
		return view;
		
	}
}
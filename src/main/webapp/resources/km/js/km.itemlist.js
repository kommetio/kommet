/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.itemlist = {
		
	create: function (options) {
	
		var defaultSettings = {
			addBtnImage: "attachment.png",
			contextPath: km.js.config.contextPath
		}
	
		var panel = {
			getSelectedValues: options.getSelectedValues,
			options: $.extend({}, defaultSettings, options),
			
			// list of records
			items: [],
			jsti: {},
			imagePath: null,
			
			render: function(onComplete) {
				var code = "<div id=\"" + this.options.id + "-container\">" + this.getInnerCode() + "</div>"; 
				var elem = $(code);
				if (typeof(onComplete) === "function")
				{
					onComplete(elem, this);
				}
				else
				{
					throw "Callback not specified while calling itemlist.render()";
				}
			},
			
			getInnerCode: function() {
			
				// start panel
				var code = "<div class=\"rmcp apc\"><ul>";
				
				if (this.options.addBtn === true)
				{
					code += "<li class=\"rmcpi rmcpadd\">";
					
					if (!km.js.utils.isEmpty(this.options.addBtnImage))
					{
						code += "<img src=\"" + this.imagePath + this.options.addBtnImage + "\">";
					}
					
					code += "</li>";
				}
				
				for (var i = 0; i < this.items.length; i++)
				{
					code += this.getItemCode(this.items[i]);
				}
				
				// end panel
				code += "</ul></div>";
				
				var elem = $(code);
				
				// add on item click event
				if (typeof(this.options.onItemClick) === "function")
				{
					var onClickCallback = function(panel) {
					
						return function() {
							
							// get record ID from elem id
							var recordId = $(this).closest("li").attr("id").substring(panel.options.id.length + "-i-".length);
							
							// find record by ID
							for (var i = 0; i < panel.items.length; i++)
							{
								if (panel.items[i][panel.options.idField] === recordId)
								{
									panel.options.onItemClick(panel.items[i]);
								}
							}
						}
						
					}
					
					elem.find("li.kmcpi > div.kmcptxt").click(onClickCallback(this));
				}
				
				// add after item delete event
				if (this.options.removeBtn === true)
				{
					var onDeleteCallback = function(panel) {
					
						return function() {
							
							// get record ID from elem id
							var recordId = $(this).closest("li").attr("id").substring(panel.options.id.length + "-i-".length);
							
							// remove the item from DOM
							$(this).closest("li").remove();
							
							if (typeof(panel.options.afterItemDelete) === "function")
							{
								// find record by ID
								for (var i = 0; i < panel.items.length; i++)
								{
									if (panel.items[i][panel.options.idField] === recordId)
									{
										panel.options.afterItemDelete(panel.items[i], recordId);
									}
								}
							}
						}
						
					}
					
					elem.find("li.kmcpi img.del").click(onDeleteCallback(this));
				}
				
				// add after item delete event
				if (this.options.addBtn === true && typeof(this.options.addBtnClick) === "function")
				{
					var addBtnCallback = function(panel) {
						return function() {
							panel.options.addBtnClick(panel);
						}
					}
					
					elem.find("li.kmcpadd").click(addBtnCallback(this));
				}
				
				return elem;
			},
			
			getItemCode: function(rec) {
				
				var code = "<li class=\"rmcpi\" id=\"" + this.options.id + "-i-" + rec[this.options.idField] + "\"><div class=\"rmcptxt\">";
				
				// display fields
				for (var i = 0; i < this.options.properties.length; i++)
				{
					code += ("<span>" + rec[this.options.properties[i].id] + "</span>");
				}
			
				code += "</div>";
				
				if (this.options.removeBtn === true)
				{
					code += "<img src=\"" + this.imagePath + "ex.png\" class=\"del\">";
				}
				
				code += "</li>";
				return code;
			},
			
			// main function used to render and rerender the panel
			update: function(onComplete) {
				
				// create a callback to be invoked when datasource returns results
				var callback = function(panel, onComplete) {
					
					return function(data) {
						panel.items = data.records;
						panel.jsti = data.jsti;
						// put panel into container
						$("#" + panel.options.id + "-container").html(panel.getInnerCode());
						
						if (typeof(onComplete) === "function")
						{
							onComplete(panel);
						}
					}
				}
				
				this.items = this.getSelectedValues(callback(this, onComplete));
				//this.items = this.values.datasource.query(this.values.jcr, callback(this, onComplete));
			}
			
		}
		
		panel.imagePath = panel.options.contextPath + "/resources/images/";
	
		return panel;
	}
		
}
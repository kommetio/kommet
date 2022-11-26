/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.objectdetails = {
		
	create: function(options) {
		
		var defaultOptions = {
				
			// callbacks will be executed immediately as soon as the form finishes rendering
			delayCallbacks: false,
			
			layout: null,
			
			customizable: false
		};
		
		var form = {
				
			options: $.extend({}, defaultOptions, options),
			
			afterRenderCallbacks: [],
			
			renderStage: "not-started",
			
			// the actual rendered table
			renderedForm: null,
			
			// record with modifications made by the user
			updatedRecord: null,
				
			render: function(newOptions) {
				
				this.renderStage = "started";
				this.afterRenderCallbacks = [];
				
				var actualOptions = $.extend({}, this.options, newOptions);
				
				this.actualOptions = actualOptions;
				
				var table = $("<div></div>").addClass("km-rd-table km-grid-section km-grid-group");
				
				if (actualOptions.customizable)
				{
					if (!options.customization)
					{
						throw "Customization context not defined";
					}
					
					table.addClass("km-rd-table-customizable");
				}
				
				var jsti = actualOptions.jsti;
				
				if (!jsti)
				{
					throw "JSTI not provided to km.objectdetails";
				}
				
				var record = actualOptions.record;
				
				if (!record)
				{
					throw "Record not passed to km.objectdetails";
				}
				
				// clone object
				this.updatedRecord = $.extend({}, record);
				
				var mode = actualOptions.mode;
				var fieldsPerRow = actualOptions.fieldsPerRow;
				
				if (!fieldsPerRow)
				{
					fieldsPerRow = 2;
				}
				
				if (!mode)
				{
					throw "Form mode (edit/view) not defined";
				}
				
				var row = null;
				var propsInCurrentRow = 0;
				
				if (!actualOptions.fields && !actualOptions.layout)
				{
					throw "Neither fields for display nor layout are specified";
				}
				
				if (actualOptions.fields && actualOptions.layout)
				{
					throw "Both fields and layout cannot be set in options of km.objectdetails";
				}
				
				if (actualOptions.fields)
				{
					actualOptions.layout = this.getDefaultLayout(actualOptions, fieldsPerRow);
				}
				
				// iterator over sections
				for (var i = 0; i < actualOptions.layout.sections.length; i++)
				{
					var section = actualOptions.layout.sections[i];
					
					var sectionDiv = $("<div></div>").addClass("km-rd-body km-property-section km-grid-col km-grid-span-2-of-2");
					
					if (section.title)
					{
						var sectionTitle = $("<div class=\"km-rd-row km-section-title\"><div class=\"km-rd-cell\">" + section.title + "</div></div>");
						sectionDiv.append(sectionTitle);
					}
					
					if (!section.rows)
					{
						throw "Layout section does not contain any rows";
					}
					
					var j = 0;
					
					// iterate over rows in section
					for (; j < section.rows.length; j++)
					{
						var row = section.rows[j];
						
						var rowDiv = $("<div></div>").addClass("km-rd-row km-grid-section km-grid-group");
						
						// iterate over fields in the row
						for (var k = 0; k < row.fields.length; k++)
						{
							var field = row.fields[k];
							
							if (field.isPlaceholder)
							{
								rowDiv.append(this.placeholder(i, j, k, actualOptions.customizable, actualOptions));
								propsInCurrentRow++;
								continue;
							}
							
							var propCell = null;
							
							// generic fields are those that don't exist in the database
							if (field.isGeneric === true)
							{
								var fieldValue = km.js.utils.propVal(record, field.name);
								
								if (!fieldValue && field.value)
								{
									fieldValue = field.value;
								}
								
								var propCell = this.propertyCell(field, field.name, fieldValue, mode, null);
							}
							else
							{
								var fieldId = field.id;
								var fieldDef = null;
								var nestedFieldName = null;
								
								if (!fieldId)
								{
									// find field by name
									if (!actualOptions.type)
									{
										throw "Type not specified in options";
									}
									
									if (!field.name)
									{
										throw "Neither field name nor ID specified: " + JSON.stringify(field);
									}
									
									nestedFieldName = field.name;
									
									// find field in JSTI
									fieldDef = this.getFieldFromJSTI(actualOptions.type, field.name, jsti);
								}
								else
								{
									fieldDef = jsti.fields[field.id];
								}
								
								if (!fieldDef)
								{
									throw "Field " + JSON.stringify(field) + " not found in JSTI";
								}
								
								if (fieldDef.apiName && !fieldDef.name)
								{
									// use "name" and "apiName" interchangibly
									fieldDef.name = fieldDef.apiName;
								}
								
								// field has form { id }
								var fieldValue = record[fieldDef.id];
								
								if (field.label)
								{
									// use user-defined label instead of original one
									fieldDef.label = field.label;
								}
								
								var propCell = this.propertyCell(fieldDef, nestedFieldName, fieldValue, mode, jsti);
							}
							
							if (!propCell)
							{
								// just skip this property, because it could not be rendered for some reason
								// do not increase the propsInCurrentRow counter
								continue;
							}
							
							if (actualOptions.customizable)
							{
								propCell.draggable({
									
									helper: function() {
								        return $(this).clone().addClass("km-rd-prop-drag-helper");
								    },
								    
								    start: (function(table, field, sectionIndex, rowIndex, fieldIndex) {
										
										return function (e, ui) {
								        	table.draggedProperty = {
								        		field: field,
								        		sectionIndex: sectionIndex,
								        		rowIndex: rowIndex,
								        		fieldIndex: fieldIndex
								        	}
								    	}
										
									})(this, field, i, j, k),
									
									stop: (function(table) {
										
										return function (e, ui) {
								        	table.draggedProperty = null;
								    	}
										
									})(this)
								    
								});
							}
							
							rowDiv.append(propCell);
							propsInCurrentRow++;
						}
						
						
						while ((propsInCurrentRow % fieldsPerRow) !== 0)
						{	
							rowDiv.append(this.placeholder(i, j, k, actualOptions.customizable, actualOptions));
							
							propsInCurrentRow++;
						}
						
						// check if there is a row render callback
						if (typeof(row.render) === "function")
						{
							// replace the row content with what the render callback returns
							rowDiv.empty().append(row.render(row, rowDiv, table));
						}
						
						sectionDiv.append(rowDiv);
					}
					
					// append another row containing only placeholders
					
					var rowDiv = $("<div></div>").addClass("km-rd-row km-grid-section km-grid-group");
					propsInCurrentRow = 0;
					
					while (propsInCurrentRow < fieldsPerRow)
					{	
						rowDiv.append(this.placeholder(i, j, propsInCurrentRow, actualOptions.customizable, actualOptions));
						
						propsInCurrentRow++;
					}
					
					sectionDiv.append(rowDiv);
					rowDiv = null;
					
					if (actualOptions.customizable)
					{
						sectionDiv.draggable({
							
							helper: function() {
						        return $(this).clone().addClass("km-rd-prop-drag-helper");
						    },
						    
						    start: (function(table, sectionIndex) {
								
								return function (e, ui) {
						        	table.draggedSection = {
						        		sectionIndex: sectionIndex
						        	}
						    	}
								
							})(this, i),
							
							stop: (function(table) {
								
								return function (e, ui) {
						        	table.draggedSection = null;
						    	}
								
							})(this)
						    
						});
						
						sectionDiv.droppable({
							
							drop: (function(table, sectionIndex, options) {
								
								return function() {
									
									if (!table.draggedSection)
									{
										return;
									}
									
									// place the field on the layout
									var layout = actualOptions.layout;
									
									var oldSection = layout.sections[sectionIndex];
									
									layout.sections[sectionIndex] = layout.sections[table.draggedSection.sectionIndex];
									layout.sections[table.draggedSection.sectionIndex] = oldSection;
									
									table.draggedSection = null;
									
									console.log("Repainting layout: " + JSON.stringify(layout));
									
									// refresh table
									table.render(table.actualOptions);
									
									if (!options.customization)
									{
										throw "Customization context not defined";
									}
									
									// update the customization in DB
									table.saveCustomization(options.type.name, options.customization.context, options.customization.contextValue, layout);
									
								}
								
							})(this, i, actualOptions)
							
						});
					}
					
					table.append(sectionDiv);
				}
				
				this.renderedForm = table;
				
				var finalCode = table;
				
				if (actualOptions.customization)
				{
					finalCode = $("<div></div>").append(this.customizationPanel(actualOptions)).append(table);
				}
				
				if (typeof(actualOptions.target) === "function")
				{
					actualOptions.target(finalCode, this);
				}
				else if (actualOptions.target instanceof jQuery)
				{
					actualOptions.target.empty().append(finalCode);
				}
				
				// mark rendering as finished
				this.renderStage = "finished";
				
				this.callAfterRenderCallbacks();
				
			},
			
			customizationPanel: function(options) {
				
				var type = this.getTypeFromJSTI(options.type, options.jsti);
				
				if (!type)
				{
					throw "Type not found while rendering customization panel";
				}
				
				var panel = $("<div></div>");
				
				var fieldList = $("<div></div>").addClass("km-field-list");
				
				// render all fields
				for (var fieldName in options.jsti.fields)
				{
					var field = options.jsti.fields[fieldName];
					
					if (field.typeId !== type.id)
					{
						continue;
					}
					
					var fieldBox = $("<div></div>").addClass("km-cust-field");
					var fieldLabel = $("<div></div>").text(field.label);
					fieldBox.append(fieldLabel);
					var fieldName = $("<div></div>").text(field.name);
					fieldBox.append(fieldName);
					//var dt = $("<div></div>").text(field.dataType.name);
					//fieldBox.append(dt);
					
					fieldBox.draggable({
						
						helper: function() {
					        return $(this).clone().css("display", "block").addClass("km-field-drag-helper");
					    },
					    
					    start: (function(table, field) {
							
							return function (e, ui) {
					        	table.draggedProperty = {
					        		field: field,
					        		sectionIndex: null,
					        		rowIndex: null,
					        		fieldIndex: null,
					        		isNew: true
					        	}
					    	}
							
						})(this, field),
						
						stop: (function(table) {
							
							return function (e, ui) {
					        	table.draggedProperty = null;
					    	}
							
						})(this)
						
					});
					
					fieldList.append(fieldBox);
				}
				
				panel.append(fieldList);
				
				return panel;
				
			},
			
			placeholder: function(sectionNo, rowNo, fieldNo, isCustomizable, actualOptions) {
				
				// fill the rest of the row with empty cells
				var cell = $("<div></div>").addClass("km-rd-property km-grid-col km-grid-span-1-of-2");
				var label = $("<div></div>").addClass("label km-rd-cell");
				var valueCell = $("<div></div>").addClass("value km-rd-cell");
				
				if (isCustomizable)
				{
					label.append($("<div>new field</div>").addClass("km-drop-label-filler"));
					var mockInput = $("<input type=\"text\"></input>").attr("disabled", true);
					valueCell.append($("<div></div>").append(mockInput).addClass("km-drop-val-filler"));
					
					cell.droppable({
						
						drop: (function(table, sectionIndex, rowIndex, fieldIndex, options) {
							
							return function() {
								
								if (!table.draggedProperty.field)
								{
									throw "Dropped null field";
								}
								
								console.log("Dropped " + JSON.stringify(table.draggedProperty));
								
								// place the field on the layout
								var layout = actualOptions.layout;
								
								var section = layout.sections[sectionIndex];
								
								if (rowIndex >= section.rows.length)
								{
									// if this was a row containing only placeholders, it will not be found in the layout
									section.rows.push({
										fields: []
									});
								}
							
								var row = section.rows[rowIndex];
								
								if (fieldIndex >= row.fields.length)
								{
									// if row is too short, fill it with placeholders
									for (var i = row.fields.length; i < fieldIndex; i++)
									{
										row.fields.push({
											isPlaceholder: true
										});
									}
								}
								
								row.fields.push(table.draggedProperty.field);
								
								if (table.draggedProperty.isNew !== true)
								{
									// remove the field from its original place - if it's not a new field
									layout.sections[table.draggedProperty.sectionIndex].rows[table.draggedProperty.rowIndex].fields[table.draggedProperty.fieldIndex] = {
										isPlaceholder: true
									}
								}
								
								table.draggedProperty = null;
								
								console.log("Repainting layout: " + JSON.stringify(layout));
								
								// refresh table
								table.render(table.actualOptions);
								
								if (!options.customization)
								{
									throw "Customization context not defined";
								}
								
								// update the customization in DB
								table.saveCustomization(options.type.name, options.customization.context, options.customization.contextValue, layout);
								
							}
							
						})(this, sectionNo, rowNo, fieldNo, actualOptions)
						
					});
				}
				
				return cell.append(label).append(valueCell);
				
			},
			
			saveCustomization: function(typeName, context, contextValue, layout) {
			
				$.post(km.js.config.contextPath + "/km/objectdetails/customize", { typeName: typeName, context: context, contextValue: contextValue, layout: JSON.stringify(layout) }, function(resp) {
					
				});
				
			},
			
			propertyDrop: function() {
				
				var box = $("<div></div>").addClass("km-rd-property km-prop-drop");
				
				var cell = $("<div></div>").addClass("label km-rd-cell km-drop-cell").text("a");
				
				box.append(cell);
				return box;
				
			},
			
			getDefaultLayout: function(options, fieldsPerRow) {
				
				var layout = {
					sections: []
				};
				
				var section = {
					title: null,
					rows: []
				};
				
				var row = null;
				var propsInCurrentRow = 0;
				
				// iterate over fields
				for (var i = 0; i < options.fields.length; i++)
				{
					if (!row)
					{
						// start new row
						row = {
							fields: []
						};
					}
					
					var field = options.fields[i];
					
					row.fields.push(field);
					propsInCurrentRow++;
					
					if ((propsInCurrentRow % fieldsPerRow) === 0)
					{
						section.rows.push(row);
						
						// start new row
						row = null;
					}
				}
				
				if (row)
				{
					section.rows.push(row);
				}
			
				layout.sections.push(section);
				
				return layout;
				
			},
			
			callAfterRenderCallbacks: function() {
				for (var i = 0; i < this.afterRenderCallbacks.length; i++)
				{
					// run callbacks
					this.afterRenderCallbacks[i]();
				}
			},
			
			/**
			 * Adds a callback to be executed after rendering is finished
			 */
			addAfterRenderCallback: function(callback) {
				
				// call callbacks if rendering if finished, unless we want to delay them and then call them explicitly using the callAfterRenderCallbacks() method 
				if (this.renderStage === "finished" && !this.options.delayCallbacks)
				{
					// run callback immediately
					callback();
				}
				else
				{
					// register callback for later
					this.afterRenderCallbacks.push(callback);
				}
				
			},
			
			getTypeFromJSTI: function(typeDef, jsti) {
				
				if (!typeDef.id && !typeDef.name)
				{
					throw "Neither type name nor ID set: " + JSON.stringify(typeDef);
				}
				
				var foundType = null;
				
				for (var typeId in jsti.types)
				{
					var type = jsti.types[typeId];
					
					if ((typeDef.id && typeDef.id === type.id) || (typeDef.name && typeDef.name === type.qualifiedName))
					{
						return type;
					}
				}
				
				return null;
				
			},
			
			getFieldFromJSTI: function(typeDef, fieldName, jsti) {
				
				var foundType = this.getTypeFromJSTI(typeDef, jsti);
				
				if (!foundType)
				{
					throw "Type not found: " + JSON.stringify(typeDef);
				}
				
				var isNested = fieldName.indexOf(".") >= 0;
				var nameParts = [];
				
				// check if this is a qualified field
				if (isNested)
				{
					// split field name by dots
					nameParts = fieldName.split(".");
					fieldName = nameParts[0];
				}
				
				// find field
				for (var fieldId in jsti.fields)
				{
					var field = jsti.fields[fieldId];
					
					if (field.apiName === fieldName && field.typeId === foundType.id)
					{
						if (!isNested)
						{
							return field;
						}
						else
						{
							// this is a nested field, so we need to drill down to get the most nested field
							if (field.dataType.id !== km.js.datatypes.object_reference.id)
							{
								throw "Expected nested field " + fieldName + " to be a type reference, but its data type is " + field.dataType.id;
							}
							
							if (!field.dataType.typeId)
							{
								throw "Referenced type ID not set on field " + fieldName + " in JSTI";
							}
							
							nameParts.shift();
							var nestedFieldName = nameParts.join(".");
							
							var nestedType = jsti.types[field.dataType.typeId];
							if (!nestedType)
							{
								throw "Nested type " + field.dataType.typeId + " not found in JSTI";
							}
							
							return this.getFieldFromJSTI(nestedType, nestedFieldName, jsti);
						}
					}
				}
				
				return null;
				
			},
			
			propertyCell: function(fieldDef, nestedFieldName, fieldVal, mode, jsti) {
				
				var cell = $("<div></div>").addClass("km-rd-property km-grid-col km-grid-span-1-of-2");
				var label = $("<div></div>").addClass("label km-rd-cell").text(fieldDef.label);
				
				var field = this.renderField(fieldDef, nestedFieldName, fieldVal, mode, jsti);
				if (!field)
				{
					// field can be null if its value cannot be rendered for some reason (e.g. it is a system field)
					return null;
				}
				
				var value = $("<div></div>").addClass("value km-rd-cell").append(field);
				cell.append(label).append(value);
				
				return cell;
			},
			
			showErrors: function(errors) {
				
				var showError = function(field, msg) {
					
					field.addClass("km-field-err-style");
					
				}
				
				for (var i = 0; i < errors.length; i++)
				{
					var err = errors[i];
					
					// show error for this field
					showError(this.renderedForm.find(".km-field-" + err.fieldId), err.message);
				}
				
			},
			
			
			/**
			 * Renders an editable input for the given field, depending on its type.
			 */
			renderField: function(fieldDef, nestedFieldName, fieldVal, mode, jsti) {
				
				var renderAsOutput = function(field) {
					field.addClass("km-output");
					field.attr("disabled", "true").attr("readonly", "true");
					return field;
				}
				
				var dt = fieldDef.dataType;
				
				if (dt.id === km.js.datatypes.text.id || dt.id === km.js.datatypes.email.id || dt.id === km.js.datatypes.number.id || (dt.id === km.js.datatypes.enumeration.id && mode === "view"))
				{
					var field = $("<input></input>").val(fieldVal).attr("type", "text");
					field.addClass("km-field-" + fieldDef.id);
					
					if (fieldDef.placeholder)
					{
						field.attr("placeholder", fieldDef.placeholder);
					}
					
					field.change((function(form, fieldDef) {
						
						return function() {
							form.updatedRecord[fieldDef.id] = $(this).val();
							km.js.utils.setPropVal(form.updatedRecord, nestedFieldName, $(this).val());
						}
						
					})(this, fieldDef));
					
					if (mode === "view")
					{
						field = renderAsOutput(field);
					}
					
					return field;
				}
				else if (dt.id === km.js.datatypes.date.id || dt.id === km.js.datatypes.datetime.id)
				{
					var field = $("<input></input>").val(fieldVal).attr("type", "text");
					field.addClass("km-field-" + fieldDef.id);
					field.datepicker({ dateFormat: "yy-mm-dd" });
					
					field.change((function(form, fieldDef) {
						
						return function() {
							form.updatedRecord[fieldDef.id] = $(this).val();
							km.js.utils.setPropVal(form.updatedRecord, nestedFieldName, $(this).val());
						}
						
					})(this, fieldDef));
					
					if (mode === "view")
					{
						field = renderAsOutput(field);
					}
					
					return field;
				}
				else if (dt.id === km.js.datatypes.object_reference.id)
				{
					var hash = "item-" + km.js.utils.random(1000000);
					var field = $("<input></input>").val(fieldVal).attr("type", "text").attr("km-hash", hash);
					field.addClass("km-field-" + fieldDef.id);
					
					// get referenced type
					var refType = jsti.types[dt.typeId];
					
					if (!refType)
					{
						throw "Referenced type with ID " + dt.typeId + " for field " + fieldDef.apiName + " not found";
					}
					
					var defaultField = jsti.fields[refType.defaultFieldId];
					
					var jcr = {
						baseTypeName: refType.qualifiedName,
						properties: [
							{ name: "id" },
							{ name: defaultField.apiName }
						]
					};
					
					var availableItemsOptions = {
						display: {
							properties: [
								{ name: defaultField.apiName, label: defaultField.label, linkStyle: true }
							],
							idProperty: { name: "id" }
						}
					};
					
					var lookup = km.js.ref.create({
						selectedRecordDisplayField: { name: defaultField.apiName },
						jcr: jcr,
						availableItemsDialogOptions: {},
						availableItemsOptions: availableItemsOptions,
						inputName: fieldDef.apiName,
						mode: mode,
						selectedRecordId: fieldVal ? fieldVal[refType.idFieldId] : null,
						afterSelect: (function(form, fieldDef, idFieldId) {
							
							return function(recordId) {
								
								var obj = {};
								obj[idFieldId] = recordId;
								
								form.updatedRecord[fieldDef.id] = obj;
								km.js.utils.setPropVal(form.updatedRecord, nestedFieldName, obj);
							}
							
						})(this, fieldDef, refType.idFieldId)
							
					});
					
					lookup.render((function(form) {
						
						return function(code) {
							
							form.addAfterRenderCallback(function() {
								$("input[km-hash='" + hash + "']").replaceWith(code);
							});
							
						}
						
					})(this));
					
					return field;
				}
				else if (dt.id === km.js.datatypes.bool.id)
				{
					var field = null;
					
					if (this.actualOptions.boolDisplay === "dropdown")
					{
						var field = $("<select></select>");
						field.addClass("km-field-" + fieldDef.id);
						field.append($("<option></option>").text("Yes").attr("value", "true"));
						field.append($("<option></option>").text("No").attr("value", "false"));
						field.val(fieldVal === true ? "Yes" : "No");
						
						field.change((function(form, fieldDef) {
							
							return function() {
								form.updatedRecord[fieldDef.id] = ($(this).val() === "true");
								km.js.utils.setPropVal(form.updatedRecord, nestedFieldName, $(this).val() === "true");
							}
							
						})(this, fieldDef));
					}
					else
					{
						var field = $("<input></input>").attr("value", "true").attr("type", "checkbox");
						field.addClass("km-field-" + fieldDef.id);
						if (fieldVal === true)
						{
							field.attr("checked", true);
						}
						
						field.change((function(form, fieldDef) {
							
							return function() {
								form.updatedRecord[fieldDef.id] = $(this).is(":checked");
								km.js.utils.setPropVal(form.updatedRecord, nestedFieldName, $(this).is(":checked"));
							}
							
						})(this, fieldDef));
					}
					
					if (mode === "view")
					{
						field = renderAsOutput(field);
					}
				
					return field;
				}
				else if (dt.id === km.js.datatypes.enumeration.id && mode === "edit")
				{
					var field = $("<select></select>");
					field.addClass("km-field-" + fieldDef.id);
					
					for (var i = 0; i < dt.enumValues.length; i++)
					{
						field.append($("<option></option>").text(dt.enumValues[i]).attr("value", dt.enumValues[i]));
					}
					field.val(fieldVal);
					
					field.change((function(form, fieldDef) {
						
						return function() {
							form.updatedRecord[fieldDef.id] = $(this).val();
							km.js.utils.setPropVal(form.updatedRecord, nestedFieldName, $(this).val());
						}
						
					})(this, fieldDef));
					
					return field;
				}
				else if (dt.id === km.js.datatypes.inverse_collection.id || dt.id === km.js.datatypes.association.id || dt.id === km.js.datatypes.rid.id || dt.id === km.js.datatypes.formula.id)
				{
					// ignore collections
					return null;
				}
				else
				{
					throw "Unsupported field data type " + JSON.stringify(dt);
				}
				
				return null;
				
			},
				
		};
		
		return form;
		
	},
	// end of "create" function,

	renderRecord: function(options) {
		
		km.js.db.query(options.query, (function(options) {
			
			return function(records, recordCount, jsti) {
				
				var record = records.length ? records[0] : {};
				
				var form = km.js.objectdetails.create({
					mode: options.mode ? options.mode : "edit",
					jsti: jsti,
					target: function(code) {
						
						options.target.empty();
						
						var wrapper = $("<div></div>");
						
						if (options.title)
						{
							var title = $("<div></div>").addClass("km-title").text(options.title);
							wrapper.append(title);
						}
						
						if (options.buttonPanel)
						{
							options.buttonPanel.render(function(code) {
								wrapper.append(code);
							});
						}
						
						wrapper.append(code);
						
						options.target.append(wrapper);
					},
					layout: options.layout,
					type: {
						name: options.typeName
					},
					record: record,
					customizable: false
				});

				form.render({});
				
			}
			
		})(options));
		
	}
		
};
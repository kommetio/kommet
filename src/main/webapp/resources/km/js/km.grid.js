/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.grid = {
	
	create: function(options) {
		
		var grid = {
		
			options: options,
			
			show: function(target) {
				
				if (target)
				{
					this.options.target = target;
				}
				
				if (!this.options.fields)
				{
					throw "Fields not set in grid";
				}
				
				if (!this.options.records)
				{
					throw "Records not passed to grid";
				}	
					
				var code = this.getGridCode(this.options, this.options.records);
				
				if (this.options.target instanceof jQuery)
				{
					this.options.target.empty().append(code);
				}
				else if (typeof(this.options.target) === "function")
				{
					this.options.target(code);
				}
				else
				{
					throw "Unsupported target data type while rendering grid";
				}
			},
			
			getGridCode: function(options, records) {
				
				var code = $("<div></div>").addClass("km-grid");
				
				code.append(this.getHeader(options));
				
				var rowNo = 0;
				
				for (var i = 0; i < records.length; i++)
				{
					var record = records[i];
					code.append(this.getRow(options, record, ++rowNo));
				}
				
				if (options.emptyRowCount)
				{
					for (var i = 0; i < options.emptyRowCount; i++)
					{
						code.append(this.getEmptyRow(options, ++rowNo));
					}
				}
				
				return code;
			},
			
			getHeader: function(options) {
				
				var row = $("<div></div>").addClass("km-grid-row km-grid-header");
				
				this.columnCount = 0;
				
				for (var i = 0; i < options.fields.length; i++)
				{
					var field = options.fields[i];
					
					if (!field.dataType)
					{
						throw "Field data type not set for field " + field.name;
					}
					
					if (!this.isValidDataTypeForGrid(field.dataType, field))
					{
						continue;
					}
					
					this.columnCount++;
					var cell = this.getHeaderCell(field, options);
					row.append(cell);
				}
				
				if (options.emptyColumnCount)
				{
					for (var i = 0; i < options.emptyColumnCount; i++)
					{
						this.columnCount++;
						var cell = this.getEmptyHeaderCell(options);
						row.append(cell);
					}
				}
				
				return row;
				
			},
			
			isValidDataTypeForGrid: function(dt, field) {
				
				if (field.name === "accessType")
				{
					return false;
				}
				
				if (km.js.datatypes.text.id === dt.id)
				{
					return true;
				}
				
				if (km.js.datatypes.number.id === dt.id)
				{
					return true;
				}
				
				if (km.js.datatypes.enumeration.id === dt.id)
				{
					return true;
				}
				
				if (km.js.datatypes.email.id === dt.id)
				{
					return true;
				}
				
				return false;
			},
			
			getEmptyRow: function(options, rowNo) {
				
				var row = $("<div></div>").addClass("km-grid-row").attr("km-record-id", "null").attr("km-row-no", rowNo);
				
			
				for (var i = 0; i < options.fields.length; i++)
				{
					var field = options.fields[i];
					
					if (!field.dataType)
					{
						throw "Field data type not set for field " + field.name;
					}
					
					if (!this.isValidDataTypeForGrid(field.dataType, field))
					{
						continue;
					}
					
					var cell = this.getEmptyCell(options, field, null);
					cell.attr("km-record-id", "null");
					row.append(cell);
				}
				
				if (options.emptyColumnCount)
				{
					for (var i = 0; i < options.emptyColumnCount; i++)
					{
						var cell = this.getEmptyCell(options, null, null);
						cell.attr("km-record-id", "null");
						row.append(cell);
					}
				}
				
				return row;
				
			},
			
			getRow: function(options, record, rowNo) {
				
				var row = $("<div></div>").addClass("km-grid-row").attr("km-record-id", record.id).attr("km-row-no", rowNo);
				
				for (var i = 0; i < options.fields.length; i++)
				{
					var field = options.fields[i];
					
					if (!field.dataType)
					{
						throw "Field data type not set for field " + field.name;
					}
					
					if (!this.isValidDataTypeForGrid(field.dataType, field))
					{
						continue;
					}
					
					var cell = this.getCell(record, field, options);
					cell.attr("km-record-id", record.id);
					row.append(cell);
				}
				
				if (options.emptyColumnCount)
				{
					for (var i = 0; i < options.emptyColumnCount; i++)
					{
						var cell = this.getEmptyCell(options, null, record.id);
						cell.attr("km-record-id", record.id);
						row.append(cell);
					}
				}
				
				return row;
				
			},
			
			getCell: function(record, field, options) {
				
				var cell = $("<div></div>").addClass("km-grid-cell");
				
				var value = record[field.name];
				
				var input = $("<input></input>").attr("type", "text").val(value).addClass("km-grid-input");
				
				input.blur((function(record, fieldName, input) {
					
					return function() {
						var newRecord = {
							id: record.id
						}
						
						newRecord[fieldName] = input.val();
						
						km.js.db.update(newRecord, null, null);
					}
					
				})(record, field.name, input));
				
				cell.append(input);
				return cell;
			},
			
			getEmptyCell: function(options, field, recordId) {
				
				var cell = $("<div></div>").addClass("km-grid-cell").attr("km-record-id", recordId)
				
				var input = $("<input></input>").attr("type", "text").addClass("km-grid-input");
				
				if (field)
				{
					input.blur((function(recordId, fieldName, input, options) {
						
						if (!options.typeName)
						{
							throw "Type name not defined in km.js.grid - cannot insert new record";
						}
						
						return function() {
							var newRecord = {
								id: recordId
							}
							
							var saveOptions = {
								typeName: options.typeName
							}
							
							newRecord[fieldName] = input.val();
							
							km.js.db.update(newRecord, null, null, saveOptions);
						}
						
					})(recordId, field.name, input, options));
				}
				
				cell.append(input);
				return cell;
			},
			
			getHeaderCell: function(field, options) {
				
				var cell = $("<div></div>").addClass("km-grid-cell km-grid-header-cell").attr("km-field", field.id);
				
				var label = field.label ? field.label : field.name;
				var value = label;
				
				var input = $("<input></input>").attr("type", "text").val(value).addClass("km-grid-input");
				cell.append(input);
				
				return cell;
			},
			
			getEmptyHeaderCell: function(options) {
				
				var cell = $("<div></div>").addClass("km-grid-cell km-grid-header-cell").attr("km-field", "null");
				
				var input = $("<input></input>").attr("type", "text").addClass("km-grid-input");
				
				input.blur((function(input, cell, options) {
					
					return function() {
						
						if (!input.val())
						{
							return;
						}
						
						if (cell.attr("km-field") !== "null")
						{
							return;
						}
						
						if (!options.typeName)
						{
							throw "Type name not defined in km.js.grid - cannot create new field";
						}
						
						var capitalizeEachWord = function(str)
						{
						    return str.replace(/\w\S*/g, function(txt){ return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
						}
						
						var fieldLabel = capitalizeEachWord(input.val());
						var fieldApiName = fieldLabel.replace(/\s+/g, "");
						
						// uncapitalize first letter
						fieldApiName = fieldApiName.charAt(0).toLowerCase() + fieldApiName.substr(1).toLowerCase();
						
						var payload = {
							type: options.typeName,
							apiName: fieldApiName,
							label: fieldLabel
						}
						
						$.post(km.js.config.contextPath + "/km/rest/field/create", payload, (function(cell) {
							
							return function(data) {
								
								if (data.success)
								{
									cell.attr("km-field", data.data.fieldId);
								}
								
							}
							
						})(cell), "json");
						
					}
					
				})(input, cell, options));
				
				cell.append(input);
				
				return cell;
			}
		
		}
		
		return grid;
	}
		
}
var tables = {
	
	create: function(options) {
	
		var defaultSettings = {
			display: "table"
		}
		
		var settings = $.extend({}, defaultSettings, options);
	
		var tableObj = {
				
			data: null,
			container: null,
			rowCount: null,
			cellCount: null,
			activeCell: null,
			options: settings,
	
			// function rendering the table code
			render: function (parent, newData) {
				
				if (newData != null && typeof newData != 'undefined') {
					this.data = newData;
				}
				
				var code = "<table";
				
				if (!$.isArray(this.options.cssClasses)) {
					this.options.cssClasses = [];
				}
				
				// kmt = "kommet table"
				this.options.cssClasses.push("kmt");
				
				if (this.options.display === "grid") {
					this.options.cssClasses.push("kmtgrid");
				}
				
				if (this.options.cssClasses != null) {
					code += " class=\"";
					for (var i = 0; i < this.options.cssClasses.length; i++) {
						code += this.options.cssClasses[i] + " ";
					}
					code +="\"";
				}
				
				code += "><tbody>";
				
				code += this.renderHeader();
				this.rowCount = this.data.length;
				this.cellCount = this.options.columns.length;
				
				for (var i = 0; i < this.data.length; i++) {
					code += this.renderRow(this.data[i], i);
				}
				
				code += "</tbody></table>";
				parent.html(code);
				this.container = parent;
				
				this.postRender();
			},
			
			postRender: function() {
				
				if (this.options.cellFocus === true) {
					this.container.find("table.kmt > tbody > tr > td").click(this.cellOnClick());
					//this.container.find("table.kmt > tbody > tr > td").blur(this.cellOnBlur());
				}
				
			},
			
			cellOnClick: function() {
				
				var inputRenderFunc = this.input;
				var outputRenderFunc = this.output;
				var setActiveCellFunc = this.setActiveCell;
				//var onCellBlurFunc = this.cellOnBlur;
				var getActiveCellFunc = this.getActiveCell;
				
				return function() {
					//$(this).closest("table.kmt").find("tbody > tr > td").removeClass("kmtc-focus");
					$(this).addClass("kmtc-focus");
					
					inputRenderFunc($(this), "text");
					//onCellBlurFunc(getActiveCellFunc(), outputRenderFunc);
					setActiveCellFunc($(this).attr("id"));
				}
			},
			
			setActiveCell: function(cellId) {
				console.log("active cell setter " + cellId);
				this.activeCell = cellId;
			},
			
			getActiveCell: function() {
				console.log("active cell getter");
				return this.activeCell;
			},
			
			cellOnBlur: function(cellId, outputRenderFunc) {
				
				if (!cellId) {
					return;
				}
				
				var elem = $("#" + cellId);
				console.log("blur " + cellId);
				elem.removeClass("kmtc-focus");
				outputRenderFunc(elem, "text");
			},
			
			output: function(cell, dataType) {
				var val = cell.html();

				if (dataType === "text") {
					console.log("CV = " + cell.children('input.tv').val());
					// tv = true value
					cell.html(cell.children("input.tv").val());
				}
				else {
					cell.html("<span style=\"color:red\">error</span>");
				}
			},
			
			input: function(cell, dataType) {
				var val = cell.html();
				
				var code = "<input type=\"hidden\" class=\"tv\" value=\"" + val + "\"></input>";
				var cssClass = "class=\"cell-input\"";

				if (dataType === "text") {
					code += "<input " + cssClass + " type=\"text\" value=\"" + val + "\">";
				}
				else {
					code += "<span style=\"color:red\">error</span>";
				}
				
				cell.find("input.cell-input").change(function() {
					// rewrite input value to hidden cell
					$(this).siblings('input[type="hidden"]').val($(this).val());
				});
				
				
				cell.find("input.cell-input").blur((function() {
					
					return function() {
						if (!cellId) {
							return;
						}
						
						var elem = $("#" + cellId);
						console.log("blur " + cellId);
						elem.removeClass("kmtc-focus");
						outputRenderFunc(elem, "text");
					}
				})());
				
				cell.html(code);
			},
			
			renderRow: function(rowData, rowIndex) {
				
				var code = "<tr>";
				var fieldVal = null;
				
				for (var i = 0; i < this.options.columns.length; i++) {
					fieldVal = rowData[this.options.columns[i].property];
					
					// check if additional formatting should be applied to the column
					if (typeof this.options.columns[i].formatFieldValue == "function") {
						fieldVal = this.options.columns[i].formatFieldValue(fieldVal);
					}
					else if (typeof this.options.columns[i].formatObjectValue == "function") {
						fieldVal = this.options.columns[i].formatObjectValue(rowData);
					}
					
					code += "<td id=\"cell-" + rowIndex + "-" + i + "\">" + fieldVal + "</td>";
				}
				
				code += "</tr>";
				return code;
				
			},
			
			renderHeader: function() {
				var code = "<thead><tr>";
				
				for (var i = 0; i < this.options.columns.length; i++) {
					code += "<th>" + this.options.columns[i].title + "</th>";
				}
				
				code += "</tr></thead>";
				return code;
			}
				
		}
		
		return tableObj;
	
	}
		
}
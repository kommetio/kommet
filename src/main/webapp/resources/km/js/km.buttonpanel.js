/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.buttonpanel = {
	
	create: function(options) {
		
		var panel = {
			buttons: [],
			id: options.id,
			cssClass: options.cssClass,
			options: options,
			
			addButton: function(btn) {
				this.buttons.push(btn);
			},
			
			render: function(target) {
				
				var code = $("<div class=\"km-btn-panel\" id=\"" + this.id + "\"></div>");
				
				if (this.cssClass)
				{
					code.addClass(this.cssClass);
				}
				
				if (options.title)
				{
					var title = $("<div></div>").text(options.title.text);
					if (options.title.cssClass)
					{
						title.addClass(options.title.cssClass);
					}
					
					code.append(title);
				}
				
				var buttonWrapper = $("<div></div>").addClass("km-btn-panel-buttons");
				
				for (var i = 0; i < this.buttons.length; i++)
				{
					buttonWrapper.append(this.getButtonCode(this.buttons[i]));
				}
				
				code.append(buttonWrapper);
				
				if (typeof(target) === "function")
				{
					target(code);
				}
				else
				{
					target.html(code);
				}
				
				return code;
			},
			
			// private method that returns JQuery code of a button
			getButtonCode: function(btn) {
				
				var code = "<a href=\"" + (btn.url ? btn.url : "javascript:;") + "\" ";
				code += "class=\"sbtn\">" + btn.label + "</a>";
				
				var jqueryCode = $(code);
				
				if (btn.id)
				{
					jqueryCode.attr("id", btn.id);
				}
				
				if (typeof(btn.onClick) === "function")
				{
					jqueryCode.click(btn.onClick);
				}
				
				return jqueryCode;
			}
		}
		
		// return button panel
		return panel;
	}
}
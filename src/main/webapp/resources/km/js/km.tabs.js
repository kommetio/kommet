/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.tabs = {
		
	/**
	 * Create a tabs object.
	 */
	create: function(options) {
		
		var defaultSettings = {
			originalContentHandling: "hide",
			cssClass: "km-tabs-std"
		}
		
		var options = $.extend({}, defaultSettings, options);
		
		var tabs = {
			
			id: options.id,
			tabs: options.tabs,
			cssClass: options.cssClass,
			originalContentHandling: options.originalContentHandling,
			afterRender: options.afterRender,
			
			// number of times this component has been rendered
			renderCount: 0,
			
			render: function(arg) {
				
				this.renderCount++;
				var headers = $("<ul class=\"km-tabs-head\"></ul>");
				var panels = $("<div class=\"km-tabs-panels\"></div>");
				
				for (var i = 0; i < this.tabs.length; i++)
				{
					var tab = this.tabs[i];
					
					// append a header
					var header = $("<li class=\"km-tabs-head-" + i + "\">" + tab.label + "</li>");
					
					// append on click event
					header.click((function(tabs, index) {
						return function() { tabs.open(index)}
					})(this, i));
					
					headers.append(header);
					
					// create panel
					var panel = $("<div class=\"km-tabs-panel km-tabs-panel-" + i + "\"></div>");
					
					if (!(tab.content instanceof jQuery))
					{
						throw "Tab content for tab " + i + " (" + tab.label + ") is not a jQuery object, its type is " + typeof(tab.content);
					}
					
					if (tab.content)
					{	
						panel.html(tab.content.clone());
						
						// remove or hide original content
						if (this.originalContentHandling === "remove")
						{
							tab.content.remove();
						}
						else
						{
							tab.content.hide();
						}
					}
					else
					{
						throw "Content not defined for tab with ID " + i + " and label " + tab.label;
					}
					
					panels.append(panel);
				}
				
				var html = "<div class=\"km-tabs-container";
				if (this.cssClass)
				{
					html += " " + tabs.cssClass;
				}
				html += "\" id=\"" + this.id + "\"></div>";
				var code = $(html);
				code.append($("<div></div>").append(headers).addClass("km-tab-names-container"));
				code.append(panels);
				
				if (typeof(arg) === "function")
				{
					arg(code);
				}
				else
				{
					// assume the arg is a jquery object
					arg.html(code);
				}
				
				this.callAfterRender();
			},
			
			container: function() {
				return $("#" + this.id);
			},
			
			/**
			 * @public
			 * Open the current tab. If a tab is defined by an anchor appended to the URL, this tab will be opened.
			 * Otherwise, the first tab will be opened.
			 */
			openActiveTab: function() {
				
				var currentTabIndex = 0;
				
				// check if a tab is defined in URL
				if (location.hash && location.hash.indexOf("#rm.tab.") === 0)
				{
					currentTabIndex = parseInt(location.hash.substring("#rm.tab.".length));
				}
				
				this.open(currentTabIndex);
			},
			
			/**
			 * Opens the tab with the given index
			 */
			open: function(index) {
				
				if (typeof(this.tabs[index].beforeOpen) === "function")
				{
					this.tabs[index].beforeOpen();
				}
				
				// hide active panel
				this.container().find("div.km-tabs-panels > div.km-tabs-panel-active").removeClass("km-tabs-panel-active").hide();
				
				// show the current panel
				this.container().find("div.km-tabs-panels > div.km-tabs-panel-" + index).addClass("km-tabs-panel-active").show();
				
				// set all tab labels as inactive
				this.container().find("ul.km-tabs-head > li.km-tabs-head-active").removeClass("km-tabs-head-active");
				
				// set tab label as active
				this.container().find("ul.km-tabs-head > li.km-tabs-head-" + index).addClass("km-tabs-head-active");
				
				// append anchor to the current URL
				location.hash = "km.tab." + index;
			},
			
			/**
			 * Appends an after render event
			 * @public
			 */
			appendAfterRender: function(callback) {
				
				if (!this.afterRender)
				{
					this.afterRender = callback;
				}
				else if ($.isArray(this.afterRender))
				{
					this.afterRender.push(callback);
				}
				
				this.callAfterRender(this.afterRender);
			},
			
			/**
			 * Calls all the afterRender callbacks. If render has not been called yet, the callbacks are not called.
			 * @public
			 */
			callAfterRender: function() {
				
				if (this.renderCount < 1)
				{
					return;
				}
				
				if (typeof(this.afterRender) === "function")
				{
					this.afterRender(this);
				}
				else if ($.isArray(this.afterRender))
				{
					for (var i = 0; i < this.afterRender.length; i++)
					{
						var func = this.afterRender[i];
						if (typeof(func) === "function")
						{
							func(this);
						}
						else
						{
							throw "After render callback is not a function";
						}
					}
				}
			}
				
		};
		
		return tabs;
	}
		
}
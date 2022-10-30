/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.ide = {
	
	getJavaHints: function(code, varName, methodName, line, position, callback) {
		
		$.post(km.js.config.contextPath + "/km/livejavahints", { code: code, varName: varName, methodName: methodName, line: line, position: position }, (function(callback) {
			
			return function(data) {
				
				var hints = [];
				
				for (var i = 0; i < data.data.methods.length; i++)
				{
					var name = data.data.methods[i].name;
					hints.push({
						text: (methodName ? "" : ".") + name + "()",
						displayText: name
					})
				}
				
				callback(hints);
			}
			
		})(callback), "json");
		
	},
		
	getControllerTemplate: function(callback) {
		
		$.get(km.js.config.contextPath + "/km/codetemplate", { type: "controller", templateFileName: "kommet.example.MyController" }, function(data) {
			
			if (data.success === true)
			{	
				var options = {
					name: "MyController",
					content: data.data.source,
					mode: "text/x-java",
					type: "class",
					fileData: data.data.fileData
				}
				
				console.log("Calling callback");
				callback(options);
			}
			else
			{
				km.js.ui.statusbar.err("Could not load template", 10000);
			}
			
		}, "json");
		
	},
	
	getClassTemplate: function(callback) {
		
		$.get(km.js.config.contextPath + "/km/codetemplate", { type: "class", templateFileName: "kommet.example.MyClass" }, function(data) {
			
			if (data.success === true)
			{	
				var options = {
					name: "MyClass",
					content: data.data.source,
					mode: "text/x-java",
					type: "class",
					fileData: data.data.fileData
				}
				
				callback(options);
			}
			else
			{
				km.js.ui.statusbar.err("Could not load template", 10000);
			}
			
		}, "json");
		
	},
	
	getTriggerTemplate: function(callback, options) {
		
		$.get(km.js.config.contextPath + "/km/codetemplate", { type: "trigger", templateFileName: "kommet.example.SampleTrigger", typeName: options ? options.type : null }, function(data) {
			
			if (data.success === true)
			{	
				var nestedOptions = {
					name: "SampleTrigger",
					content: data.data.source,
					mode: "text/x-java",
					type: "class",
					fileData: data.data.fileData
				}
				
				callback(nestedOptions);
			}
			else
			{
				km.js.ui.statusbar.err("Could not load template", 10000);
			}
			
		}, "json");
	},
	
	getBusinessActionTemplate: function(callback) {
		
		$.get(km.js.config.contextPath + "/km/codetemplate", { type: "businessAction", templateFileName: "kommet.example.MyBusinessAction" }, function(data) {
			
			if (data.success === true)
			{	
				var options = {
					name: "MyBusinessAction",
					content: data.data.source,
					mode: "text/x-java",
					type: "class",
					fileData: data.data.fileData
				}
				
				callback(options);
			}
			else
			{
				km.js.ui.statusbar.err("Could not load template", 10000);
			}
			
		}, "json");
	},
	
	getViewTemplate: function(callback) {
		
		$.get(km.js.config.contextPath + "/km/codetemplate", { type: "view", templateFileName: "kommet.views.MyView" }, function(data) {
			
			if (data.success === true)
			{	
				var options = {
					name: "MyView",
					content: data.data.source,
					mode: "xml",
					type: "view"
				}
				
				callback(options);
			}
			else
			{
				km.js.ui.statusbar.err("Could not load template", 10000);
			}
			
		}, "json");
	},
	
	getLayoutTemplate: function(callback) {
		
		$.get(km.js.config.contextPath + "/km/codetemplate", { type: "layout", templateFileName: "kommet.views.MyLayout" }, function(data) {
			
			if (data.success === true)
			{	
				var options = {
					name: "MyLayout",
					content: data.data.source,
					mode: "xml",
					type: "layout"
				}
				
				callback(options);
			}
			else
			{
				km.js.ui.statusbar.err("Could not load template", 10000);
			}
			
		}, "json");
	}
		
};
/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.utils = {
	isEmpty: function(val) {
		return val == null || val == "" || typeof(val) == "undefined";
	},
	
	random: function(max) {
		return Math.floor((Math.random() * max) + 1);
	},
	
	isMobile: function() {
		return window.matchMedia("only screen and (min-device-width : 320px) and (max-device-width : 480px)").matches;
	},
	
	/**
	 * Returns a link to the item details depending on whether the ID represents a class, object, field etc.
	 */
	getItemLink: function(id) {
		
		var prefix = id.substring(0, 3);
		
		switch(prefix)
		{
			case "002": return "km/type/" + id;
			case "003": return "km/field/" + id;
			case "006": return "km/profile/" + id;
			case "009": return "km/actions/" + id;
			case "00a": return "km/views/" + id;
			case "00b": return "km/classes/" + id;
			case "00e": return "km/uniquechecks/" + id;
			case "00f": return "km/layouts/" + id;
			case "014": return "km/webresources/" + id;
			case "015": return "km/viewresources/" + id;
			default: return null;
		}
		
	},
	
	customizableObjectDetails: function(options) {
		
		km.js.db.query("select id from " + options.typeName, (function(options) {
			
			return function(records, recordCount, jsti) {
				
				var form = km.js.objectdetails.create({
					mode: "edit",
					jsti: jsti,
					target: options.target,
					layout: options.layout,
					type: {
						name: options.typeName
					},
					record: {},
					customizable: true,
					customization: {
						context: "environment"
					}
				});

				form.render({});
				
			}
			
		})(options));
		
	},
	
	renderProfile: function() {
		
		var pic = $("<div></div>").addClass("km-profile-img");
		pic.css("background", "url(" + km.js.config.imagePath + "/mountains.jpg)");
		
		$(".km-profile-box > .km-profile-pic").append(pic);
		
		$(".km-profile-box > .km-profile-name").text(km.js.config.authData.user.userName);
		
	},
	
	/*jsti: function() {
		
		obj = {
			types: km.js.config.types,
			fields: {}
		}
		
		for (var typeId in obj.types)
		{
			var type = obj.types[typeId];
			for (var i = 0; i < type.fields.length; i++)
			{
				var field = type.fields[i];
				field.typeId = type.id;
				obj.fields[field.id] = field;
			}
		}
		
		return obj;
		
	},*/
	
	/**
	 * Returns a deep copy of the array.
	 */
	cloneArr: function(oldArr) {
		
		var newArr = [];
		
		for (var i = 0; i < oldArr.length; i++)
		{
			// clone each object
			newArr.push($.extend({}, oldArr[i]));
		}
		
		return newArr;
		
	},
	
	bind: function (source, dest, defaultValue, convertCallback) {
		
		if (!(source instanceof jQuery))
		{
			throw "km.js.utils.bind source should be a jQuery object";
		}
		
		if (!(dest instanceof jQuery))
		{
			throw "km.js.utils.bind dest should be a jQuery object";
		}
		
		source.keyup((function(dest, defaultValue, convertCallback) {
		
			return function() {
				
				if (dest.hasClass("km-bind-manually-edited"))
				{
					// if the field was manually edited by user, we don't want to overwrite their text with our own
					return;
				}
				
				var val = defaultValue;
				if ($(this).val() != "")
				{
					val = $(this).val();
				}
				
				dest.text(val);
				
				if (typeof(convertCallback) === "function")
				{
					dest.val(convertCallback(val));
				}
				else
				{
					dest.val(val);
				}
				
			}
			
		})(dest, defaultValue, convertCallback));
		
		dest.keyup(function() {
			
			// mark the input as changed manually by user
			dest.addClass("km-bind-manually-edited");
			
		});
	},
	
	capitalize: function(s) {
		return s ? s.charAt(0).toUpperCase() + s.slice(1) : s;
	},
	
	uncapitalize: function(s) {
		return s ? s.charAt(0).toLowerCase() + s.slice(1) : s;
	},
	
	getTypeFromQuery: function(query, callback) {
		
		$.get(km.js.config.contextPath + "/km/typefromquery", { query: query }, (function(callback) {
			
			return function(data) {
				callback(data.data.type, data.data.isValidQuery);
			}
			
		})(callback), "json");
		
	},
	
	/*getFieldValue: function(record, fieldName) {
		
		if (fieldName.indexOf(".") < 0)
		{
			return record[fieldName];
		}
		else
		{
			var nameParts = fieldName.split(".");
			var nestedRecord = record[nameParts[0]];
			
			if (nestedRecord == null)
			{
				return null;
			}
			else
			{
				nameParts.shift();
				return this.getFieldValue(nestedRecord, nameParts.join("."));
			}
		}
		
	},*/
	
	addPropertyNamesToJSRC: function (records, jsti) {
		
		var newRecords = [];
		
		for (var i = 0; i < records.length; i++)
		{
			var rec = records[i];
			
			for (var fieldId in rec)
			{
				// find field in jsti
				var field = jsti.fields[fieldId];
				
				if (!field)
				{
					throw "Field with KID " + fieldId + " not found in JSTI on record " + JSON.stringify(rec);
				}
				
				var val = rec[fieldId];
				
				if ($.isArray(val))
				{
					val = this.addPropertyNamesToJSRC(val, jsti);
				}
				// note that arrays are also objects, so we need to check if the value is an array first
				else if (this.isObject(val))
				{
					var list = [];
					list.push(val);
					val = this.addPropertyNamesToJSRC(list, jsti)[0];
				}
				
				rec[field.apiName] = val;
			}
			
			newRecords.push(rec);
		}
		
		return newRecords;
	},
	
	padLeft: function (s, length, c) {
		
		if (!s)
		{
			s = "";
		}
		
		if (s.length === length)
		{
			return s;
		}
		
		for (var i = 0; i < (length - s.length); i++)
		{
			s = c + s;
		}
		
		return s;
	},
	
	isObject: function(val) {
		return val !== null && typeof val === 'object';
	},
	
	propVal: function(record, property) {
		
		if (record == null)
		{
			return null;
		}
		else if (property.indexOf(".") >= 0)
		{
			var subprops = property.split(".");
			return this.propVal(record[subprops[0]], property.substring(property.indexOf(".") + 1));
		}
		else
		{
			return km.js.utils.isEmpty(record) ? null : record[property];
		}
	},
	
	/**
	 * Return the first value if it is not empty, otherwise return the second one.
	 */
	coalesce: function (val1, val2) {
		return val1 ? val1 : val2;
	},
	
	getScriptInclude: function(src) {
		return "<script type=\"text/javascript\" src=\"" + km.js.config.contextPath + "/" + src + "\"></script>";
	},
	
	getCssInclude: function(src) {
		return "<link href=\"" + km.js.config.contextPath + "/" + src + "\" rel=\"stylesheet\" type=\"text/css\" />";
	},
	
	normalizeId: function(id) {
		return id ? id.replace(/\./g, "-") : id;
	},
	
	/**
	 * @param dateString Date in format YYYY-mm-dd HH:MM:SS
	 */
	parseDate: function (dateString, timezone) {
		
		if (!timezone)
		{
			timezone = "GMT";
		}
		
		var dateParts = dateString.split(" ");
		var ymd = dateParts[0];
		var hms = dateParts[1];
		
		// split year, month, day
		var ymdParts = ymd.split("-");
		var year = parseInt(ymdParts[0]);
		var month = parseInt(ymdParts[1]) - 1;
		var day = parseInt(ymdParts[2]);
		
		var hour = 0;
		var min = 0;
		var sec = 0;
		
		if (hms)
		{
			var hmsParts = hms.split(":");
			var hour = parseInt(hmsParts[0]);
			var min = parseInt(hmsParts[1]);
			var sec = parseInt(hmsParts[2]);
		}
		
		return new Date(year, month, day, hour, min, sec);
	},
	
	/**
	 * Executes a callback method for each property of an object
	 * @param callback function taking two parameters - the property name and its value
	 */
	forEachProp: function (object, callback) {
		
		for (var prop in object)
		{
			if (object.hasOwnProperty(property))
			{
				callback(prop, object[prop]);
			}
		}
	},
	
	/**
	 * Convert new lines to BR in string
	 */
	nl2br: function(s) {
		return s != null ? s.replace(/(?:\r\n|\r|\n)/g, '<br />') : null;
	},
	
	setPropVal: function(record, property, val) {
		if (property.indexOf(".") == -1)
		{
			record[property] = val;
		}
		else
		{
			var subprops = property.split(".");
			var subRecord = record[subprops[0]];
			if (!subRecord)
			{
				subRecord = {};
			}
			this.setPropVal(subRecord, property.substring(property.indexOf(".") + 1), val);
			record[subprops[0]] = subRecord;
		}
	},
	
	lastProperty: function(pir) {
	
		if (pir == null || pir.indexOf(".") < 0)
		{
			return pir;
		}
		
		var bits = pir.split(/\./);
		return bits[bits.length - 1];
	},
	
	insert: function(index, arr, obj) {
		arr.splice(index, 0, obj);
	},
	
	remove: function(index, arr) {
		arr.splice(index, 1);
	},
	
	openURL: function(url) {
		location.href = url;
	},
	
	formatDate: function(date, pattern) {
		
		var padNum = function(s) {
			return s.toString().length < 2 ? "0" + s : s;
		}
		
		return date.getFullYear() + "-" + padNum(date.getMonth() + 1) + "-" + padNum(date.getDate());
	},
	
	/**
	 * Returns the number of properties in a Javascript object.
	 */
	size: function(obj) {
	    var size = 0;
	    for (var key in obj)
	    {
	        if (obj.hasOwnProperty(key))
	        {
	        	size++;
	        }
	    }
	    return size;
	},
	
	populateIdsOnProperties: function (properties, baseType, jsti) {
		
		if (!baseType)
		{
			throw "Cannot populate IDs on table display options because the passed base type is null";
		}
		
		for (var i = 0; i < properties.length; i++)
		{
			var prop = properties[i];
			if (prop.id)
			{
				continue;
			}
			
			if (!prop.name)
			{
				throw "Neither property name nor its ID specified in table display options";
			}
			
			prop.id = km.js.utils.nestedPropertyToPir(prop.name, baseType, jsti);
			
			if (!prop.id)
			{
				throw "Could not resolve property ID for property name " + prop.name + " on type " + baseType.qualifiedName + " (type ID " + baseType.id + ")";
			}
		}
		
	},
	
	nestedPropertyToPir: function(propName, baseType, jsti) {
		
		var subprops = propName.split(".");
		var typeName = baseType.qualifiedName;
		var firstPropPir = jsti.pirs[typeName + "." + subprops[0]]; 
		
		if (subprops.length === 1)
		{
			return firstPropPir;
		}
		else
		{
			var firstProp = jsti.fields[firstPropPir];
			
			var dtId = firstProp.dataType.id;
			var newBaseType = null;
			
			if (dtId === km.js.datatypes.object_reference.id)
			{
				newBaseType = jsti.types[firstProp.dataType.typeId]; 
			}
			else if (dtId === km.js.datatypes.inverse_collection.id)
			{
				newBaseType = jsti.types[firstProp.dataType.inverseTypeId]; 
			}
			else if (dtId === km.js.datatypes.association.id)
			{
				newBaseType = jsti.types[firstProp.dataType.associatedTypeId]; 
			}
			else
			{
				throw "Field " + firstProp.name + " is not an object reference, collection or association, but is used with nested properties";
			}
			
			if (!newBaseType)
			{
				throw "New base type not retrieved";
			}
			
			return firstPropPir + "." + this.nestedPropertyToPir(propName.substring(propName.indexOf(".") + 1), newBaseType, jsti);
		}
		
	},
	
	isSimpleRestriction: function(restriction) {
		return restriction.operator.toLowerCase() !== "or" && restriction.operator.toLowerCase() !== "and" && restriction.operator.toLowerCase() !== "not";	
	},
	
	getTypeFromJSTI: function(jcr, jsti) {
		
		if (jcr.baseTypeId)
		{
			type = jsti.types[jcr.baseTypeId];
			if (type == null)
			{
				throw "Type with ID " + jcr.baseTypeId + " not found";
			}
		}
		else if (jcr.baseTypeName)
		{
			type = this.getTypeFromJSTIByName(jcr.baseTypeName, jsti);
			if (type == null)
			{
				throw "Type with name " + jcr.baseTypeName + " not found";
			}
		}
		else
		{
			throw "Neither base type name nor ID specified in JCR";
		}
		
		return type;
	},
	
	getTypeFromJSTIByName: function(typeName, jsti) {
		
		for (var typeQualifiedName in jsti.types)
		{
			var type = jsti.types[typeQualifiedName];
			if (type.qualifiedName === typeName)
			{
				return type;
			}
		}
		return null;
	},
	
	openMenuItem: function(label) {
		//var currentItem = $("div#content td.left-menu a:contains('" + label + "')").closest("li").addClass("km-menu-highlighted");
		var currentItem = $("div#content td.left-menu a")
			.filter((function(label) {
				return function() {
					return $(this).text() == label;
				}
			})(label))
			.closest("li").addClass("km-menu-highlighted");
		
		currentItem.parent().closest("li").find("ul").show();
	},
	
	openCurrentMenuItem: function() {
		var position = window.location.href.indexOf(km.js.config.contextPath) + km.js.config.contextPath.length;
		var currentURL = window.location.href.substring(position);
		
		var currentItem = $("div#content div#left-menu a[href$='" + currentURL + "']").closest("li").addClass("km-menu-highlighted");
		
		currentItem.parent().closest("li").find("ul").show();
	},
	
	forEachRestriction: function(restrictions, callback) {
		
		var newRestrictions = [];
		
		if ($.isArray(restrictions))
		{
			newRestrictions = [].concat(restrictions);
		}
		else
		{
			// add single restriction to the array
			newRestrictions.push(restrictions);
		}
		
		for (var i = 0; i < newRestrictions.length; i++)
		{
			var restr = newRestrictions[i];
			callback(restr);
			
			// call this method for subrestrictions
			if (restr.operator.toLowerCase() === "or" || restr.operator.toLowerCase() === "and" || restr.operator.toLowerCase() === "not")
			{
				this.forEachRestriction(restr.args, callback);
			}
		}
	},
	
	adjustIframeSize: function(iframeId, minHeight, maxHeight, minWidth, maxWidth) {
	    
		var iframe = document.getElementById(iframeId);
	    var newheight = iframe.contentWindow.document.body.scrollHeight + 30;
	    var newwidth = iframe.contentWindow.document.body.scrollWidth + 30;
	    
	    if (maxHeight && newheight > maxHeight)
	    {
	    	newheight = maxHeight;
	    }
	    if (minHeight && newheight < minHeight)
	    {
	    	newheight = minHeight;
	    }
	    if (maxWidth && newwidth > maxWidth)
	    {
	    	newwidth = maxWidth;
	    }
	    if (minWidth && newwidth < minWidth)
	    {
	    	newwidth = minWidth;
	    }

	    iframe.height = (newheight) + "px";
	    iframe.width = (newwidth) + "px";
	    
	    console.log("Adjusting size: " + newheight + " / " + newwidth);
	},
	
	customTypes: function() {
		
		var customTypeList = [];
	
		for (var typeId in km.js.config.types)
		{
			var type = km.js.config.types[typeId];
			if (type.isBasic === false)
			{
				customTypeList.push(type);
			}
		}
		
		return customTypeList;
		
	},
	
	userLookup: function(options) {
		
		// jcr to query users
		var jcr = {
			baseTypeName: "kommet.basic.User",
			properties: [
				{ name: "id" },
				{ name: "userName" }
			]
		};
		
		// options of the available items list
		var availableItemsOptions = {
			display: {
				properties: [
					{ name: "userName", label: km.js.config.i18n["user.username"], linkStyle: true }
				],
				idProperty: { name: "id" }
			},
			title: km.js.config.i18n["tasks.users"],
			tableSearchOptions: {
				properties: [ { name: "userName", operator: "ilike" } ]
			}
		};
		
		// create the lookup
		var lookup = km.js.ref.create({
			selectedRecordDisplayField: { name: "userName" },
			jcr: jcr,
			availableItemsDialogOptions: {},
			availableItemsOptions: availableItemsOptions,
			inputName: options.inputName,
			inputId: options.inputId,
			selectedRecordId: options.recordId,
			visibleInput: options.visibleInput,
			afterSelect: options.afterSelect,
			mode: options.mode,
			editable: options.editable
		});
		
		lookup.render(options.target);
		return lookup;
	},
	
	isInteger: function(val) {
		return parseInt(val, 10);
	},
	
	dateToGMT: function(date) {
		
		if (date)
		{
			if (km.js.utils.isInteger(date))
			{
				date = new Date(date);
			}
			
			var newDate = new Date(date.getTime());
			newDate.setHours(date.getHours() - 1 * km.js.config.authData.timeZoneOffset);
			return newDate;
		}
		else
		{
			return null;
		}
	},
	
	/**
	 * Creates a date/time picker from the field.
	 */
	dateTimePicker: function(options) {
		
		var input = $("<input></input>").datetimepicker({
			dateFormat: "yy-mm-dd",
			addSliderAccess: true,
			showHour: !options.dateOnly,
			showMinute: !options.dateOnly,
			showTime: !options.dateOnly,
			sliderAccessArgs: { touchonly: false },
			onSelect: (function(onSelectCallback) {
				return function(val) {
					$(this).removeClass("km-output");
					
					if (typeof(onSelectCallback) === "function")
					{
						onSelectCallback(val);
					}
				}
			})(options.onSelect)
		});
			
		input.addClass("km-input");
		input.datepicker("setDate", options.value);
		
		if (!options.mode)
		{
			options.mode = "edit";
		}
		
		if (options.mode === "view")
		{
			input.addClass("km-output");
		}
		
		input.click(function() {
			$(this).removeClass("km-output");
		});
		
		input.blur(function() {
			$(this).addClass("km-output");
		});
		
		if (options.target instanceof jQuery)
		{
			options.target.empty().append(input);
		}
		else if (typeof(options.target) === "function")
		{
			options.target(input);
		}
		
		var picker = {
			input: input,
			
			enable: function() {
				this.input.removeClass("km-output");
			}
		}
		
		return picker;
	},
	
	userGroupLookup: function(options) {
		
		// jcr to query groups
		var jcr = {
			baseTypeName: "kommet.basic.UserGroup",
			properties: [
				{ name: "id" },
				{ name: "name" }
			]
		};
		
		// options of the available items list
		var availableItemsOptions = {
			display: {
				properties: [
					{ name: "name", label: km.js.config.i18n["usergroups.groupname"], linkStyle: true }
				],
				idProperty: { name: "id" }
			},
			title: km.js.config.i18n["usergroups.list.title"],
			tableSearchOptions: {
				properties: [ { name: "name", operator: "ilike" } ]
			}
		};
		
		// create the lookup
		var lookup = km.js.ref.create({
			selectedRecordDisplayField: { name: "name" },
			jcr: jcr,
			availableItemsDialogOptions: {},
			availableItemsOptions: availableItemsOptions,
			inputName: options.inputName,
			inputId: options.inputId,
			selectedRecordId: options.recordId,
			visibleInput: options.visibleInput,
			mode: options.mode,
			afterSelect: options.afterSelect,
			editable: options.editable
		});
		
		lookup.render(options.target);
		return lookup;
	}
};
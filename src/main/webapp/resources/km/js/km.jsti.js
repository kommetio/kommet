/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.jsti = {
		
	// get JSTI for type and call the onSuccess method when it is fetched
	forTypes: function (typePrefixes, typeNames, onSuccess) {
	
		var payload = {
				
		}
		
		if ($.isArray(typePrefixes))
		{
			payload.typePrefixes = typePrefix.join(",");
		}
		
		if ($.isArray(typeNames))
		{
			payload.typeNames = typeNames.join(",");
		}
	
		$.post(km.js.config.sysContextPath + "/rest/jsti", payload, onSuccess, "json");
	},

	// return a JSTI for the given type and field IDs
	get: function (typeIds, fieldIds) {
	
		var jstiObj = {
			fields: {},
			types: {},
			
			fieldInfo: function (propId) {
				var info = this.fields[propId];
				return km.js.utils.isEmpty(info) ? null : info;
			},
			
			typeInfo: function (typeId) {
				var info = this.types[typeId];
				return km.js.utils.isEmpty(info) ? null : info;
			}
		};
		
		if (km.js.utils.isEmpty(fieldIds))
		{
			fieldIds = [];
		}
		
		if (km.js.utils.isEmpty(typeIds))
		{
			typeIds = [];
		}
		
	
		for (var i = 0; i < fieldIds.length; i++)
		{
			// get the last property from PIR
			var propId = km.js.utils.lastProperty(fieldIds[i]);
			
			// TODO add actual field info here
			jstiObj.types[propId] = {
				
			}
		}
	
		return jstiObj;
	},
	
	fieldData: function(jsti, pir) {
		var propId = pir.indexOf('.') > -1 ? pir.substring(pir.indexOf('.') + 1) : pir;
		return jsti.fields[propId];
	}
}
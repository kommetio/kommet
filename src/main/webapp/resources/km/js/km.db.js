/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.db = {
		
	query: function(query, onSuccess, onFailure) {
		
		var ds = km.js.datasource.create({
			type: "database"
		});
		
		ds.query(query, onSuccess, onFailure);
	},
	
	update: function(record, onSuccess, onFail, options) {
		
		var recordId = record.id;
		delete record.id;
		
		var payload = {
			id: recordId,
			record: JSON.stringify(record),
		};
		
		if (options)
		{
			if (options.typeName)
			{
				payload.typeName = options.typeName;
			}
		}
		
		$.post(km.js.config.contextPath + "/km/rest/record/save", payload, function(data) {
			
			if (typeof(onSuccess) === "function")
			{
				onSuccess(data.success, data);
			}
			
		}, "json")
		.fail(function(resp) {
			
			if (typeof(onFail) === "function")
			{
				onFail(JSON.parse(resp.responseText));
			}
			
		});
		
	},
	
	deleteRecord: function(recordId, onSuccess, onFail) {
		
		$.post(km.js.config.contextPath + "/km/rest/record/delete", { id: recordId }, function(data) {
			
			if (typeof(onSuccess) === "function")
			{
				onSuccess(data.success, data);
			}
			
		}, "json")
		.fail(function(resp) {
			
			if (typeof(onFail) === "function")
			{
				onFail(JSON.parse(resp.responseText));
			}
			
		});
		
	}
};
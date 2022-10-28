/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.data = {
		
	associate: function (relationFieldId, parentId, childId, onSuccess, onFailure) {
	
		var onResponseCallback = function(onSuccess, onFailure) {
			return function(data) {
				
				if (data.success === true)
				{
					if (km.js.utils.isEmpty(data.id))
					{
						throw "Association successful, but ID of linking record not returned";
					}
					
					// pass the ID of the created linking record to the callback
					if (typeof(onSuccess) === "function")
					{
						onSuccess(data.id);
					}
				}
				else
				{
					if (typeof(onFailure) === "function")
					{
						onFailure(data);
					}
				}
			}
		}
	
		$.post(km.js.config.sysContextPath + "/rest/associate", { relationFieldId: relationFieldId, parentId: parentId, childId: childId }, onResponseCallback(onSuccess, onFailure), "json");
	},
	
	unassociate: function (relationFieldId, parentId, childId, onSuccess) {
		
		var onResponseCallback = function(onSuccess) {
			return function(data) {
				
				// pass the ID of the created linking record to the callback
				if (typeof(onSuccess) === "function")
				{
					onSuccess(data.id);
				}
			}
		}
	
		$.post(km.js.config.sysContextPath + "/rest/unassociate", { relationFieldId: relationFieldId, parentId: parentId, childId: childId }, onResponseCallback(onSuccess), "json");
	}
		
}
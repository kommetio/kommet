/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.userlookup = {
	
	create: function(settings) {
	
		var defaultSettings = {
			id: "user-lookup"
		}
	
		var options = $.extend({}, defaultSettings, settings);
		
		// jcr to query profiles
		var jcr = {
			baseTypeName: "kommet.basic.User",
			properties: [
				{ name: "userName" },
				{ name: "id" }
			]
		};
		
		// options of the available items list
		var availableItemsOptions = {
			options: {
				id: "user-search"
			},
			display: {
				properties: [
					{ name: "userName", label: "User Name", linkStyle: true }
				],
				idProperty: { name: "id" }
			},
			title: "Users",
			tableSearchOptions: {
				properties: [ { name: "userName", operator: "ilike" } ]
			}
		};
		
		// create the lookup
		var userLookup = km.js.ref.create({
			selectedRecordDisplayField: { name: "userName" },
			jcr: jcr,
			availableItemsDialogOptions: {},
			availableItemsOptions: availableItemsOptions,
			inputName: options.inputName,
			inputId: options.inputId,
			visibleInput: options.visibleInput,
			selectedRecordId: options.selectedRecordId
		});
		
		return userLookup;
	}
}
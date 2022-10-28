/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.relatedlist = {
		
	create: function(options) {
			
		var ds = km.js.datasource.create({
			type: "database"
		});
		
		if (!options.jcr)
		{
			throw "[km.js.relatedlist] jcr is null";
		}
		
		if (!options.target)
		{
			throw "[km.js.relatedlist] target not defined";
		}
		
		var defaultDisplayOptions = {
			/*properties: [
				{ name: "url", label: "Domain URL", linkStyle: false },
				{ name: "id", label: "Delete", content: deleteCallback }
			],*/
			idProperty: { name: "id" }
		};
		
		var defaultTableOptions = {
			id: "relatedlist"
		};
		
		var displayOptions = $.extend({}, defaultDisplayOptions, options.displayOptions);
		var tableOptions = $.extend({}, defaultTableOptions, options.tableOptions);
			
		var availableItemsTable = km.js.table.create(ds, options.jcr, displayOptions, tableOptions);
		
		availableItemsTable.render(options.target);
	}
		
};
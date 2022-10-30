/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.fileupload = {
	
	create: function(settings) {
	
		var defaultSettings = {
			id: "file-upload",
			parentDialog: null
		}
	
		var options = $.extend({}, defaultSettings, settings);
		
		var dialog = km.js.ui.dialog.create({
			id: options.id,
			size: {
				width: "800px",
				height: "600px"
			},
			url: km.js.config.sysContextPath + "/files/new?" + (options.parentDialog ? "parentDialog=" + options.parentDialog : "") + (options.recordId ? "&recordId=" + options.recordId : ""),
			afterClose: options.afterClose
		});
		
		return dialog;
	}
}
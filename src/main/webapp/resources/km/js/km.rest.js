/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.rest = {
	
	call: function(url, onSuccess) {
		
		// trim the trailing slash
		if (url.indexOf("/") == 0)
		{
			url = url.substring(1);
		}
		
		$.get(km.js.config.contextPath + "/" + url, onSuccess, "json");
		
	}
		
};
/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

if (typeof(km.js.scope) === "undefined")
{
	// define scope object
	km.js.scope = {
		// dialogs by id
		dialogs: {},
		
		// km.js.ref objects by id
		refs: {},
		
		// km.js.rel objects by id
		rels: {},
		
		// event whose listeners are notifier when window is resized (as a result of some action)
		resizeEvent: {
			
			// list of callback javascript functions to be called
			listeners: [],
			
			listen: function(callback) {
				
				if (typeof(callback) !== "function")
				{
					throw "Cannot register listener as it is not a function: " + callback;
				}
				
				this.listeners.push(callback);
			},
			
			notify: function() {
				
				for (var i = 0; i < this.listeners.length; i++)
				{
					var listener = this.listeners[i];
					if (typeof(listener) === "function")
					{
						// invoke callback
						listener(window);
					}
				}
				
			}
			
		}
	}
}
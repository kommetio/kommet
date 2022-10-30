/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.notifier = {
		
	get: function() {
	
		var notifier = {
				
			waits: {},
			onComplete: null,
			isComplete: false,
		
			wait: function(waitName) {
				this.waits[waitName] = false;
				this.isComplete = false;
			},
			
			reach: function(waitName) {
				this.waits[waitName] = true;
				
				var allWaitsReached = true;
				
				// check if all waits are reached
				for (wait in this.waits)
				{
					if (this.waits[wait] !== true)
					{
						allWaitsReached = false;
						break;
					}
				}
				
				if (allWaitsReached === true)
				{
					this.isComplete = true;
					this.onComplete(this);
				}
			}		
		}
		
		return notifier;
	}
		
}
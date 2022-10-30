/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.cookies = {
		
	create: function(name, value, days) {
	    var expires = "";
	    if (days) {
	        var date = new Date();
	        date.setTime(date.getTime() + (days*24*60*60*1000));
	        expires = "; expires=" + date.toUTCString();
	    }
	    document.cookie = name + "=" + value + expires + "; path=/";
	},

	read: function(name) {
	    var nameEQ = name + "=";
	    var ca = document.cookie.split(';');
	    for(var i=0;i < ca.length;i++) {
	        var c = ca[i];
	        while (c.charAt(0)==' ') {
	        	c = c.substring(1,c.length);
	        }
	        if (c.indexOf(nameEQ) == 0) {
	        	return c.substring(nameEQ.length,c.length);
	        }
	    }
	    return null;
	},

	erase: function(name) {
	    create(name, "", -1);
	}
		
}
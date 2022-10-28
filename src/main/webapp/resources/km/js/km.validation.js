/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.validation = {
	init: function (target) {
		
		// if target is not defined, initialize validation on the whole document
		if (!target)
		{
			target = $(document);
		}
		else if (!(target instanceof jQuery))
		{
			throw "Target of validation.init must be a jQuery object";
		}
		
		target.find("input[km-validate=number]").each((function(validator) {
			
			return function() {
				
				$(this).change(function(e) {
					validator.validateNumber($(this), e);
				});
				
				$(this).blur(function(e) {
					validator.validateNumber($(this), e);
				});
			}
			
		})(this));
		
	},
	
	/**
	 * @public
	 */
	isValidNumber: function(val) {
		return (new RegExp(km.js.config.localeNumberFormat)).test(val);
	},
	
	validateNumber: function(input, e) {
		
		var val = input.val();
		
		// validate numeric, but allow empty values
		if (val && !this.isValidNumber(input.val()))
		{
			input.addClass(this.errorClass);
			input.focus();
		}
		else
		{
			input.removeClass(this.errorClass);
		}
	},
	
	errorClass: "km-validate-err"
}
/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.ui = {
		
	boolButton: function(options) {
		
		var manualCallbacks = typeof(options.onSetTrue) === "function" && typeof(options.onSetFalse) === "function";
		
		if (!manualCallbacks)
		{
			if (!(options.target instanceof jQuery))
			{
				throw "Target of a boolButton call has to be a jQuery object"; 
			}
			
			if (!options.recordId)
			{
				throw "Record ID not defined in boolButton call"; 
			}
			
			if (!options.field)
			{
				throw "Field not defined in boolButton call"; 
			}
			
			if (!options.type)
			{
				throw "Type not defined in boolButton call"; 
			}
		}
		
		var initialValue = options.value;
		
		var initButton = function(options)
		{
			console.log("Initializing boolButton with options: " + JSON.stringify(options));
			
			var icon = $("<i class=\"fa km-bool\"></i>").addClass(options.value ? "fa-check km-bool-checked" : "fa-close km-bool-unchecked").addClass("km-bool-button-icon");
			options.target.find("i.km-bool").remove();
			options.target.prepend(icon);
			
			if (!options.refreshMode)
			{
				options.target.click((function(options) {
					
					return function() {
						
						console.log("Button clicked");
						
						var icon = $(this).find("i.km-bool");
						
						var newValue = !icon.hasClass("km-bool-checked");
						
						var onSuccess = (function(options, icon) {
							return function() {
								console.log("Record updated")
								options.value = newValue;
								options.refreshMode = true;
								initButton(options);
							} 
						})(options, icon);
						
						if (newValue && typeof(options.onSetTrue) === "function")
						{
							options.onSetTrue(record, options, onSuccess);
						}
						else if (!newValue && typeof(options.onSetFalse) === "function")
						{
							options.onSetFalse(record, options, onSuccess);
						}
						else
						{
							// if onSetTrue/False callbacks are not set, call the classic update function on the record
							
							var record = {
								id: options.recordId
							}
							
							record[options.field] = newValue;
							
							console.log("Updating record");
							
							km.js.db.update(record, onSuccess, null);
						}
					}
					
				})(options));
			}
		}
		
		// if value not available, fetch it
		if (initialValue !== true && initialValue !== false)
		{
			if (manualCallbacks)
			{
				throw "Initial value not set in boolButton called with manual callbacks";
			}
			
			console.log("Fetching initial value");
			
			km.js.db.query("select " + options.field + " from " + options.type + " where id = '" + options.recordId + "' limit 1", (function(callback, options) {
				
				return function(data, count, jsti)
				{
					console.log("Initial value: " + JSON.stringify(data));
					
					data = km.js.utils.addPropertyNamesToJSRC(data, jsti);
					
					if (!data.length)
					{
						throw "Record '" + options.recordId + "' of type " + options.type + " not found in km.js.ui.boolButton function";
					}
					
					options.value = data[0][options.field];
					callback(options);
				}
				
			})(initButton, options));
		}
		else
		{
			initButton(options);
		}
		
	},
		
	buttonWait: function(options) {
		
		if (options.isRestore == null)
		{
			// restore text after save
			options.isRestore = true;
		}
		
		if (!(options.button instanceof jQuery))
		{
			throw "Button passed to km.ui.buttonWait method is not a jQuery object";
		}
		
		var elem = $("<div class=\"km-spinner\"></div>");
		for (var i = 0; i <= 5; i++)
		{
			elem.append($("<div></div>").addClass("km-spinner-" + i));
		}
		
		var oldContent = options.button.text() ? options.button.text() : options.button.val();
		
		options.button.empty().append(elem).append($("<div></div>").text(options.text ? options.text : "").addClass("km-waitbtn-text"));
		
		return {
			options: options,
			text: oldContent,
			isRestore: options.isRestore,
			startTime: (new Date()).getTime()
		}
	},
	
	buttonWaitStop: function(waitObj) {
		
		var restoreText = function() {
			var button = waitObj.options.button;
			if (typeof(button.val) === "function")
			{
				button.empty().val(waitObj.text);
			}
			if (typeof(button.text) === "function")
			{
				button.empty().text(waitObj.text);
			}
		}
		
		if (waitObj.isRestore)
		{
			var currentTime = (new Date()).getTime();
			
			if ((currentTime - waitObj.startTime) > 1000)
			{
				// restore text immediately
				restoreText();
			}
			else
			{
				// schedule text for restoring to avoid blinking
				setTimeout(restoreText, 1000);
			}
		}
		
	},
	
	/**
	 * Return header code with the given text.
	 * @param text - header text
	 * @returns jQuery object
	 */
	header: function(text, cssClass, cssStyle) {
		var head = $("<div class=\"km-ui-header km-title\">" + text + "</div>");
		if (cssClass)
		{
			head.addClass(cssClass);
		}
		if (cssStyle)
		{
			head.attr("style", cssStyle);
		}
		return head;
	},
	
	ripple: function(target) {
		
		if (!target)
		{
			target = $(document.body);
		}
		
		target.click(function(e) {
			
			var circle = $("<div></div>").addClass("km-ripple");
			
			circle.css("top", (e.pageY) + "px").css("left", (e.pageX) + "px");
			circle.css("background-color", "#ccc");
			$(document.body).append(circle);
			
			circle.animate({
				opacity: 0.3,
				width: "+=40px",
				height: "+=40px",
				left: "-=20px",
				top: "-=20px"
			}, 300, function() {
						$(this).hide();
				});
			
		});
		
	},
	
	appendMenuImages: function() {
		
		if (!$(".km-menu").hasClass("has-menu-icons"))
		{
			$(".km-menu").addClass("has-menu-icons");
			
			$(".km-menu > ul > li > a").each(function() {
				var icon = $("<img></img>").attr("src", km.js.config.imagePath + "/menuarrow.png");
				$(this).prepend($("<span></span>").append(icon).addClass("km-menu-icon"));
			});
		}
	},
	
	tooltip: function(options) {
		
		var icon = $("<img></img>").attr("src", km.js.config.imagePath + "/tooltip.png").addClass("km-tooltip-icon");
		
		icon.click((function(options) {
			
			return function(e) {
				
				//var closeBtn = $("<img></img>").attr("src", km.js.config.imagePath + "/close.gif");
				//closeBtn.css("position", "absolute").css("top", "5px").css("right", "5px");
				
				var textWrapper = $("<div></div>").css("position", "relative").text(options.text);
				//textWrapper.append(closeBtn);
				
				var msgWrapper = $("<div></div>").addClass("km-tooltip-text").append(textWrapper);
				
				// add random id
				var id = "tooltip-" + Math.floor(Math.random() * (10000000));
				msgWrapper.attr("id", id);
				
				/*closeBtn.click((function(wrapper) {
					
					return function() {
						wrapper.remove();
					}
					
				})(msgWrapper));*/
				
				$("div.km-tooltip-text").remove()
				$(document).find("body").append(msgWrapper);
				
				msgWrapper.css("top", e.pageY - msgWrapper.outerHeight() / 2).css("left", e.pageX + 20);
				
				$(document).find("body").click(function() {
					
					// hide all tooltips
					$("div.km-tooltip-text").remove();
					
				});
				
				e.stopPropagation();
				
			}
			
		})(options));
		
		var iconWrapper = $("<span></span>").append(icon).addClass("km-tooltip-wrapper");
		
		if (options.afterTarget instanceof jQuery)
		{
			options.afterTarget.after(iconWrapper);
		}
		
	},
	
	formatApiName: function(val) {
		
		if (!val)
		{
			return;
		}
		
		val = km.js.utils.capitalize(val);
		
		// replace whitespace
		val = val.replace(/\s/g, "_");
		
		return val;
		
	},
	
	formatFieldName: function(val) {
		
		if (!val)
		{
			return;
		}
		
		val = km.js.utils.uncapitalize(val);
		
		// replace whitespace
		val = val.replace(/\s/g, "_");
		
		return val;
		
	},
	
	autoFormatName: function(options) {
		
		if (!(options.target instanceof jQuery))
		{
			throw "Target of the autoformatApiName function must be a jQuery object";
		}
		
		var formatCallback = function() {
			
			var oldVal = $(this).val();
			
			if (!oldVal)
			{
				return;
			}
			
			var packageName = "";
			var bareName = "";
			
			if (oldVal.indexOf(".") >= 0)
			{
				packageName = oldVal.substring(0, oldVal.lastIndexOf("."));
				bareName = oldVal.substring(oldVal.lastIndexOf(".") + 1, oldVal.length);
			}
			else
			{
				bareName = oldVal;
			}
			
			// if it is a qualified name, only format the last part
			var newVal = (packageName ? packageName.toLowerCase() + "." : "") + km.js.ui.formatApiName(bareName);
			
			if (newVal !== oldVal)
			{
				$(this).val(newVal);
				$(this).addClass("km-auto-format-highlight");
			}
			else
			{
				$(this).removeClass("km-auto-format-highlight");
			}
			
		}
		
		options.target.focusout(formatCallback);
		
		if (options.onKeyUp)
		{
			options.target.keyup(formatCallback);
		}
	},
	
	autoFormatFieldName: function(options) {
		
		if (!(options.target instanceof jQuery))
		{
			throw "Target of the autoformatApiName function must be a jQuery object";
		}
		
		var formatCallback = function() {
			
			var oldVal = $(this).val();
			
			if (!oldVal)
			{
				return;
			}
			
			var newVal = km.js.ui.formatFieldName(oldVal);
			
			if (newVal !== oldVal)
			{
				$(this).val(newVal);
				$(this).addClass("km-auto-format-highlight");
			}
			else
			{
				$(this).removeClass("km-auto-format-highlight");
			}
			
		}
		
		options.target.focusout(formatCallback);
		
		if (options.onKeyUp)
		{
			options.target.keyup(formatCallback);
		}
	},
	
	editable: function(inputOptions) {
		
		var defaultOptions = {};
		
		var options = $.extend({}, defaultOptions, inputOptions);
		
		var input = $("<input></input>").attr("type", "text");
		if (options.inputName)
		{
			input.attr("name", options.inputName);
		}
		
		// when enter is clicked, accept the changes and leave
		input.keyup(function (e) {
		    if (e.keyCode == 13) {
		       $(this).focusout();
		    }
		});
		
		if (options.inputId)
		{
			input.attr("id", options.inputId);
		}
		
		if (options.cssClass)
		{
			input.addClass(options.cssClass);
		}
		
		if (!(options.target instanceof jQuery))
		{
			throw "Target should be a jQuery object";
		}
		
		input.focusout((function(target, onAccept) {
			
			return function() {
				
				if (typeof(onAccept) === "function")
				{
					// call the onAccept callback
					onAccept($(this).val());
				}
				
				if ($(this).val() === "")
				{
					$(this).val("<empty>");
				}
				
				target.text($(this).val());
				$(this).hide();
				target.show();
			}
			
		})(options.target, options.onAccept));
		
		options.target.click((function(input, onActivate) {
			
			return function() {
				
				if (typeof(onActivate) === "function")
				{
					// call the onAccept callback
					onActivate();
				}
				
				input.insertAfter($(this));
				$(this).hide();
				input.val($(this).text());
				input.show();
				input.select();
			}
			
		})(input, options.onActivate));
		
	},
		
	confirm: function(options) {
		
		if (!(options.target instanceof jQuery))
		{
			throw "Target must be a jQuery object";
		}
		
		options.target.click((function(options) {
			
			return function() {
				
				if (!options.target.is(":visible")) {
					return;
				}
				
				options.target.hide();
				
				var btns = $("<div class=\"km-ui-confirm\"><span class=\"km-ui-confirm-q\">" + (options.question ? options.question : km.js.config.i18n["msg.delete.warning"]) + "</span></div>");
				
				var yesBtn = $("<a href=\"javascript:;\">" + (options.yesLabel ? options.yesLabel : km.js.config.i18n["btn.yes"]) + "</a>");
				yesBtn.click(options.callback);
				
				var noBtn = $("<a href=\"javascript:;\">" + (options.noLabel ? options.noLabel : km.js.config.i18n["btn.no"]) + "</a>");
				
				noBtn.click(function() {
					$(this).closest(".km-ui-confirm").remove();
					options.target.show();
				});
				
				btns.append(yesBtn).append(noBtn);
				options.target.after(btns);
			}
			
		})(options));
		
	},
	
	error: function(msg, target, cssClass, cssStyle) {
		return this.message(msg, target, "error", cssClass, cssStyle);
	},
	
	info: function(msg, target, cssClass, cssStyle) {
		return this.message(msg, target, "info", cssClass, cssStyle);
	},
	
	message: function(msg, target, type, cssClass, cssStyle) {
		
		var cls = "msg-tag";
		var img = "<img src=\"" + km.js.config.imagePath + "/";
		
		if (type === "error")
		{
			cls += " action-errors"
			img += "erricon.png";
		}
		else if (type === "info")
		{
			cls += " action-msgs"
			img += "infoicon.png";
		}
		
		img += "\" />";
		
		if (cssClass != null)
		{
			cls += " " + cssClass;
		}
		
		var actualMsg = "";
		
		if (msg instanceof Array)
		{
			actualMsg = "<ul>";
			for (i = 0; i < msg.length; i++)
			{
				actualMsg += "<li>" + msg[i] + "</li>";
			}
			actualMsg += "</ul>";
		}
		else
		{
			actualMsg = "<ul><li>" + msg + "</li></ul>";
		}
		
		var tr = $("<tr></tr>").append($("<td></td>").append($(img))).append($("<td></td>").html(actualMsg));
		var table = $("<table></table>").append($("<tbody></tbody>").append(tr));
		var code = $("<div class=\"" + cls + "\" style=\"" + cssStyle + "\"></div>").append(table);
		
		if (typeof(target) === "function")
		{
			target(code);
		}
		else if (target instanceof jQuery)
		{
			target.empty().append(code).show();
		}
	},
	
	/**
	 * Transform the given DOM-jQuery element into a tabbed panel.
	 * This method only adds javascript events for switching and hiding tabs.
	 * 
	 * This method is mainly used by the km:tabs tag.
	 */
	applyTabs: function(target) {
		
		if (!(target instanceof jQuery))
		{
			throw "Argument of function km.ui.applyTabs must be a jQuery object, instead got " + typeof(target);
		}
		
		var openTab = function (tabIndex, container) {
			
			// hide all previously opened panels
			container.find("div.km-tabs-panels > div.km-tabs-panel").removeClass("km-tabs-panel-active").hide();
			
			// show the current panel
			container.find("div.km-tabs-panels > div.km-tabs-panel-" + tabIndex).addClass("km-tabs-panel-active").show();
			
			// set all tab labels as inactive
			container.find("ul.km-tabs-head > li.km-tabs-head-active").removeClass("km-tabs-head-active");
			
			// set tab label as active
			container.find("ul.km-tabs-head > li.km-tabs-head-" + tabIndex).addClass("km-tabs-head-active");
			
			// append anchor to the current URL
			location.hash = "km.tab." + tabIndex;
		};
		
		target.find("ul.km-tabs-head > li").click((function(tabContainer, openTabCallback) {
			
			return function() {
			
				// open the clicked tab and hide all others
				openTabCallback($(this).index(), tabContainer);
			}
		
		})(target, openTab));
		
		var currentTabIndex = 0;
		
		// check if a some tab is not defined as open in the URL
		if (location.hash && location.hash.indexOf("#km.tab.") === 0)
		{
			currentTabIndex = parseInt(location.hash.substring("#km.tab.".length));
		}
		
		// show the active panel
		openTab(currentTabIndex, target);
	},
	
	/*inprogress: function(options) {
		
		if (!(options.button instanceof jQuery))
		{
			throw "Button passed to km.ui.inprogress method is not a jQuery object";
		}
		
		var icon = $("<img></img>").addClass("km-btn-icon").attr("src", km.js.config.imagePath + "/ellipsis.gif");
		
		options.button.empty().append(icon).append(options.text ? options.text : "");
		
	},*/
	
	/**
	 * A status bar displayed on top of the page.
	 */
	statusbar: {
		
		// a timer that hides the status bar
		timer: null,
		stdCssClass: null,
		container: null,
		
		/**
		 * @private
		 * Shows a status bar with the specified messages.
		 * @param msg A single message or array of messages
		 * @param hideAfterMillis Number of milliseconds after which the status bar will be hidden
		 */
		render: function (msg, hideAfterMillis, cssClass) {
			
			// clear any previous timer so that the status bar does not get closed too soon
			if (this.timer)
			{
				clearTimeout(this.timer);
			}
			
			var bar = null;
			
			if ($("div." + this.cssClass).length === 0)
			{
				barWrapper = $("<div></div>").addClass("km-status-bar").addClass(cssClass);
				bar = $("<div></div>").addClass("content").appendTo(barWrapper);
				
				// add close button
				bar.append(km.js.ui.closeBtn(function() {
					barWrapper.remove();
				}));
				
				// status bar does not exist, create it
				$(document.body).append(barWrapper);
			}
			else
			{
				bar = $("div." + this.cssClass + " div.content");
				
				// clear previously displayed messages
				bar.find("ul.km-status-msgs").remove();
			}
			
			this.container = barWrapper;
			
			// now display the message
			var msgUL = $("<ul></ul>").addClass("km-status-msgs");
			
			if (!$.isArray(msg))
			{
				var msgList = [];
				msgList.push(msg);
			}
			else
			{
				msgList = msg;
			}
			
			for (var i = 0; i < msgList.length; i++)
			{
				var msgItem = $("<li></li>").text(msgList[i]);
				msgUL.append(msgItem);
			}
			
			// append list to status bar
			bar.append(msgUL);
			
			if (hideAfterMillis)
			{
				this.timer = setTimeout(function() { km.js.ui.statusbar.hide(); }, hideAfterMillis);
			}
		},
		
		/**
		 * @public
		 * Shows a status bar with the specified messages.
		 * @param msg A single message or array of messages
		 * @param hideAfterMillis Number of milliseconds after which the status bar will be hidden
		 */
		show: function(msg, hideAfterMillis) {
			
			// add info css class
			var stdCssClass = "km-status-bar-std km-status-bar-std-info";
			// render standard status bar
			this.render(msg, hideAfterMillis, stdCssClass);
			
		},
		
		/**
		 * @public
		 * Shows a status bar with the specified error messages.
		 * @param msg A single message or array of messages
		 * @param hideAfterMillis Number of milliseconds after which the status bar will be hidden
		 */
		err: function(msg, hideAfterMillis) {
			
			// render standard status bar
			this.render(msg, hideAfterMillis, "km-status-bar-std km-status-bar-std-err");
			
		},
		
		/**
		 * Hides the status bar, removing it from DOM as well.
		 */
		hide: function() {
			$(".km-status-bar").remove();
		}
		
	},
	
	/**
	 * Returns JQuery code of a close button.
	 * @param callback Javascript function to be called when the close button is clicked.
	 */
	closeBtn: function(callback) {
		
		var img = $("<img>").attr("src", km.js.config.imagePath + "/ex.png").addClass("km-close");
		img.click(function() {
			callback();
		})
		
		return img;
	},
	
	bool: function(container) {
		container.each(function() {
			val = $(this).text();
			if (val)
			{
				val = val.trim();
			}
			var img = $("<img></img>").attr("src", km.js.config.imagePath + "/" + (val == "true" ? "check.gif" : "uncheck.png"));
			$(this).empty().append(img);
		});
	},
	
	typeLookup: function (options) {
		
		// target, inputName, selectedRecordId, types, afterSelect, dialogAfterClose, visibleInput
		
		if (!options.types)
		{
			// if types are not defined, take all custom types from JSTI
			options.types = km.js.utils.customTypes();
		}
		
		// create an JSON datasource from the available types
		var offlineDS = km.js.datasource.create({
			type: "json",
			data: options.types
		});
		
		// jcr to query profiles
		var jcr = {
			properties: [
				{ name: "qualifiedName" },
				{ name: "id" },
				{ name: "label" }
			]
		};
		
		if ("" === options.selectedRecordId)
		{
			selectedRecordId = null;
		}
		
		// options of the available items list
		var availableItemsOptions = {
			options: {
				id: "type-lookup-search"
			},
			display: {
				properties: [
					{ name: "label", label: "Label", linkStyle: true },
					{ name: "qualifiedName", label: "API Name", linkStyle: true }
				],
				idProperty: { name: "id" },
				defaultProperty: { name: "label" }
			},
			title: "Types"
		};
		
		// create the lookup
		var typeLookup = km.js.ref.create({
			datasource: offlineDS,
			selectedRecordDisplayField: { name: "label" },
			jcr: jcr,
			availableItemsDialogOptions: {
				afterClose: options.dialogAfterClose
			},
			availableItemsOptions: availableItemsOptions,
			inputName: options.inputName,
			selectedRecordId: options.selectedRecordId,
			afterSelect: options.afterSelect,
			visibleInput: options.visibleInput
		});
		
		typeLookup.render(options.target);
		return typeLookup;
	},
	
	dialog: {
		
		create: function(settings) {

			var defaultSettings = {
				size: {
					/*width: Math.floor($(window).width() * 0.7),
					height: Math.floor($(window).height() * 0.7),
					maxWidth: Math.floor($(window).width() * 0.7),
					maxHeight: Math.floor($(window).height() * 0.7)*/
					width: Math.floor(window.document.body.clientWidth * 0.7),
					height: Math.floor(window.document.body.clientHeight * 0.8),
					maxWidth: Math.floor(window.document.body.clientWidth * 0.7),
					maxHeight: Math.floor(window.document.body.clientHeight * 0.8)
				}
			}
			
			var options = $.extend(true, {}, defaultSettings, settings);
			
			if (!options.id)
			{
				// generate random ID
				options.id = "km-dialog-" + km.js.utils.random(1000000);
			}
			
			if (options.size.fitToContent === true)
			{
				options.size.width = options.size.height = null;
			}

			var dg = {
				
				//addOpenDialog: (function(rmdialog) { return function(dialog) { rmdialog.addOpenDialog(dialog); } })(this),
				//removeOpenDialog: (function(rmdialog) { return function(dialogId) { rmdialog.removeOpenDialog(dialogId); } })(this),
				id: options.id,
				imagePath: km.js.config.imagePath + "/",
				options: options,
				
				addOpenDialog: function(dialog) {
					km.js.scope.dialogs[km.js.utils.normalizeId(dialog.id)] = dialog;
				},
				
				removeOpenDialog: function(dialogId) {
					delete km.js.scope.dialogs[km.js.utils.normalizeId(dialogId)];
				},
				
				show: function(target) {
				
					var code = "<div class=\"km-dialog-overlay";
					
					if (!km.js.utils.isEmpty(this.options.cssClass))
					{
						code += " " + this.options.cssClass;
					}
					
					if (km.js.utils.isMobile())
					{
						code += " km-dialog-mobile";
					}
					
					code += "\" id=\"" + this.id + "\">";
					code += "<div class=\"km-dialog\"";
					
					var style = "";
					
					if (!km.js.utils.isMobile())
					{
						if (options.size.width)
						{
							style += "width: " + options.size.width;
						}
						
						if (options.size.height)
						{
							style += ";height: " + options.size.height;
						}
					}
					else
					{
						style += "height: " + $(window).height() + "px";
					}
					
					code += "style=\"" + style + "\">";
					code += "<div class=\"topbar\">";
					
					if (!km.js.utils.isEmpty(options.title))
					{
						code += options.title;
					}
					
					code += "<img src=\"" + this.imagePath + "ex.png\" class=\"close-btn\"></div>";
					
					// start the main div that will contain the dialog's content
					code += "<div class=\"km-dialog-content\"";
					
					if (!km.js.utils.isEmpty(options.style))
					{
						code += " style=\"" + options.style + "\"";
					}
					
					code += ">";
					code += "</div></div></div>";
					
					var elem = $(code);
					
					if (km.js.utils.isMobile())
					{
						elem.find(".km-dialog-content").css("height", $(window).height() + "px");
					}
					
					// append close event on the overlay
					var closeCallback = function(dialog) {
						return function() {
							dialog.close();
						}
					}
					
					elem.find(".close-btn").click(closeCallback(this)).mouseover(function() {
						$(this).attr("src", km.js.config.imagePath + "/exh.png");
					}).mouseout(function() {
						$(this).attr("src", km.js.config.imagePath + "/ex.png");
					});
					
					elem.click(closeCallback(this));
					
					elem.find(".km-dialog").click(function(e) {
						// intercept click on the dialog and prevent propagating them to the overlay
						// otherwise every click on the dialog would fire the close event attached to the overlay
						e.stopPropagation();
					});
					
					// if element already exists, replace it, otherwise add to DOM
					if (document.getElementById(this.id) != null)
					{
						this.self().replaceWith(elem);
					}
					else
					{
						$("body").append(elem);
					}
					
					if (!km.js.utils.isEmpty(options.url))
					{
						// add an iframe with the given ID to the dialog
						elem.find(".km-dialog-content").append("<iframe src=\"" + options.url + "\" style=\"height: " + options.size.height + "\"></iframe>");
					}
					else
					{
						// check if content should be rendered within an iframe
						if (options.iframe)
						{
							var iframeContent = $("<html></html>");
							
							var head = $("<head></head>");
							
							if (options.iframe.addDefaultStyles === true)
							{
								// add default kommet scripts and styles
								head.append($(km.js.utils.getCssInclude("resources/km/css/km.all.min.css")));
								head.append($(km.js.utils.getCssInclude("resources/layout.css")));
								head.append($(km.js.utils.getCssInclude("resources/tag-styles.css")));
								head.append($(km.js.utils.getCssInclude("resources/header.css")));
								head.append($(km.js.utils.getCssInclude("resources/themes/std/styles.css")));
								head.append($(km.js.utils.getScriptInclude("resources/km/js/km.core.js")));
								head.append($(km.js.utils.getScriptInclude("js/km.config.js")));
								head.append($(km.js.utils.getScriptInclude("resources/km/js/km.all.min.js")));
								head.append($(km.js.utils.getScriptInclude("resources/km/js/km.ui.js")));
								
								// TODO remove this
								// head.append($(km.js.utils.getScriptInclude("resources/km/js/km.scope.js")));
							}
							
							// append additional styles
							if (window.kommet && $.isArray(window.kommet.styles))
							{
								for (var i = 0; i < window.kommet.styles.length; i++)
								{
									head.append($("<link href=\"" + window.kommet.styles[i] + "\" rel=\"stylesheet\" type=\"text/css\" />"));
								}
							}
							
							iframeContent.append(head);
							
							var body = $("<body></body>");
							if (options.iframe.bodyCssClass)
							{
								body.addClass(options.iframe.bodyCssClass);
							}
							
							if (options.iframe.bodyCssStyle)
							{
								body.attr("style", options.iframe.bodyCssStyle);
							}
							
							iframeContent.append(body.append(target));
							
							var iframeId = this.id + "-iframe";
							
							var iframe = $("<iframe></iframe>").attr("id", iframeId);
							
							if (options.iframe.cssClass)
							{
								iframe.addClass(options.iframe.cssClass);
							}
							
							// temporarily hide iframe
							iframe.hide();
							
							// put the iframe inside the dialog window
							elem.find(".km-dialog-content").empty().append(iframe);
							
							// add load icon
							var loadIcon = $("<img></img>").attr("src", km.js.config.imagePath + "/ellipsis.gif");
							elem.find(".km-dialog-content").append(loadIcon);
							
							// body of the document inside the iframe
							var iframeBody = elem.find(".km-dialog-content > iframe").contents().find("body");
							
							// contents of an iframe has to be set after the iframe has been
							// appended to the document
							iframeBody.addClass("km-font-scale").append(iframeContent);
							
							if (options.iframe.cssBodyClass)
							{
								iframeBody.addClass(options.iframe.cssBodyClass);
							}
							
							// for some reason in firefox the body element must be replaced with its modified instance
							// in chrome the below instruction is not necessary
							elem.find(".km-dialog-content > iframe").contents().find("body").replaceWith(iframeBody);
							
							// if dialog width and height have not been specified, adjust them to iframe
							iframe.ready((function(dialog, loadIcon, iframe) {
								
								return function() {
									
									loadIcon.hide();
									iframe.show();
									
									dialog.adjustSize();
									
									// register listener so that the dialog is resized every time
									// the size of the iframe content changes.
									km.js.scope.resizeEvent.listen((function() {
										
										return function(window) {
											console.log("Adjusting iframe to changed content")
											dialog.adjustSize();
										}
										
									})());
								}
								
							})(this, loadIcon, iframe));
							
							//this.adjustSize();
						}
						else
						{
							elem.find(".km-dialog-content").append(target);
						}
					}
					
					if (typeof(this.options.beforeShow) === "function")
					{
						this.options.beforeShow(this);
					}
				
					$("body").css("overflow", "hidden");
					$("html").css("overflow", "hidden");
					
					// add dialog to list of open dialogs
					this.addOpenDialog(this);
					
					// return for chaining
					return this;
				},
				
				/**
				 * Adjusts dialog size to the dimensions specified by the user and possibly to the content.
				 * @private
				 */
				adjustSize: function() {
					
					var iframeHeight = null, iframeWidth = null;
					var dialogPadding = 30;
					
					// assume default values - they are always set
					var width = this.options.size.width;
					var height = this.options.size.height;
					
					var hasIframe = false;
					
					if (this.options.size.fitToContent === true)
					{					
						// check if dialog contains an iframe
						if (this.self().find("iframe").size() > 0)
						{
							hasIframe = true;
							
							var iframe = this.self().find("iframe");
							var iframeId = iframe.attr("id");
							
							//console.log("IFRAME body: " + document.getElementById(iframeId).contentWindow.document.body.scrollWidth);
							//console.log("IFRAME body2: " + iframe.contents().find("body").width());
							
							// scale the body of the iframe
							/*if (this.options.size.maxWidth && document.getElementById(iframeId).contentWindow.document.body.scrollWidth > this.options.size.maxWidth)
							{
								document.getElementById(iframeId).contentWindow.document.body.scrollWidth.style.width = this.options.size.maxWidth + "px";
								document.getElementById(iframeId).contentWindow.document.body.scrollWidth.style.maxWidth = this.options.size.maxWidth + "px";
							}
							
							if (this.options.size.maxHeight && document.getElementById(iframeId).contentWindow.document.body.scrollHeight > this.options.size.maxHeight)
							{
								document.getElementById(iframeId).contentWindow.document.body.scrollWidth.style.height = this.options.size.maxHeight + "px";
								document.getElementById(iframeId).contentWindow.document.body.scrollWidth.style.maxHeight = this.options.size.maxHeight + "px";
							}*/
							
							
							//iframeHeight = document.getElementById(iframeId).contentWindow.document.body.scrollHeight + 20;
							//iframeWidth = document.getElementById(iframeId).contentWindow.document.body.scrollWidth + 20;
							iframeHeight = iframe.contents().find("body").height() + 20;
							iframeWidth = iframe.contents().find("body").width() + 100;
							
							// set iframe size according to content - do not restrict the size in any way
						    iframe.css("width", iframeWidth + "px");
						    iframe.css("height", iframeHeight + "px");
						    
						    height = iframeHeight;
						    width = iframeWidth;
						}
					}
					
					width += (2 * dialogPadding) + 100;
					height += (2 * dialogPadding) + 40;
					
					if (this.options.size.maxHeight && height > this.options.size.maxHeight)
				    {
						height = this.options.size.maxHeight;
				    }
				    if (this.options.size.minHeight && height < this.options.size.minHeight)
				    {
				    	height = this.options.size.minHeight;
				    }
				    if (this.options.size.maxWidth && width > this.options.size.maxWidth)
				    {
				    	width = this.options.size.maxWidth;
				    }
				    if (this.options.size.minWidth && width < this.options.size.minWidth)
				    {
				    	width = this.options.size.minWidth;
				    }
				    
				    console.log("Allow body scale: " + this.options.iframe.allowAdjustBodyWidth);
				    
				    // sometimes we may be allowed to set the width of the iframe's body
				    // e.g. if it contains a km.js.table record list or a record form,
				    // their width can be adjusted
				    if (hasIframe && this.options.iframe.allowAdjustBodyWidth)
				    {
				    	iframe.contents().find("body").css("width", Math.floor(width * 0.9));
				    }
		
					var dialogWindow = this.self().find(".km-dialog");
					
					if (!km.js.utils.isMobile())
					{
						console.log("not mobile");
						dialogWindow.css("width", width + "px");
						dialogWindow.css("height", height + "px");
						
						var dialogWindowContent = dialogWindow.find(".km-dialog-content");
						dialogWindowContent.css("width", width + "px");
						dialogWindowContent.css("height", height + "px");
					}
					else
					{
						console.log("mobile");
						dialogWindow.css("width", "100%");
						dialogWindow.css("height", $(window).height());
					}
					
					this.self().find(".km-dialog-content").css("padding", dialogPadding + "px");
				},
				
				self: function() {
					return $("div[id='" + this.id + "']");
				},
				
				// Hides the dialog element and removes it from DOM.
				// After close() has been called, show() needs to be called to render the dialog again.
				close: function() {
					this.self().remove();
					$("body").css("overflow", "initial");
					$("html").css("overflow", "auto");
					
					if (typeof(this.options.afterClose) === "function")
					{
						this.options.afterClose();
					}
					
					// remove from the list of open dialogs
					this.removeOpenDialog(this.id);
				},
				
				// Hides the dialog element, but does not remove it from DOM.
				hide: function() {
					this.self().hide();
					$("body").css("overflow", "auto");
					$("html").css("overflow", "auto");
					
					// remove from the list of open dialogs
					this.removeOpenDialog(this.id);
				},
				
				content: function(html) {
					this.self().find(".km-dialog-content").html(html);
				},
				
				// resize dialog to full screen
				fullscreen: function() {
					this.self().find(".km-dialog")
						.css("width", "100%")
						.css("height", "100vh")
						.addClass("km-dialog-fullscreen");
					
					// return for chaining
					return this;
				}
			}
			
			return dg;
		},
		// end of km.js.ui.dialog.create
		
		closeAllDialogs: function() {
			for (var dialogId in km.js.scope.dialogs)
			{
				km.js.scope.dialogs[dialogId].close();
			}
		}
	}
};

// check if km.js.ui.dialog is already defined, because if it is, we don't want to overwrite
// the openDialogs list in it
/*if (typeof(km.js.ui.dialog) === "undefined")
{
	km.js.ui.dialog = {
		
		closeAllDialogs: function() {
			console.log("Calling close all dialogs")
			for (var dialogId in km.js.scope.dialogs)
			{
				console.log("Closing dialog " + dialogId);
				km.js.scope.dialogs[dialogId].close();
			}
		}
	}
}*/

/*km.js.ui.dialog.addOpenDialog = function(dialog) {
	km.js.scope.dialogs[dialog.id.replace(/\./g, "-")] = dialog;
};

km.js.ui.dialog.removeOpenDialog = function(dialogId) {
	delete km.js.scope.dialogs[dialogId.replace(/\./g, "-")];
};*/


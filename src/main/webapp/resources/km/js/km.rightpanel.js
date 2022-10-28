/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.rightpanel = {
	
	create: function(settings) {
		
		var defaultSettings = {
			
			recentItems: { show: true, count: 5 },
			notifications: { show: true, count: 5 },
			
			// icon is a jQuery object pointing to an icon (usually the bell icon) that displays
			// the right panel
			// Numbers indicating number of notifications and tasks will be overlayed over this icon
			icon: null
			
		}
		
		var mergedSettings = $.extend({}, defaultSettings, settings);
		
		var widget = {
				
			options: mergedSettings,
			container: null,
				
			render: function(target) {
				
				this.container = $("<div></div>").addClass("km-rp-container");
				
				this.container.append(this.userInfo());
				
				if (typeof(target) === "function")
				{
					// invoke callback
					target(this.container, this);
				}
				else if (target instanceof jQuery)
				{
					target.empty().append(this.container);
				}
				
				// query notifications
				this.initNotifications(this.container);
				this.initTasks(this.container);
				this.initEvents(this.container);
				
			},
			
			initNotifications: function(container) {
				
				var ds = km.js.datasource.create({
					type: "database"
				});
				
				var jcr = {
					baseTypeName: "kommet.basic.Notification",
					properties: [
						{ name: "id" },
						{ name: "text" },
						{ name: "title" },
						{ name: "viewedDate" },
					],
					restrictions: [ 
					                { property_name: "assignee.id", operator: "eq", args: [ km.js.config.authData.user.id ]},
					                { property_name: "viewedDate", operator: "isnull", args: [] }
								]
				};
				
				ds.query(jcr, (function(container, icon, openCallback) {
					
					return function(records, recordCount, jsti) {
						
						records = km.js.utils.addPropertyNamesToJSRC(records, jsti);
						
						// create notification list
						var list = $("<div></div>").addClass("km-rp-list-wrapper");
						var ul = $("<ul></ul>").addClass("km-rp-list");
						var title = $("<div></div>").addClass("km-rp-list-title").text(km.js.config.i18n["notif.notification.title"] + " (" + records.length + ")");
						list.append(title);
						
						// number of non-viewed notifications
						var nonViewedNotificationCount = 0;
						
						if (records.length > 0)
						{
							for (var i = 0; i < records.length; i++)
							{	
								var rec = records[i];
								
								var li = $("<li></li>");
								
								var title = $("<div></div>").addClass("km-rp-list-item-title").text(rec.title);
								
								var closeBtn = $("<img></img>").attr("src", km.js.config.imagePath + "/ex.png").addClass("km-rp-list-close");
								
								closeBtn.click((function(notificationId, item) {
									
									return function() {
										
										console.log("Deleting notification " + notificationId);
										$.post(km.js.config.contextPath + "/km/notifications/setviewed", { id: notificationId }, function(data) {
											
											if (data.status === "success")
											{
												li.remove();
											}
											else
											{
												km.js.ui.statusbar.show("Could not set notification as viewed", 10000);
											}
											
										}, "json");
										
									}
									
								})(rec.id, li));
								
								title.append(closeBtn);
								
								var text = $("<div></div>").addClass("km-rp-list-item-text").text(rec.text);
								
								li.append(title).append(text);
								
								if (!rec.viewedDate)
								{
									nonViewedNotificationCount++;
									li.addClass("km-unread");
								}
								
								li.click((function(notificationId) {
									
									return function() {
										
										var record = {
											id: notificationId,
											viewedDate: (new Date()).getTime()
										};
										
										km.js.db.update(record, (function(li) {
											return function() {
												li.removeClass("km-unread");
												li.fadeOut(500);
											}
										})($(this)));
										
									}
									
								})(rec.id));
								
								ul.append(li);
							}
							
							var listWrapper = $("<div></div>").append(ul).addClass("km-rp-item-wrapper");
							
							if (records.length > 5)
							{
								listWrapper.addClass("km-rp-scroll");
							}
							
							list.append(listWrapper);
							
							if (nonViewedNotificationCount && (icon instanceof jQuery))
							{
								var numbers = $("<div></div>").addClass("km-alert-numbers");
								numbers.text(nonViewedNotificationCount);
								
								numbers.click((function(openCallback) {
									return function() {
										openCallback();
									}
								})(openCallback));
								
								icon.append(numbers);
							}
						}
						else
						{
							// append empty list message
							list.append($("<div></div>").addClass("km-rp-list-noitems").text(km.js.config.i18n["rightpanel.list.noitems"]));
						}
						
						container.append(list);
						
					}
					
				})(container, this.options.icon, this.options.openCallback));
				
			},
			
			initTasks: function(container) {
				
				var ds = km.js.datasource.create({
					type: "database"
				});
				
				var jcr = {
					baseTypeName: "kommet.basic.Task",
					properties: [
						{ name: "id" },
						{ name: "content" },
						{ name: "title" }
					],
					restrictions: [ 
		                { property_name: "assignedUser.id", operator: "eq", args: [ km.js.config.authData.user.id ]}
					]
				};
				
				ds.query(jcr, (function(container) {
					
					return function(records, recordCount, jsti) {
						
						records = km.js.utils.addPropertyNamesToJSRC(records, jsti);
						
						// create notification list
						var list = $("<div></div>").addClass("km-rp-list-wrapper");
						var ul = $("<ul></ul>").addClass("km-rp-list");
						var title = $("<div></div>").addClass("km-rp-list-title km-rp-list-title-clickable").text(km.js.config.i18n["rightpanel.tasks.title"] + " (" + records.length + ")");
						
						// open task list
						title.click(function() {
							km.js.utils.openURL(km.js.config.contextPath + "/km/tasks/userlist");
						});
						
						list.append(title);
						
						if (records.length > 0)
						{
							for (var i = 0; i < records.length; i++)
							{
								var rec = records[i];
								var li = $("<li></li>").addClass("km-rp-list-item-link");
								
								var title = $("<div></div>").addClass("km-rp-list-item-title").text(rec.title);
								
								li.click((function(taskId, item) {
									
									return function() {
										// open task details
										km.js.utils.openURL(km.js.config.contextPath + "/km/tasks/userlist?id=" + taskId);
									}
									
								})(rec.id, li));
								
								var text = $("<div></div>").addClass("km-rp-list-item-text").text(rec.content);
								
								li.append(title).append(text);
								ul.append(li);
							}
							
							var listWrapper = $("<div></div>").append(ul).addClass("km-rp-item-wrapper");
							
							if (records.length > 5)
							{
								listWrapper.addClass("km-rp-scroll");
							}
							
							list.append(listWrapper);
						}
						else
						{
							// append empty list message
							list.append($("<div></div>").addClass("km-rp-list-noitems").text(km.js.config.i18n["rightpanel.list.noitems"]));
						}
						
						container.append(list);
						
					}
					
				})(container));
				
			},
			
			initEvents: function(container) {
				
				var today = new Date();
				today.setHours(0);
				today.setMinutes(0);
				today.setSeconds(0);
				
				// add seven days
				var endDate = new Date(today);
				endDate.setDate(endDate.getDate() + 6);
				endDate.setHours(23);
				endDate.setMinutes(59);
				endDate.setSeconds(59);
				
				$.get(km.js.config.contextPath + "/km/rest/events", { startDate: today.getTime(), endDate: endDate.getTime(), userId: km.js.config.authData.user.id }, (function(container) {
					
					return function(data) {
						
						var events = data.data.events;
						
						// create notification list
						var list = $("<div></div>").addClass("km-rp-list-wrapper");
						var ul = $("<ul></ul>").addClass("km-rp-list");
						var title = $("<div></div>").addClass("km-rp-list-title").text(km.js.config.i18n["rightpanel.events.title"] + " (" + events.length + ")");
						list.append(title);
						
						if (events.length > 0)
						{
							for (var i = 0; i < events.length; i++)
							{
								var rec = events[i];
								var li = $("<li></li>").addClass("km-rp-list-item-link");
								
								var title = $("<div></div>").addClass("km-rp-list-item-title").text(rec.name);
								
								li.click((function(eventId, item) {
									
									return function() {
										// open task details
										km.js.utils.openURL(km.js.config.contextPath + "/km/events/" + eventId);
									}
									
								})(rec.id, li));
								
								var text = $("<div></div>").addClass("km-rp-list-item-text").text(rec.description ? rec.description.substring(0, 40) : "");
								
								li.append(title).append(text);
								ul.append(li);
							}
							
							var listWrapper = $("<div></div>").append(ul).addClass("km-rp-item-wrapper");
							
							if (records.length > 5)
							{
								listWrapper.addClass("km-rp-scroll");
							}
							
							list.append(listWrapper);
						}
						else
						{
							// append empty list message
							list.append($("<div></div>").addClass("km-rp-list-noitems").text(km.js.config.i18n["rightpanel.list.noitems"]));
						}
						
						container.append(list);
						
					}
					
				})(container));
				
			},
			
			userInfo: function() {
				
				var code = $("<div></div>").addClass("km-rp-username-wrapper");
				code.append($("<div></div>").addClass("km-rp-username-title").text(km.js.config.i18n["rightpanel.loggedin.as"]));
				code.append($("<div></div>").addClass("km-rp-username").text(km.js.config.authData.user.userName));
				return code;
				
			}
				
		};
		
		return widget;
		
	}
		
}
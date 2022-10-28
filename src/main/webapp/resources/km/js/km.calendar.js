/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.calendar = {

	create: function(options) {
	
		var defaultOptions = {
			range: "week",
			daysPerRow: 7,
			cssClass: "km-cal-std",
			hourStart: 7,
			hourEnd: 18,
			eventUrl: "events/{id}",
			
			// whether hours should be shown
			hours: true,
			
			formatDate: function(date) {
				return date !== null ? this.stdDateFormat(date, "-") : "";
			}
		};
		
		options = $.extend({}, defaultOptions, options);
		
		return {
		
			range: options.range,
			startDate: options.startDate,
			endDate: options.endDate,
			hourStart: options.hourStart,
			hourEnd: options.hourEnd,
			hours: options.hours,
			formatDate: options.formatDate,
			daysPerRow: options.daysPerRow,
			cssClass: options.cssClass,
			defaultEventDuration: 3600000,
			eventUrl: options.eventUrl,
			id: options.id ? options.id : "cal-" + (Math.floor(Math.random() * 1000) + 1),
			
			/**
			 * @public
			 */
			addEvents: function(events) {
				
				for (var i = 0; i < events.length; i++)
				{
					this.addEvent(events[i]);
				}
				
			},
			
			showRange: function (startDate, endDate) {
			
				if (startDate < this.startDate)
				{
					startDate = this.startDate;
				}
				
				if (endDate > this.endDate)
				{
					endDate = this.endDate;
				}
				
				// hide all day cells
				this.container().find(".km-cal-day").hide();
				
				// show dates in range
				for (var d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1))
				{
					var dateId = this.stdDateFormat(d, "");
					this.container().find(".km-cal-day-" + dateId).show();
				}
			},
			
			/**
			 * @private
			 */
			addEvent: function(event) {
				
				var hour = this.hourStart;
				var duration = event.duration ? event.duration : this.defaultEventDuration;
				var eventStartDate = new Date(event.startDate);
				var eventEndDate = new Date(event.startDate + duration);
				
				// check the hour at which the event is to be positioned
				if (eventStartDate.getHours() < this.hourStart)
				{
					if (eventEndDate.getHours() < this.hourStart)
					{
						console.log("Ignoring event " + event.name);
						// the whole event is outside the hour range, so we just ignore it
						return;
					}
				}
				else
				{
					hour = eventStartDate.getHours();
				}
				
				var code = $("<div class=\"km-cal-event\"></div>");
				code.append("<div class=\"km-cal-event-inner\"><a class=\"km-cal-event-link\" href=\"" + this.eventUrl.replace(/{id}/g, event.id) + "\">" + event.name + "</a></div>");
				
				var dayAndHourId = this.stdDateFormat(eventStartDate, "") + hour;
				
				//console.log("Next: " + this.stdDateFormat(event.startDate, "") + eventEndDate.getHours());
				//console.log("End: " + new Date((new Date(2015, 1, 2, 7)).getTime() + duration).toString());
				
				var containingHourCell = this.container().find("div.km-cal-hour-" + dayAndHourId + " > div.km-cal-event-content").first();
				var nextHourCell = this.container().find("div.km-cal-hour-" + this.stdDateFormat(eventStartDate, "") + (eventEndDate.getHours() + (eventEndDate.getMinutes() > 0 ? 1 : 0)) + " > div.km-cal-event-content").first();
				var nextHourCellY = nextHourCell.position().top;
				
				var lastButOneHourCell = null;
				if (eventEndDate.getHours() > eventStartDate.getHours())
				{
					lastButOneHourCell = this.container().find("div.km-cal-hour-" + (this.stdDateFormat(eventStartDate, "") + (eventEndDate.getHours() - 1)) + " > div.km-cal-event-content").first();
				}
				else
				{
					lastButOneHourCell = containingHourCell;
				}
				
				var lastButOneHourCellY = lastButOneHourCell.position().top;
				
				console.log("Event: " + event.name + ", MIN: " + eventEndDate.getMinutes());
				console.log("NEXT: " + nextHourCellY + ", BUT: " + lastButOneHourCellY);
				
				// get the height of the event box depending on its duration
				var startY = containingHourCell.position().top;
				var endY = eventEndDate.getMinutes() > 0 ? (lastButOneHourCellY + (eventEndDate.getMinutes() * (nextHourCellY - lastButOneHourCellY))/60) : nextHourCellY;
				code.css("height", endY - startY);
				
				containingHourCell.append(code);
				
			},
			
			container: function() {
				return $("#" + this.id);
			},
			
			render: function(arg) {
			
				var html = "<div class=\"km-cal-container " + this.cssClass + "\" id=\"" + this.id + "\">";
				
				var days = $("<div class=\"km-cal-days\"></div>");
				
				var dayIndex = 0;
				
				for (var d = new Date(options.startDate); d <= options.endDate; d.setDate(d.getDate() + 1))
				{
					if (dayIndex % this.daysPerRow === 0)
					{
						// start new row
						days.append("<div class=\"km-cal-day-row\"></div>");
					}
					
					days.append(this.renderDay(d));
					
					dayIndex++;
					if (dayIndex % this.daysPerRow === 0)
					{
						// end row
						days.append("</div>");
					}
				}
			
				// close container
				html += "</div>";
				
				var code = $(html);
				code.append(days);
				
				if (typeof(arg) === "function")
				{
					arg(code);
				}
				else
				{
					arg.append(code);
				}
			
			},
			
			stdDateFormat: function(date, separator) {
			
				var padInt = function(i) {
					var s = i + "";
					return s.length == 1 ? "0" + s : s;
				}
			
				return date.getFullYear() + separator + padInt(date.getMonth() + 1) + separator + padInt(date.getDate());
			},
			
			/**
			 * @private
			 * @param dateId - string date, e.g. 20110322
			 */
			renderDay: function(date) {
			
				var dateId = this.stdDateFormat(date, "");
				var code = $("<div class=\"km-cal-day km-cal-day-" + dateId + "\"></div>");
				code.append("<div class=\"km-cal-date-label\">" + this.formatDate(date) + "</div>");
				
				if (this.hours === true)
				{
					code.append(this.renderHours(date));
				}
				
				return code;
			},
			
			renderHours: function(date) {
			
				var html = "<div class=\"km-cal-hours\">";
				
				for (var hour = this.hourStart; hour <= this.hourEnd; hour++)
				{
					html += "<div class=\"km-cal-hour km-cal-hour-" + this.stdDateFormat(date, "") + hour + "\"><div class=\"km-cal-hour-num\">";
					html += hour + ".00";
					html += "</div><div class=\"km-cal-event-content\"></div></div>";
				}
				
				// end hourse
				html += "</div>";
				
				return $(html);
			
			},
		
		};
	
	}

}
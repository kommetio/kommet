/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.tasks = {
		
	renderStage: "not-started",
	
	afterRenderCallbacks: [],
	
	options: null,
		
	show: function(settings) {
		
		this.renderStage = "started";
		this.afterRenderCallbacks = [];
		
		var defaultOptions = {
			query: "select id, title, priority, status, content, dueDate, assignedUser.id, assignedUser.userName, assignedGroup.id, assignedGroup.name from Task",
			fields: [
				{ name: "id" },
				{ name: "title" },
				{ name: "content" },
				{ name: "dueDate" },
				{ name: "priority" },
				{ name: "assignedUser" },
				{ name: "assignedGroup" }
			],
			mode: "view",
			title: km.js.config.i18n["tasks.list.title"],
			
			// either "boxes" or "objectdetails"
			display: "boxes",
		
			dalFilters: {
				orderBy: "dueDate asc",
				dateCondition: null,
				ownerCondition: null
			},
		
			btnFilters: {
				dueDate: "all",
				owner: "all",
				orderBy: "dueDate asc"
			},
			
			// if we want to display one task on top of the list, this is the place to put its ID
			// the task will be queried even if it does not meet the criteria in dalFilters
			taskId: null,
			
			recordId: null,
			
			// tells if this task form is embedded in object details
			// if yes, then some borders will not be rendered
			isEmbedded: false,
			
			contentInRow: false
		};
		
		this.options = $.extend({}, defaultOptions, settings);
		
		// build query
		var dalQuery = this.buildQuery(this.options);
		
		// the buildQuery method uses the taskId, but after the query has been built, we want to clear its value
		// so that it is not displayed when user filters the tasks
		this.options.taskId = null;
		
		km.js.db.query(dalQuery, (function(taskObj, options) {
			
			return function(tasks, recordCount, jsti) {
			
				var fields = [
					{ name: "userName" },
					{ name: "id" },
					{ name: "profile" },
					{ name: "isActive" },
					{ name: "createdDate" },
					{ name: "locale" }
				];
				
				var taskContainer = $("<div></div>").addClass("km-tasks");
				
				if (options.isEmbedded)
				{
					taskContainer.addClass("km-tasks-embedded");
				}
				
				if (!options.contentInRow)
				{
					// do not display task content in row view
					taskContainer.addClass("km-tasks-no-content");
				}
				
				var newTaskFormWrapper = $("<div></div>");
				
				taskContainer.append(taskObj.header(options, newTaskFormWrapper));
				
				tasks = km.js.utils.addPropertyNamesToJSRC(tasks, jsti);
				
				taskContainer.append(newTaskFormWrapper);
				
				var dayOnly = function(d) {
					var newDate = new Date(d.getTime());
					newDate.setHours(0, 0, 0, 0);
					return newDate;
				}
				
				var sameDay = function(d1, d2) {
					
					if (!d1 || !d2)
					{
						return false;
					}
					
					return dayOnly(d1).getTime() === dayOnly(d2).getTime();
				}
				
				var prevDate = null;
				
				if (tasks.length)
				{
					var taskList = $("<div></div>").addClass("km-task-list-items");
					
					for (var i = 0; i < tasks.length; i++)
					{
						var task = tasks[i];
						var dueDate = task.dueDate ? km.js.utils.parseDate(task.dueDate) : null;
						
						if ((i === 0 || (!sameDay(prevDate, dueDate) && prevDate)))
						{
							// start new day
							var dayHeader = $("<div></div>").addClass("km-tasks-dayheader");
							
							if (dueDate)
							{
								dayHeader.text(km.js.config.i18n["dayofweek." + dueDate.getDay()] + ", " + dueDate.getDate() + " " + km.js.config.i18n["month.decl." + (dueDate.getMonth() + 1)]);
							}
							else
							{
								dayHeader.text(km.js.config.i18n["tasks.noduedate.header"]);
							}
							
							taskList.append(dayHeader);
						}
						
						if (options.display === "objectdetails")
						{
							taskList.append(taskObj.renderTaskAsObjectDetails(task, options, jsti, taskObj));
						}
						else if (options.display === "boxes")
						{
							taskList.append(taskObj.renderTaskAsBox(task, options, jsti, taskObj));
						}
						else
						{
							throw "Unsupported display mode '" + options.display + "' for task list";
						}
						
						prevDate = dueDate;
					}
					
					taskContainer.append(taskList);
				}
				else
				{
					var noTasksMsg = $("<div></div>").text(km.js.config.i18n["tasks.notaskstodisplay"]).addClass("km-no-tasks-msg");
					taskContainer.append(noTasksMsg);
				}
				
				if (options.target instanceof jQuery)
				{
					options.target.empty().append(taskContainer);
				}
				else if (typeof(options.target) === "function")
				{
					options.target(taskContainer);
				}
				
				taskContainer.find(".km-task").hide().fadeIn(500);
				
				// mark rendering as finished
				this.renderStage = "finished";
				
				for (var i = 0; i < taskObj.afterRenderCallbacks.length; i++)
				{
					// run callbacks
					taskObj.afterRenderCallbacks[i]();
				}
			}
		
		})(this, this.options));
		
	},
	
	/*adjustInputLength: function(target)	{
		
		var minLength = 20;
		
		var val = target.val();
		var length = val ? (val.length - 15) : 0;
		length = (length && length > minLength) ? length : minLength;
		target.css("width", length + "em");
	},*/
	
	buildQuery: function(options) {
		
		var query = options.query;
		var conditions = [];
		
		if (options.recordId)
		{
			// find tasks for the specific record
			conditions.push("recordId = '" + options.recordId + "'");
		}
		
		if (options.dalFilters || options.taskId)
		{
			if (options.dalFilters.dateCondition)
			{
				conditions.push(options.dalFilters.dateCondition);
			}
			
			if (options.dalFilters.ownerCondition)
			{
				conditions.push(options.dalFilters.ownerCondition);
			}
			
			if (conditions.length || options.taskId)
			{
				var orConditions = [];
				if (conditions.length)
				{
					orConditions.push(conditions.join(" AND "));
				}
				
				if (options.taskId)
				{
					orConditions.push("id = '" + options.taskId + "'");
				}
				
				query += " WHERE " + orConditions.join(" OR ");
			}
			
			if (options.dalFilters.orderBy)
			{
				query += " ORDER BY " + options.dalFilters.orderBy; 
			}
		}
		
		return query;
		
	},
	
	getTaskForm: function() {
		
		var form = $("<div></div>").addClass("km-newtask-form");
		
		var title = $("<div></div>").addClass("km-title").text(km.js.config.i18n["tasks.newtask.title"]);
		form.append(title);
		
		form.append($("<div></div>").attr("id", "km-task-err-wrapper").hide());
		
		var taskId = $("<input></input>").attr("type", "hidden");
		form.append(taskId);
		
		var titleInput = $("<input></input>").attr("type", "text").attr("id", "new-task-title").addClass("km-input");
		titleInput.attr("placeholder", km.js.config.i18n["tasks.form.title.placeholder"]);
		form.append(titleInput);
		
		var content = $("<textarea></textarea>").attr("id", "new-task-content").addClass("km-input");
		content.attr("placeholder", km.js.config.i18n["tasks.form.content.placeholder"]);
		form.append($("<div></div>").append(content).addClass("km-task-content-wrapper"));
		
		var optionsPanel = $("<div></div>").addClass("km-newtask-options");
		
		var dueDateCell = $("<div></div>");
		var dueDateWrapper = $("<div></div>").addClass("km-task-option-wrapper");
		dueDateWrapper.append($("<div></div>").text(km.js.config.i18n["tasks.duedate"]).addClass("label"));
		var dueDatePicker = $("<input></input>").attr("id", "due-date").datetimepicker({
			dateFormat: "yy-mm-dd",
			addSliderAccess: true,
			sliderAccessArgs: { touchonly: false }
		}).addClass("km-input");
		dueDateWrapper.append($("<div></div>").append(dueDatePicker));
		optionsPanel.append(dueDateCell.append(dueDateWrapper));
		
		var priorityCell = $("<div></div>");
		var priorityWrapper = $("<div></div>").addClass("km-task-option-wrapper");
		priorityWrapper.append($("<div></div>").text(km.js.config.i18n["tasks.priority"]).addClass("label"));
		var priorityInput = $("<select></select>").attr("id", "priority").addClass("km-input");
		
		for (var i = 0; i < 5; i++)
		{
			var option = $("<option></option>").attr("value", i).text(km.js.config.i18n["tasks.priority." + i]);
			priorityInput.append(option);
		}
		
		priorityInput.val(3);
		priorityWrapper.append($("<div></div>").append(priorityInput));
		priorityCell.append(priorityWrapper);
		optionsPanel.append(priorityCell);
		
		var userCell = $("<div></div>");
		var userWrapper = $("<div></div>").addClass("km-task-option-wrapper");
		userWrapper.append($("<div></div>").text(km.js.config.i18n["tasks.assigneduser"]).addClass("label"));
		var userInput = $("<div></div>").attr("id", "assigned-user-container")
		userWrapper.append($("<div></div>").append(userInput));
		userCell.append(userWrapper);
		optionsPanel.append(userCell);
		
		form.append(optionsPanel);
		
		// add buttons
		
		var buttons = $("<div></div>").addClass("km-newtask-form-btns")
		var saveBtn = $("<a></a>").attr("href", "javascript:;").addClass("sbtn").text(km.js.config.i18n["btn.save"]);
		
		saveBtn.click((function(taskObj, form, titleInput, content, dueDatePicker, priorityInput) {
			
			return function() {
				
				var saveBtn = $(this);
				$(this).text(km.js.config.i18n["btn.saving"]);
				
				var payload = {
					title: titleInput.val(),
					content: content.val(),
					dueDate: dueDatePicker.val() ? (new Date(dueDatePicker.val())).getTime() : null,
					priority: priorityInput.val(),
					assignedUserId: $(".km-task-assigned-user").val(),
					recordId: taskObj.options.recordId
				};
				
				$.post(km.js.config.contextPath + "/km/tasklist/save", payload, (function(form) {
					
					return function(data) {
					
						$(".km-task-err-wrapper").fadeOut(400);
						
						saveBtn.text(km.js.config.i18n["btn.save"]);
						
						if (data.success)
						{
							km.js.ui.statusbar.show(km.js.config.i18n["tasks.successfully.created"], 5000);
							taskObj.show(taskObj.options);
							km.js.ui.dialog.closeAllDialogs();
						}
						else
						{
							km.js.ui.error(data.messages, $(".km-task-err-wrapper"));
							$(".km-task-err-wrapper").fadeIn(400);
						}
					}
					
				})(form), "json");
				
			}
			
		})(this, form, titleInput, content, dueDatePicker, priorityInput));
		
		buttons.append(saveBtn);
		
		var cancelBtn = $("<a></a>").attr("href", "javascript:;").addClass("sbtn").text(km.js.config.i18n["btn.cancel"]);
		cancelBtn.click((function(form) {
			return function() {
				// remove new task form
				//form.remove();
				km.js.ui.dialog.closeAllDialogs();
			}
		})(form));
		buttons.append(cancelBtn);
		
		form.append(buttons);
		
		km.js.utils.userLookup({
			target: function(lookupCode) {
				// note: use "append" on a container, not a replace function, because replacing the content will mean the next time
				// this lookup is refreshed, the element to replace will not be found and the lookup will not be rerendered
				form.find("#assigned-user-container").empty().append(lookupCode);
			},
			inputId: "km-task-assigned-user",
			inputName: "km-task-assigned-user",
			visibleInput: {
				cssClass: "km-input"
			}
		});
		
		return form;
		
	},
	
	header: function(options, newTaskFormWrapper) {
		
		var header = $("<div></div>").addClass("km-tasks-header");
		
		var titleBar = $("<div></div>").addClass("km-tasks-titlebar");
		var title = $("<div></div>").text(options.title).addClass("km-tasklist-title");
		titleBar.append(title);
		
		var icons = $("<div></div>").addClass("km-tasklist-icons");
		var newIcon = $("<img></img>").attr("src", km.js.config.imagePath + "/newicon.png");
		
		newIcon.click((function(taskObj) {
			
			return function() {
				
				km.js.ui.dialog.create({
					id: "task-dialog",
					size: {
						width: "75%",
						height: "600px"
					}
				}).show(taskObj.getTaskForm());
				
				/*newTaskFormWrapper.empty().append(taskObj.getTaskForm());
				newTaskFormWrapper.find("#new-task-title").focus();
				newTaskFormWrapper.hide().fadeIn(500);*/
			}
			
		})(this, newTaskFormWrapper));
		
		icons.append(newIcon);
		titleBar.append(icons);
		
		header.append(titleBar);
		
		var addCondition = function(taskObj, conditionName, conditionValue, btnFilterName, btnFilterValue) {
			
			return function() {
				taskObj.options.dalFilters[conditionName] = conditionValue;
				taskObj.options.btnFilters[btnFilterName] = btnFilterValue;
				taskObj.show(taskObj.options);
			}
			
		};
		
		////////// add owner buttons ////////////
		
		var ownerBtns = $("<div></div>").addClass("km-btn-group");
		
		var myTasksBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.mytasks"]);
		myTasksBtn.click(addCondition(this, "ownerCondition", "assignedUser.id = '" + km.js.config.authData.user.id + "'", "owner", "me"));
		if (options.btnFilters.owner === "me")
		{
			myTasksBtn.addClass("km-active");
		}
		ownerBtns.append(myTasksBtn);
		
		var myGroupTasksBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.mygroup"]);
		myGroupTasksBtn.click(addCondition(this, "ownerCondition", "assignedGroup.id IN (select parentGroup.id from UserGroupAssignment where childUser.id = '" + km.js.config.authData.user.id + "')", "owner", "mygroup"));
		if (options.btnFilters.owner === "mygroup")
		{
			myGroupTasksBtn.addClass("km-active");
		}
		ownerBtns.append(myGroupTasksBtn);
		
		var allTasksBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.alltasks"]);
		if (options.btnFilters.owner === "all")
		{
			allTasksBtn.addClass("km-active");
		}
		allTasksBtn.click(addCondition(this, "ownerCondition", null, "owner", "all"));

		ownerBtns.append(allTasksBtn);
		
		ownerBtns.find("a").click(function() {
			
			$(this).closest("div.km-btn-group").find("a").removeClass("km-active");
			$(this).addClass("km-active");
			
		});
		
		var ownerBtnWrapper = $("<div></div>").addClass("km-tasks-btns-wrapper");
		ownerBtnWrapper.append($("<div></div>").text(km.js.config.i18n["tasks.btn.assignedto"]).addClass("km-task-btns-title"));
		ownerBtnWrapper.append(ownerBtns);
		
		header.append(ownerBtnWrapper);
		
		////////// add due date buttons ////////////
		
		var dueBtns = $("<div></div>").addClass("km-btn-group");
		
		var todayBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.duetoday"]);
		
		var startOfDay = new Date();
		startOfDay.setHours(0, 0, 0, 0);
		var endOfDay = new Date();
		endOfDay.setHours(23, 59, 59, 999);
		
		var endOfWeek = new Date();
		endOfWeek.setDate(endOfWeek.getDate() + 7);
		endOfWeek.setHours(23, 59, 59, 999);
		
		var threeDaysDate = new Date();
		threeDaysDate.setDate(threeDaysDate.getDate() + 3);
		threeDaysDate.setHours(23, 59, 59, 999);
		
		todayBtn.click(addCondition(this, "dateCondition", "dueDate >= " + startOfDay.getTime() + " and dueDate <= " + endOfDay.getTime(), "dueDate", "today"));
		if (options.btnFilters.dueDate === "today")
		{
			todayBtn.addClass("km-active");
		}
		dueBtns.append(todayBtn);
		
		var threeDaysBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.threedays"]);
		threeDaysBtn.click(addCondition(this, "dateCondition", "dueDate >= " + startOfDay.getTime() + " and dueDate <= " + threeDaysDate.getTime(), "dueDate", "3days"));
		if (options.btnFilters.dueDate === "3days")
		{
			threeDaysBtn.addClass("km-active");
		}
		dueBtns.append(threeDaysBtn);
		
		var weekDueBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.duethisweek"]);
		weekDueBtn.click(addCondition(this, "dateCondition", "dueDate >= " + startOfDay.getTime() + " and dueDate <= " + endOfWeek.getTime(), "dueDate", "week"));
		if (options.btnFilters.dueDate === "week")
		{
			weekDueBtn.addClass("km-active");
		}
		dueBtns.append(weekDueBtn);
		
		var pastDueBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.duepast"]);
		pastDueBtn.click(addCondition(this, "dateCondition", "dueDate < " + (new Date()).getTime(), "dueDate", "past"));
		if (options.btnFilters.dueDate === "past")
		{
			pastDueBtn.addClass("km-active");
		}
		dueBtns.append(pastDueBtn);
		
		var allDueBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.dueall"]);
		allDueBtn.click(addCondition(this, "dateCondition", null, "dueDate", "all"));
		if (options.btnFilters.dueDate === "all")
		{
			allDueBtn.addClass("km-active");
		}
		dueBtns.append(allDueBtn);
		
		var calendarIcon = $("<img></img>").attr("src", km.js.config.imagePath + "/calendar-white.png");
		var dueDateCalendarPicker = $("<div></div>").addClass("cal-wrapper");
		
		var picker = km.js.utils.dateTimePicker({
			target: dueDateCalendarPicker,
			value: this.options.btnFilters["dueDate"] instanceof Date ? this.options.btnFilters["dueDate"] : null,
			dateOnly: true,
			mode: "edit",
			onSelect: (function(taskObj) {
				
				return function(newDate) {
					
					// newDate is a string value, so we parse to date
					var date = new Date(newDate);
					
					var startOfDay = new Date(date);
					startOfDay.setHours(0, 0, 0, 0);
					
					var endOfDay = new Date(date);
					endOfDay.setHours(23, 59, 59, 0);
					
					addCondition(taskObj, "dateCondition", "dueDate <= " + endOfDay.getTime() + " and dueDate >= " + startOfDay.getTime(), "dueDate", date)();
					
				}
				
			})(this)
		});
		
		var calendarBtn = $("<div></div>").append(dueDateCalendarPicker).append($("<span></span>").append(calendarIcon)).addClass("btn cal-btn");
		
		if (options.btnFilters.dueDate instanceof Date)
		{
			calendarBtn.addClass("km-active");
		}
		
		dueBtns.append(calendarBtn);
		
		dueBtns.find("a").click(function() {
			
			$(this).closest("div.km-btn-group").find("a").removeClass("km-active");
			$(this).addClass("km-active");
			
		});
		
		var dueBtnWrapper = $("<div></div>").addClass("km-tasks-btns-wrapper");
		dueBtnWrapper.append($("<div></div>").text(km.js.config.i18n["tasks.btn.duetitle"]).addClass("km-task-btns-title"));
		dueBtnWrapper.append(dueBtns);
		
		header.append(dueBtnWrapper);
		
		// sort buttons
		var sortBtns = $("<div></div>").addClass("km-btn-group");
		
		var dueDateAscBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.sortby.duedate.asc"]);
		dueDateAscBtn.click(addCondition(this, "orderBy", "dueDate asc", "orderBy", "dueDate asc"));
		if (options.btnFilters.orderBy === "dueDate asc")
		{
			dueDateAscBtn.addClass("km-active");
		}
		sortBtns.append(dueDateAscBtn);
		
		var dueDateDescBtn = $("<a></a>").text(km.js.config.i18n["tasks.btn.sortby.duedate.desc"]);
		dueDateDescBtn.click(addCondition(this, "orderBy", "dueDate desc", "orderBy", "dueDate desc"));
		if (options.btnFilters.orderBy === "dueDate desc")
		{
			dueDateDescBtn.addClass("km-active");
		}
		sortBtns.append(dueDateDescBtn);
		
		var sortBtnWrapper = $("<div></div>").addClass("km-tasks-btns-wrapper");
		sortBtnWrapper.append($("<div></div>").text(km.js.config.i18n["tasks.btn.sortby.title"]).addClass("km-task-btns-title"));
		sortBtnWrapper.append(sortBtns);
		
		header.append(sortBtnWrapper);
		
		return header;
	},
	
	/**
	 * Adds a callback to be executed after rendering is finished
	 */
	addAfterRenderCallback: function(callback) {
		
		if (this.renderStage === "finished")
		{
			// run callback immediately
			callback();
		}
		else
		{
			// register callback for later
			this.afterRenderCallbacks.push(callback);
		}
		
	},
	
	renderTaskAsBox: function(task, options, jsti, taskList) {
		
		var box = $("<div></div>").addClass("km-task").addClass("km-task-display-row");
		box.attr("id", "km-task-" + task.id);
		
		var topContainer = $("<div></div>").addClass("km-task-tc");
		
		var onFail = function(data) {
		
			km.js.ui.statusbar.err(data.message ? data.message : data.messages);
			
		};
		
		var showCheck = function(taskBox) {
			
			var check = $("<img></img>").attr("src", km.js.config.imagePath + "/check.gif").css("position", "absolute").css("right", "5em").css("top", "2em");
			taskBox.css("position", "relative");
			taskBox.append(check);
			
			check.hide().fadeIn(700);
			
			setTimeout((function() {
				return function() {
					check.fadeOut(700);
				}
			})(check), 3000);
			
		};
		
		var showDeleteBtn = function(taskBox, topContainer, taskId) {
			
			var check = $("<img></img>").attr("src", km.js.config.imagePath + "/trashicon.png").addClass("task-icon");
			
			var btnWrapper = $("<div></div>").addClass("km-task-delete-wrapper");
			btnWrapper.append(check);
			topContainer.append(btnWrapper);
			
			// ask user to confirm before removing
			km.js.ui.confirm({
				target: check,
				callback: (function(taskId, taskBox) {
					
					return function() {
						km.js.db.deleteRecord(taskId, (function(taskBox) {
							
							return function() {
								taskBox.fadeOut(400);
							}
							
						})(taskBox));
					}
					
				})(taskId, taskBox) 
			})
			
		}
		
		var onSuccess = function(isSuccess, data) {
			
			if (!isSuccess)
			{
				km.js.ui.statusbar.err(data.message ? data.message : data.messages);
			}
			else
			{
				// show green check icon
				//showCheck($(".km-task-" + data.id));
			}
			
		};
		
		var addEditEvents = function(input, field, taskId) {
			
			/*input.click(function(e) {
				
				$(this).addClass("km-editable");
				$(this).attr("readonly", false);
				
			});*/
			
			input.blur((function(taskId, field) {
				
				return function() {
					//$(this).removeClass("km-editable");
					//$(this).attr("readonly", true);
					
					// save task title
					var record = {
						id: taskId
					};
					
					record[field] = $(this).val() ? $(this).val() : null;
					
					km.js.db.update(record, onSuccess, onFail);
				}
				
			})(task.id, field));
			
		}
		
		var openTaskForm = function(box, contentTextarea, taskList) {
			
			// open current task as form
			box.addClass("km-task-display-form");
			box.removeClass("km-status-resolved-row");
			box.removeClass("km-task-display-row");
			
			km.js.ui.dialog.create({
				id: "existing-task-dialog",
				size: {
					width: "75%",
					height: "600px"
				},
				afterClose: (function(taskList) {
					
					return function() {
						taskList.show(taskList.options);
					}
					
				})(taskList)
			}).show(box);
		}
		
		var statusWrapper = $("<div></div>").addClass("km-task-status-wrapper");
		var statusIcon = $("<img></img>").attr("src", km.js.config.imagePath + "/" + (task.status === "Resolved" ? "check.gif" : "greycheck.png"));
		statusIcon.addClass("task-icon");
		
		if (task.status === "Resolved")
		{
			statusIcon.addClass("km-status-resolved");
			box.addClass("km-status-resolved-row");
		}
		
		statusWrapper.append(statusIcon);
		
		statusIcon.click((function(taskId, row) {
			
			return function() {
				
				var status = null;
				
				if ($(this).hasClass("km-status-resolved"))
				{
					$(this).removeClass("km-status-resolved");
					row.removeClass("km-status-resolved-row");
					$(this).attr("src", km.js.config.imagePath + "/greycheck.png");
					status = "Open";
				}
				else
				{
					$(this).addClass("km-status-resolved");
					row.addClass("km-status-resolved-row");
					$(this).attr("src", km.js.config.imagePath + "/check.gif");
					status = "Resolved";
				}
				
				var record = {
					id: taskId
				};
				
				record["status"] = status;
				km.js.db.update(record, onSuccess, onFail);
			}
			
		})(task.id, box));
		
		topContainer.append(statusWrapper);
		
		var title = $("<div></div>").addClass("km-task-title");
		var titleEdit = $("<input></input>").attr("type", "text").val(task.title).addClass("km-task-edit");
		addEditEvents(titleEdit, "title", task.id);
		
		var actualContent = task.content ? task.content : "";
		//var contentEdit = $("<input></input>").attr("type", "text").val(actualContent).addClass("km-task-edit");
		var contentTextarea = $("<textarea></textarea>").val(actualContent).addClass("km-task-edit");
		
		if (km.js.utils.isMobile())
		{
			titleEdit.focus((function(box, contentTextarea, taskList) {
			
				return function() {
					openTaskForm(box, contentTextarea, taskList);
				}
				
			})(box, contentTextarea, this));
			
		}
		
		title.append(titleEdit);
		
		
		var contentWrapper = $("<div></div>").addClass("km-task-content-wrapper").append(contentTextarea);
		
		//addEditEvents(contentEdit, "content", task.id);
		addEditEvents(contentTextarea, "content", task.id);
		
		topContainer.append(title).append(contentWrapper);
		
		var assigneeCell = $("<div></div>").addClass("km-task-assignee-wrapper");
		
		var assigneeWrapper = $("<div></div>").addClass("km-task-option-wrapper");
		var taskIcon = $("<img></img>").attr("src", km.js.config.imagePath + "/personicon.png").addClass("task-icon");
		var assigneeLabel = $("<span></span>").append(taskIcon).addClass("task-icon-wrapper");
		assigneeWrapper.append(assigneeLabel);
		
		var userInputHash = "ua-" + km.js.utils.random(1000000);
		var userInput = $("<div></div>").attr("id", userInputHash);
		var userOutput = $("<div></div>").text(task.assignedUser ? task.assignedUser.userName : "");
		assigneeWrapper.append($("<div></div>").append(userInput).append(userOutput));
		assigneeCell.append(assigneeWrapper);
		
		var showUserLookup = function(taskId, userId, target, userInputHash) {
			
			// hide user output
			target.hide();
			
			km.js.utils.userLookup({
				target: ((function(hash) {
					return function(code) {
						$("#" + hash).empty().append(code);
					}
				})(userInputHash)),
				inputName: "km-task-assignee",
				visibleInput: {
					cssClass: "km-input"
				},
				mode: "view",
				editable: true,
				recordId: userId,
				afterSelect: function(userId) {
					
					// save task title
					var record = {
						id: taskId
					};
					
					record["assignedUser"] = userId ? { id: userId } : null;
					
					km.js.db.update(record, null, onFail);
					
				}
			});
			
		};
		
		this.addAfterRenderCallback(function() {
			
			var showLookupFunction = showUserLookup(task.id, task.assignedUser ? task.assignedUser.id : null, userOutput, userInputHash); 
			
			// show user lookup when either inactive lookup or label are clicked
			userOutput.click(showLookupFunction);
			assigneeLabel.click(showLookupFunction);
		});
		
		topContainer.append(assigneeCell);
		
		var priorityCell = $("<div></div>").addClass("km-tasks-priority-cell");
		var priorityWrapper = $("<div></div>").addClass("km-task-option-wrapper");
		var priorityIcon = $("<img></img>").attr("src", km.js.config.imagePath + "/impicon.png").addClass("task-icon");
		var priorityLabel = $("<span></span>").append(priorityIcon).addClass("task-icon-wrapper");
		priorityWrapper.append(priorityLabel);
		var priorityInput = $("<select></select>").attr("id", "priority").addClass("km-task-edit");
		
		for (var i = 0; i < 5; i++)
		{
			var option = $("<option></option>").attr("value", i).text(km.js.config.i18n["tasks.priority." + i]);
			priorityInput.append(option);
		}
		
		priorityInput.val(task.priority);
		
		addEditEvents(priorityInput, "priority", task.id);
		
		priorityWrapper.append($("<div></div>").append(priorityInput));
		priorityCell.append(priorityWrapper);
		topContainer.append(priorityCell);
		
		var dueDateCell = $("<div></div>").addClass("km-task-duedate-wrapper");
		var dueDateWrapper = $("<div></div>").addClass("km-task-option-wrapper");
		var dateIcon = $("<img></img>").attr("src", km.js.config.imagePath + "/alarmclock.png").addClass("task-icon");
		var dueDateLabel = $("<span></span>").append(dateIcon).addClass("task-icon-wrapper");
		dueDateWrapper.append(dueDateLabel);
		var dueDatePicker = $("<span></span>").text(task.dueDate ? task.dueDate : "");
		addEditEvents(dueDatePicker, "dueDate", task.id);
		
		var picker = km.js.utils.dateTimePicker({
			target: dueDatePicker,
			value: task.dueDate,
			mode: "view",
			onSelect: (function(taskId) {
				
				return function(newDate) {
					
					var record = {
						id: taskId,
						dueDate: newDate ? (new Date(newDate)).getTime() : null
					};
					
					km.js.db.update(record, onSuccess, onFail);
				}
				
			})(task.id)
		});
		
		dueDateLabel.click((function(picker) {
			
			return function() {
				// activate date picker when label is clicked
				picker.enable();
			}
			
		})(picker));
		
		dueDateWrapper.append(dueDatePicker);
		dueDateCell.append(dueDateWrapper);
		topContainer.append(dueDateCell);
		
		var openIcon = $("<img></img>").attr("src", km.js.config.imagePath + "/uncollapse.png").addClass("km-task-open-icon task-icon");
		
		openIcon.click((function(box, contentTextarea, taskList, openTaskForm) {
			
			return function() {
				
				if (box.hasClass("km-task-display-form"))
				{
					// rewrite content
					//contentEdit.val(contentTextarea.val());
					
					// close all other task forms
					box.removeClass("km-task-display-form");
					box.addClass("km-status-resolved-row");
					box.addClass("km-task-display-row");
					
					km.js.ui.dialog.closeAllDialogs();
				}
				else
				{
					// rewrite content
					//contentTextarea.val(contentEdit.val());
					
					// close all other task forms
					//$("div.km-task").removeClass("km-task-display-form");
					
					openTaskForm(box, contentTextarea, taskList);
				}
			}
			
		})(box, contentTextarea, this, openTaskForm));
		
		topContainer.append($("<div></div>").append(openIcon).addClass("km-task-open-wrapper"));
		
		box.append(topContainer);
		
		showDeleteBtn(box, topContainer, task.id);
		
		return box;
	},
	
	renderTaskAsObjectDetails: function(task, options, jsti, taskList) {
		
		var hash = "hash-" + km.js.utils.random(10);
		var box = $("<div></div>").addClass("ibox").attr("km-hash", hash);
		
		var form = km.js.objectdetails.create({
			mode: options.mode,
			jsti: jsti,
			delayCallbacks: true,
			
			// this is complicated, so pay attention
			// as soon as the form finishes rendering, the target callback is called ("formRenderCallback")
			// but it does not render the form, instead it schedules it to be render later, when the whole task list is finished
			// and when it does, the objectDetailsForm.callAfterRenderCallbacks() method is called, which in turn calls the cached callbacks
			// for the object details form
			target: (function(hash, list) {
				
				var formRenderCallback = function(code, objectDetailsForm) {
					
					taskList.addAfterRenderCallback(function() {
						$("div[km-hash='" + hash + "']").empty().append(code);
						
						objectDetailsForm.callAfterRenderCallbacks();
					});
				}
				
				return formRenderCallback;
				
			})(hash, taskList),
			
			
			fields: options.fields,
			fieldsPerRow: 2,
			type: {
				name: "kommet.basic.Task"
			}
		});
		
		form.render({
			record: task
		});
		
		return box;
		
	}
		
};
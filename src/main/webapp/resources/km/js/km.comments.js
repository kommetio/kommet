/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.comments = {
		
	create: function(options) {
		
		var defaultOptions = {
			cssClass: "km-cmt-std",
			addLabel: km.js.config.i18n["comments.save"],
			deleteLabel: km.js.config.i18n["comments.delete"],
			respondLabel: km.js.config.i18n["comments.reply"],
			responsesLabel: km.js.config.i18n["comments.replies"],
			responseTitleLabel: km.js.config.i18n["comments.response.title"],
			noAnswersLabel: km.js.config.i18n["comments.no.replies"],
			title: km.js.config.i18n["comments.list.title"],
			commentTypePrefix: "00l",
			sortMode: "desc",
			
			// maximum level of subcomments at which subcomments will be indented
			// comments below this level will not be intented any further to avoid making them too narrow
			maxResponseIndentLevel: 4,
			
			// maximum level at which subcomments can be added to comments
			maxResponseAddLevel: null,
			
			addComments: false
		}
		
		options = $.extend({}, defaultOptions, options);
		
		if (!options.id)
		{
			throw "No ID passed in configuration options to the comments object";
		}
		
		return {
			
			id: options.id,
			comments: options.comments,
			recordId: options.recordId,
			title: options.title,
			cssClass: options.cssClass,
			addLabel: options.addLabel,
			deleteLabel: options.deleteLabel,
			respondLabel: options.respondLabel,
			responsesLabel: options.responsesLabel,
			responseTitleLabel: options.responseTitleLabel,
			noAnswersLabel: options.noAnswersLabel,
			recentInvocationParams: {},
			commentTypePrefix: options.commentTypePrefix,
			maxResponseIndentLevel: options.maxResponseIndentLevel,
			maxResponseAddLevel: options.maxResponseAddLevel,
			addComments: options.addComments,
			options: options,
			
			// one of "asc|desc|rating" (rating to be implemented)
			sortMode: options.sortMode,
			
			render: function(arg) {
				
				// remember this parameter
				this.recentInvocationParams.renderArg = arg;
				
				var onFetchData = function(commentObj, target) {
					
					return function(data) {
						
						var comments = data.data;
						
						var commentContainer = $("<div id=\"" + commentObj.id + "\" class=\"km-cmt " + commentObj.cssClass + "\"></div>");
						
						// append title
						commentContainer.append("<div class=\"km-cmt-title km-title\">" + commentObj.title + "</div>");
						
						// add new comment form
						if (commentObj.recordId && commentObj.addComments === true)
						{
							var newComment = $("<div class=\"km-cmt-new\"></div>");
							newComment.append("<div class=\"km-cmt-add-error\"></div>")
							newComment.append("<textarea id=\"newcomment\"></textarea>");
							var addBtn = $("<a href=\"javascript:;\" class=\"sbtn\">" + commentObj.addLabel + "</a>");
							
							addBtn.click((function(comments) {
								
								return function() {
									// get comment text from textarea
									var text = $(this).closest(".km-cmt-new").find("textarea#newcomment").first().val();
									
									var textarea = comments.container().find("textarea#newcomment");
									
									// disable comment input field
									textarea.prop("disabled", true);
									
									var newRecord = {
											content: text,
											recordId: comments.recordId
										};
										
									// add comment via REST API
									$.post(km.js.config.sysContextPath + "/comments/save", newRecord, (function(comments, textarea) {
										
										return function(data) { 
											
											if (data.success === true)
											{
												comments.refresh();
											}
											else
											{	
												// unlock textarea field
												textarea.prop("disabled", false);
												
												// show error 
												km.js.ui.error(data.message, comments.container().find(".km-cmt-new .km-cmt-add-error"), null);
											}
										}
									})(commentObj, textarea), "json");
								}
								
							})(commentObj));
							
							newComment.append(addBtn);
							commentContainer.append(newComment);
						}
						
						var getCommentListBtns = function() {
							
							var code = $("<ul class=\"km-cmt-list-btns\"></ul>");
							
							var sortBtn = $("<li><img class=\"km-cmt-sort-" + commentObj.sortMode + "\" src=\"" + km.js.config.imagePath + "/sort" + commentObj.sortMode + ".png\" /></li>");
							sortBtn.find("img").click((function(comments) {
								return function() {
									if ($(this).hasClass("km-cmt-sort-desc"))
									{
										comments.sort("asc");
									}
									else
									{
										comments.sort("desc");
									}
								}
							})(commentObj));
							code.append(sortBtn);
							
							return code;
						}
						
						// add comment sort buttons
						commentContainer.append(getCommentListBtns());
						
						var commentList = $("<div class=\"km-cmt-list\"></div>");
						
						for (var i = 0; i < comments.length; i++)
						{
							commentList.append(commentObj.commentCode(comments[i], 0, commentObj.options));
						}
						
						commentContainer.append(commentList);
						
						if (typeof(target) === "function")
						{
							target(commentContainer);
						}
						else if (target instanceof jQuery)
						{
							target.empty().append(commentContainer);
						}
						else
						{
							throw "Unsupported type " + typeof(target) + " of parameter target in call to km.js.comments.render()";
						}
					}
				}
				
				if (this.comments && this.recordId)
				{
					throw "Both comments list and record ID cannot be set";
				}
				
				if (this.recordId)
				{
					var payload = {
						sort: this.sortMode,
						subcommentLevels: -1,
						// any additional user-defined fields to query - a comma-separated list
						additionalFields: this.options.queriedFields
					}
					
					$.get(km.js.config.sysContextPath + "/rest/recordcomments/" + this.recordId, payload, onFetchData(this, arg), "json");
				}
				else
				{
					(onFetchData(this, arg))({ comments: this.comments });
				}
				
				// return self for chaining
				return this;
			},
			
			/**
			 * Method whose calling is equivalent to the last call of render().
			 * @public
			 */
			refresh: function() {
				// call render with remembered param
				this.render(this.recentInvocationParams.renderArg);
			},
			
			container: function() {
				return $("#" + this.id);
			},
			
			commentCode: function(comment, level, options) {
				
				if (!comment.id)
				{
					throw "Comment has no ID";
				}
				
				if (!comment.content)
				{
					throw "Comment has no text";
				}
				
				// start comment container
				var code = $("<div class=\"km-cmt-item km-cmt-item-" + comment.id + "\"></div>");
				
				var commentBox = $("<div class=\"km-cmt-box\"></div>");
				
				var header = $("<div class=\"km-cmt-head\"></div>");
				
				var author = comment.createdBy ? comment.createdBy.userName : "";
				header.append($("<span class=\"km-cmt-author\">" + author + "</span>"));
				header.append($("<span class=\"km-cmt-date\">" + comment.createdDate + "</span>"));
				commentBox.append(header);
			
				// append content
				var content = $("<div class=\"km-cmt-content\"></div>").text(km.js.utils.nl2br(comment.content));
				commentBox.append(content);
				
				var buttons = $("<div class=\"km-cmt-btns\"></div>");
				
				if (comment.canDelete === true)
				{
					// show delete button if the current user has permissions to delete this comment
					buttons.append($("<a class=\"km-cmt-del btn\" href=\"javascript:;\">" + this.deleteLabel + "</a>"));
				}
				
				if (this.addComments === true)
				{
					buttons.append($("<a class=\"km-cmt-resp btn\" href=\"javascript:;\">" + this.respondLabel + "</a>"));
				}
				
				var answerCount = comment.comments ? comment.comments.length : 0;
				buttons.append($("<a class=\"km-cmt-show-replies\" href=\"javascript:;\">" + this.responsesLabel + " (" + answerCount + ")</a>"));
				
				if (typeof(options.buttonsPostprocessor) === "function")
				{
					// call a callback on the button list
					options.buttonsPostprocessor(comment, buttons, options);
				}
				
				commentBox.append(buttons);
				code.append(commentBox);
				
				
				code.append(this.answerList(comment, level, options));
				
				// append show replies event
				code.find("a.km-cmt-show-replies").click((function(commentId) {
					return function() {
						$(this).closest(".km-cmt-item").find(".km-cmt-answer-list-" + commentId).toggle();
					}
				})(comment.id));
				
				if (comment.canDelete === true)
				{
					// append delete event
					code.find("a.km-cmt-del").each((function(comments, commentId) {
						
						return function() {
							var deleteComment = function() {
								// disable comment input field
								comments.container().find("textarea#newcomment").attr("disabled", "disabled");
								
								// add comment via REST API
								$.post(km.js.config.sysContextPath + "/comments/delete", { commentId: commentId }, function(data) {
									comments.refresh();
								}, "json");
							}
							
							km.js.ui.confirm({
								callback: deleteComment,
								question: km.js.config.i18n["comments.delete.warning"],
								target: $(this)
							});
						}
						
					})(this, comment.id));
				}
				
				if (this.addComments === true)
				{
					// append respond event
					code.find("a.km-cmt-resp").click((function(comments, commentId) {
						
						return function() {
							$(this).closest(".km-cmt-item").find(".km-cmt-resp-form-" + commentId).toggle();
							$(this).closest(".km-cmt-item").find(".km-cmt-resp-form-" + commentId + " > textarea").attr("id", "km-cmt-resp-text-" + commentId);
						}
						
					})(this, comment.id));
				
					if (!this.maxResponseAddLevel || level <= this.maxResponseAddLevel)
					{
						var respForm = $("<div class=\"km-cmt-resp-form km-cmt-resp-form-" + comment.id + "\"><div class=\"km-cmt-resp-title\">" + this.responseTitleLabel + "</div><textarea></textarea></div>");
						
						// indent answers, but not below certain level because then comment boxes would be too narrow
						if (level <= this.maxResponseIndentLevel)
						{
							respForm.css("padding-left", "5%");
						}
						
						var respSaveBtn = $("<a href=\"javascript:;\" class=\"sbtn\">" + this.addLabel + "</a>");
						
						respSaveBtn.click((function(comments) {
							
							return function() {
								
								var textarea = $(this).closest(".km-cmt-resp-form").find("textarea");
								// get response text from textarea
								var text = textarea.first().val();
								
								// disable response input field and button
								textarea.attr("disabled", "disabled");
								$(this).attr("disabled", "disabled");
								
								var parentCommentId = textarea.attr("id").substring("km-cmt-resp-text-".length);
								
								var newRecord = {
									content: text,
									parentId: parentCommentId,
									recordId: comments.recordId
								};
								
								// add comment via REST API
								$.post(km.js.config.sysContextPath + "/comments/save", newRecord, "json")
									.done(function(data) { console.log("S: " + JSON.stringify(data)); comments.refresh(); })
									.fail(function(data) { console.log("E: " + JSON.stringify(data)); comments.refresh(); });
							}
							
						})(this));
						
						respForm.append(respSaveBtn);
						code.append(respForm);
					}
				}
				
				return code;
			},
			
			/**
			 * Sorts comments in given order
			 * @param [string] "asc"|"desc"
			 */
			sort: function(sortMode) {
				
				if (sortMode !== "asc" && sortMode !== "desc")
				{
					throw "Unsupported sort direction \"" + direction + "\"";
				}
				
				this.sortMode = sortMode;
				this.refresh();
			},
			
			/**
			 * Note: level param is not used, but we keep it because it's likely to come in handy at some point.
			 */
			answerList: function(comment, level, options) {
				var answerList = $("<div class=\"km-cmt-answer-list km-cmt-answer-list-" + comment.id + "\"></div>");
				
				// indent answers, but not below certain level because then comment boxes would be too narrow
				if (level <= this.maxResponseIndentLevel)
				{
					answerList.css("padding-left", "5%");
				}
				
				if (comment.comments && comment.comments.length > 0)
				{
					for (var i = 0; i < comment.comments.length; i++)
					{
						answerList.append(this.commentCode(comment.comments[i], level + 1, options));
					}
				}
				else
				{
					answerList.append("<div class=\"km-cmt-no-replies\">" + this.noAnswersLabel + "</div>");
				}
				
				return answerList;
			}
			
		}
		
	}
		
};
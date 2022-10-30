/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.filelookup = {
	
	show: function(settings) {
		
		var defaultSettings = {
			chooseSystemFiles: true,
			uploadedFileName: "uploadedFile",
			
			// tells whether files should be uploaded immediately after being selected
			immediate: false
		}
		
		var options = $.extend({}, defaultSettings, settings);
		
		// query the file
		km.js.db.query("select id, name from File where id = '" + (options.fileId ? options.fileId : "00h0000000000") + "'", (function(options) {
			
			return function(records, recordCount, jsti) {
				
				records = km.js.utils.addPropertyNamesToJSRC(records, jsti);
				
				var icon = $("<img></img>").attr("src", km.js.config.imagePath + "/attachicon.png");
				
				var iconWrapper = $("<div></div>").addClass("km-file-icon").append(icon);
				
				// render file
				var fileLookup = $("<div></div>").append(iconWrapper).addClass("km-file-lookup");
				
				var fileInputName = "km-file-" + km.js.utils.random(1000000);
				
				if (options.inputName)
				{
					var hiddenValue = $("<input></input>").attr("type", "hidden").val(options.fileId).attr("name", options.inputName);
					fileLookup.append(hiddenValue);
				}
				
				var fileInput = $("<input></input>").attr("type", "file").attr("name", options.uploadedFileName).hide();
				
				var fileForm = km.js.filelookup.getFileForm(fileInput, fileInputName);
				
				fileLookup.append(fileForm);
				
				if (records.length)
				{
					var record = records[0];
					var fileName = $("<div></div>").text(record.name).addClass("km-filename");
					fileLookup.append(fileName);
					
					var removeBtn = $("<img></img>").attr("src", km.js.config.imagePath + "/ex.png");
					removeBtn.click((function(hiddenValue, fileName) {
						
						return function() {
							
							// clear file id
							hiddenValue.val(null);
							
							// clear file name
							fileName.empty();
							
							// remove the button
							$(this).remove();
						}
						
					})(hiddenValue, fileName))
					
					fileLookup.append($("<div></div>").append(removeBtn).addClass("km-remove"));
				}
				
				icon.click((function(options, fileInput, fileForm, options) {
					
					return function() {
						
						var dialog = km.js.ui.dialog.create({
							id: "km-fileupload-" + km.js.utils.random(1000000),
							size: {
								width: "800px",
								height: "600px"
							},
							afterClose: null
						});
						
						dialog.show(km.js.filelookup.getFileDialog(fileInput, fileForm, fileLookup, options, dialog));
					}
					
				})(options, fileInput, fileForm, options));
				
				options.target.empty().append(fileLookup);
				
			}
			
		})(options));
		
	},
	
	getFileForm: function(fileInput, fileInputName) {
		
		var form = $("<form></form>").attr("enctype", "multipart/form-data").attr("method", "post");
		form.append($("<input></input>").attr("type", "hidden").attr("name", "fileName"));
		form.append($("<input></input>").attr("type", "hidden").attr("name", "revisionId"));
		form.append($("<input></input>").attr("type", "hidden").attr("name", "nativeUpload").val("true"));
		form.append($("<input></input>").attr("type", "hidden").attr("name", "fileParam").val(fileInputName));
		form.append(fileInput);
		form.css("display", "none");
		
		return form;
		
	},
	
	/**
	 * private
	 */
	getFileDialog: function(fileInput, fileForm, fileLookup, options, dialog) {
		
		var code = $("<div></div>").addClass("km-file-upload-wrapper");
		
		var drag = $("<div></div>").addClass("km-file-drag");
		drag.append($("<div></div>").text(km.js.config.i18n["files.drag.file"]));
		
		var browseBtn = $("<a></a>").addClass("sbtn").attr("href", "javascript:;").text(km.js.config.i18n["files.browse"])
		browseBtn.click((function(fileInput) {
			
			return function() {
				fileInput.click();
			}
			
		})(fileInput));
		
		drag.append($("<div></div>").append(browseBtn).addClass("km-file-browse"));
		
		if (options.chooseSystemFiles)
		{
			// allow user to choose from existing files in the system
			
			var chooseBtn = $("<a></a>").addClass("sbtn").attr("href", "javascript:;").text(km.js.config.i18n["files.choose.existing"])
			chooseBtn.click((function(target, dialog) {
				
				return function() {
	
					km.js.filelookup.fileList(target, dialog, options);
					
				}
				
			})(code, dialog, options));
			
			drag.append($("<div></div>").append(chooseBtn).addClass("km-file-choose"));
		}
		
		var panel = $("<div></div>").addClass("km-file-panel").hide();
		var fileName = $("<input></input>").attr("type", "text").attr("id", "filename").addClass("km-input");
		
		// function called when nativeupload request returns
		var onUploadCallback = function(data, drag, panel, dialog, options) {
			
			if (data.success)
			{	
				drag.hide();
				
				var msg = $("<div></div>").text(km.js.config.i18n["files.upload.complete"]).addClass("km-upload-success");
				panel.empty().append(msg);
				
				dialog.close();
				
				if (typeof(options.afterSave) === "function")
				{
					if (!data.fileId || !data.fileRevisionId)
					{
						throw "File ID or file revision ID not returned after file upload";
					}
					
					options.afterSave(data.fileId, data.fileRevisionId, data.fileName, options);
				}
			}
			else
			{
				var text = km.js.config.i18n["files.upload.failed"];
				
				if (data.messages && data.messages.length)
				{
					text += ": " + data.messages[0];
				}
				
				var msg = $("<div></div>").text(text).addClass("km-upload-failed");
				panel.empty().append(msg);
			}
			
		};
		
		var doUpload = (function(fileInput, fileName, fileForm, options, dialog) {
			
			return function() {
				
				// upload dropped files
				if (options.isDropUpload)
				{
					if (options.dropCallbacks)
					{
						for (var i = 0; i < options.dropCallbacks.length; i++)
						{
							console.log("Processing upload");
							// call dropzone's "done" callback for each uploaded file
							options.dropCallbacks[i]();
						}
					}
				}
				// upload files added using the browse button
				else
				{
					var iframeId = "km-iframe-" + km.js.utils.random(1000000);
					
					var iframe = $('<iframe style="display: none" />').attr("id", iframeId).attr("name", iframeId);
					$("body").append(iframe);
					
					// rewrite file name filled by user to the submitted form
					fileForm.find("input[name=fileName]").val(fileName.val());
					
					// submit file form to iframe
					fileForm.attr("target", iframeId);
					fileForm.attr("action", km.js.config.contextPath + "/km/files/nativeupload");
					fileForm.submit();
					
					var waitIcon = km.js.ui.buttonWait({
						button: $(this),
						text: "Saving"
					});
					
					iframe.load((function (waitIcon, panel, options) {
						
						return function() {
							km.js.ui.buttonWaitStop(waitIcon);
							
							iframeContents = $(this)[0].contentWindow.document.body.innerHTML;
		
			                var data = JSON.parse(iframeContents);
							onUploadCallback(data, drag, panel, dialog, options);
						}
						
					})(waitIcon, panel, options));
				}
			}
			
		})(fileInput, fileName, fileForm, options, dialog);
		
		if (typeof(drag.dropzone) === "function")
		{
			// add dropzone component to the drag div
			drag.dropzone({
				url: km.js.config.contextPath + "/km/files/nativeupload",
				createImageThumbnails: false,
				paramName: options.uploadedFileName,
				success: (function(dialog, panel, options) {
					
					return function(file, response) {
						var respObj = JSON.parse(response);
						onUploadCallback(respObj, drag, panel, dialog, options);
					}
					
				})(dialog, panel, options),
				previewTemplate: "<div class=\"dz-details\">\n<div class=\"dz-size\"><span data-dz-size></span></div>\n    <div class=\"dz-filename\"><span data-dz-name></span></div>",
				accept: (function(panel, options, doUpload) {
					
					return function(file, doneCallback) {
						
						var fn = file.name.split('\\').pop().split('/').pop();
						
						panel.find("#filename").val(fn);
						panel.show();
						
						options.isDropUpload = true;
						
						// add the doneCallback function to be called later (when we want to process the upload)
						if (!options.dropCallbacks)
						{
							options.dropCallbacks = [];
						}
						options.dropCallbacks.push(doneCallback);
						
						if (options.immediate)
						{
							doUpload();
						}
					}
					
				})(panel, options, doUpload)
			});
		}
		
		code.append(drag);
		panel.append(fileName);
		
		var saveBtn = $("<a></a>").attr("href", "javascript:;").text(km.js.config.i18n["btn.save"]).addClass("sbtn");
		
		saveBtn.click(doUpload);
		
		panel.append(saveBtn);
		
		code.append(panel);
		
		fileInput.on("change", (function(panel, doUpload, options) {
			
			return function() {
				console.log("File: " + $(this).val());
				
				var fn = $(this).val().split('\\').pop().split('/').pop();
				
				panel.find("#filename").val(fn);
				panel.show();
				
				if (options.immediate)
				{
					doUpload();
				}
			}
			
		})(panel, doUpload, options));
		
		return code;
		
	},
	
	/**
	 * Renders a data table with all files to choose from
	 */
	fileList: function(target, dialog, options) {
		
		var ds = km.js.datasource.create({
			type: "database"
		});
		
		var jcr = {
			baseTypeName: "kommet.basic.File",
			properties: [
				{ name: "id" },
				{ name: "createdDate" },
				{ name: "name" }
			]
			/*restrictions: [
			               {
			            	   operator: "not",
			            	   property_name: "id",
			            	   args: [
			            	      {
			            	    	  property_name: "id",
			            	    	  operator: "in",
			            	    	  args: [
			            	    	         
			            	    	         {
			            	    	        	baseTypeName: "kommet.basic.WebResource",
			            	    	 			properties: [
			            	    	 				{ name: "file.id" }
			            	    	 			]
			            	    	        	
			            	    	         }
			            	    	         
			            	    	  ]
			            	      }
			            	   ]
			               }
			]*/
		};
		
		var displayOptions = {
			properties: [
				{ 
					name: "name", label: "File name", linkStyle: true, onClick: (function(dialog, options) {
						
						return function(fileId) {
							
							if (typeof(options.afterSave) === "function")
							{
								options.afterSave(fileId, null, null, options);
							}
							
							dialog.close();
							
						}
						
					})(dialog, options)
				},
				{ name: "createdDate", label: "Created date", linkStyle: true }
			],
			idProperty: { name: "id" }
		};
		
		var tableOptions = {
			id: "km-files",
			pagination: {
				active: true,
				pageSize: 15
			}
		}
		
		var fileTable = km.js.table.create(ds, jcr, displayOptions, tableOptions);
		
		fileTable.render(jcr, target);
	}
	
}
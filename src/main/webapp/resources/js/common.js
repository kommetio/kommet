/**
 * Function that reloads an object list when sort, paging or search are performed.
 * @param type
 * @param fields
 * @param sortBy
 * @param currentSortOrder
 * @param containerId
 * @param context
 * @return
 */
function sortObjectList (config, mode, currentSortOrder, sortField, formId, containerId)
{
	// toggle sort order - the one that comes as a parameter is the old sort order
	var sortOrder = currentSortOrder.toLowerCase() == "asc" ? "desc" : "asc";
	
	// update sort specification in config
	config.sortBy = sortField + ' ' + sortOrder;
	
	return searchObjectList($.extend({}, config, $('#' + formId).serializeFormJSON()), mode, containerId, config.sysContextPath);
}

function submitObjectListSearch (config, formId, containerId, sysContextPath)
{
	return searchObjectList($.extend({}, config, $('#' + formId).serializeFormJSON()), 'search', containerId, sysContextPath);
}

/**
 * Method called when search panel form is submitted on an object list.
 * @param formId
 * @return
 */
function searchObjectList (config, mode, containerId, sysContextPath)
{	
	$.ajax ({ url: sysContextPath + '/reloadobjectlist?mode=' + mode, type: 'POST', data: JSON.stringify(config), dataType: 'html', contentType: 'application/json', mimeType: 'application/json',
		success: function(data) {
			$('#' + containerId).html(data);
		},
		error: function(data) {
			$('#' + containerId).html(data);
		}
	});
}

function showMsg (id, msg, type, cssStyle)
{
	showMsg(id, msg, type, null, null);
}

function showMsg (id, msg, type, cssStyle)
{
	showMsg(id, msg, type, cssStyle, null, null);
}

function showMsg (id, msg, type, cssStyle, cssClass, contextPath)
{
	var cls = "msg-tag";
	var img = "<img src=\"" + contextPath + "/resources/images/";
	
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
	
	if (contextPath == null)
	{
		img = "";
	}
	
	var tr = $("<tr></tr>").append($("<td></td>").append($(img))).append($("<td></td>").html(actualMsg));
	var table = $("<table></table>").append($("<tbody></tbody>").append(tr));
	
	$("#" + id).append($("<div class=\"" + cls + "\" style=\"" + cssStyle + "\"></div>").append(table));
	$("#" + id).show();
}

function openUrl (url)
{
	location.href = url;
}

function ask(msg, elementId, callback)
{
	return ask(msg, elementId, callback, null);
}

function ask(msg, elementId, callback, cssClass)
{
	return ask(msg, elementId, callback, cssClass, null, null);
}

function ask(msg, elementId, callback, cssClass, yesLabel, noLabel)
{
	var id = "ask-" + randomInt(1000);
	$("#" + elementId).html(getAskPrompt(id, msg, callback, cssClass, yesLabel, noLabel));
}

function getAskPrompt (id, msg, callback, cssClass, yesLabel, noLabel)
{
	var code = "<div class=\"ask";
	if (cssClass != null)
	{
		code += " " + cssClass;
	}
	code += "\" id=\"" + id + "\"><div class=\"ask-msg\">";
	code += msg + "</div>";
	code += "<input type=\"button\" value=\"" + (yesLabel != null ? yesLabel : "Yes") + "\" onclick=\"$('#" + id + "').remove(); f = " + callback + ";f();\" class=\"sbtn\">";
	// on 'No', remove the prompt from DOM
	code += "<input type=\"button\" value=\"" + (noLabel != null ? noLabel : "No") + "\" onclick=\"$('#" + id + "').remove()\" class=\"sbtn\">";
	code += "</ask>";
	return code;
}

function randomInt(max)
{
    return Math.floor(Math.random() * (max + 1));
}

function getSelectFromJSON(id, name, options, optionValueField, optionNameField)
{
	return getSelectFromJSON(id, name, options, optionValueField, optionNameField, null);
}

function getSelectFromJSON(id, name, options, optionValueField, optionNameField, value)
{
	var s = "<select";
	if (id != null)
	{
		s += " id=\"" + id + "\"";
	}
	if (name != null)
	{
		s += " name=\"" + name + "\"";
	}
	s += ">";
	
	for (i = 0; i < options.length; i++)
	{
		s += "<option value=\"" + options[i][optionValueField] + "\"" + (options[i][optionValueField] == value ? " selected" : "") + ">" + options[i][optionNameField] + "</options>";
	}
	s += "</select>";
	return s;
}

/* JQuery extension that creates opens a dialog with the given URL */
(function($) {
	$.fn.rialog = function(options) {
		
		if (options instanceof String)
		{
			if ("close" === options)
			{
				//$(".rialog").dialog('close');
			}
		}
		else
		{
			var settings = $.extend({
	            width: "620px",
	            height: "500px"
	        }, options);
			
			var code = "<div class=\"rialog-overlay\" id=\"" + settings.id + "\" onClick=\"$('#" + settings.id + "').closeRialog()\">";
			code += "<div class=\"rialog\" style=\"width: " + settings.width + "; height: " + settings.height + "\">";
			code += "<div class=\"topbar\">";
			
			if (settings.title !== undefined && settings.title != null)
			{
				code += settings.title;
			}
			
			code += "<a class=\"close-btn\" onclick=\"$('#" + settings.id + "').closeRialog()\">x</a></div>";
			
			code += "<div style=\"" + settings.style + "\">";
			
			if (settings.url === undefined)
			{
				code += this.html();
			}
			else
			{
				// add an iframe with the given ID to the dialog
				code += "<iframe src=\"" + settings.url + "\" style=\"height: " + settings.height + "\" onload=\"\"></iframe>";
			}
			
			code += "</div></div></div>";
			
			this.hide();
			$("body").append(code);
			
			$("body").css("overflow", "hidden");
			$("html").css("overflow", "hidden");
		}
		
	}
	
	$.fn.closeRialog = function() {
		this.closest(".rialog-overlay").remove();
		$("body").css("overflow", "auto");
		$("html").css("overflow", "auto");
	}
	
	$.closeRialog = function() {
		$(".rialog-overlay").remove();
		$("body").css("overflow", "auto");
		$("html").css("overflow", "auto");
	}
	
}(jQuery));

/*function selectLookupItem (lookupId, id, name, event)
{
	if (id != null && name != null)
	{
		window.parent.document.getElementById(lookupId + '_hidden_lookup').value = id;
		window.parent.document.getElementById(lookupId + '_visible_lookup').value = name;
	}
	
	// close dialog window
	window.parent.$('#' + lookupId + "_list_rialog").closeRialog();
	
	if (event != null)
	{
		event.preventDefault();
	}
}*/

function unassociate (associationFieldId, recordId, associatedRecordId, contextPath)
{
	var url = contextPath + '/km/unassociate?assocField=' + associationFieldId + '&recordId=' + recordId + '&assocRecordId=' + associatedRecordId;
	$.post(url, function(data) {
		return data.status === 'success';
	}, 
	"json");
}

function associationPanel (containerId, associationFieldId, recordId, displayFields, addAction, viewAction, contextPath, refreshedPanelId)
{
	var params = {
		associationFieldId: associationFieldId,
		recordId: recordId,
		displayFields: displayFields,
		addAction: addAction, 
		viewAction: viewAction,
		refreshedPanelId: refreshedPanelId
	}
	
	$.post(contextPath + '/km/associationpanel', params, function(html) {
		$('#' + containerId).html(html);
	});
}

(function($) {
	$.fn.serializeFormJSON = function() {
	
	   var o = {};
	   var a = this.serializeArray();
	   $.each(a, function() {
	       if (o[this.name]) {
	           if (!o[this.name].push) {
	               o[this.name] = [o[this.name]];
	           }
	           o[this.name].push(this.value || '');
	       } else {
	           o[this.name] = this.value || '';
	       }
	   });
	   return o;
	};
})(jQuery);

var objectListConfigs = typeof(objectListConfigs) == 'undefined' ? new Array() : objectListConfigs;
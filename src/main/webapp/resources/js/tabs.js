tabs = {
		
	showTab: function (tabId) {
	
		var tabs = $("#" + tabId).parent().children(".tab");
	
		// hide all tabs
		for (i = 0; i < tabs.length; i++) {
			$("#" + tabs[i].id).hide();
		}
	
		$("#" + tabId).parent().find("ul.tabbar > li").removeClass("active");
		$("#" + tabId).show();
		$("#tablink_" + tabId).addClass("active");
	},

	create: function(options) {
		
		var defaultSettings = {
			activeTab: 0,
			tabLinkClass: "tab-link"
		}
			
		var settings = $.extend({}, defaultSettings, options);
		
		var tabsObj = {
				
			settings: settings,
				
			render: function() {
				
				var tabbar = "<ul class=\"tabbar\">";
			
				for (i = 0; i < this.settings.tabs.length; i++) {
					
					var tab = this.settings.tabs[i];
					
					// add tab to top tab bar
					tabbar += "<li id=\"tablink_" + tab.id + "\" class=\"" + settings.tabLinkClass + "\"><div onClick='tabs.showTab(\"" + tab.id + "\")'>" + tab.name + "</div></li>";
					
					$("#" + tab.id).addClass("tab");
				}
				
				tabbar += "</ul>";
				
				$("#" + settings.id).prepend(tabbar);
				tabs.showTab(this.settings.activeTab);
			}
				
		}
		
		return tabsObj;
	}
		
}
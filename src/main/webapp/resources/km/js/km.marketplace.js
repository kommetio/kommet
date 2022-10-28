/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

km.js.marketplace = {
		
	cachedData: null,

	render: function(userOptions) {
		
		// clear cached data
		this.cachedData = {
			availableLibs: [],
			installedLibs: [],
			notifier: km.js.notifier.get()
		}
		
		var defaultOptions = {
			allowInstall: true,
			detailsLink: "rm/lib/marketplaceimport?id="
		};
		
		options = $.extend({}, defaultOptions, userOptions);
		
		this.cachedData.notifier.wait("availableLibs");
		
		if (options.allowInstall)
		{
			this.cachedData.notifier.wait("installedLibs");
		}
		
		this.cachedData.notifier.onComplete = (function(cachedData) {
			
			return function() {
				
				var libs = cachedData.availableLibs;
				var code = $("<div></div>").addClass("km-mrkt");
				
				for (var i = 0; i < libs.length; i++)
				{
					var lib = libs[i];
					var box = $("<div></div>").addClass("km-lib");
					
					box.click((function(options) {
						
						return function() {
							location.href = km.js.config.contextPath + "/" + options.detailsLink + lib.id;
						}
						
					})(options));
					
					var logo = $("<img></img>").attr("src", "https://kommet.io/km/download/" + lib.logo.id).addClass("km-lib-logo");
					box.append(logo);
					
					var label = $("<div></div>").addClass("km-lib-label").text(lib.label);
					var name = $("<div></div>").addClass("km-lib-name").text(lib.name);
					
					if (lib.background)
					{
						label.css("background", lib.background);
						name.css("background", lib.background);
					}
					
					box.append(label);
					box.append(name);
					
					var desc = $("<div></div>").addClass("km-lib-desc").text(lib.description);
					box.append(desc);
					
					var btnSection = $("<div></div>").addClass("km-lib-btns");
					
					if (options.allowInstall)
					{
						// check if the given library is on the list of installed libs
						var installationStatus = function(lib, installedLibs) {
							
							for (var i = 0; i < installedLibs.length; i++)
							{
								if (installedLibs[i].name === lib.name && installedLibs[i].version === lib.version)
								{
									console.log(JSON.stringify(installedLibs[i]));
									if (installedLibs[i].isEnabled === true)
									{
										return "installed";
									}
									else
									{
										return "installed/disabled";
									}
								}
							}
							
							return "not installed";
						}
						
						var installIcon = $("<i></i>").addClass("fa fa-download");
						var status = installationStatus(lib, cachedData.installedLibs);
						
						if (status === "installed")
						{
							var installBtn = $("<a></a>").addClass("sbtn sbtn-inactive").append(installIcon).append("Installed").attr("href", "javascript:;");
							btnSection.append(installBtn);
						}
						else if (status === "installed/disabled")
						{
							var installBtn = $("<a></a>").addClass("sbtn sbtn-inactive").append(installIcon).append("Downloaded").attr("href", "javascript:;");
							btnSection.append(installBtn);
						}
						else
						{
							var installBtn = $("<a></a>").addClass("sbtn").append(installIcon).append("Install").attr("href", km.js.config.contextPath + "/" + options.detailsLink + lib.id);
							btnSection.append(installBtn);
						}
					}
					
					var detailsIcon = $("<i></i>").addClass("fa fa-commenting-o");
					var detailsBtn = $("<a></a>").addClass("sbtn").append(detailsIcon).append("Details").attr("href", km.js.config.contextPath + "/" + options.detailsLink + lib.id);
					btnSection.append(detailsBtn);
					
					box.append(btnSection);
					
					code.append(box);
				}
				
				options.target.empty().append(code);
				
			}
			
		})(this.cachedData);
		
		if (options.allowInstall)
		{
			km.js.db.query("select id, name, source, version, isEnabled from Library where source = 'External (library repository)'", (function(cachedData) {
				
				return function(libs, count, jsti) {
					lib = km.js.utils.addPropertyNamesToJSRC(libs, jsti);
					cachedData.installedLibs = libs;
					cachedData.notifier.reach("installedLibs");
				}
				
			})(this.cachedData));
		}
		
		var payload = {
			limit: options.limit,
			keyword: options.keyword
		};
		
		$.get("https://kommet.io/rest/marketplace/libs", payload, (function(options, cachedData) {
						
			return function(libs) {
				cachedData.availableLibs = libs;
				cachedData.notifier.reach("availableLibs");
			}
			
		})(options, this.cachedData));

	}

}
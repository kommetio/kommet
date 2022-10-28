<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="Libraries" importRMJS="true">

	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ui.js"></script>
	
		<style>
		
			div.km-err-list {
				margin: 2rem 0;
			    color: #e80000;
			    border: 1px solid #ddd;
			    padding: 1rem;
			    border-radius: 0.2em;
			    background: #fbfbfb;
			}
			
			div#km-header {
				padding-bottom: 1rem;
			}
			
			div#libDescription {
				margin-bottom: 2rem
			}
			
			div#libAbout {
				margin: 2rem 0;
			}
		
		</style>
	
		<script>
		
			var library = {
					
				cachedData: {},
					
				showErrors: function(options) {
					
					var list = $("<ul></ul>");
					
					for (var i = 0; i < options.errors.length; i++)
					{
						var err = options.errors[i];
						list.append($("<li></li>").text(err));
					}
					
					var code = $("<div></div>");
					var title = $("<div></div>").text("Errors occurred during library installation");
					code.append(title).append(list);
					
					$(".km-errors").empty().append(code).addClass("km-err-list");
					
				},
					
				renderDetails: function(options) {
					
					// clear cached data
					this.cachedData = {
						availableLib: null,
						installedLibs: [],
						notifier: km.js.notifier.get()
					}
					
					this.cachedData.notifier.wait("availableLib");
					this.cachedData.notifier.wait("installedLibs");
					
					this.cachedData.notifier.onComplete = (function(cachedData) {
						
						return function() {
							
							var lib = cachedData.availableLib;
							
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
								$("#installBtn").replaceWith(installBtn);
							}
							else if (status === "installed/disabled")
							{
								var installBtn = $("<a></a>").addClass("sbtn sbtn-inactive").append(installIcon).append("Downloaded").attr("href", "javascript:;");
								$("#installBtn").replaceWith(installBtn);
							}
							else
							{
								// append action to the install button
								$("#installBtn").click((function(lib) {
									
									return function() {
										
										$(".km-errors").empty().removeClass("km-err-list");
										
										var waitIcon = km.js.ui.buttonWait({
											button: $(this),
											text: "Installing"
										});
										
										var payload = {
											fileId: lib.file.id
										}
										
										$.post(km.js.config.contextPath + "/km/lib/marketplace/doinstall", payload, (function(button) {
											
											return function(data) {
												
												km.js.ui.buttonWaitStop(button);
												
												if (data.success)
												{
													location.href = km.js.config.contextPath + "/km/libraries/" + data.data.libraryId;
												}
												else
												{
													library.showErrors({
														errors: data.messages
													})
												}
												
											}
											
										})(waitIcon), "json");
										
									}
									
								})(lib));
							}
							
							$(".km-lib-title").text(lib.label);
							$("#libName").text(lib.name);
							$("#libProvider").text(lib.provider);
							$("#libVersion").text(lib.version);
							$("#libDescription").text(lib.description);
							
							// use "append" because it can be HTML
							$("#libAbout").append(lib.longDescription);
							
							if (lib.website)
							{
								$("#libWebsite").append($("<a></a>").text(lib.website).attr("href", lib.website));
							}
							
						}
						
					})(this.cachedData);
						
					km.js.db.query("select id, name, source, version, isEnabled from Library where source = 'External (library repository)'", (function(cachedData) {
						
						return function(libs, count, jsti) {
							lib = km.js.utils.addPropertyNamesToJSRC(libs, jsti);
							cachedData.installedLibs = libs;
							cachedData.notifier.reach("installedLibs");
						}
						
					})(this.cachedData));
					
					$.get("https://kommet.io/rest/marketplace/lib?id=" + options.libraryId, (function(options, cachedData) {
									
						return function(lib) {
							cachedData.availableLib = lib;
							cachedData.notifier.reach("availableLib");
						}
						
					})(options, this.cachedData));
					
				}
					
			}
			
			$(document).ready(function() {
				
				library.renderDetails({
					
					libraryId: "${libId}",
					target: $(".km-lib")
					
				});
				
			});
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
			<ko:pageHeader id="km-header"><i class="fa fa-cloud-download"></i><span id="km-lib-title"></span></ko:pageHeader>
			<div id="libDescription"></div>
			
			<ko:buttonPanel>
				<a href="javascript:;" id="installBtn" class="sbtn">Install library</a>
				<a href="${pageContext.request.contextPath}/km/appmarketplace"" class="sbtn">App marketplace</a>
			</ko:buttonPanel>
			
			<div id="km-errors"></div>
			
			<ko:propertyTable>
					
				<ko:propertyRow>
					<ko:propertyLabel value="Name"></ko:propertyLabel>
					<ko:propertyValue id="libName"></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Provider"></ko:propertyLabel>
					<ko:propertyValue id="libProvider"></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Website"></ko:propertyLabel>
					<ko:propertyValue id="libWebsite"></ko:propertyValue>
				</ko:propertyRow>
				<ko:propertyRow>
					<ko:propertyLabel value="Version"></ko:propertyLabel>
					<ko:propertyValue id="libVersion"></ko:propertyValue>
				</ko:propertyRow>
				
			</ko:propertyTable>
			
			<div id="libAbout"></div>
			
		</div>
	
	</jsp:body>
	
</ko:homeLayout>
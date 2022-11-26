<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<%-- ztree docs: http://www.treejs.cn/v3/api.php --%>

<ko:ideLayout title="Code editor">
	<jsp:body>
	
		<script async src="https://use.fontawesome.com/f2870818a5.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ide.js"></script>
	
		<style>
		
			#ide-container {
				font-size: 0.8rem;
			}
	
			#ide-container div.CodeMirror {
				border: 1px solid #ddd;
				border-left: none;
				text-align: left;
			}
			
			#ide-container div.CodeMirror-code {
				font-size: 0.8rem;
			}
			
			<%-- search dialog --%>
			#ide-container div.CodeMirror-dialog {
				font-family: Arial;
				font-size: 0.75rem;
				padding: 0.5rem;
			}
			
			#ide-container div.CodeMirror-dialog input[type=text] {
				font-family: Arial;
				font-size: 0.75rem;
			}
			
			#ide-container .CodeMirror-scroll {
				overflow-y: auto;
				overflow-x: auto;
				width: 100%
			}
			
			#ide-container div.CodeMirror pre {
				font-family: Consolas, Courier New;
			}
			
			#ide-container div.CodeMirror span.CodeMirror-matchingbracket {
  				color: #FF4444;
  				font-weight: bold;
  			}
  			
  			#ide-container div.CodeMirror div.CodeMirror-cursor {
  				border-left: 2px solid rgb(52, 52, 52);
			}
			
			#ide-container .CodeMirror-gutter {
				background-color: rgb(233, 241, 249);
			}
			
			#ide-container .CodeMirror-lines .CodeMirror-linenumber {
				padding-right: 5px;
				color: #A3A3A3;
				font-family: monospace;
			}
			
			#ide-container .CodeMirror-gutter-wrapper {
				left: -36px;
			}
			
			.CodeMirror-hint {
				text-align: left;
			}
			
			div#km-search {
				position: fixed;
			    top: 0;
			    right: 0;
			    padding: 0.5rem 1rem;
			    background: #fff;
			    text-align: left;
			    font-size: 0.75rem;
			    border-bottom-left-radius: 0.2em;
			}
			
			div#km-search > input {
				width: 20rem;
				margin: 0 1rem;
			}
			
			ul.tabbar {
				list-style: none;
				font-family: Arial;
				font-size: 12px;
				color: rgba(78, 78, 78, 1);
				margin: 0;
				padding: 0;
				background-color: #F7F7F7;
				width: 5000%;
			}
			
			ul.tabbar > li {
				display: inline-block;
				vertical-align: middle;
				padding: 0.5em 0.5em 0.4em 1em;
				border: 1px solid #DDD;
				border-bottom: none;
				margin: 0;
				margin-right: 1px;
				background-color: transparent;
				float: left;
				height: 21px;
				border-radius: 0.3em 0.3em 0 0;
			}
			
			ul.tabbar > li:hover {
				background-color: #FCFCFC
			}
			
			ul.tabbar > li .unsaved {
				margin-left: 3px;
				font-weight: normal;
				font-size: 13px;
			}
			
			ul.tabbar > li.active, ul.tabbar > li:hover {
				background-color: #fff;
				color: #000;
			}
			
			ul.tabbar > li > div.codename {
			  display: inline-block;
			  margin-right: 0.5em;
			  cursor: pointer;
			}
			
			ul.tabbar > li.active > div.codename {
				color: #000;
			}
			
			ul.tabbar > li:hover > img.close {
				display: inline-block;
			}
			
			img.close {
				position: relative;
				top: 3px;
				margin-right: 7px;
				cursor: pointer;
			}
			
			#dir-tree {
				border: 1px solid #ccc;
			    background-color: #fff;
			    height: 2000px;
			    width: 100%;
			    box-sizing: border-box;
			    border-radius: 0.2rem;
			    font-family: Consolas;
			    font-size: 0.8rem;
			}
			
			td#treecell {
				border-right: 1px solid #ccc;
				box-shadow: 0 2px 8px rgba(136, 124, 124, 0.56);
				width: 200px;
			}
			
			#ide-container {
				width: 100%;
				height: 100%;
			}
			
			#ide-bottom-tabs {
				display: block;
			    bottom: 0;
			    border-bottom: 1px solid #ccc;
			    width: 100%;
			    background-color: #fff;
			}
			
			#ide-bottom-tabs > ul {
				list-style-type: none;
			}
			
			#ide-bottom-tabs > .fn {
				font-size: 14px;
				color: rgb(174, 51, 51);
				text-decoration: underline;
				margin: 10px 0 3px 0;
			}
			
			.line-error {
				background-color: rgb(255, 231, 235);
			}

			
			input#tree-search {
				width: 100%;
				border: 1px solid #d0cdcd;
				padding: 0.5em;
				background-color: #fff;
				margin-bottom: 0.5rem;
			}
			
			#no-open-files {
				margin: 20px 0 0 30px;
			}
			
			ul#ide-menu, ul#ide-menu ul {
				list-style-type: none;
			}
			
			ul#ide-menu li {
				padding: 0.5em 1em;
				display: inline-block;
				position: relative;
			}
			
			ul#ide-menu li:hover {
				background-color: #E8E8E8;
			}
			
			ul#ide-menu a {
				text-decoration: none;
				color: #424242;
				font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif,"Apple Color Emoji","Segoe UI Emoji";
			}
			
			ul#ide-menu a:hover {
				background-color: #E8E8E8;
			}
			
			ul#ide-menu > li > ul {
				display: none;
				z-index: 100;
				border: 1px solid #ccc;
				padding: 0.5em;
				min-width: 10em;
				background-color: #fff;
				box-shadow: 0 0 1em rgb(187, 187, 187);
			}
			
			ul#ide-menu > li > ul > li {
				display: block;
			}
			
			ul#ide-menu > li:hover > ul {
				display: block;
				position: absolute;
				top: 30px;
				left: 0;
				width: 100%;
			}
			
			div#tabbar-scroll {
				overflow: hidden;
			}
			
			div#km-tabbar-container {
				overflow: hidden;
				width: 100%;
				display: table-cell;
				background-color: #ececec
			}
			
			div#km-tabbar-container img.scroll {
				height: 1em;
				position: relative;
				cursor: pointer;
			}
			
			ul.file-error-list {
				margin: 1em;
				list-style-type: none;
				text-align: left;
			}
			
			ul.file-error-list > li {
				font-size: 0.8rem;
			}
			
			div#file-container {
				border-collapse: collapse;
				width: 100%;
				height: 100%;
			}
			
			div.km-err-header {
				font-size: 0.75rem;
				background-color: #6092bd;
				padding: 0.5em 0.7em 0.5em 0.7em;
				color: #fff;
				text-align: left;
			}
			
			div.km-ide-err-icon {
				vertical-align: top;
				display: inline-block;
				margin-right: 1em;
			}
			
			div#km-ide-errors {
				border: 1px solid #dcdcdc;
    			border-radius: 0.3em;
    			overflow: auto;
    			background: #fff;
			}
			
			div.km-ide-err-icon > img {
				height: 1.3em;
			}
			
			#km-ide-topmenu {
				width: 100%;
				border-bottom: 1px solid #ccc;
			}
			
			div.km-ide-topmenu-cell {
				display: table-cell;
				height: 20px;
				text-align: left;
			}
			
			#km-ide-lower-container {
				width: 100%;
				border-top: 1px solid #ccc;
				background-color: #eaeaea;
			}
			
			#ide {
				width: 100%;
				height: 100%;
				display: table;
				border-collapse: collapse;
			}
			
			div#ide > div {
				display: table-row;
			}
			
			#ide ul.ztree {
				margin-top: 0;
			}
			
			#ide ul.ztree li {
				padding: 0.2em;
			}
			
			#ide ul.ztree li ul {
				margin-top: 0.2em;
			}
			
			div#km-ide-right-container {
				display: table-cell;
				padding: 0.5rem;
			}
			
			div#km-ide-right-subcontainer {
				display: table;
				width: 100%;
			}
			
			div#km-ide-right-subcontainer > div {
				display: table-row;
			}
			
			div#km-ide-leftmenu {
				width: 15em;
				display: table-cell;
				vertical-align: top;
				padding: 0.5rem;
			}
			
			div#km-ide-scroll-btns {
				vertical-align: middle;
				float: right;
				margin-right: 1em;
			}
			
			div#km-ide-scroll-btns > span {
				height: 100%;
				vertical-align: middle;
				display: inline-block;
			}
			
			div.km-ide-errors-title {
				font-weight: bold;
				font-size: 0.75rem;
			}
			
			div#km-ide-editors-cell {
				display: table-cell;
			}
			
			div#km-ide-editors {
				border-left: 1px solid #dcdcdc;
			}
			
			div#km-ide-bottom-tabs-cell {
				height: 100px;
				padding: 0.5em 0.5em 0.5em 0;
				background: #eaeaea;
			}
			
			body {
				overflow: hidden;
			}
			
			i.close-file {
				color: #9c9c9c;
				font-size: 1.2em;
			    margin: 0 0.3em 0 0.3em;
			    cursor: pointer;
			}
			
			i.close-file:hover {
				color: #5d5d5d;
			}
			
			div.km-hint-line {
				padding: 0.2em;
				font-size: 0.8rem;
			}
			
			div.km-hint-line > span.km-hint-import {
				margin-left: 1em;
				color: #555;
				float: right;
			}
			
			.CodeMirror-hints {
				border-radius: 0em;
				overflow-x: scroll;
			}
			
			.CodeMirror-hint {
				overflow: visible;
				max-width: none;
			}
			
			li.CodeMirror-hint-active {
  				background: #ddd;
  				color: inherit;
  			}
  			
  			.km-spinner > div {
  				background-color: #6092bd;
  			}
  			
  			.km-waitbtn-text {
  				color: #666;
  				padding: 0 0.5em;
  			}
	
		</style>
		
		<div id="ide-container">
		
			<div id="ide">
			
				<div id="km-ide-topmenu">
				
					<div class="km-ide-topmenu-cell">
						<ul id="ide-menu">
							<li>
								<a href="${pageContext.request.contextPath}/km/setup">Setup</a>
							</li>
							<li>
								<a href="javascript:;">File</a>
								<ul>
									<li><a href="">Save</a></li>
								</ul>
							</li>
							<li>
								<a href="javascript:;">Templates</a>
								<ul>
									<li><a href="javascript:;" onClick="km.js.ide.getControllerTemplate(newFileCallback())">Controller</a></li>
									<li><a href="javascript:;" onClick="km.js.ide.getClassTemplate(newFileCallback())">Class</a></li>
									<li><a href="javascript:;" onClick="km.js.ide.getTriggerTemplate(newFileCallback())">Trigger</a></li>
									<li><a href="javascript:;" onClick="km.js.ide.getBusinessActionTemplate(newFileCallback())">Business Action</a></li>
									<li><a href="javascript:;" onClick="km.js.ide.getViewTemplate(newFileCallback())">View</a></li>
									<li><a href="javascript:;" onClick="km.js.ide.getLayoutTemplate(newFileCallback())">Layout</a></li>
								</ul>
							</li>
						</ul>
					</div>
					<div class="km-ide-topmenu-cell"></div>
				
				</div>
				
				<div id="km-ide-lower-container">
				
					<div id="km-ide-leftmenu">
						<div class="km-ide-search">
							<input type="text" class="std-input" id="tree-search" placeholder="search files" autocomplete="off">
						</div>
						<ul id="dir-tree" class="ztree"></ul>
					</div>
					
					<div id="km-ide-right-container">
					
						<div id="km-ide-right-subcontainer">
									
							<div id="km-tabbar-container-row">
							
								<div id="km-tabbar-container">
									<div id="km-ide-scroll-btns">
										<span><img class="scroll-left scroll" src="${pageContext.request.contextPath}/resources/images/arrowleft.png" /></span>
										<span><img class="scroll-right scroll" src="${pageContext.request.contextPath}/resources/images/arrowright.png" /></span>
									</div>
									<ul class="tabbar"></ul>
								</div>
							
							</div>
							
							<div id="km-ide-editors">
							
								<div id="km-ide-editors-cell"></div>
							
							</div>
									
							<div id="ide-bottom-tabs">
								<div id="km-ide-bottom-tabs-cell">
									<div id="km-ide-errors">
										<div class="km-ide-errors-title"></div>
									</div>
								</div>
							</div>
						
						</div>
					
					</div>
				
				</div>
							
			</div>
		
		</div>
		
		<script>
		
			window.ide = {
				bottomPanel: {
					// height in pixels
					height: 150
				},
				leftMenu: {
					width: 150
				},
				files: { },
				fileIdToHash: {},
				fileHashToId: {},
				search: {
					keyword: null
				}
			}
			
			function currentFileHash()
			{
				return $("#ide-container ul.tabbar li.active").attr("id").split("-")[1];
			}

			$(document).ready(function() {
				
				$("#tree-search").keyup(function() {
					searchTree($("#tree-search").val());
				});
				
				<%-- intercept clicking ctrl + s/c --%>
				$(window).bind('keydown', function(event) {
				    if (event.ctrlKey || event.metaKey) {
				        switch (String.fromCharCode(event.which).toLowerCase())
				        {
					        case 's':
					            event.preventDefault();
					            saveFile(currentFileHash());
					            break;
					        case 'q':
					            event.preventDefault();
					            closeFile(currentFileHash());
					            break;
				        }
				    }
				});
				
				addTabbarScroll();
				loadAutocompletions();
				openCurrentFiles();
				
				if ("${template}")
				{
					openTemplate();
				}
				
			});
			
			/*function openSearch()
			{
				var code = $("<div></div>").attr("id", "km-search");
				code.append("Search");
				
				var input = $("<input></input>").attr("type", "text").addClass("km-input");
				code.append(input);
				
				var closeBtn = $("<a></a>").attr("href", "javascript:;").text("hide");
				closeBtn.click((function(code) {
					return function() {
						code.remove();
					}
				})(code))
				code.append(closeBtn);
				
				$("body").append(code);
			}*/
			
			function openTemplate()
			{
				if ("${template}" === "trigger")
				{
					km.js.ide.getTriggerTemplate(newFileCallback(), {
						type: "${templateTypeName}"
					});
				}
			}
			
			function getCurrentXmlTag (cm, cur)
			{
				var tag = "";
				var curChar = cur.ch;
				var found = false;
				
				for (var lineNo = cur.line; lineNo >= 0; lineNo--)
				{
					var line = cm.getLine(lineNo);
					
					if (lineNo != cur.line)
					{
						curChar = line.length - 1;
					}
					
					for (var ch = curChar; ch >= 0; ch--)
					{
						if (line.charAt(ch) === "<")
						{
							found = true;
							break;
						}
						
						var currentChar = line.charAt(ch);
						
						// if it is a whitespace, we nullify the tag
						if (/\s/.test(currentChar))
						{
							tag = "";
						}
						else
						{
							tag = line.charAt(ch) + tag;
						}
					}
					
					if (found)
					{
						break;
					}
				}
				
				return tag;
			}
			
			function newFile(name, content, mode, type, fileData)
			{
				// generate random file ID
				var hash = km.js.utils.random(1000000);
				openEditor(null, hash, name, fileData, content, mode);
				showTab(hash);
				
				$("#unsaved-" + hash).show();
				
				window.ide.files[hash] = {
					isNew: true,
					isSaved: false,
					type: type
				};
				
				adjustFileWindows();
				showFileErrors(fileErrors);
			}
			
			function loadAutocompletions()
			{
				$.get(km.js.config.contextPath + "/km/ide/hints", function(data) {
					
					// register Java hints
					CodeMirror.registerHelper("hint", "text/x-java", (function(hints) {
						
						var myCallback = function(cm, callback, options) {
							
							var cur = cm.getCursor();
							var token = cm.getTokenAt(cur);
							var start = token.start;
							var end = token.end;
							
							var selectedHints = [];
						    
							// find hints matching the token
							for (var i = 0; i < hints.length; i++)
							{
								var hintText = hints[i].text;
								if (hintText.indexOf(".") === -1)
								{
									if (hintText.indexOf(token.string) === 0)
									{
										selectedHints.push(hints[i]);
									}
								}
								else
								{
									// if it's a qualified name
									var parts = hintText.split(".");
									
									// check if last part of the qualified name starts with the token
									if (parts[parts.length - 1].indexOf(token.string) === 0)
									{
										var hint = hints[i];
										hint.text = hint.displayText;
										
										hint.render = (function(hint) {
											return function (elem, self, data) {
												var line = $("<div></div>").text(hint.displayText ? hint.displayText : hint.text).addClass("km-hint-line");
												
												if (hint.javaImport)
												{
													line.append($("<span></span>").text(hint.javaImport).addClass("km-hint-import"));
												}
												
												elem.appendChild(line.get(0));
											}
										})(hint);
										
										selectedHints.push(hint);
									}
								}
							}
							
							var methodName = null;
							var varName = null;
							
							// if token ends with a dot
							if (token.string === ".")
							{
								var line = cm.getLine(cur.line);
								varName = "";
								
								// go back and collect characters until whitespace is encountered
								for (var ch = cur.ch - 2; ch >= 0; ch--)
								{
									// search until whitespace
									if (!line.charAt(ch).trim())
									{
										break;
									}
									
									varName  = line.charAt(ch) + varName;
								}
							}
							else
							{
								var currentWord = "";
								var line = cm.getLine(cur.line);
								
								var ch = cur.ch - 1;
								
								// go back and collect characters until whitespace or dot is encountered
								for (; ch >= 0; ch--)
								{
									// search until whitespace
									if (!line.charAt(ch).trim())
									{
										// whitespace encountered, so the word was a variable name
										varName = currentWord;
										break;
									}
									else if (line.charAt(ch) === ".")
									{
										methodName = currentWord;
										break;
									}
									
									currentWord  = line.charAt(ch) + currentWord;
								}
								
								
								varName = "";
								
								// if we have read a method name, we need a variable name that stands before it
								for (var i = ch - 1; i >= 0; i--)
								{
									// search until whitespace
									if (!line.charAt(i).trim())
									{
										break;
									}
									
									varName  = line.charAt(i) + varName;
								}
							}
							
							if (varName)
							{
								km.js.ide.getJavaHints(editors[window.ide.currentFile.hash].getValue(), varName, methodName, cur.line, cur.ch, (function(callback) {
									
									return function(hints) {
										var oCompletions = { list: hints, from: CodeMirror.Pos(cur.line, start), to: CodeMirror.Pos(cur.line, end) } ;
										callback(oCompletions);
									}
									
								})(callback));
							}
							
							// sort hints by match precision
							selectedHints.sort(function(a, b) {
								if (a.displayText.length != b.displayText.length)
								{
									return a.displayText.length - b.displayText.length;
								}
								else
								{
									// if both names have the same length, choose java built in libraries first
									if (a.javaImport.indexOf("java.") === 0)
									{
										return -1;
									}
									else if (b.javaImport.indexOf("java.") === 0)
									{
										return 1;
									}
									else
									{
										// prefer classes with shorter package names
										return a.javaImport.length - b.javaImport.legth;
									}
								}
							});
							
							var oCompletions = { list: selectedHints, from: CodeMirror.Pos(cur.line, start), to: CodeMirror.Pos(cur.line, end) } ;
							
							//CodeMirror.on( oCompletions, 'shown',  CompletionsShown ) ; 
						    //CodeMirror.on( oCompletions, 'select', CompletionsSelect ) ; 
						    
						    CodeMirror.on(oCompletions, "pick", function(item) {
						    	
						    	if (item.javaImport)
						    	{
						    		// check if the package is already imported
						    		var lineCount = cm.lineCount();
						    		var isAlreadyImported = false;
						    		var lastImportLine = null;
						    		
						    		for (var i = 0; i < lineCount; i++)
						    		{
						    			var line = cm.getLine(i);
						    			
						    			if ((new RegExp("^import\\s+")).test(line))
						    			{
						    				lastImportLine = i;
						    			}
						    			
						    			if ((new RegExp("^import\\s+" + item.javaImport.replace(/\./g, "\."))).test(line))
						    			{
						    				isAlreadyImported = true;
						    				break;
						    			}
						    		}
						    		
						    		if (!isAlreadyImported)
						    		{
						    			console.log("Imported " + item.javaImport);
						    			var doc = cm.getDoc();
						    			var pos = {
						    				line: lastImportLine,
						    				ch: 0
						    			};
						    			doc.replaceRange("import " + item.javaImport + ";\r\n", pos);
						    			//cm.setLineContent(lastImportLine, cm.getLine(lastImportLine) + "import " + item.javaImport + ";\r\n");
						    		}
						    	}
						    });
						    
						    //CodeMirror.on( oCompletions, 'close',  CompletionsClose ) ; 
						    return callback(oCompletions); 
						};
						
						return (function(hintCallback) {
							
							var hintFunction = function(cm, callback, options) {
								hintCallback(cm, callback, options);
							};
							
							hintFunction["async"] = true;
							return hintFunction;
							
						})(myCallback);
						
					})(data.data.javaHints));
					
					// register view tag lib hints
					CodeMirror.registerHelper("hint", "xml", (function(namespaces, vrel) {
						
						var callback =  function(cm, codeMirrorCallback, options) {
							
							var cur = cm.getCursor();
							var token = cm.getTokenAt(cur);
							var start = token.start;
							var end = token.end;
							
							var selectedHints = [];
							
							var currentTag = token.state.tagName;//getCurrentXmlTag(cm, cur);
							
							var hintAttributes = (currentTag != token.string);
							var allTagsMatch = (!hintAttributes && token.string === "<");
							var emptyToken = false;
							
							if (/\s+/.test(token.string))
							{
								start++;
								emptyToken = true;
							}
							
							var parentTagName = token.state.context ? token.state.context.tagName : null;
						    
							// find hints matching the token
							for (var i = 0; i < namespaces.length; i++)
							{
								var ns = namespaces[i];
								
								// iterate over tags
								for (var k = 0; k < ns.tags.length; k++)
								{
									var tag = ns.tags[k];
									
									var tagName = ns.name + ":" + tag.name;
									
									if (allTagsMatch || tagName.indexOf(currentTag) > -1)
									{
										if (hintAttributes)
										{
											for (var j = 0; j < tag.attributes.length; j++)
											{
												var attr = tag.attributes[j];
												selectedHints.push({
													text: attr.name + "=\"\"",
													displayText: attr.name,
													insertCursorAt: emptyToken ? (start + attr.name.length + 2) : (start + attr.name.length - token.string.length + 2)
												});
											}
										}
										else
										{
											var hint = {
												text: tagName + "></" + tagName + ">",
												displayText: tagName,
												insertCursorAt: emptyToken ? (start + tagName.length + ns.name.length + 1) : (start + tagName.length + 1)
											}
											
											selectedHints.push(hint);
										}
									}
								}
							}
							
							// iterate over vrel expressions
							for (var i = 0; i < vrel.length; i++)
							{
								if (vrel[i].text.indexOf(token.string) === 0)
								{
									selectedHints.push(vrel[i]);
								}
							}
							
							var oCompletions = { list: selectedHints, from: CodeMirror.Pos(cur.line, start), to: CodeMirror.Pos(cur.line, end) } ;
						    
						    CodeMirror.on(oCompletions, "pick", function(item) {
						    	
						    	var cur = cm.getCursor();
						    	cm.setCursor({
						    		line: cur.line,
						    		ch: item.insertCursorAt 
						    	})
						    	
						    });
						    
						    codeMirrorCallback(oCompletions); 
						};
						
						callback.async = true;
						return callback;
						
					})(data.data.tags, data.data.vrel));
					
				}, "json");
				
				// this is a hack I figured will work to get async hints to work
				CodeMirror.helpers["hint"]["auto"].async = true;
			}
			
			function saveFile(hash)
			{	
				var waitIcon = km.js.ui.buttonWait({
					button: $("#tablink-" + hash + " .km-tab-fn"),
					text: "Saving",
					isRestore: false
				});
				
				$("#error_" + hash).hide();
				$("#code_" + hash).val(editors[hash].getValue());
				
				var fileInfo = window.ide.files[hash];
				var isNew = fileInfo && fileInfo.isNew;
				var fileType = fileInfo ? fileInfo.type : null;
				
				var fileId = window.ide.fileHashToId[hash];
				if (!fileId && !isNew)
				{
					throw "No file ID found for hash " + hash;
				}
				
				$.post("${pageContext.request.contextPath}/km/ide/save", { fileId: fileId, code: editors[hash].getValue(), isNew: isNew, type: fileType }, (function(waitIcon) {
					
					return function(data) {
						
						$("#unsaved-" + hash).hide();
						
						if (data.success)
						{	
							window.ide.fileIdToHash[data.data.fileId] = hash;
							window.ide.fileHashToId[hash] = data.data.fileId;
							window.ide.files[hash].isNew = false;
							
							// set the current file name
							$("#tablink-" + window.ide.currentFile.hash + " span.km-tab-fn").text(data.data.fileName);
							
							clearFileErrors(hash);
							refreshDirTree();
						}
						else
						{
							waitIcon.isRestore = true;
							km.js.ui.buttonWaitStop(waitIcon);
							clearFileErrors(hash);
							
							fileErrors[hash] = data.messages;
							//editors[fileId].setGutterMarker(1, "errors", showErrorMarker());
							
							for (var i = 0; i < data.messages.length; i++)
							{
								var err = data.messages[i];
								if (err.line)
								{
									editors[hash].addLineClass(err.line - 1, 'background', 'line-error');
								}
							}
						}
						
						showFileErrors(fileErrors);
					}
				})(waitIcon), "json");
			}
			
			var fileErrors = {};
			
			var editors = {};
			
			function clearFileErrors(hash)
			{
				var errors = fileErrors[hash];
				
				if (!errors)
				{
					return;
				}
				
				// clear all line error markers
				for (var i = 0; i < errors.length; i++)
				{
					var err = errors[i];
					if (err.line)
					{
						editors[hash].removeLineClass(err.line - 1, 'background');
					}
				}
				
				fileErrors[hash] = null;
			}
			
			function newFileCallback()
			{
				return function(options) {
					newFile(options.name, options.content, options.mode, options.type, options.fileData);
				}
			}
			
			function showFileErrors (fileErrors)
			{
				var code = $("<ul></ul>").addClass("file-error-list");
				var hasErrors = false;
				
				for (var hash in fileErrors)
				{
					var errors = fileErrors[hash];
					if (!errors || errors.length == 0)
					{
						continue;
					}
					
					var fileName = hash;
					
					hasErrors |= (errors.length > 0);
					
					for (var i = 0; i < errors.length; i++)
					{
						var err = errors[i];
						// if file name is available, use it instead of ID to display errors
						if (err.item)
						{
							fileName = err.item;
						}
						
						var errText = fileName;
						if (err.line)
						{
							errText += " (" + err.line + ")";
						}
						errText += ": " + err.message;
						
						var errMsg = $("<span></span>").text(errText);
						var errIcon = $("<div></div>").append($("<img></img>").attr("src", km.js.config.imagePath + "/ideerr.png")).addClass("km-ide-err-icon");
						code.append($("<li></li>").append(errIcon).append(errMsg));
					}
				}
				
				if (code)
				{
					var errHeader = $("<div></div>").text("Errors").addClass("km-err-header");
					if (hasErrors)
					{
						$("#km-ide-errors").empty().append(errHeader).append(code);
						//$("#ide-bottom-tabs").show();
					}
					else
					{
						code.append($("<li></li>").append($("<span>no errors</span>").css("font-style", "italic")));
						$("#km-ide-errors").empty().append(errHeader).append(code);
					}
				}
				else
				{
					$("#km-ide-errors").empty();
				}
			}
			
			function openCurrentFiles()
			{
				hideAllTabs();
				
				<c:forEach var="file" items="${files}">
				openFile("${file.id}", "${file.name}", "${currentFileId}" === "${file.id}");
				</c:forEach>
				
				adjustFileWindows();
			}
			
			function getModeFromFileId(fileId)
			{
				if (fileId.indexOf("00b") === 0)
				{
					return "text/x-java";
				}
				<%-- views and layouts --%>
				else if (fileId.indexOf("00a") === 0 || fileId.indexOf("00f") === 0)
				{
					return "xml";
				}
				else
				{
					return null;
				}
			}
				
			function addFileEditor (hash, fileName, fileData, mode)
			{	
				var fileId = window.ide.fileHashToId[hash];
				if (!fileId && !mode)
				{
					throw "File ID not found (probably file not saved) and no mode passed to editor";
				}
				
				var editor = CodeMirror.fromTextArea(document.getElementById("code-" + hash), {
					lineNumbers: true,
					lineWrapping: false,
					matchBrackets: true,
					mode: mode ? mode : getModeFromFileId(fileId),
					cursorHeight: 1,
					indentWithTabs: true,
					smartIndent: false,
					foldGutter: true,
					tabSize: 4,
					indentUnit: 4,
					extraKeys: { "Alt-F": "findPersistent", "Tab": "indentMore", "Shift-Tab": "indentLess", "Ctrl-Space": "autocomplete" },
					gutters: ["CodeMirror-linenumbers", "errors", "CodeMirror-foldgutter"]
				});
				
				editor${file.id}.on("change", function() {
					$("#unsaved-" + hash).show();
				});
				
				editor.setSize(window.innerWidth - 235, window.innerHeight - 200);
				
				// collapse sections
				if (fileData && $.isArray(fileData.collapse))
				{
					for (var i = 0; i < fileData.collapse.length; i++)
					{
						var section = fileData.collapse[i];
						editor.foldCode(CodeMirror.Pos(section.start, 0));
						editor.foldCode(CodeMirror.Pos(section.end, 0));
					}
				}
				
				editors[hash] = editor;
			}
			
			function adjustFileWindows()
			{	
				$("#km-ide-leftmenu").css("width", window.ide.leftMenu.width + "px");
				
				$("#km-ide-bottom-tabs-cell").height(window.ide.bottomPanel.height);
				
				var editorHeight = window.innerHeight - $("div.header-bar").outerHeight() - $("#km-ide-topmenu").outerHeight() - $("#km-ide-bottom-tabs-cell").outerHeight();
				var editorWidth = window.innerWidth - document.getElementById("km-ide-leftmenu").offsetWidth - 20;
				
				// set height of the editors manually
				$("#km-ide-editors-cell").height(editorHeight + "px");
				
				for (var hash in editors)
				{
					editors[hash].setSize(editorWidth, editorHeight);
				}
			}
			
			function hideAllTabs()
			{
				$("div.km-ide-tab").hide();
			}
			
			function hideTab(hash)
			{
				$("#tab-" + hash).hide();
				$("#tablink-" + hash).removeClass("active");
			}
			
			function showTab(hash)
			{
				$("div.km-ide-tab").hide();
				$("ul.tabbar > li").removeClass("active");
				$("#tab-" + hash).show();
				$("#tablink-" + hash).addClass("active");
				
				$("#no-open-files").hide();
				
				window.ide.currentFile = {
					hash: hash
				}
			}
			
			function openEditor (fileId, hash, fileName, fileData, content, mode)
			{
				// add file errors container
				$("#ide #ide-bottom-tabs").append("<div id=\"file-errors-" + hash + "\"></div>");
				
				var closeBtn = "<i class=\"fa close close-file fa-close\" aria-hidden=\"true\"></i>";
				
				var newTab = $("<li id=\"tablink-" + hash + "\"><div class=\"codename\"><span class=\"km-tab-fn\">" + fileName + "</span><span class=\"unsaved\" id=\"unsaved-" + hash + "\" style=\"display:none\">*</span></div>" + closeBtn + "</li>");
				newTab.find(".codename").click(function() {
					showTab(hash);
				});
				
				newTab.find(".close").click(function() {
					closeFile(hash);
				});
				
				// add file tab
				$("#ide ul.tabbar").append(newTab);
				
				// add file textarea
				$("#km-ide-editors-cell").append("<div id=\"tab-" + hash + "\" class=\"km-ide-tab\"><textarea id=\"code-" + hash + "\">" + content + "</textarea></div>");
				
				addFileEditor(hash, fileName, fileData, mode);
			}
			
			function openFile (fileId, fileName, isSelected)
			{	
				var hash = null;
				
				// if file is already opened
				if (window.ide.fileIdToHash[fileId])
				{
					hash = window.ide.fileIdToHash[fileId]
					
					// make sure the file is not already open, if it is, just switch to its tab
					if (editors[hash])
					{
						showTab(hash);
						return;
					}
					else
					{
						throw "File open, but no editor available";
					}
				}
	
				// generate a hash for the file
				hash = window.ide.fileIdToHash[fileId];
				
				var hash = km.js.utils.random(1000000);
				window.ide.fileIdToHash[fileId] = hash;
				window.ide.fileHashToId[hash] = fileId;
				
				$.get("${pageContext.request.contextPath}/km/ide/openfile", { fileId: fileId }, (function(fileHash) {
				
						return function(data) {
					
							if (data.status == "success")
							{	
								openEditor(fileId, fileHash, fileName, data.data, data.data.code);
								
								if (isSelected)
								{
									showTab(fileHash);
								}
								else
								{
									hideTab(fileHash);
								}
							}
							else
							{
								fileErrors[fileHash] = data.messages;
							}
							
							window.ide.files[fileHash] = {
								isNew: false,
								isSaved: true
							};
							
							adjustFileWindows();
							showFileErrors(fileErrors);
						}
						
					
				})(hash), "json");
			}
			
			function showErrorMarker()
			{
				var marker = document.createElement("div");
				marker.style.color = "#822";
				marker.innerHTML = "●";
				return marker;
			}
			
			function closeFile(hash)
			{
				var doCloseFile = function(hash) {
					$("#tab-" + hash).remove();
					$("#tablink-" + hash).remove();
					delete editors[hash];
					delete fileErrors[hash];
					
					var fileId = window.ide.fileHashToId[hash];
					delete window.ide.fileHashToId[hash];
					delete window.ide.fileIdToHash[fileId];
					
					delete window.ide.files[hash];
					
					// show other file
					for (var otherFileHash in window.ide.files)
					{
						showTab(otherFileHash);
						break;
					}
				};
				
				if (window.ide.files[hash] && window.ide.files[hash].isNew)
				{
					// file is not persisted, so we just close it
					doCloseFile(hash);
				}
				else
				{
					var fileId = window.ide.fileHashToId[hash];
					if (!fileId)
					{
						throw "No file ID found for hash " + hash;
					}
					
					$.post("${pageContext.request.contextPath}/km/ide/close", { fileId: fileId }, function(data) {
						
						var hash = window.ide.fileIdToHash[fileId];
						if (!hash)
						{
							throw "No hash for file " + fileId;
						}
						
						if (data.status == "success")
						{
							doCloseFile(hash);
							
							// if there is any other file left open, switch to its tab
							if (data.data.currentFileId)
							{
								showTab(window.ide.fileIdToHash[data.data.currentFileId]);
							}
						}
						else
						{
							fileErrors[hash] = data.messages;
						}
						
						showFileErrors(fileErrors);
						
						if (km.js.utils.size(editors) === 0)
						{
							$("#no-open-files").show();
						}
						
					}, "json");
				}
			}
			
			window.onresize = function(event) {
				adjustFileWindows();
			}
			
			var setting = {
				callback: {
					onClick: treeItemClick
				}
			};
			
			var fileNodes = ${dirMap};
		
			$(document).ready(function(){
				window.ide.dirtree = $.fn.zTree.init($("#dir-tree"), setting, fileNodes);
			});
			
			function refreshDirTree()
			{
				$.get(km.js.config.contextPath + "/km/ide/dirtree", function(data) {
					
					if (data.success)
					{
						window.ide.dirtree = $.fn.zTree.init($("#dir-tree"), setting, data.data);
						
						if ($("#tree-search").val())
						{
							// after dir tree has been refreshed, we want to open it where it previously was
							searchTree($("#tree-search").val());
						}
					}
					else
					{
						km.js.ui.statusbar.err("Could not retrieve directory tree");
					}
				}, "json");
			}
			
			function searchTree (keyword)
			{
				var wrappedNodes = findNodes(keyword);
				
				// expand all
				// if keyword is empty, all nodes are collapsed, otherwise, all are expanded
				window.ide.dirtree.expandAll(keyword);
				
				// hide all nodes
				$("ul#dir-tree li").hide();
				
				$("ul#dir-tree span").each(function() {
					
					if ($(this).text().toLowerCase().indexOf(keyword.toLowerCase()) >= 0) {
						//console.log("expand" + $(this).text());
						expandNode($(this));
					}
					
				});
				
				// first hide all nodes
				/*for (var nodeId in wrappedNodes)
				{
					var wrappedNode = wrappedNodes[nodeId];
					
					if (!wrappedNode.isVisible && wrappedNode.node.parent)
					{
						window.ide.dirtree.hideNode(wrappedNode.node);
					}
				}*/
				
				
				// expand all top nodes
				/*for (var i = 0; i < window.ide.dirtree.getNodes().length; i++)
				{
					window.ide.dirtree.expandNode(window.ide.dirtree.getNodes()[i], true, true, false);
				}*/
				
				//console.log("Wrapped nodes: " + JSON.stringify(wrappedNodes));
				
				// show visible nodes and their parents
				/*for (var nodeId in wrappedNodes)
				{
					var wrappedNode = wrappedNodes[nodeId];
					
					//console.log((wrappedNode.isVisible ? "Showing" : "Hiding") + " " + wrappedNode.node.name);
					
					if (wrappedNode.isVisible)
					{
						var parent = wrappedNode.parent;
						
						while (parent)
						{
							//console.log("Expand: " + JSON.stringify(wrappedNode.node));
							//window.ide.dirtree.expandNode(parent, true, true, false);
						
							window.ide.dirtree.hideNode(wrappedNode.node);
							parent = parent.parent;
						}
					}
				}*/
			}
			
			function findNodes (keyword, wrappedNodes, nodes, parent)
			{
				if (!wrappedNodes)
				{
					wrappedNodes = {};
				}
				
				if (keyword)
				{
					if (!nodes)
					{
						nodes = window.ide.dirtree.getNodes();
					}
					
					for (var i = 0; i < nodes.length; i++)
					{
						var node = nodes[i];
						
						var nodeId = node.id ? node.id : node.name;
						
						if (!wrappedNodes[nodeId])
						{
							wrappedNodes[nodeId] = {
								node: node,
								parent: parent,
								isVisible: false
							}
						}
						
						//console.log("check " + node.name);
						
						if (parent && node.name.toUpperCase().indexOf(keyword.toUpperCase()) > -1)
						{
							wrappedNodes[nodeId].isVisible = true;
						}
						
						if ($.isArray(node.children))
						{
							wrappedNodes = findNodes(keyword, wrappedNodes, node.children, node);
						}
					}
					
					return wrappedNodes;
					
					//console.log("Nodes: " + JSON.stringify(nodes));
					
					/*$("ul#dir-tree li").hide();
					$("ul#dir-tree li > a > span").each(function() {
						if ($(this).text().toUpperCase().indexOf(keyword.toUpperCase()) > -1) {
							expandNode($(this));
						}
					});*/
				}
			}
			
			function expandNode (elem)
			{
				elem.parentsUntil("ul#dir-tree").each(function() {
					$(this).show();
				});
			}
			
			function treeItemClick(event, treeId, treeNode)
			{	
				if (treeNode.id)
				{
					openFile(treeNode.id, treeNode.name, true);
				}
			}
			
			function addTabbarScroll()
			{
				$("#km-tabbar-container .scroll-left").click(function () {
			        $("#tabbar-scroll").animate({marginLeft: '-=100px'}, 0);
			    });

				$("#km-tabbar-container .scroll-right").click(function () {
					$("#tabbar-scroll").animate({ marginLeft: '+=100px' }, 0);
			    });
			}
			
		</script>
	
	</jsp:body>
</ko:ideLayout>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:processBuilderLayout title="Process builder">
	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.ui.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.utils.js"></script>
		<script src="https://use.fontawesome.com/f2870818a5.js"></script>
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/raphael.js"></script>
		<link href="${pageContext.request.contextPath}/resources/css/smoothness/jquery-ui-1.10.3.custom.min.css" rel="stylesheet" type="text/css" />
	
		<style>
		
			div#diagram {
				position: relative;
				height: 100%;
				width: 100%;
				font-size: 1rem;
			}
			
			div.gridbg {
 				background-color: transparent;
    			background-image: linear-gradient(0deg, transparent 24%, rgba(1, 84, 144, .05) 25%, rgba(1, 84, 144, .05) 26%, transparent 27%, transparent 74%, rgba(1, 84, 144, .05) 75%, rgba(1, 84, 144, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(1, 84, 144, .05) 25%, rgba(1, 84, 144, .05) 26%, transparent 27%, transparent 74%, rgba(1, 84, 144, .05) 75%, rgba(1, 84, 144, .05) 76%, transparent 77%, transparent);
  				height:100%;
  				background-size:50px 50px;
			}
			
			div#diagram-wrapper {
				background-image: linear-gradient(#ffffff, #dedede);
				height: 800px;
				overflow: auto;
			}
			
			svg {
				width: 100%;
				height: 100%
			}
		
			table#process-table {
				width: 100%;
				border-collapse: collapse;
			}
			
			table#process-table > tbody > tr > td {
				vertical-align: top;
				padding: 0;
			}
			
			#bp-toolbar {
				width: 15%;
			    position: absolute;
			    background: #ffffff;
			    z-index: 1;
			    border: 1px solid #e0e0e0;
			    box-shadow: 0 2px 4px 0 #d4d4d4;
			}
			
			table#process-table td#bp-details {
				width: 20%;
				background: #fff;
				border-left: 1px solid #ccc;
			}
			
			div.bp-action-icon, div.bp-trans-button {
				border: 1px solid #ddd;
				cursor: pointer;
				padding: 0.7em;
			    font-size: 0.9rem;
			    display: inline-block;
			    border-radius: 0.2em;
			    margin-bottom: 0.2em;
			    background-color: #fff;
			    color: #fff;
			    width: 100%;
			    box-sizing: border-box;
			}
			
			div.bp-trans-button-in {
				background: #3cb0fd;
				background-image: -webkit-linear-gradient(top, #3cb0fd, #3498db);
				background-image: -moz-linear-gradient(top, #3cb0fd, #3498db);
				background-image: -ms-linear-gradient(top, #3cb0fd, #3498db);
				background-image: -o-linear-gradient(top, #3cb0fd, #3498db);
				background-image: linear-gradient(to bottom, #3cb0fd, #3498db);
				text-decoration: none;
			}
			
			div.bp-action-icon:hover, div.bp-trans-button:hover {
				border: 1px solid #ccc;
				box-shadow: 0 0 0.5em rgb(187, 187, 187);
				background-color: #fffedf;
			}
			
			div.bp-action-panel {
				padding: 0 1em 1em 1em;
			}
			
			div#bp-process-details {
				padding: 1em;
				font-size: 0.85rem
			}
			
			div#bp-process-details input#process-name, div#bp-process-details input#process-label {
				min-width: 10em;
				width: 100%;
			}
			
			div#bp-process-details input.process-save-btn {
				margin-top: 1em;
			}
			
			div#bp-process-details label.draft-process-label {
				margin-top: 0.6em;
				display: block;
			}
			
			td#bp-details {
				padding-left: 1em;
			}
			
			div.inv-box {
				border: 1px solid #dcdcdc;
				border-radius: 0.2em;
				background-color: #fff;
				cursor: move;
				overflow: hidden;
				position: absolute;
			}
			
			div.inv-box:hover {
				box-shadow: 0 0 1em rgb(187, 187, 187);
			}
			
			div.inv-props {
				font-size: 0.9rem;
			}
			
			div.inv-props .inv-attrs > ul {
				list-style-type: none;
				font-size: 0.75rem;
				padding: 0;
				margin: 0;
				cursor: default;
			}
			
			div.inv-props .inv-attrs > ul i.attr-save-check {
				color: green;
				font-size: 1em;
				cursor: pointer;
			}
			
			i.km-fa-check {
				color: green;
				font-size: 1em;
				cursor: pointer;
			}
			
			div.inv-props div.param-section-title {
				color: #26648a;
			    background-color: #e6e6e6;
			    padding: 1em;
			    padding: 0.5em;
			    margin: 0 1em;
			    border-radius: 0.3em;
			}
			
			div.inv-props .inv-attrs > ul > li {
				display: inline-block;
				width: 100%;
				box-sizing: border-box;
			}
			
			div.inv-props .inv-attrs > ul > li {
				padding: 0.5em 0 0.5em 0;
			}
			
			div.inv-props .inv-attrs > ul > li .attr-input {
				display: inline-block;
				width: 45%;
				min-width: 0;
				margin-right: 0.5em;
				font-size: 0.75rem;
			}
			
			div.inv-box div.inv-name {
				font-weight: bold;
				font-size: 0.9em;
				box-sizing: border-box;
				width: 100%;
				padding: 0.5em 1.1em;
				color: #525252;
				text-align: center;
				background-color: #fff7bf;
				border-bottom: 1px solid #ccc;
				position: relative;
			}
			
			div.inv-box div.inv-name > span {
				cursor: pointer;
			}
			
			input.bp-inv-name-input {
				border: none;
				padding: 0;
				background-color: transparent;
				color: #525252;
				text-align: center;
				font-weight: bold;
				box-sizing: border-box;
				width: 100%; 
			}
			
			ul.param-list {
				list-style-type: none;
				margin: 0;
				cursor: default;
				padding: 0;
			}
			
			ul.param-list > li {
				padding: 0.5em;
			}
			
			ul.param-list > li:LAST_CHILD {
				border-bottom; none;
			}
			
			ul.side-param-list {
				list-style-type: none;
				padding: 0;
				cursor: default;
				float: left;
			}
			
			div#invocation-details-container ul.param-list > li {
				padding: 0.5em 0 0.5em 0;
			}
			
			ul.param-list > li .param-header {
				color: #5f5f5f;
			}
			
			ul.param-list > li div.param-name-line {
				padding: 0.5em;
			}
			
			div#invocation-details-container ul.param-list > li div.param-name-line {
				padding: 0.5em 1em;
			}
			
			ul.param-list > li > .param-dt {
				color: #5f5f5f;
				display: inline;
				width: 100%;
    			text-overflow: ellipsis;
    			margin-top: 0.3em;
    			margin-left: 0.3em;
			}
			
			ul.param-list > li > span {
				cursor: pointer;
			}
			
			div.inv-action-name {
				color: #6b6b6b;
			    padding: 0.7em;
			    font-size: 0.7em;
			    text-align: center;
			    border-bottom: 1px solid #ddd;
			    position: relative;
			}
			
			ul.param-list > li > div.selected-param {
				background-color: #ffb59f;
			}
			
			div.param-assignments, div#process-params {
				background-color: #fff;
				margin-bottom: 0.5rem;
				margin-top: 2rem
			}
			
			div.right-section-title {
			    color: #26648a;
			    background: #ececec;
			    padding: 0.5em 1em;
			    border-radius: 0.3em;
			}
			
			div.param-assignments > ul, div#process-params > ul {
				list-style-type: none;
				padding: 0;
				margin: 0;
			}
			
			div.param-assignments > ul > li {
				display: inline-block;
				padding: 1em 0 1em 0;
			}
			
			div#process-params > ul > li {
				display: inline-block;
				padding: 0.5em 0 0.5em 0;
			}
			
			i.assignment-arrow {
				padding: 0 1em 0 1em;
    			color: #668e43;
			}
			
			i.inline-assignment-arrow {
    			color: #668e43;
    			margin: 0 0.5rem 0 0;
    			font-size: 0.8rem;
			}
			
			i.inline-assignment-close {
				float: right;
				color: #aaa;
				cursor: pointer;
			}
			
			i.inline-assignment-close:hover {
				color: #666;
			}
			
			i.close, i.remove-inv-btn {
				color: #9c9c9c;
			    cursor: pointer;
			    font-size: 1em;
			}
			
			div.inline-param-assignment {
				padding: 0 0.5em 0.5em 0.5em;
				display: none;
			}
			
			i.close {
				margin-left: 1em;
			}
			
			i.close:hover {
				color: #333;
			}
			
			div.bp-action-panel div.title {
				font-size: 1.2em;
			    margin: 1em 0;
			    padding-bottom: 0.5em;
			    border-bottom: 1px solid #e4e4e4;
			    color: #5f5f5f;
			}
			
			div.inline-assignment-param {
				display: inline;
				margin-left: 0.3em;
			}
			
			div.inline-assignment-wrapper {
				padding: 0.4em 0 0.4em 0;
			}
			
			div#invocation-details-container {
				background-color: #fff;
			    margin-bottom: 1em;
			    display: none;
			}
			
			div.inv-details-name {
				font-size: 1rem;
			    color: rgb(48, 104, 132);
			    font-weight: bold;
			    padding: 1em;
			}
			
			div.inv-props div.attr-wrapper {
				display: table;
				width: 100%;
			}
			
			div.inv-props div.km-attrs-error > input {
				background-color: #ffe8e8;
			}
			
			div.inv-props div.km-attr-err-panel {
				color: #bf1818;
				padding: 0.5em 0;
			}
			
			div.inv-props div.attr-wrapper > label, div.inv-props div.attr-wrapper > i {
				display: table-cell;
			}
			
			div.rotate-45 {
				-webkit-transform: rotate(45deg);
				-moz-transform: rotate(45deg);
				-o-transform: rotate(45deg);
				-ms-transform: rotate(45deg);
				transform: rotate(45deg);
			}
			
			div.if-transition-circle {
				border-radius: 50%;
				width: 1em;
				height: 1em;
				background-color: #fff;
				border: 2px solid #d65858;
			}
			
			div.if-transition-handle {
				position: absolute;
				cursor: pointer;
				font-size: 1.4rem;
				font-weight: bold;
				color: #555;
			}
			
			div.if-transition-handle:hover {
				color: #ca2c1a;
			}
			
			input.process-name {
				margin-top: 0.7em
			}
			
			div.param-drop {
				border: 2px dashed #eee;
				font-size: 1rem;
				padding: 1.5em;
				margin: 1em 0 1em 0;
				cursor: pointer;
				text-align: center;
				color: #555;
			}
			
			div.param-drop:hover {
				border-color: #aaa;
			}
			
			div.dragged-param {
				background-color: #fff;
				border: 1px solid #ccc;
				border-radius: 0.2em;
				padding: 1em;
				font-size: 0.8em;
				cursor: pointer;
			}
			
			div.process-param-form > .title {
				font-size: 1.1em;
			    font-weight: bold;
			    color: #666;
			    margin-bottom: 0.5em;
			}
			
			div.process-param-form .source-line {
				font-size: 0.75rem
			}
			
			div.process-param-form {
				border: 2px dashed #eee;
				padding: 1.5em;
				margin: 1em 0 1em 0;
			}
			
			i.process-param-arrow {
				color: green;
				margin: 0 1em 0 1em;
			}
			
			i.process-param-save-check {
				margin: 0 0 0 1em;
			}
			
			i.process-param-close {
				margin: 0 0 0 1em;
				color: #9c9c9c;
			    cursor: pointer;
			    font-size: 1em;
			}
			
			i.inv-close-icon {
				position: absolute;
				top: 0.3em;
				right: 0.3em;
			}
			
			i.param-large-arrow, i.if-large-arrow {
				font-size: 2rem;
				color: #aaa;
			}
			
			i.if-large-arrow {
				margin: 0 0.5rem 0 0.5rem
			}
			
			ul.side-param-list > li {
				font-size: 0.75rem;
				padding: 0.8em;
				background-color: #fff;
				border: 1px solid #ccc;
				border-radius: 0.2em;
				display: block;
				margin: 0.2em 0 0.2em 0;
				cursor: move;
			}
			
			i.fa {
				cursor: pointer;
			}
			
			i.inv-settings-btn {
				font-size: 1rem;
			    position: absolute;
			    right: 0.5em;
			    color: #888
			}
			
			div.param-wrapper {
				display: table;
				position: absolute;
			}
			
			div.param-wrapper div.cell {
				display: table-cell;
				vertical-align: middle;
				padding: 0.5rem
			}
			
			path:hover {
				cursor: pointer;
			}
			
			i.param-link {
				font-size: 0.9rem;
    			margin: 0 0.5em 0 0.5em;
    			color: #888;
    			display: none;
			}
			
			i.param-link-active {
				display: inline-block;
			}
			
			i.param-lines-visible {
				color: #000;
			}
			
			ul.param-assignment-list {
				list-style-type: none;
			    padding: 0;
			    margin: 0;
			}
			
			ul.param-assignment-list > li {
				padding: 0.4em 0 0.4em 1em;
			}
			
			i.delete-assignment-btn {
				color: #888;
				margin-left: 1rem;
				font-size: 0.5rem;
			}
			
			i.delete-assignment-btn:hover {
				color: #000;
			}
			
			div.bp-op-trans {
				width: auto;
			}
			
			div.bp-op-trans > i.trans-icon {
				font-size: 1.2rem;
			}
			
			div.if-condition-text-wrapper > textarea {
				width: 100%;
    			box-sizing: border-box;
    			border: 1px solid #ddd;
    			border-radius: 0.2em;
    			height: 8em;
			}
			
			div.if-condition-text-wrapper {
				padding: 0.5rem;
			}
			
			div.accepted-type-selection {
				padding: 0.5rem 1rem 0.5rem 1rem
			}
			
			div.accepted-type-selection input[type="text"], div.accepted-type-selection select {
				border: 1px solid #ccc;
				border-radius: 0.2em;
				padding: 0.5em;
    			box-sizing: border-box;
			}
			
			ul.accepted-types-list {
				margin-bottom: 0.5rem;
				padding: 0 1em;
			}
			
			div.inv-props ul.inv-attr-list {
				margin: 1rem;
			}
			
			textarea.km-err-input {
				border: 1px solid red !important;
			}
			
			div#invalid-query-msg {
				padding: 0.5rem;
    			color: rgb(255, 0, 0);
			}
			
			#bp-topmenu > ul {
				padding: 0;
				margin: 0;
			    background: #fff;
			    list-style-type: none;
			    color: #4a4a4a;
			}
			
			#bp-topmenu > ul > li {
				display: inline-block;
				padding: 0.7em;
				cursor: pointer;
			}
			
			#bp-topmenu > ul > li:hover {
				background: #eee;
			}
			
			#bp-topmenu > ul > li:FIRST-CHILD {
				padding-left: 2em;
			}
			
			#bp-topmenu {
				border-bottom: 1px solid #d6d6d6;
			}
			
			.action-btn {
			  background: #3498db;
			  background-image: -webkit-linear-gradient(top, #3498db, #2980b9);
			  background-image: -moz-linear-gradient(top, #3498db, #2980b9);
			  background-image: -ms-linear-gradient(top, #3498db, #2980b9);
			  background-image: -o-linear-gradient(top, #3498db, #2980b9);
			  background-image: linear-gradient(to bottom, #3498db, #2980b9);
			  -webkit-border-radius: 28;
			  -moz-border-radius: 28;
			}
			
			.action-btn:hover {
			  background: #3cb0fd;
			  background-image: -webkit-linear-gradient(top, #3cb0fd, #3498db);
			  background-image: -moz-linear-gradient(top, #3cb0fd, #3498db);
			  background-image: -ms-linear-gradient(top, #3cb0fd, #3498db);
			  background-image: -o-linear-gradient(top, #3cb0fd, #3498db);
			  background-image: linear-gradient(to bottom, #3cb0fd, #3498db);
			  text-decoration: none;
			}
			
			.inv-prop-wrapper {
				padding: 0.5em;
			}
			
		</style>
		
		<script>
		
			window.builder = {
					
				draggedItem: null,
				
				settings: {
					invocationsResizable: false,
					defaultSize: {
						width: "13em",
						height: "auto"
					}
				},
				
				anyRecordLabel: "Any Record",
				
				process: {
					name: null,
					id: null,
					invocations: [],
					transitions: [],
					paramAssignments: [],
					inputs: [],
					outputs: [],
					invocationsByActionId: {},
					invocationsByProcessId: {},
					newTransitionStart: null,
					newTransitionEnd: null,
					newTransitionMode: false,
					newParamAssignmentMode: false,
					newParamAssignmentStart: null,
					newParamAssignmentEnd: null,
					newTransitionLines: null,
					paramAssignmentLines: [],
					
					getTransitionType: function(trans) {
						
						var prevInv = trans.prevAction;
						if (prevInv.invokedAction.type !== "If")
						{
							return "Cannot determine transition type for non-if invocation";
						}
						
						if (!prevInv.attributes)
						{
							return null;
						}
							
						// check attributes of the action
						for (var i = 0; i < prevInv.attributes.length; i++)
						{
							if (prevInv.attributes[i].name === "ifTrueInvocationName" && prevInv.attributes[i].value === trans.nextAction.name)
							{
								return "true";
							}
							else if (prevInv.attributes[i].name === "ifFalseInvocationName" && prevInv.attributes[i].value === trans.nextAction.name)
							{
								return "false";
							}
						}
						
						throw "Undetermined transition type for transition " + trans.hash;
						
					},
					
					saveNewTransition: function(options) {
						
						var transition = {
							prevAction: this.getInvocationByHash(this.newTransitionStart),
							nextAction: this.getInvocationByHash(this.newTransitionEnd),
							hash: "transition-" + this.randomInt(1000000),
							label: this.newTransitionLabel
						}
						
						if (options && options.ifTransitionType)
						{
							transition.ifTransitionType = options.ifTransitionType;
						}
						
						var duplicateTransition = false;
						
						// make sure a transition with this start-end invocation combination does not exist
						for (var i = 0; i < this.transitions.length; i++)
						{
							var trans = this.transitions[i];
							
							if (trans.prevAction.hash === transition.prevAction.hash && trans.nextAction.hash === transition.nextAction.hash)
							{
								duplicateTransition = true;
								break;
							}
						}
						
						if (!duplicateTransition)
						{
							this.transitions.push(transition);
						}
						
						// automatch param assignments between the invocations for which transition is created
						this.updateParamAutoMatch(transition);
						
						this.cancelNewTransition();
						
						this.rerenderDiagram();
						
						return transition;
					},
					
					/**
					 * Adds custome functions to the invocation object
					 */
					addInvocationProperties: function(inv) {
						
						inv.getInput = function(input) {
							
							if (this.inputs)
							{
								for (var i = 0; i < this.inputs.length; i++)
								{
									if (this.inputs[i].name === input)
									{
										return this.inputs[i];
									}
								}
								
								return null;
							}
							
						};
						
						inv.getOutput = function(output) {
							
							if (this.outputs)
							{
								for (var i = 0; i < this.outputs.length; i++)
								{
									if (this.outputs[i].name === output)
									{
										return this.outputs[i];
									}
								}
								
								return null;
							}
							
						};
						
						return inv;
					},
					
					cancelNewTransition: function() {
						this.newTransitionMode = false;
						this.newTransitionStart = null;
						this.newTransitionEnd = null;
						this.newTransitionEndX = null;
						this.newTransitionEndY = null;
						
						if (this.newTransitionLine)
						{
							this.newTransitionLine.remove();
							this.newTransitionLine = null;
						}
						
						if (this.newTransitionLabelObj)
						{
							this.newTransitionLabelObj.remove();
							this.newTransitionLabelObj = null;
						}
						
						$(".bp-trans-button").removeClass("bp-trans-button-in");
					},
					
					addParamAssignment: function(assignment) {
						
						// make sure such assignment does not exist
						for (var i = 0; i < this.paramAssignments.length; i++)
						{
							var a = this.paramAssignments[i];
							
							if (a.source && a.source.invocation && a.target && a.target.invocation && assignment.source && assignment.target && a.source.invocation.hash === assignment.source.invocation.hash && a.target.invocation.hash === assignment.target.invocation.hash && a.source.param == assignment.source.param && a.target.param === assignment.target.param)
							{
								// duplicate assignment found
								return;
							}
						}
						
						this.paramAssignments.push(assignment);
						
					},
					
					saveNewParamAssignment: function() {
						
						var invalidAssignment = false;
						
						// make sure the source and target parameters are different
						if (this.newParamAssignmentStart.invocation && this.newParamAssignmentEnd.invocation && this.newParamAssignmentStart.invocation.hash === this.newParamAssignmentEnd.invocation.hash)
						{
							invalidAssignment = true;
						}
						
						var isSourceParamRaimmeType = this.newParamAssignmentStart.param.isCustomType || this.newParamAssignmentStart.param.dataTypeName === "kommet.basic.RecordProxy";
						var isValidTypeCast = isSourceParamRaimmeType && this.newParamAssignmentEnd.param.dataTypeName === "kommet.basic.RecordProxy";
						
						// make sure the types of the assigned parameters are the same
						if (this.newParamAssignmentStart.param.dataTypeLabel !== this.newParamAssignmentEnd.param.dataTypeLabel && !(isValidTypeCast))
						{
							invalidAssignment = true;
						}
						
						if (this.newParamAssignmentStart.type === this.newParamAssignmentEnd.type)
						{
							invalidAssignment = true;
						}
						
						if (!invalidAssignment)
						{
							var source = null;
							var target = null;
							
							if (this.newParamAssignmentStart.type === "input")
							{
								source = this.newParamAssignmentEnd;
								target = this.newParamAssignmentStart;
							}
							else
							{
								target = this.newParamAssignmentEnd;
								source = this.newParamAssignmentStart;
							}
							
							var assignment = {
								source: source,
								target: target,
								hash: "paramAssignment-" + this.randomInt(1000000)
							};
							
							// set actual data type on the output parameter
							this.setActualDataTypeForAssignmentTarget(assignment);
							
							this.addParamAssignment(assignment);
							
							km.js.ui.statusbar.show("Added parameter assignment", 5000);
						}
						else
						{
							km.js.ui.statusbar.err("Invalid parameter assignment", 5000);
						}
						
						this.cancelNewParamAssignment();
						
						this.showParamAssignments();
						
						this.rerenderDiagram();
					},
					
					setActualDataTypeForAssignmentTarget: function(a) {
						
						var targetInvocation = this.getInvocationByHash(a.target.invocation.hash);
						
						if (!targetInvocation)
						{
							throw "Did not find invocation with hash " + a.target.invocation.hash;
						}
						
						var srcParam = a.source != null ? this.getInvocationByHash(a.source.invocation.hash).getOutput(a.source.param.name) : a.processInput;
						
						var actualDataType = {
							typeId: srcParam.dataTypeId,
							typeName: srcParam.dataTypeLabel ? srcParam.dataTypeLabel : srcParam.dataTypeName
						};
						
						targetInvocation.getInput(a.target.param.name).actualDataType = actualDataType;
						return a;
					},
					
					removeParamAssignment: function(assignment) {
						
						var newAssignments = [];
						
						for (var i = 0; i < this.paramAssignments.length; i++)
						{
							if (this.paramAssignments[i].hash !== assignment.hash)
							{
								newAssignments.push(this.paramAssignments[i]);
							}
						}
						
						this.paramAssignments = newAssignments;
						
						// remove actual data type from target parameter
						var targetInvocation = this.getInvocationByHash(assignment.target.invocation.hash);
						
						if (!targetInvocation)
						{
							throw "Did not find invocation with hash " + assignment.target.invocation.hash;
						}
						
						// clear the actual data type
						targetInvocation.getInput(assignment.target.param.name).actualDataType = null;
						
					},
					
					cancelNewParamAssignment: function() {
						this.newParamAssignmentStart = null;
						this.newParamAssignmentEnd = null;
						this.newParamAssignmentMode = false;
						
						$(".selected-param").removeClass("selected-param");
					},
					
					addInvocation: function(blockId, posX, posY) {
						
						var setDataTypeLabels = function(inv) {
							
							if (inv.inputs)
							{
								for (var i = 0; i < inv.inputs.length; i++)
								{
									if (inv.inputs[i].dataTypeLabel === "kommet.basic.RecordProxy")
									{
										inv.inputs[i].dataTypeLabel = window.builder.anyRecordLabel;
									}	
								}
							}
							
							if (inv.outputs)
							{
								for (var i = 0; i < inv.outputs.length; i++)
								{
									if (inv.outputs[i].dataTypeLabel === "kommet.basic.RecordProxy")
									{
										inv.outputs[i].dataTypeLabel = window.builder.anyRecordLabel;
									}	
								}
							}
							
						};
						
						if (window.builder.toolbarActions[blockId])
						{	
							var action = window.builder.toolbarActions[blockId];
							
							var invocation = {
								invokedAction: action,
								callable: action,
								inputs: km.js.utils.cloneArr(action.inputs),
								outputs: km.js.utils.cloneArr(action.outputs),
								name: this.proposeInvocationName(action),
								attributes: [],
								position: {
									x: posX,
									y: posY
								},
								size: window.builder.settings.defaultSize,
								hash: "inv-" + this.randomInt(1000000),
								paramsVisible: false
							};
							
							// set data type labels on input and output params
							setDataTypeLabels(invocation);
							
							invocation = window.builder.process.addInvocationProperties(invocation);
							
							this.invocations.push(invocation);
							
							if (!this.invocationsByActionId[action.id])
							{
								this.invocationsByActionId[action.id] = [];
							}
							
							this.invocationsByActionId[action.id].push(invocation);
							
							if (action.isInitial)
							{
								if (action.inputs.length != 1)
								{
									throw "Initial action is expected to have exactly one input: " + JSON.stringify(action);
								}
								
								var singleInput = invocation.inputs[0];
								singleInput.invocation = invocation;
								
								// if it is an initial action, add its input parameters automatically to the process inputs
								window.builder.process.saveProcessInput(singleInput, "record");
							}
						}
						else if (window.builder.toolbarProcesses[blockId])
						{
							
						}
						
						// rerender process diagram
						this.rerenderDiagram();
						
						// show invocation details in side panel
						this.showInvocationDetails(invocation);
						
					},
					
					proposeInvocationName: function(action) {
						if (!this.invocationsByActionId[action.id])
						{
							return action.label + " 1";
						}
						else
						{
							return action.label + " " + (this.invocationsByActionId[action.id].length + 1);
						}
					},
					
					removeInvocation: function(inv) {
						
						var newInvocations = [];
						
						for (var i = 0; i < this.invocations.length; i++)
						{
							if (this.invocations[i].hash !== inv.hash)
							{
								newInvocations.push(this.invocations[i]);
							}
						}
						
						this.invocations = newInvocations;
						this.removeTransitionsForInvocation(inv);
						this.removeParamAssignmentsForInvocation(inv);
						
						$("#diagram div[id='inv-" + inv.hash + "']").remove();
						$("#diagram input-params-" + inv.hash).remove();
						$("#diagram output-params-" + inv.hash).remove();
					},
					
					removeTransitionsForInvocation: function(inv) {
						
						var newTransitions = [];
						
						for (var i = 0; i < this.transitions.length; i++)
						{
							var trans = this.transitions[i];
							if (trans.prevAction.hash !== inv.hash && trans.nextAction.hash !== inv.hash)
							{
								newTransitions.push(trans);
							}
						}
						
						this.transitions = newTransitions;
						
					},
					
					/**
					 * Find transitions that start from this invocation
					 */
					getTransitionsFromInvocation: function(inv) {
						
						var newTransitions = [];
						
						for (var i = 0; i < this.transitions.length; i++)
						{
							var trans = this.transitions[i];
							if (trans.prevAction.hash === inv.hash)
							{
								newTransitions.push(trans);
							}
						}
						
						return newTransitions;
						
					},
					
					deleteTransition: function(transition) {
						
						var newTransitions = [];
						
						for (var i = 0; i < this.transitions.length; i++)
						{
							var trans = this.transitions[i];
							if (transition.hash !== trans.hash)
							{
								newTransitions.push(trans);
							}
						}
						
						this.transitions = newTransitions;
						
						window.builder.drawing.paintLines();
						
					},
					
					/**
					 * Find all parameter assignments whose source is the given parameter.
					 */
					getParamAssignmentsFromParam: function(inv, paramName) {
						
						var assignments = [];
						
						for (var i = 0; i < this.paramAssignments.length; i++)
						{
							var a = this.paramAssignments[i];
							if (a.source && a.source.invocation && a.source.invocation.hash === inv.hash && a.source.param.name === paramName)
							{
								assignments.push(a);
							}
						}
						
						return assignments;
						
					},
					
					removeParamAssignmentsForInvocation: function(inv) {
						
						var newAssignments = [];
						
						for (var i = 0; i < this.paramAssignments.length; i++)
						{
							var assignment = this.paramAssignments[i];
							if ((!assignment.source || !assignment.source.invocation || assignment.source.invocation.hash !== inv.hash) && (!assignment.target || !assignment.target.invocation || assignment.target.invocation.hash !== inv.hash))
							{
								newAssignments.push(assignment);
							}
						}
						
						this.paramAssignments = newAssignments;
						
						// hide assignment lines if they are displayed
						for (var i = 0; i < inv.inputs.length; i++)
						{
							// hide line but do not repaint, because there are other lines that need to be hidden as well
							// and if we tried to render them, an error would be thrown because their corresponding
							// UI elements/invocations have already been removed
							window.builder.process.hideParamAssignmentLines(inv.inputs[i], false);
						}
						
						for (var i = 0; i < inv.outputs.length; i++)
						{
							window.builder.process.hideParamAssignmentLines(inv.outputs[i], false);
						}
						
					},
					
					updateInvocation: function(inv, oldInvocationName) {
						
						var newInvocations = [];
						
						for (var i = 0; i < this.invocations.length; i++)
						{
							if (this.invocations[i].hash === inv.hash)
							{
								newInvocations.push(inv);
							}
							else
							{
								newInvocations.push(this.invocations[i]);
							}
						}
						
						this.invocations = newInvocations;
						
						// if invocation name has changed, update all references to this name
						if (oldInvocationName !== inv.name)
						{
							// update if-condition transition attributes and if-condition expressions
							for (var i = 0; i < this.invocations.length; i++)
							{
								var currentInv = this.invocations[i];
								if (currentInv.invokedAction && currentInv.invokedAction.type === "If")
								{
									// iterate over attributes of this invocation
									if (!currentInv.attributes)
									{
										continue;
									}
									
									var newAttributes = [];
									
									for (var k = 0; k < currentInv.attributes.length; k++)
									{
										var attr = currentInv.attributes[k];
										
										if (attr.name === "condition" && attr.value.indexOf("{" + oldInvocationName + "}") > -1)
										{
											var re = new RegExp("\\{" + oldInvocationName + "\\}", "g");
											attr.value = attr.value.replace(re, "{" + inv.name + "}");
										}
										
										if (attr.value === oldInvocationName && (attr.name === "ifTrueInvocationName" || attr.name === "ifFalseInvocationName"))
										{
											attr.value = inv.name;
										}
										
										newAttributes.push(attr);
									}
									
									currentInv.attributes = newAttributes;
								}
							}
						}
						
					},
					
					getInvocationByHash: function(hash) {
						
						for (var i = 0; i < this.invocations.length; i++)
						{
							if (this.invocations[i].hash === hash)
							{
								return this.invocations[i];
							}
						}
						
						return null;
						
					},
					
					getParamAssignmentByHash: function(hash) {
						
						for (var i = 0; i < this.paramAssignments.length; i++)
						{
							if (this.paramAssignments[i].hash === hash)
							{
								return this.paramAssignments[i];
							}
						}
						
						return null;
						
					},
					
					randomInt: function (max) {
					    return Math.floor(Math.random() * (max + 1));
					},
					
					rerenderDiagram: function() {
						
						this.clearDiagram();
						
						for (var i = 0; i < this.invocations.length; i++)
						{
							this.renderInvocation(this.invocations[i]);
						}
						
						window.builder.drawing.paintLines();
						
						this.showParamAssignments();
						
					},
					
					rerenderInvocation: function(inv) {
						$("#inv-" + inv.hash).remove();
						this.renderInvocation(inv);
					},
					
					showParamAssignments: function() {
						this.showProcessParamPanel();
					},
					
					/*getInlineParamAssignments: function() {
						
						var getSourceParam = function(assignment) {
							
							var wrapper = $("<div></div>").addClass("inline-assignment-wrapper");
							
							var arrow = $("<i class=\"inline-assignment-arrow fa fa-arrow-left\" aria-hidden=\"true\"></i>");
							var targetInvocation = $("<div></div>").text(assignment.source.invocation.name + ": " + assignment.source.param.name);
							targetInvocation.addClass("inline-assignment-param");
							wrapper.append(arrow).append(targetInvocation);
							
							var removeBtn = $("<i class=\"inline-assignment-close fa fa-close\" aria-hidden=\"true\"></i>");
							
							removeBtn.click((function(a) {
								
								return function() {
									window.builder.process.removeParamAssignment(a);
									window.builder.process.showParamAssignments();
									
									// prevent selecting this param
									e.stopPropagation();
								}
								
							})(assignment));
							
							wrapper.append(removeBtn);
							
							return wrapper;
						}
						
						var getTargetParam = function(assignment) {
							
							var wrapper = $("<div></div>").addClass("inline-assignment-wrapper");
							
							var arrow = $("<i class=\"inline-assignment-arrow fa fa-arrow-right\" aria-hidden=\"true\"></i>");
							var sourceInvocation = $("<div></div>").text(assignment.target.invocation.name + ": " + assignment.target.param.name);//.attr("id", "source-inv-" + assignment.source.invocation.hash + "-" + input.name);
							sourceInvocation.addClass("inline-assignment-param");
							wrapper.append(arrow).append(sourceInvocation);
							
							var removeBtn = $("<i class=\"inline-assignment-close fa fa-close\" aria-hidden=\"true\"></i>");
							
							removeBtn.click((function(a) {
								
								return function(e) {
									window.builder.process.removeParamAssignment(a);
									window.builder.process.showParamAssignments();
									
									// prevent selecting this param
									e.stopPropagation();
								}
								
							})(assignment));
							
							wrapper.append(removeBtn);
							
							return wrapper;
						}
						
						for (var i = 0; i < this.paramAssignments.length; i++)
						{
							var a = this.paramAssignments[i];
							
							if (!a.target || !a.source)
							{
								continue;
							}
							
							if (a.target.invocation)
							{
								var assignmentContainer = $("#diagram #inline-param-assignment-" + a.target.invocation.hash + "-" + a.target.param.id);
								assignmentContainer.append(getSourceParam(a)).show();
							}
							
							if (a.source.invocation)
							{
								var assignmentContainer = $("#diagram #inline-param-assignment-" + a.source.invocation.hash + "-" + a.source.param.id);
								assignmentContainer.append(getTargetParam(a)).show();
							}
						}
						
					},*/
					
					addProcessInputForm: function(input) {
						this.addProcessParamForm(input, "input");
					},
					
					addProcessOutputForm: function(output) {
						this.addProcessParamForm(output, "output");
					},
					
					addProcessParamForm: function(param, type) {
						
						var form = $("<div></div>").addClass("process-param-form");
						
						var sourceLine = $("<div></div>").addClass("source-line").append($("<span></span>").text(type === "input" ? "source: " : "target: "));
						var sourceParam = $("<span></span>").text(this.getInvocationByHash(param.invocation.hash).name + ": " + param.name);
						sourceLine.append(sourceParam);
						
						var saveBtn = $("<i class=\"process-param-save-check km-fa-check fa fa-check\" aria-hidden=\"true\"></i>");
						sourceLine.append(saveBtn);
						
						form.append(sourceLine);
						
						var textField = $("<input></input>").addClass("std-input").attr("placeholder", "parameter name").css("margin-top", "1em");
						form.append(textField);
						
						$("#process-" + type + "s div.param-drop").hide().after(form);
						
						saveBtn.click((function(param, textField, type) {
							
							return function() {
								
								if (textField.val())
								{
									if (type === "input")
									{
										window.builder.process.saveProcessInput(param, textField.val());
									}
									else
									{
										window.builder.process.saveProcessOutput(param, textField.val());
									}
									window.builder.process.showProcessParamPanel();
								}
								
							}
							
						})(param, textField, type));
						
						var closeBtn = $("<i class=\"process-param-close fa fa-close\" aria-hidden=\"true\"></i>");
						closeBtn.click((function() {
							
							return function() {
								window.builder.process.showProcessParamPanel();
							}
							
						})());
						
						sourceLine.append(closeBtn)
						
					},
					
					saveProcessInput: function(input, paramName) {
						
						var processInput = {
							name: paramName,
							dataTypeId: input.dataTypeId,
							dataTypeName: input.dataTypeName,
							hash: "process-input-" + this.randomInt(1000000)
						}
						
						// add input to process
						this.inputs.push(processInput);
						
						// add param assignment between the source input and the process input
						var assignment = {
							processInput: processInput,
							target: {
								param: input,
								invocation: this.getInvocationByHash(input.invocation.hash),
								type: null
							},
							hash: "paramAssignment-" + this.randomInt(1000000)
						};
						
						this.addParamAssignment(assignment);
						
					},
					
					showProcessParamPanel: function() {
						$("div[id='process-params']").empty();
						this.showProcessInputPanel();
						this.showProcessOutputPanel();
					},
					
					showProcessInputPanel: function() {
					
						var inputPanel = $("<div></div>").addClass("process-params").attr("id", "process-inputs")
						inputPanel.append($("<div></div>").text("Process input parameters").addClass("title right-section-title"));
						
						var dropParam = $("<div></div>").text("Drop parameter here").addClass("param-drop");
						inputPanel.append(dropParam);
						
						dropParam.droppable({
							drop: function() {
								
								var draggedParam = window.builder.process.draggedParam;
								if (!draggedParam)
								{
									return;
								}
								
								window.builder.process.addProcessInputForm(draggedParam);
								window.builder.process.draggedParam = null;
							}
						});
						
						var ul = $("<ul></ul>");
						
						for (var i = 0; i < this.inputs.length; i++)
						{
							var param = this.inputs[i];
							
							// find all param assignments for this process param
							for (var k = 0; k < this.paramAssignments.length; k++)
							{
								var a = this.paramAssignments[k];
								
								if (a.processInput && a.processInput.hash === param.hash)
								{
									var li = $("<li></li>");
									li.append(param.name);
									
									var arrow = $("<i class=\"fa fa-arrow-right\" aria-hidden=\"true\"></i>").addClass("process-param-arrow");
									li.append(arrow);
									
									var target = $("<span></span>").text(a.target.invocation.name + ": " + a.target.param.name);
									li.append(target)
									
									var closeBtn = $("<i class=\"close fa fa-close\" aria-hidden=\"true\"></i>");
									closeBtn.click((function(assignment) {
										
										return function() {
											window.builder.process.removeProcessInput(assignment);
											window.builder.process.showProcessParamPanel();
										}
										
									})(a));
									
									li.append(closeBtn);
									
									ul.append(li);		
								}
							}
						}
						
						inputPanel.append(ul);
						$("div[id='process-params']").append(inputPanel);
						
					},
					
					showProcessOutputPanel: function() {
						
						var outputPanel = $("<div></div>").addClass("process-params").attr("id", "process-outputs")
						outputPanel.append($("<div></div>").text("Process output parameters").addClass("title right-section-title"));
						
						var dropParam = $("<div></div>").text("Drop parameter here").addClass("param-drop");
						outputPanel.append(dropParam);
						
						dropParam.droppable({
							drop: function() {
								
								var draggedParam = window.builder.process.draggedParam;
								if (!draggedParam)
								{
									return;
								}
								
								window.builder.process.addProcessOutputForm(draggedParam);
								window.builder.process.draggedParam = null;
							}
						})
						
						var ul = $("<ul></ul>");
						
						for (var i = 0; i < this.outputs.length; i++)
						{
							var param = this.outputs[i];
							
							// find all param assignments for this process param
							for (var k = 0; k < this.paramAssignments.length; k++)
							{
								var a = this.paramAssignments[k];
								
								if (a.processOutput && a.processOutput.hash === param.hash)
								{
									var li = $("<li></li>");
									li.append(param.name);
									
									var arrow = $("<i class=\"fa fa-arrow-left\" aria-hidden=\"true\"></i>").addClass("process-param-arrow");
									li.append(arrow);
									
									var target = $("<span></span>").text(a.source.invocation.name + ": " + a.source.param.name);
									li.append(target)
									
									var closeBtn = $("<i class=\"close fa fa-close\" aria-hidden=\"true\"></i>");
									closeBtn.click((function(assignment) {
										
										return function() {
											window.builder.process.removeProcessOutput(assignment);
											window.builder.process.showProcessParamPanel();
										}
										
									})(a));
									
									li.append(closeBtn);
									
									ul.append(li);		
								}
							}
						}
						
						outputPanel.append(ul);
						$("div[id='process-params']").append(outputPanel);
						
					},
					
					removeProcessInput: function(assignment) {
						
						var newInputs = [];
						for (var i = 0; i < this.inputs.length; i++)
						{
							if (this.inputs[i].hash !== assignment.processInput.hash)
							{
								newInputs.push(this.inputs[i]);
							}
						}
						
						this.inputs = newInputs;
						
						// remove param assignments for this process input
						this.removeParamAssignment(assignment);
					},
					
					rerenderLines: function() {
						
						window.builder.drawing.paintLines();
						
					},
					
					clearDiagram: function() {
					
						$("#diagram div.inv-box").remove();
						$("#diagram div.param-wrapper").remove();
						$("#diagram div.if-transition-handle").remove();
						window.builder.drawing.paper.clear();
					
					},
					
					/**
					 * @public
					 * Displays the details of the invocation in right-hand side panel
					 */
					showInvocationDetails: function(inv) {
						var propertyPanel = window.builder.process.getInvocationProperties(inv);
						var title = $("<div></div>").text(inv.name).addClass("inv-details-name");
						
						// hide process details
						$("#process-details-container").hide();
						$("#invocation-details-container").hide().empty().append(title).append(propertyPanel).fadeIn(500);
					},
					
					renderInvocation: function(inv) {
						
						var box = $("<div></div>").addClass("inv-box");
						box.attr("id", "inv-" + inv.hash);
						
						var name = $("<div></div>").addClass("inv-name");
						
						var invLabel = $("<span></span>").text(inv.name);
						
						var closeBtn = $("<i class=\"close inv-close-icon fa fa-close\" aria-hidden=\"true\"></i>");
						closeBtn.click((function(inv) {
							
							return function() {
								window.builder.process.removeInvocation(inv);
								window.builder.process.rerenderDiagram();
							}
							
						})(inv));
						name.append(closeBtn);
						
						name.append(invLabel);
						
						km.js.ui.editable({
							inputName: "invName" + inv.hash,
							inputId: "invName" + inv.hash,
							cssClass: "bp-inv-name-input",
							target: invLabel,
							onAccept: ((function(inv, closeBtn) {
								
								return function(val) {
									
									var oldName = inv.name;
									inv.name = val;
									window.builder.process.updateInvocation(inv, oldName);
									
									// also update invocation name is side panel
									$("#invocation-details-container .inv-details-name").text(inv.name);
									
									closeBtn.show();
								}
								
							})(inv, closeBtn)),
							
							onActivate: ((function(closeBtn) {
								
								return function() {
									closeBtn.hide();
								}
								
							})(closeBtn))
						});
						
						box.append(name);
						
						var actionName = $("<div></div>").addClass("inv-action-name").text(inv.invokedAction.callableType === "action" ? inv.invokedAction.name : inv.invokedAction.label);
						
						var gearBtn = $("<i class=\"inv-settings-btn fa fa-gears\" aria-hidden=\"true\"></i>");
						gearBtn.click((function(inv) {
							
							return function() {
								window.builder.process.toggleInvocationParams(inv.hash);
							}
							
						})(inv));
						actionName.append(gearBtn);
						
						box.append(actionName);
						
						box.click((function(inv) {
							
							return function(event) {
								if (window.builder.process.newTransitionMode)
								{
									if (!window.builder.process.newTransitionStart)
									{
										// start drawing a transition if it's not an if-condition
										// because transitions from if conditions can only start at the "true" and "false" handles, not at the if-box itseld
										if (inv.invokedAction.type !== "If")
										{
											window.builder.process.newTransitionStart = inv.hash;
										}
									}
									else
									{
										// finish transition
										if (window.builder.process.newTransitionStart === inv.hash)
										{
											// invalid node - same same start
										}
										else
										{
											window.builder.process.newTransitionEnd = inv.hash;
											window.builder.process.saveNewTransition();
										}
									}
								}
								
								event.stopPropagation();
							}
							
						})(inv));
						
						// wrap the whole invocation box in a wrapper
						this.addInvocationParams(inv);
						
						$("#diagram").append(box);
						
						box.click((function(inv) {
							
							return function() {
								window.builder.process.showInvocationDetails(inv);
							}
							
						})(inv));
						
						box.css("top", inv.position.y);
						box.css("left", inv.position.x);
						
						// height may not be specified
						if (inv.size.height)
						{
							box.css("height", inv.size.height);
						}
						
						box.css("width", inv.size.width);
						
						// it is essential that the .draggable() be called AFTER the box is appended to the document
						// this way jQuery won't override the position: absolute setting with relative and the boxes will be positioned correctly
						box.draggable({
							
							drag: (function(inv) {
								return function() {
									
									window.builder.process.paintInvocationParams(inv.hash);
									
									if (inv.invokedAction.type === "If")
									{
										window.builder.process.positionIfHandles(inv.hash)
									}
									
									window.builder.process.rerenderLines();
								}
							})(inv),
							
							stop: (function(invHash) {
								
								return function(event, ui) {
									var inv = window.builder.process.getInvocationByHash(invHash);
									inv.position = {
										x: parseInt(Math.ceil(ui.helper.position().left)),
										y: parseInt(Math.ceil(ui.helper.position().top))
									}
									
									window.builder.process.paintInvocationParams(inv.hash);
									
									//window.builder.process.rerenderDiagram();
								}
								
							})(inv.hash)
						
						});
						
						if (window.builder.settings.invocationsResizable === true)
						{
							box.resizable({
								
								stop: (function(inv) {
									
									return function() {
										var changedInv = window.builder.process.getInvocationByHash(inv.hash);
										changedInv.size = {
											width: $(this).width(),
											height: $(this).height()
										};
										window.builder.process.updateInvocation(changedInv);
									}
									
								})(inv),
							});
						}
						
						box.droppable({
							drop: (function(inv) {
								return function(event, ui) {
						        	var ifTransition = window.builder.process.draggedIfTransition;
						        	
						        	if (!ifTransition)
						        	{
						        		return;
						        	}
						        	
						        	window.builder.process.newTransitionMode = true;
						        	window.builder.process.newTransitionStart = ifTransition.invocation.hash;
						        	window.builder.process.newTransitionEnd = inv.hash;
						        	window.builder.process.newTransitionLabel = ifTransition.transitionType;
						        	
						        	var newTransition = window.builder.process.saveNewTransition({
						        		ifTransitionType: ifTransition.transitionType 
						        	});
						        	
						        	// remove the if-condition handle for this condition
						        	$("#if-transition-" + ifTransition.transitionType + "-" + ifTransition.invocation.hash).remove();
						        	
						        	// add an attribute for the true transition to the if-condition invocation
						        	var ifInvocation = window.builder.process.getInvocationByHash(ifTransition.invocation.hash);
						        	ifInvocation.attributes.push({
						        		
						        		name: ifTransition.transitionType === "true" ? "ifTrueInvocationName" : "ifFalseInvocationName",
						        		value: newTransition.nextAction.name,
						        		hidden: true
						        		
						        	});
						        	
						        	window.builder.process.draggedIfTransition = null;
						        	
						        	window.builder.process.rerenderDiagram();
								}
							})(inv)
						});
						
						if (inv.invokedAction.type === "If")
						{
							this.renderIfCondition(inv, box);
						}
						
						if (inv.paramsVisible)
						{
							this.showInvocationParams(inv.hash);
						}
					},
					
					/**
					 * Checks if parameters between the two invocations can be matched automatically based on
					 * their data types
					 */
					updateParamAutoMatch: function(transition) {
						
						if (!transition.prevAction)
						{
							throw "Prev action on transition not set";
						}
						
						if (!transition.nextAction)
						{
							throw "Next action on transition not set";
						}
						
						var prevAction = transition.prevAction;
						var nextAction = transition.nextAction;
						
						var areParamsAssigned = function(sourceParam, destParam) {
							
							for (var i = 0; i < window.builder.process.paramAssignments.length; i++)
							{
								var a = window.builder.process.paramAssignments[i];
								if (a.source && a.source.invocation.hash === sourceParam.invocation.hash && a.source.param.id === sourceParam.id &&
									a.target && a.target.invocation.hash === destParam.invocation.hash && a.target.param.id === destParam.id)
								{
									return true;
								}
							}
							
						};
						
						var assignParams = function(sourceParam, destParam) {
							
							window.builder.process.newParamAssignmentStart = {
								param: sourceParam,
								invocation: sourceParam.invocation,
								type: "output"
							};
							
							window.builder.process.newParamAssignmentEnd = {
								param: destParam,
								invocation: destParam.invocation,
								type: "input"
							};
							
							window.builder.process.saveNewParamAssignment();
							
						};
						
						for (var i = 0; i < prevAction.outputs.length; i++)
						{
							var output = prevAction.outputs[i];
							if (!output.dataTypeLabel)
							{
								throw "Data type label not set on output: " + JSON.stringify(output);
							}
							
							// try to match with every input of the next action
							for (var i = 0; i < nextAction.inputs.length; i++)
							{
								var input = nextAction.inputs[i];
								if (!input.dataTypeLabel)
								{
									throw "Data type label not set on input: " + JSON.stringify(input);
								}
								
								//var outputDataType = output.dataTypeLabel ? output.dataTypeLabel : (output.dataTypeName ? output.dataTypeName : window.builder.typesById[output.dataTypeId].qualifiedName);
								
								var isValidTypeAssignment = (output.dataTypeLabel === input.dataTypeLabel) || (output.isCustomType && input.dataTypeName === "kommet.basic.RecordProxy");
								
								// if params match, but we need to check if such assignment does not already exists
								if (isValidTypeAssignment && !areParamsAssigned(output, input))
								{
									// create an assignment
									assignParams(output, input);
								}
							}
						}
						
					},
					
					paintInvocationParams: function(invHash) {
						
						var boxHeight = $("#inv-" + invHash).outerHeight();
						
						// set position of the input and output params
						$("#input-params-" + invHash).css("top", $("#inv-" + invHash).position().top + (boxHeight - $("#input-params-" + invHash).outerHeight()) / 2);
						$("#input-params-" + invHash).css("left", $("#inv-" + invHash).position().left - $("#input-params-" + invHash).outerWidth());
						
						$("#output-params-" + invHash).css("top", $("#inv-" + invHash).position().top + (boxHeight - $("#output-params-" + invHash).outerHeight()) / 2);
						$("#output-params-" + invHash).css("left", $("#inv-" + invHash).position().left + $("#inv-" + invHash).outerWidth());
					},
					
					positionIfHandles: function(invHash) {
						
						var box = $("#inv-" + invHash);
						
						// get the bottom corners of the if invocation
						var topLeft = {
							top: box.position().top,
							left: box.position().left
						}
						
						var topRight = {
							top: box.position().top,
							left: box.position().left + box.width()
						}
						
						var trueHandle = $("#if-handle-" + invHash + "-true");
						var falseHandle = $("#if-handle-" + invHash + "-false");
						
						trueHandle.css("top", topRight.top + (box.height() - trueHandle.height()) / 2).css("left", topLeft.left - trueHandle.outerWidth() - 10);
						falseHandle.css("top", topLeft.top + (box.height() - trueHandle.height()) / 2).css("left", topRight.left + 10);
					},
					
					showInvocationParams: function(invHash) {
						
						this.paintInvocationParams(invHash);
						
						var inv = this.getInvocationByHash(invHash);
						inv.paramsVisible = true;
						
						$("#input-params-" + invHash).show();
						$("#output-params-" + invHash).show();
					},
					
					hideInvocationParams: function(invHash) {
						
						if (this.getInvocationByHash(invHash).paramsVisible === false)
						{
							// already hidden
							return;
						}
						
						this.getInvocationByHash(invHash).paramsVisible = false;
						
						$("#input-params-" + invHash).hide();
						$("#output-params-" + invHash).hide();
						
					},
					
					toggleInvocationParams: function(invHash) {
						
						var box = $("#inv-" + invHash);
						
						var inv = this.getInvocationByHash(invHash);
						
						if (this.getInvocationByHash(invHash).paramsVisible)
						{
							this.hideInvocationParams(invHash);
							
							for (var i = 0; i < inv.inputs.length; i++)
							{
								window.builder.process.hideParamAssignmentLines(inv.inputs[i], true);
							}
							
							for (var i = 0; i < inv.outputs.length; i++)
							{
								window.builder.process.hideParamAssignmentLines(inv.outputs[i], true);
							}
						}
						else
						{
							this.showInvocationParams(invHash);
							
							for (var i = 0; i < inv.inputs.length; i++)
							{
								window.builder.process.showParamAssignmentLines(inv.inputs[i]);
							}
							
							for (var i = 0; i < inv.outputs.length; i++)
							{
								window.builder.process.showParamAssignmentLines(inv.outputs[i]);
							}
						}
						
						window.builder.drawing.paintLines();
						
					},
					
					addInvocationParams: function(inv) {
						
						var arrow = $("<i class=\"param-large-arrow fa fa-arrow-right\" aria-hidden=\"true\"></i>");
						
						if (inv.inputs && inv.inputs.length)
						{
							var inputParamWrapper = $("<div></div>").addClass("param-wrapper").attr("id", "input-params-" + inv.hash);
							var inputParamCell = $("<div></div>").addClass("cell");
							inputParamCell.append(this.getInvocationParams(inv, "input"));
							inputParamWrapper.append(inputParamCell);
							
							// append arrow
							inputParamWrapper.append($("<div></div>").addClass("cell").append(arrow.clone())).css("display", "none");
							
							$("#diagram").append(inputParamWrapper);
						}
						
						if (inv.outputs && inv.outputs.length)
						{
							var outputParamWrapper = $("<div></div>").addClass("param-wrapper").attr("id", "output-params-" + inv.hash);
							var outputParamCell = $("<div></div>").addClass("cell");
							
							// append arrow
							outputParamWrapper.append($("<div></div>").addClass("cell").append(arrow.clone())).css("display", "none");
							
							outputParamCell.append(this.getInvocationParams(inv, "output"));
							outputParamWrapper.append(outputParamCell);
							
							$("#diagram").append(outputParamWrapper);
						}
				
					},
					
					hasParamAssignments: function(param) {
						
						for (var i = 0; i < this.paramAssignments.length; i++)
						{
							var a = this.paramAssignments[i];
							if (a.source && a.source.invocation.hash === param.invocation.hash && a.source.param.id === param.id)
							{
								return true;
							}
							
							if (a.target && a.target.invocation.hash === param.invocation.hash && a.target.param.id === param.id)
							{
								return true;
							}
						}
						
						return false;
						
					},
					
					hasAttributeAssignments: function(param) {
						
						var inv = this.getInvocationByHash(param.invocation.hash);
						
						if (inv.attributes)
						{
							for (var i = 0; i < inv.attributes.length; i++)
							{
								if (inv.attributes[i].name === param.name)
								{
									return true;
								}
							}
						}
						
						return false;
						
					},
					
					hideParamAssignmentLines: function(param, repaint) {
						
						var newParamLines = [];
						for (var i = 0; i < window.builder.process.paramAssignmentLines.length; i++)
						{
							if (window.builder.process.paramAssignmentLines[i].id !== param.id)
							{
								newParamLines.push(window.builder.process.paramAssignmentLines[i]);
							}
						}
						
						window.builder.process.paramAssignmentLines = newParamLines;
						
						if (repaint)
						{
							// repaint the lines, so that the assignment line will be hidden
							window.builder.drawing.paintLines();
						}
					},
					
					showParamAssignmentLines: function(param) {
						window.builder.process.paramAssignmentLines.push(param);
						
						// show assignment lines for this param
						window.builder.drawing.drawParamAssignmentLines(param);
					},
					
					getInvocationParams(inv, direction) {
						
						var paramList = $("<ul></ul>").addClass("side-param-list");
						
						var params = direction === "input" ? inv.inputs : inv.outputs;
						
						for (var k = 0; k < params.length; k++)
						{
							var param = params[k];
							
							// assign the invocation, but just the hash to avoid circular reference when this structure is parsed to string
							param.invocation = {
								hash: inv.hash
							}
							
							var li = $("<li></li>").attr("id", "side-param-" + inv.hash + "-" + param.id);
							
							var dataTypeLabel = param.actualDataType ? param.actualDataType.typeName : param.dataTypeLabel;
							
							var paramName = $("<span></span>").text(param.name + ": " + dataTypeLabel);
							li.append(paramName);
							
							var linkIcon = $("<i class=\"param-link fa fa-link\" aria-hidden=\"true\"></i>");
							
							if (window.builder.process.hasParamAssignments(param) || window.builder.process.hasAttributeAssignments(param))
							{
								linkIcon.addClass("param-link-active");
							}
							
							// clicking this icon will toggle param assignment lines for this param
							linkIcon.click((function(param) {
								
								return function() {
									
									// check if assignment lines are already visible for this item
									if ($(this).hasClass("param-lines-visible"))
									{
										$(this).removeClass("param-lines-visible");
										
										window.builder.process.hideParamAssignmentLines(param, true);	
									}
									else
									{
										$(this).addClass("param-lines-visible");
										
										window.builder.process.showParamAssignmentLines(param, true);
									}
								}
								
							})(param));
							
							li.append(linkIcon);
							
							li.draggable({
								
								helper: "clone",
								
								start: (function(param, direction) {
									
									return function (e, ui) {
							        	window.builder.process.draggedParam = param;
							        	window.builder.process.draggedParam.type = direction;
							    	}
									
								})(param, direction),
								
								stop: function() {
									window.builder.process.draggedParam = null;
									window.builder.drawing.paintLines();
								}
							});
							
							li.droppable({
								drop: (function(param, direction, linkIcon) {
									
									return function() {
										var draggedParam = window.builder.process.draggedParam;
										if (!draggedParam)
										{
											return;
										}
										
										window.builder.process.newParamAssignmentStart = {
											param: draggedParam,
											invocation: draggedParam.invocation,
											type: draggedParam.type
										};
										
										window.builder.process.newParamAssignmentEnd = {
											param: param,
											invocation: param.invocation,
											type: direction
										};
										
										window.builder.process.saveNewParamAssignment();
										
										// show assignment lines for this param
										window.builder.drawing.drawParamAssignmentLines(window.builder.process.draggedParam);
										window.builder.process.paramAssignmentLines.push(window.builder.process.draggedParam);
										
										// find the link icon on the param that was dragged
										$("#side-param-" + window.builder.process.draggedParam.invocation.hash + "-" + window.builder.process.draggedParam.id + " i.param-link").addClass("param-lines-visible")
										
										window.builder.process.draggedParam = null;
									}
								})(param, direction, linkIcon)
							});
							
							paramList.append(li);
						}
						
						return paramList;
						
					},
					
					renderIfCondition: function(inv, box) {
						
						var renderIfHandle = function(inv, transitionType) {
							
							var handle = $("<div></div>").addClass("if-transition-handle").attr("id", "if-handle-" + inv.hash + "-" + transitionType)
							
							if (transitionType === "true")
							{
								var arrow = $("<i class=\"if-large-arrow fa fa-arrow-left\" aria-hidden=\"true\"></i>");
								handle.append($("<span></span>").text("TRUE")).append(arrow);
							}
							else
							{
								var arrow = $("<i class=\"if-large-arrow fa fa-arrow-right\" aria-hidden=\"true\"></i>");
								handle.append(arrow).append($("<span></span>").text("FALSE"));
							}
							
							return handle;
						};
						
						var diagram = $("#diagram");
						
						var trueHandle = renderIfHandle(inv, "true");
						var falseHandle = renderIfHandle(inv, "false");
						
						// draw true and false transition handle
						$("#diagram").append(trueHandle);
						$("#diagram").append(falseHandle);
						
						this.positionIfHandles(inv.hash);
						
						trueHandle.draggable({
							drag: function() {
								window.builder.process.rerenderLines();
							},
							start: (function(inv, transitionType) {
								return function() {
									window.builder.process.draggedIfTransition = {
										invocation: inv,
										transitionType: transitionType
									}
								}
							})(inv, "true")
						});
						
						falseHandle.draggable({
							drag: function() {
								window.builder.process.rerenderLines();
							},
							start: (function(inv, transitionType) {
								return function() {
									window.builder.process.draggedIfTransition = {
										invocation: inv,
										transitionType: transitionType
									}
								}
							})(inv, "false")
						});
						
					},
					
					/**
					 * @private
					 */
					getInvocationProperties: function(inv) {
						
						var inputs = $("<ul></ul>").addClass("param-list");
						
						for (var k = 0; k < inv.inputs.length; k++)
						{
							var input = inv.inputs[k];
							
							// assign the invocation, but just the hash to avoid circular reference when this structure is parsed to string
							input.invocation = {
								hash: inv.hash
							}
							
							var li = $("<li></li>");
							
							var paramWrapper = $("<div></div>").addClass("param-name-line");
							paramWrapper.append($("<span></span>").text("input: ").addClass("param-header"));
							var inputName = $("<span></span>").text(input.name);
							paramWrapper.append(inputName);
							paramWrapper.append($("<span></span>").text("(" + input.dataTypeLabel + ")").addClass("param-dt"));
							li.append(paramWrapper);
							
							var paramAssignments = $("<ul></ul>").addClass("param-assignment-list");
							
							var arrow = $("<i class=\"inline-assignment-arrow fa fa-arrow-left\" aria-hidden=\"true\"></i>");
							var close = $("<i class=\"fa fa-close\" aria-hidden=\"true\"></i>").addClass("delete-assignment-btn");
							
							// get param assignments
							for (var i = 0; i < window.builder.process.paramAssignments.length; i++)
							{
								var a = window.builder.process.paramAssignments[i];
								if (a.target && a.target.invocation.hash === input.invocation.hash && a.target.param.id === input.id && a.source)
								{
									// render param assignment
									var paramLi = $("<li></li>");
									
									var dataTypeLabel = a.source.param.actualDataType ? a.source.param.actualDataType.typeName : a.source.param.dataTypeLabel;
									
									var paramName = $("<span></span>").text(a.source.param.name + ": " + dataTypeLabel);
									paramLi.append(arrow.clone()).append(paramName);
									
									var closeBtn = close.clone();
									
									closeBtn.click((function(a) {
										
										return function() {
											window.builder.process.removeParamAssignment(a);
											window.builder.process.rerenderDiagram();
											window.builder.process.showInvocationDetails(inv);
										}
										
									})(a));
									
									paramLi.append(closeBtn);
									
									paramAssignments.append(paramLi);
								}
							}
							
							li.append(paramAssignments);
							
							inputs.append(li);
							
						}
						
						var outputs = $("<ul></ul>").addClass("param-list");
						
						for (var k = 0; k < inv.outputs.length; k++)
						{
							var output = inv.outputs[k];
							
							// assign the invocation, but just the hash to avoid circular reference when this structure is parsed to string
							output.invocation = {
								hash: inv.hash
							}
							
							var li = $("<li></li>");
							
							var paramWrapper = $("<div></div>").addClass("param-name-line");
							paramWrapper.append($("<span></span>").text("output: ").addClass("param-header"));
							var outputName = $("<span></span>").text(output.name);
							paramWrapper.append(outputName);
							paramWrapper.append($("<span></span>").text("(" + output.dataTypeLabel + ")").addClass("param-dt"));
							li.append(paramWrapper);
							
							var paramAssignments = $("<ul></ul>").addClass("param-assignment-list");
							
							var arrow = $("<i class=\"inline-assignment-arrow fa fa-arrow-right\" aria-hidden=\"true\"></i>");
							var close = $("<i class=\"fa fa-close\" aria-hidden=\"true\"></i>").addClass("delete-assignment-btn");
							
							// get param assignments
							for (var i = 0; i < window.builder.process.paramAssignments.length; i++)
							{
								var a = window.builder.process.paramAssignments[i];
								if (a.source && a.source.invocation.hash === output.invocation.hash && a.source.param.id === output.id)
								{
									// render param assignment
									var paramLi = $("<li></li>");
									
									var paramName = $("<span></span>").text(a.target.param.name + ": " + a.target.param.dataTypeLabel);
									paramLi.append(arrow.clone()).append(paramName);
									
									var closeBtn = close.clone();
									
									closeBtn.click((function(a) {
										
										return function() {
											window.builder.process.removeParamAssignment(a);
											window.builder.process.rerenderDiagram();
											window.builder.process.showInvocationDetails(inv);
										}
										
									})(a));
									
									paramLi.append(closeBtn);
									
									paramAssignments.append(paramLi);
								}
							}
							
							li.append(paramAssignments);
							
							outputs.append(li);
						}
						
						var invPropertiesWrapper = $("<div></div>").addClass("inv-props");
						
						var paramContainer = $("<div></div>").addClass("param-container");
						
						if (inv.inputs.length)
						{
							paramContainer.append($("<div></div>").text("Input parameters").addClass("param-section-title"));
							paramContainer.append(inputs);
						}
						
						if (inv.outputs.length)
						{
							paramContainer.append($("<div></div>").text("Output parameters").addClass("param-section-title"));
							paramContainer.append(outputs);
						}
						
						// find transitions for this invocations
						/*var incomingTransitions = [];
						var outgoingTransitions = [];
						for (var i = 0; i < this.transitions.length; i++)
						{
							var t = this.transitions[i];
							if (t.nextAction.hash === inv.hash)
							{
								incomingTransitions.push(t);	
							}
							else if (t.prevAction.hash === inv.hash)
							{
								outgoingTransitions.push(t);	
							}
						}*/
						
						invPropertiesWrapper.append(paramContainer);
						
						if (inv.invokedAction)
						{
							if (inv.invokedAction.isCustom)
							{
								if (inv.invokedAction.name === "QueryUnique")
								{
									this.renderQueryUniqueDetails(invPropertiesWrapper, inv);	
								}
								else
								{
									invPropertiesWrapper.append(this.renderInvocationAttributes(inv, "Attributes"));
								}
							}
							else if (inv.invokedAction.type === "If")
							{
								this.renderIfInvocationDetails(invPropertiesWrapper, inv);
							}
							else if (inv.invokedAction.type === "FieldUpdate")
							{
								invPropertiesWrapper.append(this.renderInvocationAttributes(inv, "Field values", window.builder.process.fieldUpdateAttributeCallback(inv)));
							}
							else if (inv.invokedAction.type === "FieldValue")
							{
								this.renderFieldValueDetails(invPropertiesWrapper, inv);
							}
							else if (inv.invokedAction.type === "RecordCreate" || inv.invokedAction.type === "RecordUpdate" || inv.invokedAction.type === "RecordSave")
							{
								this.renderRecordSaveDetails(invPropertiesWrapper, inv)
							}
						}
						
						return invPropertiesWrapper;
					},
					
					isAnyRecord: function(typeName) {
						
						return typeName === window.builder.anyRecordLabel || typeName === "kommet.basic.RecordProxy";  
						
					},
					
					/**
					 * This function returns a callback that is called when an attribute is set or unset
					 * on the FieldValue invocation. It checks if the attribute name is valid, i.e. if it
					 * corresponds to an actual field on the record type provided to the action.
					 */
					fieldNameSetCallback: function(inv) {
						
						return function(attr, mode) {
							
							inv = window.builder.process.getInvocationByHash(inv.hash);
							var outputParam = inv.getOutput("value");
							
							if (attr.name !== "field")
							{
								return "Invalid attribute " + attr.name;
							}
							
							if (mode === "set")
							{
								var invocationInputActualDataType = inv.getInput("record").actualDataType;
								
								var typeName = invocationInputActualDataType ? invocationInputActualDataType.typeName : window.builder.anyRecordLabel;
								var type = null;
								
								if (window.builder.process.isAnyRecord(typeName))
								{
									type = window.builder.genericType;	
								}
								else
								{
									// try to get type name from the input parameter of the field value action
									type = window.builder.typesByName[typeName];
								}
								
								if (!type)
								{
									// return error
									return "Type with name " + typeName + " not found among cached types";
								}
								
								var field = type.getField(attr.value);
								
								if (!field)
								{
									// return error
									return "Field " + attr.value + " not found on type " + typeName;
								}
								
								outputParam.actualDataType = {
									typeName: field.javaType
								};
								
								outputParam.dataTypeLabel = field.javaType;
								outputParam.isCustomType = field.dataTypeId === km.js.datatypes.object_reference.id;
								
								// we need to rerender the whole diagram
								window.builder.process.rerenderDiagram();
							}
							else if (mode === "unset")
							{
								outputParam.actualDataType = null;
								outputParam.dataTypeLabel = null;
								outputParam.isCustomType = false;
							}
							
							// return no error
							return null;
						}
					},
					
					/**
					 * This function returns a callback that is called when an attribute is set or unset
					 * on the FieldUpdate invocation. It checks if the attribute name is valid, i.e. if it
					 * corresponds to an actual field on the record type provided to the action.
					 */
					fieldUpdateAttributeCallback: function(inv) {
						
						return function(attr, mode) {
							
							inv = window.builder.process.getInvocationByHash(inv.hash);
							
							if (mode === "set")
							{
								var invocationInputActualDataType = inv.getInput("record").actualDataType;
								
								var typeName = invocationInputActualDataType ? invocationInputActualDataType.typeName : window.builder.anyRecordLabel;
								var type = null;
								
								if (window.builder.process.isAnyRecord(typeName))
								{
									type = window.builder.genericType;	
								}
								else
								{
									// try to get type name from the input parameter of the field value action
									type = window.builder.typesByName[typeName];
								}
								
								if (!type)
								{
									// return error
									return "Type with name " + typeName + " not found among cached types";
								}
								
								var field = type.getField(attr.name);
								
								if (!field)
								{
									// return error
									return "Field " + attr.name + " not found on type " + typeName;
								}
							}
							
							// return no error
							return null;
						}
					},
					
					castEntryPointOutput: function(inv, currentTypes, rerenderDiagram) {
						this.castInvocationOutput(inv, currentTypes, rerenderDiagram, "record");
					},
					
					propagateCastOutput: function(inv, paramName) {
						
						// find all assignments for which this parameter is a source
						var assignments = this.getParamAssignmentsFromParam(inv, paramName);
						
						for (var i = 0; i < assignments.length; i++)
						{
							var a = assignments[i];
							window.builder.process.setActualDataTypeForAssignmentTarget(a);
							
							if (a.target && a.target.invocation)
							{
								// take the target invocation and propagate all of its output parameters
								var targetInv = this.getInvocationByHash(a.target.invocation.hash);
								deduceAndPropagateParamTypes(targetInv);
							}
						}
						
					},
					
					castInvocationOutput: function(inv, currentTypes, rerenderDiagram, paramName) {
						
						// if current types contains only one type, then the data type of the output parameter is unambiguous and we can show
						// a direct cast to this type
						if (currentTypes && currentTypes.indexOf(",") === -1)
						{
							var type = window.builder.typesById[currentTypes];
							
							if (!type)
							{
								throw "Type with ID '" + currentTypes + "' not found";
							}
							
							for (var i = 0; i < inv.outputs.length; i++)
							{
								if (inv.outputs[i].name === paramName)
								{
									inv.outputs[i].dataTypeLabel = type.qualifiedName;
									inv.outputs[i].isCustomType = true;
									
									this.propagateCastOutput(inv, paramName);
								}
							}
						}
						else
						{
							for (var i = 0; i < inv.outputs.length; i++)
							{
								if (inv.outputs[i].name === paramName)
								{
									inv.outputs[i].dataTypeLabel = window.builder.anyRecordLabel;
									inv.outputs[i].isCustomType = false;
									
									this.propagateCastOutput(inv, paramName);
								}
							}
						}
						
						if (rerenderDiagram)
						{
							window.builder.process.rerenderDiagram();
						}
						
					},
					
					/**
					 * Show details of a record-save action in the right-hand panel
					 */
					renderFieldValueDetails: function(wrapper, inv) {
						
						wrapper.append($("<div></div>").text("Field to get value from").addClass("param-section-title"));
						
						var typeLookupWrapper = $("<div></div>").addClass("accepted-type-selection");
						
						var picklist = $("<select></select>");
						
						// append empty option
						var option = $("<option></option>").text("-- select field --");
						picklist.append(option);
						
						inv = window.builder.process.getInvocationByHash(inv.hash);
						var outputParam = inv.getOutput("value");
						
						var invocationInputActualDataType = inv.getInput("record").actualDataType;
						
						var typeName = invocationInputActualDataType ? invocationInputActualDataType.typeName : window.builder.anyRecordLabel;
						var type = null;
						
						if (window.builder.process.isAnyRecord(typeName))
						{
							type = window.builder.genericType;	
						}
						else
						{
							// try to get type name from the input parameter of the field value action
							type = window.builder.typesByName[typeName];
						}
						
						if (!type)
						{
							// return error
							return "Type with name " + typeName + " not found among cached types";
						}
						
						// append type names
						for (var i = 0; i < type.fields.length; i++)
						{
							var field = type.fields[i];
							var option = $("<option></option>").text(field.label).attr("value", field.apiName);
							picklist.append(option);
						}
						
						picklist.val(window.builder.process.getAttribute(inv.hash, "field"));
						
						typeLookupWrapper.append(picklist);
						wrapper.append(typeLookupWrapper);
						
						picklist.change((function(inv, outputParam, type) {
							
							return function() {
								
								var selectedFieldName = $(this).val();
								var field = type.getField(selectedFieldName);
								window.builder.process.setAttribute(inv.hash, "field", selectedFieldName);
								
								deduceAndPropagateParamTypes(inv);
								
								// we need to rerender the whole diagram
								window.builder.process.rerenderDiagram();
								
								/*if (selectedFieldName)
								{	
									var field = type.getField(selectedFieldName);
									
									window.builder.process.setAttribute(inv.hash, "field", selectedFieldName);
									
									outputParam.actualDataType = {
										typeName: field.javaType
									};
									
									outputParam.dataTypeLabel = field.javaType;
									outputParam.isCustomType = field.dataTypeId === km.js.datatypes.object_reference.id;
									
									// we need to rerender the whole diagram
									window.builder.process.rerenderDiagram();
								}
								else
								{
									outputParam.actualDataType = null;
									outputParam.dataTypeLabel = null;
									outputParam.isCustomType = false;
								}*/
							}
							
						})(inv, outputParam, type));
						
					},
					
					/**
					 * Show details of a record-save action in the right-hand panel
					 */
					renderRecordSaveDetails: function(wrapper, inv) {
						
						wrapper.append($("<div></div>").text("Trigger action for").addClass("param-section-title"));
						
						var typeLookupWrapper = $("<div></div>").addClass("accepted-type-selection");
						
						var picklist = $("<select></select>");
						
						// append empty option
						var option = $("<option></option>").text("-- select type --");
						picklist.append(option);
						
						// append type names
						for (var i = 0; i < window.builder.availableTypes.length; i++)
						{
							var type = window.builder.availableTypes[i];
							var option = $("<option></option>").text(type.label).attr("value", type.id);
							picklist.append(option);
						}
						
						typeLookupWrapper.append(picklist);
						wrapper.append(typeLookupWrapper);
						
						picklist.change((function(inv) {
							
							return function() {
								
								var selectedTypeId = $(this).val();
								
								// add attribute
								var currentTypes = window.builder.process.getAttribute(inv.hash, "acceptedTypes");
								currentTypes = currentTypes ? (currentTypes + "," + selectedTypeId) : selectedTypeId;
								
								window.builder.process.setAttribute(inv.hash, "acceptedTypes", currentTypes);
								
								// refresh the whole details panel to reflect newly added type
								window.builder.process.showInvocationDetails(inv);
								
								window.builder.process.castEntryPointOutput(inv, currentTypes, true);
							}
							
						})(inv))
						
						/*var typeLookupStub = $("<div></div>");
						wrapper.append(typeLookupWrapper.append(typeLookupStub));
						km.js.ui.typeLookup(typeLookupStub, "stub", null, window.builder.availableTypes, (function(inv) {
							
							return function(selectedTypeId) {
								
								// add attribute
								var currentTypes = window.builder.process.getAttribute(inv.hash, "acceptedTypes");
								currentTypes = currentTypes ? (currentTypes + "," + selectedTypeId) : selectedTypeId;
								
								window.builder.process.setAttribute(inv.hash, "acceptedTypes", currentTypes);
								
								// refresh the whole details panel to reflect newly added type
								window.builder.process.showInvocationDetails(inv);
								
								window.builder.process.castEntryPointOutput(inv, currentTypes, true);
							}
							
						})(inv), (function() {
							
							// for some unknown reason after the lookup has been displaye,d jQuery mouseover event starts causing exceptions
							// so we just reattach all events to the $("#diagram") container
							//preparePanel();
							
							//window.builder.process.rerenderDiagram();
							
						})());*/
						
						var paramAssignments = $("<ul></ul>").addClass("param-list accepted-types-list");
						
						var sAcceptedTypeIds = null;
						
						if (inv.attributes)
						{
							for (var i = 0; i < inv.attributes.length; i++)
							{
								var attr = inv.attributes[i];
								if (attr.name === "acceptedTypes")
								{
									sAcceptedTypeIds = attr.value;
									break;
								}
							}
						}
						
						if (!sAcceptedTypeIds)
						{
							return;
						}
						
						var acceptedTypeIds = sAcceptedTypeIds.split(",");
						
						var close = $("<i class=\"process-param-close fa fa-close\" aria-hidden=\"true\"></i>");
						
						for (var i = 0; i < acceptedTypeIds.length; i++)
						{
							var typeId = acceptedTypeIds[i];
							var type = window.builder.typesById[typeId];
							
							var li = $("<li></li>").append(type.qualifiedName);
							
							var closeBtn = close.clone();
							closeBtn.click((function(typeId, inv) {
								
								return function() {
									
									// update attribute
									var currentTypes = window.builder.process.getAttribute(inv.hash, "acceptedTypes");
									
									var newVal = null;
									
									if (currentTypes)
									{
										var typeIds = currentTypes.split(",");
										
										var newTypeIds = [];
										for (var i = 0; i < typeIds.length; i++)
										{
											if (typeIds[i] != typeId)
											{
												newTypeIds.push(typeIds[i]);
											}
										}
										
										newVal = newTypeIds.join(",");
										
										// remove type ID from list
										window.builder.process.setAttribute(inv.hash, "acceptedTypes", newVal);
										
										// refresh the whole details panel to reflect newly added type
										window.builder.process.showInvocationDetails(inv);
										
										window.builder.process.castEntryPointOutput(inv, window.builder.process.getAttribute(inv.hash, "acceptedTypes"), true);
									}
								}
								
							})(typeId, inv));
							
							li.append(closeBtn);
							
							paramAssignments.append(li);
						}
						
						wrapper.append(paramAssignments);
						
					},
					
					/**
					 * Display details of an if-action
					 */
					renderIfInvocationDetails: function(wrapper, inv) {
						wrapper.append($("<div></div>").text("Condition").addClass("param-section-title"));
						
						var condText = null;
						
						if (inv.attributes)
						{
							for (var i = 0; i < inv.attributes.length; i++)
							{
								if (inv.attributes[i].name === "condition")
								{
									condText = inv.attributes[i].value; 
								}
							}
						}
						
						var input = $("<textarea></textarea>").val(condText);
						
						input.keyup((function(inv) {
							
							return function() {
								
								var query = $(this).val();
								window.builder.process.setAttribute(inv.hash, "condition", query);
								
								/*km.js.utils.getTypeFromQuery(query, (function(inv) {
									
									return function(type) {
										window.builder.process.castInvocationOutput(inv, type ? type.id : null, true, "record");
									}
									
								})(inv));*/
								
							}
							
						})(inv));
						
						var conditionTextArea = $("<div></div>").addClass("if-condition-text-wrapper").append(input);
						wrapper.append(conditionTextArea);
					},
					
					/**
					 * Display details of an query unique action
					 */
					renderQueryUniqueDetails: function(wrapper, inv) {
						wrapper.append($("<div></div>").text("Query").addClass("param-section-title"));
						
						var condText = null;
						
						if (inv.attributes)
						{
							for (var i = 0; i < inv.attributes.length; i++)
							{
								if (inv.attributes[i].name === "query")
								{
									condText = inv.attributes[i].value; 
								}
							}
						}
						
						var input = $("<textarea></textarea>").val(condText);
						
						input.keyup((function(inv) {
							
							return function() {		
								var query = $(this).val();
								window.builder.process.setAttribute(inv.hash, "query", query);
							}
							
						})(inv));
						
						var propWrapper = $("<div></div>").addClass("inv-prop-wrapper");
						
						var queryErr = $("<div></div>").attr("id", "invalid-query-msg").css("display", "none");
						
						input.focusout((function(inv, queryErr) {
							
							return function() {
								
								window.builder.process.castUniqueQueryOutput(inv, $(this).val(), $(this), queryErr, true);
								
							}
							
						})(inv, queryErr));
						
						var conditionTextArea = $("<div></div>").addClass("if-condition-text-wrapper").append(input);
						
						propWrapper.append(queryErr).append(conditionTextArea);
						wrapper.append(propWrapper);
					},
					
					castUniqueQueryOutput: function(inv, query, queryTextarea, queryErr, isPanelAvailable) {
						
						km.js.utils.getTypeFromQuery(query, (function(inv) {
							
							return function(type, isValidQuery) {
								
								if (isValidQuery === true)
								{
									if (isPanelAvailable)
									{
										queryTextarea.removeClass("km-err-input");
										queryErr.hide();
									}
									window.builder.process.castInvocationOutput(inv, type ? type.id : null, true, "record");
								}
								else
								{
									if (isPanelAvailable)
									{
										queryTextarea.focus().addClass("km-err-input");
										queryErr.text("Invalid query").show();
									}
								}
							}
							
						})(inv, queryTextarea));
						
					},
					
					castFieldValueOutput: function(fieldName, typeName, callback) {
						
						$.get(km.js.config.contextPath + "/km/bp/castfieldvalueoutput", function(data) {
							
							if (data.success)
							{
								if (typeof(callback) === "function")
								{
									callback(true, data.data.dataType);
								}
							}
							else
							{
								if (typeof(callback) === "function")
								{
									callback(false);
								}
							}
							
						}, "json");
						
					},
					
					setAttribute: function(invHash, name, val) {
						
						// get the invocation
						var inv = this.getInvocationByHash(invHash);
						var attrFound = false;
						
						var newAttributes = [];
						
						if (inv.attributes)
						{
							// iterate over attributes
							for (var i = 0; i < inv.attributes.length; i++)
							{
								if (inv.attributes[i].name === name)
								{
									if (!attrFound)
									{
										inv.attributes[i].value = val;
										attrFound = true;
										newAttributes.push(inv.attributes[i]);
									}
								}
								else
								{
									newAttributes.push(inv.attributes[i]);
								}
							}
						}
						
						if (!attrFound)
						{
							newAttributes.push({
								name: name,
								value: val,
								hidden: false
							});
						}
						
						inv.attributes = newAttributes;
						this.updateInvocation(inv);
						
					},
					
					getAttribute: function(invHash, name) {
						
						var inv = this.getInvocationByHash(invHash);
						
						if (inv.attributes)
						{
							for (var i = 0; i < inv.attributes.length; i++)
							{
								if (inv.attributes[i].name === name)
								{
									return inv.attributes[i].value;
								}
							}
						}
						
						return null;
						
					},
					
					removeAttribute: function(invHash, attrHash) {
						
						// get the invocation
						var inv = this.getInvocationByHash(invHash);
						
						var newAttributes = [];
						
						if (inv.attributes)
						{
							// iterate over attributes
							for (var i = 0; i < inv.attributes.length; i++)
							{
								if (inv.attributes[i].hash !== attrHash)
								{
									newAttributes.push(inv.attributes[i]);
					
								}
							}
						}
						
						inv.attributes = newAttributes;
						this.updateInvocation(inv);
						
					},
					
					addAttribute: function(invHash, name, val) {
						
						var inv = this.getInvocationByHash(invHash);
						var attrFound = false;
						
						if (inv.attributes)
						{
							for (var i = 0; i < inv.attributes.length; i++)
							{
								if (inv.attributes[i].name === name && inv.attributes[i].value === val)
								{
									attrFound = true;
								}
							}
						}
						
						if (!attrFound)
						{
							inv.attributes.push({
								name: name,
								value: val,
								hash: "attr-" + this.randomInt(1000000),
								hidden: false
							});
						}
						
						this.updateInvocation(inv);
						
					},
					
					renderInvocationAttributes: function(inv, title, onAttributeSave) {
						
						var attrList = $("<ul></ul>").addClass("inv-attr-list");
						
						var newAttr = $("<li></li>");
						
						var attrWrapper = $("<div></div>").addClass("attr-wrapper");
						
						var newAttrName = $("<input></input>").addClass("std-input attr-input").attr("placeholder", "name");
						var newAttrValue = $("<input></input>").addClass("std-input attr-input").attr("placeholder", "value");
						
						var saveBtn = $("<i class=\"attr-save-check fa fa-check\" aria-hidden=\"true\"></i>");
						saveBtn.click((function(inv, nameInput, valueInput, attrWrapper, onAttributeSave) {
							
							return function() {
								
								attrWrapper.removeClass("km-attrs-error");
								attrWrapper.find("div.km-attr-err-panel").remove();
								
								var isError = false;
								
								if (nameInput.val() && valueInput.val())
								{
									var attr = {
										name: nameInput.val(),
										value: valueInput.val(),
										// some attributes may be strictly configurational and not displayed to users
										hidden: false
									};
									
									// call callback if defined
									if (typeof(onAttributeSave) === "function")
									{
										var errorMsg = onAttributeSave(attr, "set");
										
										if (errorMsg)
										{
											// the attribute is invalid, so we display an error message
											var errPanel = $("<div></div>").text(errorMsg).addClass("km-attr-err-panel");
											attrWrapper.prepend(errPanel);
											
											isError = true;
											attrWrapper.addClass("km-attrs-error")
										}
									}
									
									if (!isError)
									{
										inv.attributes.push(attr);
									}
								}
								
								if (!isError)
								{
									window.builder.process.rerenderInvocation(inv);
									window.builder.process.showInvocationDetails(inv);
								}
								
							}
							
						})(inv, newAttrName, newAttrValue, attrWrapper, onAttributeSave));
						
						attrWrapper.append(newAttrName).append(newAttrValue).append(saveBtn);
						newAttr.append(attrWrapper);
						attrList.append(newAttr);
						
						var close = $("<i class=\"fa fa-close\" aria-hidden=\"true\"></i>").addClass("delete-assignment-btn");
						
						for (var i = 0; i < inv.attributes.length; i++)
						{
							var attr = inv.attributes[i];
							
							// some attributes may be strictly configurational and not displayed to users
							if (!attr.hidden)
							{
								var li = $("<li></li>").text(attr.name + ": " + attr.value);
								
								var closeBtn = close.clone();
								
								closeBtn.click((function(inv, attr, onAttributeSave) {
									
									return function() {
										window.builder.process.removeAttribute(inv.hash, attr.hash);
										window.builder.process.rerenderDiagram();
										window.builder.process.showInvocationDetails(inv);
										
										// call callback if defined
										if (typeof(onAttributeSave) === "function")
										{
											onAttributeSave(attr, "unset");
										}
									}
									
								})(inv, attr, onAttributeSave));
								
								li.append(closeBtn);
								
								attrList.append(li);
							}
						}
						
						var title = $("<div></div>").text(title).addClass("param-section-title");
						
						var attrPanel = $("<div></div>").append(title).append(attrList).addClass("inv-attrs");
						
						return attrPanel;
					}
				},
				
				toolbarActions: {
					// actions by ID
				},
				
				toolbarProcesses: {
					// processes by ID
				}
				
			}
		
			$(document).ready(function() {
				
				initDrawing();
				loadToolbar();
				preparePanel();
				
				$("#action-menu-item").click(function() {
					
					$("#bp-toolbar").css("top", $("#bp-lower-container").position().top);
					
					$("#bp-toolbar").toggle("slide", { direction: "left", complete: function() {
						//$(this).addClass("km-menu-visible");
					}});
					
				});
				
				renderProcessProperties();
				
				$("#process-menu-item").click(function() {
					showProcessProperties();
				});
				
				$("#process-list-menu-item").click(function() {
					km.js.utils.openURL(km.js.config.contextPath + "/km/bp/processes/list");
				});
				
				$("#setup-menu-item").click(function() {
					km.js.utils.openURL(km.js.config.contextPath + "/km/setup");
				});
				
			});
			
			function initProcess()
			{
				if (!"${processId}")
				{
					return;
				}
				
				var existingProcess = ${serializedProcess};
				
				// initialize existing process
				window.builder.process.id = existingProcess.id;
				window.builder.process.name = existingProcess.name;
				window.builder.process.label = existingProcess.label;
				window.builder.process.description = existingProcess.description;
				window.builder.process.isCallable = existingProcess.isCallable;
				window.builder.process.isTriggerable = existingProcess.isTriggerable;
				window.builder.process.isDraft = existingProcess.isDraft;
				window.builder.process.isActive = existingProcess.isActive;
				
				window.builder.process.invocations = existingProcess.invocations ? existingProcess.invocations : [];
				window.builder.process.transitions = existingProcess.transitions ? existingProcess.transitions : [];
				window.builder.process.paramAssignments = existingProcess.paramAssignments ? existingProcess.paramAssignments : [];
				window.builder.process.inputs = existingProcess.inputs ? existingProcess.inputs : [];
				window.builder.process.outputs = existingProcess.outputs ? existingProcess.outputs : [];
				
				var invocationsByKID = {};
				var processInputsByKID = {};
				var processOutputsByKID = {};
				var inputsByKID = {};
				var outputsByKID = {};
				
				var displaySettingsByInvName = {};
				if (existingProcess.displaySettings)
				{
					var displaySettings = JSON.parse(existingProcess.displaySettings);
					for (var i = 0; i < displaySettings.invocations.length; i++)
					{
						var inv = displaySettings.invocations[i];
						displaySettingsByInvName[inv.invocation.name] = inv.position;
					}
				}
				
				var newInvocations = [];
				
				// rewrite invocations
				for (var i = 0; i < window.builder.process.invocations.length; i++)
				{
					var inv = window.builder.process.invocations[i];
					
					if (inv.invokedAction)
					{
						inv.callable = inv.invokedAction;
					}
					else if (inv.invokedProcess)
					{
						inv.callable = inv.invokedProcess;
					}
					else
					{
						throw "Neither invoked process nor action set on invocation " + inv.name;
					}
					
					// get the action/process from the actions/processes retrieved from REST, because it has the "callableType" property initialized
					inv.invokedAction = window.builder.toolbarActions[inv.callable.id];
					inv.callable = window.builder.toolbarActions[inv.callable.id];
					
					inv.inputs = km.js.utils.cloneArr(inv.callable.inputs);
					
					for (var k = 0; k < inv.inputs.length; k++)
					{
						inv.inputs[k].type = "input";
					}
					
					inv.outputs = km.js.utils.cloneArr(inv.callable.outputs);
					
					for (var k = 0; k < inv.outputs.length; k++)
					{
						inv.outputs[k].type = "output";
					}
					
					// generate hash for invocation
					inv.hash = "inv-" + window.builder.process.randomInt(1000000);
					
					inv.position = {
						x: 100,
						y: 100
					};
					
					if (displaySettingsByInvName[inv.name])
					{
						inv.position = displaySettingsByInvName[inv.name];
					}
					
					inv.size = window.builder.settings.defaultSize;
					
					if (inv.invokedAction.callableType === "action")
					{
						inv.invokedAction.label = inv.invokedAction.name;
					}
					
					invocationsByKID[inv.id] = inv;
					
					if (inv.inputs)
					{
						for (var k = 0; k < inv.inputs.length; k++)
						{
							var input = inv.inputs[k];
							input.dataTypeLabel = input.dataTypeId ? window.builder.types[input.dataTypeId].qualifiedName : input.dataTypeName;
							input.isCustomType = input.dataTypeId != null;
							
							if (input.dataTypeLabel === "kommet.basic.RecordProxy")
							{
								input.dataTypeLabel = window.builder.anyRecordLabel;
							}
							
							inputsByKID[inv.inputs[k].id] = input;
						}
					}
					
					if (inv.outputs)
					{
						for (var k = 0; k < inv.outputs.length; k++)
						{
							var output = inv.outputs[k];
							output.dataTypeLabel = output.dataTypeId ? window.builder.types[output.dataTypeId].qualifiedName : output.dataTypeName;
							output.isCustomType = output.dataTypeId != null;
							
							if (output.dataTypeLabel === "kommet.basic.RecordProxy")
							{
								output.dataTypeLabel = window.builder.anyRecordLabel;
							}
							
							outputsByKID[inv.outputs[k].id] = output;
						}
					}
					
					window.builder.process.castEntryPointOutput(inv, window.builder.process.getAttribute(inv.hash, "acceptedTypes"), false);
					
					if (inv.callable.name === "QueryUnique")
					{
						window.builder.process.castUniqueQueryOutput(inv, window.builder.process.getAttribute(inv.hash, "query"), null, null, false);
					}
					
					inv = window.builder.process.addInvocationProperties(inv);
					
					newInvocations.push(inv);
				}
				
				window.builder.process.invocations = newInvocations;
				
				var newTransitions = [];
				
				// rewrite transitions
				for (var i = 0; i < window.builder.process.transitions.length; i++)
				{
					var trans = window.builder.process.transitions[i];
					
					// generate hash
					trans.hash = "transition-" + window.builder.process.randomInt(1000000);
					
					trans.prevAction = invocationsByKID[trans.previousAction.id];
					trans.nextAction = invocationsByKID[trans.nextAction.id];
					
					if (trans.prevAction.invokedAction.type === "If")
					{
						trans.ifTransitionType = window.builder.process.getTransitionType(trans);
					}
					
					newTransitions.push(trans);
				}
				
				window.builder.process.transitions = newTransitions;
				
				// rewrite inputs
				for (var i = 0; i < window.builder.process.inputs.length; i++)
				{
					var input = window.builder.process.inputs[i];
					window.builder.process.inputs[i].hash = "process-input-" + window.builder.process.randomInt(1000000);
					window.builder.process.inputs[i].type = "input";
					
					processInputsByKID[input.id] = window.builder.process.inputs[i]; 
				}
				
				// rewrite outputs
				for (var i = 0; i < window.builder.process.outputs.length; i++)
				{
					var output = window.builder.process.outputs[i];
					window.builder.process.outputs[i].hash = "process-output-" + window.builder.process.randomInt(1000000);
					window.builder.process.outputs[i].type = "output";
					
					processOutputsByKID[output.id] = window.builder.process.outputs[i];
				}
				
				var newParamAssignments = [];
				
				// rewrite param assignments
				for (var i = 0; i < window.builder.process.paramAssignments.length; i++)
				{
					var a = window.builder.process.paramAssignments[i];
					
					if (a.sourceInvocation)
					{
						a.source = {
							invocation: invocationsByKID[a.sourceInvocation.id],
							param: outputsByKID[a.sourceParam.id]
						}
					}
					
					if (a.targetInvocation)
					{
						a.target = {
							invocation: invocationsByKID[a.targetInvocation.id],
							param: inputsByKID[a.targetParam.id]
						}
					}
					
					if (a.processInput)
					{
						a.processInput = processInputsByKID[a.processInput.id];
					}
					
					if (a.processOutput)
					{
						a.processOutput = processOutputsByKID[a.processOutput.id];
					}
					
					a.hash = "param-assignment-" + window.builder.process.randomInt(1000000);
					
					window.builder.process.setActualDataTypeForAssignmentTarget(a);
					
					newParamAssignments.push(a);
				}
				
				// after param assignments have been processed and actual data types of parameters have been set,
				// we can deduce the actual types basing on attributes on actions
				for (var i = 0; i < window.builder.process.invocations.length; i++)
				{
					var inv = window.builder.process.invocations[i];
					
					// deduce all types basing on attributes
					// call this after addInvocationProperties() has been called on inv because it uses methods that addInvocationProperties adds
					deduceAndPropagateParamTypes(inv);
				}
				
				window.builder.process.paramAssignments = newParamAssignments;
				
				window.builder.process.rerenderDiagram();
				
				// show process properties
				$("#process-name").val(window.builder.process.name);
				$("#process-label").val(window.builder.process.label);
				$("#isProcessDraft").prop("checked", window.builder.process.isDraft);
				$("#isProcessActive").prop("checked", window.builder.process.isActive);
				$("#isProcessCallable").prop("checked", window.builder.process.isCallable);
				$("#isProcessTriggerable").prop("checked", window.builder.process.isTriggerable);
			}
			
			function deduceAndPropagateParamTypes(inv)
			{
				inv = window.builder.process.getInvocationByHash(inv.hash);
				
				if (inv.callable.type === "FieldValue" && inv.attributes)
				{
					for (var k = 0; k < inv.attributes.length; k++)
					{
						window.builder.process.fieldNameSetCallback(inv)(inv.attributes[k], "set");
					}
				}
				else if (inv.callable.type === "FieldUpdate" && inv.attributes)
				{
					for (var k = 0; k < inv.attributes.length; k++)
					{
						window.builder.process.fieldUpdateAttributeCallback(inv)(inv.attributes[k], "set");
					}
				}
				
				for (var i = 0; i < inv.outputs.length; i++)
				{
					window.builder.process.propagateCastOutput(inv, inv.outputs[i].name);
				}
			}
			
			function preparePanel()
			{
				$("div[id='diagram-wrapper']").droppable({
					drop: function(event, ui) {
			        	var blockId = window.builder.draggedItem;
			        	
			        	if (!blockId)
			        	{
			        		return;
			        	}
			        	
			        	var diagram = $("#diagram");
			        	window.builder.process.addInvocation(blockId, parseInt(ui.helper.position().left - diagram.offset().left), parseInt(ui.helper.position().top));
					}
				});
				
				$("#diagram").mousemove(function(event) {
					
					if (window.builder.process.newTransitionMode)
					{
						var diagram = $("#diagram");
						window.builder.process.newTransitionEndX = event.pageX - diagram.offset().left;
						window.builder.process.newTransitionEndY = event.pageY - diagram.offset().top;
						
						// repaint lines to rerender the new transition line
						window.builder.drawing.paintNewTransitionLine();
					}
					
					if (window.builder.process.draggedParam)
					{
						var diagram = $("#diagram");
						window.builder.drawing.paintDraggedParamLine(event.pageX - diagram.offset().left, event.pageY - diagram.offset().top);
					}
				});
				
				$("#diagram").click(function() {
					
					window.builder.process.cancelNewTransition();
					window.builder.drawing.paintLines();
				
				});
				
				$(document).keyup(function(e) {
					
					if (e.keyCode == 46 && window.builder.process.selectedTransition) { 
						window.builder.process.deleteTransition(window.builder.process.selectedTransition);
					}
					
				});
				
				window.builder.availableTypes = ${availableTypes};
				window.builder.typesById = {};
				window.builder.typesByName = {};
				
				for (var i = 0; i < window.builder.availableTypes.length; i++)
				{
					var type = addTypeProperties(window.builder.availableTypes[i]);
					window.builder.typesById[window.builder.availableTypes[i].id] = type;
					window.builder.typesByName[window.builder.availableTypes[i].qualifiedName] = type;
				}
				
				createGenericType();
			}
			
			function createGenericType()
			{
				// create a generic type with just the system fields
				window.builder.genericType = {
					fields: []
				};
				
				window.builder.genericType = addTypeProperties(window.builder.genericType);
				
				if (window.builder.availableTypes.length)
				{
					var mockType = window.builder.availableTypes[0];
					
					for (var i = 0; i < mockType.fields.length; i++)
					{
						var field = mockType.fields[i];
						if (field.apiName === "id" || field.apiName === "createdDate" || field.apiName === "lastModifiedDate" || field.apiName === "createdBy" || field.apiName === "lastModifiedBy")
						{
							window.builder.genericType.fields.push(field);
						}
					}
				}
				else
				{
					throw "Cannot create generic type because no types are available";
				}
			}
			
			function addTypeProperties(type)
			{
				type.getField = function(name) {
					
					if (this.fields == null)
					{
						return null;
					}
					
					for (var i = 0; i < this.fields.length; i++)
					{
						if (this.fields[i].apiName === name)
						{
							return this.fields[i];
						}
					}
					
				}
				
				return type;
			}
			
			function loadToolbar()
			{
				$.get(km.js.config.contextPath + "/km/bp/builder/actions", function(data) {
					
					window.builder.toolbarActions = {};
					
					// map types by ID
					window.builder.types = {};
					
					for (var i = 0; i < data.data.types.length; i++)
					{
						window.builder.types[data.data.types[i].id] = data.data.types[i];
					}
					
					clearRightPanel();
					renderCallablePanel(data.data.actions, data.data.processes);
					
					initProcess();
					
				}, "json");
			}
			
			function clearRightPanel()
			{
				$("#bp-toolbar").empty();
			}
			
			function showProcessProperties()
			{
				$("#invocation-details-container").hide();
				$("#process-details-container").fadeIn(500);
			}
			
			function renderProcessProperties()
			{
				var panel = $("<div></div>").attr("id", "bp-process-details");
				
				var labelInput = $("<input></input>").attr("placeholder", "New process label").attr("type", "text").addClass("std-input").attr("id", "process-label");
				panel.append(labelInput);
				
				var input = $("<input></input>").attr("placeholder", "New process name").attr("type", "text").addClass("std-input process-name").attr("id", "process-name");
				panel.append(input);
				
				labelInput.keyup((function(dest) {
					
					return function() {
						var name = $(this).val();
						
						// if process name has been set manually or is empty, copy its name from the label
						if (!dest.hasClass("edited-manually") || !dest.val())
						{
							// replace invalid characters with empty string
							dest.val("com.businessprocesses." + name.replace(/[^A-z0-9]/g, ""));
						}
					}
					
				})(input));
				
				input.keyup(function() {
					if ($(this).val())
					{
						$(this).addClass("edited-manually");
					}
				});
				
				var isDraft = $("<input></input>").attr("type", "checkbox").attr("name", "isDraft").attr("id", "isProcessDraft").attr("value", "true");
				panel.append($("<label></label>").append($("<span></span>").append(isDraft).css("vertical-align", "middle")).append($("<span>draft</span>")).addClass("draft-process-label"));
				
				var isActive = $("<input></input>").attr("type", "checkbox").attr("name", "isDraft").attr("id", "isProcessActive").attr("value", "true");
				panel.append($("<label></label>").append($("<span></span>").append(isActive).css("vertical-align", "middle")).append($("<span>active</span>")).addClass("draft-process-label"));
				
				var isCallable = $("<input></input>").attr("type", "checkbox").attr("name", "isCallable").attr("id", "isProcessCallable").attr("value", "true");
				panel.append($("<label></label>").append($("<span></span>").append(isCallable).css("vertical-align", "middle")).append($("<span>callable</span>")).addClass("draft-process-label"));
				
				var isTriggerable = $("<input></input>").attr("type", "checkbox").attr("name", "isTriggerable").attr("id", "isProcessTriggerable").attr("value", "true");
				panel.append($("<label></label>").append($("<span></span>").append(isTriggerable).css("vertical-align", "middle")).append($("<span>triggerable</span>")).addClass("draft-process-label"));
				
				// add save button
				var saveBtn = $("<input></input>").attr("type", "button").addClass("sbtn process-save-btn").attr("value", "Save");
				panel.append(saveBtn);
				
				saveBtn.click(function() {
					
					km.js.ui.statusbar.show("Saving process...");
					
					var serializedProcess = {
						id: window.builder.process.id,
						name: $("#process-name").val(),
						label: $("#process-label").val(),
						invocations: window.builder.process.invocations,
						transitions: window.builder.process.transitions,
						paramAssignments: window.builder.process.paramAssignments,
						inputs: window.builder.process.inputs,
						outputs: window.builder.process.outputs,
						isDraft: $("#isProcessDraft").is(":checked"),
						isActive: $("#isProcessActive").is(":checked"),
						isCallable: $("#isProcessCallable").is(":checked"),
						isTriggerable: $("#isProcessTriggerable").is(":checked")
					}
					
					// send a save request
					$.post(km.js.config.contextPath + "/km/bp/processes/save", { serializedProcess: JSON.stringify(serializedProcess) }, function(data) {
						
						km.js.ui.statusbar.show("Process saved successfully", 3000);
						
						if (data.success === true)
						{
							window.builder.process.id = data.data.processId;
						}
						else
						{
							km.js.ui.statusbar.err(data.messages ? data.messages : data.message);
						}
						
					}, "json");
					
				});
				
				panel.hide();
				
				var paramPanel = $("<div></div>").attr("id", "process-params");
				panel.append(paramPanel);
				$("#process-details-container").empty().append(panel);
				
				window.builder.process.showProcessParamPanel();
				
				panel.fadeIn(500);
			}
			
			function renderCallablePanel (actions, processes)
			{
				var panel = $("<div></div>").addClass("bp-action-panel");
				
				var transIcon = $("<i class=\"trans-icon fa fa-arrows-h\" aria-hidden=\"true\"></i>") 
				
				// add transition button
				var transBtn = $("<div></div>").addClass("bp-trans-button bp-op-trans action-btn").append(transIcon);
				transBtn.click(function() {
					$(this).toggleClass("bp-trans-button-in");
					
					if (window.builder.process.newTransitionMode)
					{
						window.builder.process.newTransitionMode = false;
					}
					else
					{
						window.builder.process.newTransitionMode = true;
					}
				});
				
				var btnPanel = $("<div></div>").addClass("bp-action-panel-btns");
				btnPanel.append($("<div></div>").addClass("title").text("Operations"));
				btnPanel.append(transBtn);
				
				var stdActionsPanel = $("<div></div>").addClass("bp-action-panel-std");
				stdActionsPanel.append($("<div></div>").addClass("title").text("Standard actions"));
				
				var initialActionsPanel = $("<div></div>").addClass("bp-action-panel-initial");
				initialActionsPanel.append($("<div></div>").addClass("title").text("Process triggers"));
				
				var customActionsPanel = $("<div></div>").addClass("bp-action-panel-custom");
				customActionsPanel.append($("<div></div>").addClass("title").text("Custom actions"));
				
				var processPanel = $("<div></div>").addClass("bp-action-panel-process");
				processPanel.append($("<div></div>").addClass("title").text("Processes"));
				
				var callables = actions.concat(processes);
				
				for (var i = 0; i < callables.length; i++)
				{
					var callable = callables[i];
					
					window.builder.toolbarActions[callable.id] = callable;
					
					if (callable.callableType === "action")
					{
						callable.label = callable.name;
						
						if (callable.isCustom)
						{
							customActionsPanel.append(renderCallableIcon(callable));
						}
						else
						{
							if (callable.isInitial)
							{
								initialActionsPanel.append(renderCallableIcon(callable));
							}
							else
							{
								stdActionsPanel.append(renderCallableIcon(callable));
							}
						}
					}
					else
					{
						processPanel.append(renderCallableIcon(callable));
					}
				}
				
				panel.append(btnPanel);
				panel.append(initialActionsPanel);
				panel.append(stdActionsPanel);
				panel.append(customActionsPanel);
				panel.append(processPanel);
				
				$("#bp-toolbar").append(panel);
			}
			
			function renderCallableIcon (action)
			{
				var icon = $("<div></div>").addClass("bp-action-icon").addClass("action-btn");
				icon.attr("id", "block-" + action.id);
				icon.append($("<div></div>").addClass("bp-action-icon-name").text(action.callableType === "action" ? action.name : action.label));
				
				icon.draggable({
					helper: "clone",
					
					start: (function(itemId) {
						return function (e, ui) {
				        	ui.helper.css("width", "200px");
				        	window.builder.draggedItem = itemId;
				    	}
					})(action.id),
					
					stop: function() {
						window.builder.draggedItem = null;
					},
				    
					cursor: "move"
				});
				
				return icon;
			}
			
			function initDrawing()
			{
				window.builder.drawing = {
			
					paper: Raphael(document.getElementById('diagram'), 1000, 1000),
						
					paintLines: function()
					{
						this.paper.clear();
						
						for (var i = 0; i < window.builder.process.transitions.length; i++)
						{
							var trans = window.builder.process.transitions[i];
							
							if (trans.prevAction.invokedAction.type !== "If")
							{
								this.join(trans.prevAction.hash, trans.nextAction.hash, "border", trans.label, trans);
							}
							else
							{
								var handle = $("#if-handle-" + trans.prevAction.hash + "-" + trans.ifTransitionType);
								
								if (!handle.length)
								{
									throw "No if handle for invocation " + trans.prevAction.hash;
								}
								
								var nextBox = $("#inv-" + trans.nextAction.hash);
								
								this.line({
									startX: handle.position().left + handle.outerWidth() / 3 + (trans.ifTransitionType === "true" ? 0 : handle.outerWidth() / 3),
									startY: handle.position().top + handle.height() + 5,
									endX: nextBox.position().left + nextBox.outerWidth() / 2,
									endY: nextBox.position().top, 
									colour: "#666666",
									isRenderArrows: true,
									transition: trans
								});
							}
						}
						
						this.paintNewTransitionLine();
						
						for (var i = 0; i < window.builder.process.paramAssignmentLines.length; i++)
						{
							this.drawParamAssignmentLines(window.builder.process.paramAssignmentLines[i]);
						}
					},
					
					paintNewTransitionLine: function() {
						
						// draw a line for the new transition
						if (window.builder.process.newTransitionMode)
						{
							if (window.builder.process.newTransitionLine)
							{
								window.builder.process.newTransitionLine.remove(); 
							}
							if (window.builder.process.newTransitionLabelObj)
							{
								window.builder.process.newTransitionLabelObj.remove(); 
							}
							this.newTransitionLine(window.builder.process.newTransitionStart, window.builder.process.newTransitionEndX, window.builder.process.newTransitionEndY, "New transition");
						}
						
					},
					
					paintDraggedParamLine: function(mouseX, mouseY) {
						
						// draw a line for the new transition
						if (window.builder.process.draggedParam)
						{
							if (window.builder.process.draggedParamLine)
							{
								window.builder.process.draggedParamLine.remove(); 
							}
					
							this.drawDraggedParamLine(window.builder.process.draggedParam, mouseX, mouseY);
						}
						
					},
					
					drawDraggedParamLine: function(param, mouseX, mouseY) {
						
						var startX = null;
						var startY = null;
						var endX = null;
						var endY = null;
						
						var diagram = $("#diagram");
						
						if (param.type === "output")
						{
							// start at the right side of the param box
							var paramBox = $("#side-param-" + param.invocation.hash + "-" + param.id);
							startX = paramBox.offset().left + paramBox.width() - diagram.offset().left;
							startY = paramBox.offset().top + paramBox.height() / 2  - diagram.offset().top;
							
							endX = mouseX;
							endY = mouseY;
						}
						else if (param.type === "input")
						{
							var invBox = $("#inv-" + param.invocation.hash);
							
							// start at the left side of the param box
							var paramBox = $("#side-param-" + param.invocation.hash + "-" + param.id);
							endX = paramBox.offset().left - diagram.offset().left;
							endY = paramBox.offset().top + paramBox.height() / 2 - diagram.offset().top;
							
							startX = mouseX;
							startY = mouseY;
						}
						else
						{
							throw "Invalid parameter type " + param.type;
						}
						
						// draw a line for this assignment
						window.builder.process.draggedParamLine = this.line({
							startX: startX,
							startY: startY,
							endX: endX,
							endY: endY,
							colour: "#ccc",
							label: null,
							"stroke-dasharray": [ "-" ]
						});
						
					},
					
					/**
					 * Draws assignment lines for the given parameter
					 */
					drawParamAssignmentLines: function(param) {
						
						var startX = null;
						var startY = null;
						var endX = null;
						var endY = null;
						
						var diagram = $("#diagram");
						
						if (param.type === "output")
						{
							// start at the right side of the param box
							var paramBox = $("#side-param-" + param.invocation.hash + "-" + param.id);
							
							if (!window.builder.process.getInvocationByHash(param.invocation.hash).paramsVisible)
							{
								return;
							}
							
							startX = paramBox.offset().left + paramBox.width() - diagram.offset().left;
							startY = paramBox.offset().top + paramBox.height() / 2  - diagram.offset().top;
						}
						else if (param.type === "input")
						{
							var invBox = $("#inv-" + param.invocation.hash);
							
							// start at the left side of the param box
							var paramBox = $("#side-param-" + param.invocation.hash + "-" + param.id);
							
							if (!window.builder.process.getInvocationByHash(param.invocation.hash).paramsVisible)
							{
								return;
							}
							
							endX = paramBox.offset().left - diagram.offset().left;
							endY = paramBox.offset().top + paramBox.height() / 2 - diagram.offset().top;
						}
						else
						{
							throw "Invalid parameter type " + param.type;
						}
						
						// find all assignments for this param
						for (var i = 0; i < window.builder.process.paramAssignments.length; i++)
						{
							var a = window.builder.process.paramAssignments[i];
							
							if (!a.source || !a.target)
							{
								continue;
							}
							
							if (param.type === "output")
							{
								// output param can only be a source of an assignment, not a target
								if (a.source.invocation.hash === param.invocation.hash && a.source.param.id === param.id)
								{
									var targetBox = $("#side-param-" + a.target.invocation.hash + "-" + a.target.param.id);
									
									// if the other param is not visible, do not draw the line
									if (!window.builder.process.getInvocationByHash(a.target.invocation.hash).paramsVisible)
									{
										return;
									}
									
									endX = targetBox.offset().left - diagram.offset().left;
									endY = targetBox.offset().top + targetBox.height() / 2 - diagram.offset().top;
								}
								else
								{
									continue;
								}
							}
							else if (param.type === "input")
							{
								// input param can only be a target of an assignment, not a target
								if (a.target.invocation.hash === param.invocation.hash && a.target.param.id === param.id)
								{
									var sourceBox = $("#side-param-" + a.source.invocation.hash + "-" + a.source.param.id);
									
									// if the other param is not visible, do not draw the line
									if (!window.builder.process.getInvocationByHash(a.source.invocation.hash).paramsVisible)
									{
										return;
									}
									
									startX = sourceBox.offset().left + sourceBox.width() - diagram.offset().left;
									startY = sourceBox.offset().top + sourceBox.height() / 2 - diagram.offset().top;
								}
								else
								{
									continue;
								}
							}
							
							// draw a line for this assignment
							this.line({
								startX: startX,
								startY: startY,
								endX: endX,
								endY: endY,
								colour: "#ccc",
								label: null,
								"stroke-dasharray": [ "-" ]
							});
						}
						
					},
					
					paintParamAssignment: function(assignment) {
						
						if (!assignment.source || !assignment.target)
						{
							// skip process input/output assignments
							return;
						}
						
					},
						
					join: function (box1Id, box2Id, joinPoint, label, transition)
					{
						var box1 = $("#inv-" + box1Id);
						var box2 = $("#inv-" + box2Id);
						
						var offsetX1 = box1.position().left;
						var offsetY1 = box1.position().top;
						var width1 = box1.width();
						var height1 = box1.height();
						
						var offsetX2 = box2.position().left;
						var offsetY2 = box2.position().top;
						var width2 = box2.width();
						var height2 = box2.height();
						
						var centerX1 = Math.ceil(offsetX1 + width1 / 2);
						var centerY1 = Math.ceil(offsetY1 + height1 / 2);
						
						var centerX2 = Math.ceil(offsetX2 + width2 / 2);
						var centerY2 = Math.ceil(offsetY2 + height2 / 2);
						
						if (joinPoint === "center")
						{	
							var path = this.curve({
								startX: parseInt(centerX1),
								startY: parseInt(centerY1),
								endX: parseInt(centerX2),
								endY: parseInt(centerY2),
								transition: transition
							});
						}
						else if (joinPoint === "border")
						{
							var path = this.curve({
								startX: parseInt(centerX1),
								startY: parseInt(offsetY1 + height1),
								endX: parseInt(centerX2),
								endY: parseInt(offsetY2),
								isRenderArrow: false,
								transition: transition
							});
						}
						else
						{
							throw "Unsupported join point " + joinPoint;
						}
						
						if (label)
						{
							this.labelPath(path, label);
						}
					},
					
					line: function (options)
					{	
						if (!options.colour)
						{
							options.colour = "#ff0000";
						}
						
						// draw a line in red
						var path = this.curve(options);
						
						if (options.label)
						{
							this.labelPath(path, options.label);
						}
						
						return path;
					},
					
					newTransitionLine: function (box1Id, endX, endY, label)
					{
						var box1 = $("#inv-" + box1Id);
						
						if (!box1.length)
						{
							return;
						}
						
						var offsetX1 = box1.position().left;
						var offsetY1 = box1.position().top;
						var width1 = box1.width();
						var height1 = box1.height();
						var centerX1 = Math.ceil(offsetX1 + width1 / 2);
						var centerY1 = Math.ceil(offsetY1 + height1 / 2);
						
						// draw a line in red
						var path = this.curve({
							startX: parseInt(centerX1),
							startY: parseInt(centerY1),
							endX: endX,
							endY: endY,
							color: "#ff0000",
							isRenderArrows: false
						});
						
						if (label)
						{
							window.builder.process.newTransitionLabelObj = this.labelPath(path, label);
						}
						
						window.builder.process.newTransitionLine = path;
					},
						
					curve: function (options)
					{				
						// curve options: startX, startY, endX, endY, colour, isRenderArrows
						
						var sign = (options.startX < options.endX) ? 1 : -1;
						
						var cx1 = options.startX + sign * Math.abs(options.startX - options.endX)/3;
						var cy1 = options.startY + Math.abs(options.startY - options.endY) * 3/4;
						var cx2 = options.endX - sign * Math.abs(options.startX - options.endX)/3;
						var cy2 = options.endY - Math.abs(options.startY - options.endY) * 3/4;
						
						if (!options.colour)
						{
							options.colour = "#999";
						}
						
						// curse
						//var path = paper.path("M" + startX + " " + startY + " C " + cx1 + " " + cy1 + " " + cx2 + " " + cy2 + " " + endX + " " + endY).attr({"stroke": "#999", "stroke-width": 2});
						
						var lineAttr = {
							"stroke": options.colour,
							"stroke-width": 1
						}
						
						if (options["stroke-dasharray"])
						{
							lineAttr["stroke-dasharray"] = options["stroke-dasharray"];
						}
						
						if (options.isRenderArrows)
						{
							lineAttr['arrow-end'] = 'classic-wide-long';
							lineAttr['arrow-start'] = 'classic-wide-long';
						}
						
						var d = "M" + options.startX + " " + options.startY + " C " + cx1 + "," + cy1 + " " + cx2 + "," + cy2 + " " + options.endX + " " + options.endY;
						
						var path = this.paper.path(d).attr(lineAttr);
						
						// same line, only transparent
						var pathClone = this.paper.path(d).attr({
							"opacity": 0,
							"stroke": options.colour,
							"stroke-width": 10
						});
					
						
						pathClone.click(function() {
							console.log("CL " + $(this));							
						});
						
						pathClone.mouseover((function(d, lineAttr, paper, transition) {
							return function(e) {
								lineAttr["stroke"] = "red";
								
								window.builder.process.selectedTransition = transition;
								
								var path = paper.path(d).attr(lineAttr);
							}
						})(d, lineAttr, this.paper, options.transition));
						
						pathClone.mouseout((function(d, lineAttr, paper) {
							return function() {
								window.builder.process.selectedTransition = null;
								window.builder.drawing.paintLines();
							}
						})(d, lineAttr, this.paper));
						
						// straight line
						//var path = this.paper.path("M" + startX + " " + startY + " L " + endX + " " + endY).attr(lineAttr);
						
						return path;
					},
					
					labelPath: function (path, text, textattr)
					{
					    if ( textattr == undefined )
					    {
					        textattr = { 'font-size': 12, fill: '#000', stroke: 'none', 'font-family': 'Arial,Helvetica,sans-serif', 'font-weight': 400 };
					    }
					    var bbox = path.getBBox();
					    var textObj = path.paper.text( bbox.x + bbox.width / 2, bbox.y + bbox.height / 2 + 10, text ).attr( textattr );
					    return textObj;
					}
				
				}
					
			}
		
		</script>
	
		<table id="process-table">
			<tbody>
				
				<tr>
					<td colspan="3" id="bp-topmenu">
						<ul>
							<li id="setup-menu-item">Setup</li>
							<li id="process-list-menu-item">All processes</li>
							<li id="process-menu-item">Process properties</li>
							<li id="action-menu-item">Actions</li>
						</ul>
					</td>
				</tr>
				
				<tr id="bp-lower-container">
				
					<td>
					
						<div id="diagram-wrapper">
							<div id="diagram" class="gridbg">
							</div>
						</div>
						
					</td>
					<td id="bp-details">
						<div id="process-details-container"></div>
						<div id="invocation-details-container"></div>
					</td>
				
				</tr>
			</tbody>
		</table>
		
		<div id="bp-toolbar" style="display: none"></div>
	
	</jsp:body>
</ko:processBuilderLayout>
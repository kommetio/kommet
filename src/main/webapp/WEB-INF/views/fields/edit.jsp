<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="ko" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="kolmu" uri="/WEB-INF/tld/kolmu-tags.tld" %>
<%@ taglib prefix="km" uri="/WEB-INF/tld/km-tags.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<ko:homeLayout title="${pageTitle}">
	<jsp:body>
	
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/js/jquery-ui-1.10.3.custom.min.js"></script>
		<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ui-lightness/jquery-ui-1.10.3.custom.min.css" />
		<script type="text/javascript" src="${pageContext.request.contextPath}/resources/km/js/km.table.js"></script>
	
		<style>
		
			span.km-tooltip-wrapper {
				position: absolute;
			}
			
			table.new-prop td.value {
				vertical-align: top;
			}
			
			table.new-prop .km-lookup {
				width: 25em;
			}
		
		</style>
	
		<script>

			$(document).ready(function() {

				$("#dataType").change(function() {
					showDataTypeDetails($("#dataType").val());
				});
				
				$("#associationType").change(function() {
					showAssociationType();
				});

				km.js.utils.bind($("#fieldApiName"), $("#fieldTitle"), "New field");
				km.js.utils.openMenuItem("Data Types");
				
				<%-- added live() function to react to changes in select by arrow keys --%>
				<%--$("#dataType").live("keypress",function() {
				    $(this).trigger("change");
				});--%>

				showDataTypeDetails($("#dataType").val())

				<c:if test="${not empty linkingTypeId && associationType == 'through linking type'}">
				onChangeLinkingType({ "selectedTypeId": "${linkingTypeId}" });
				</c:if>
				
				$("select#decimalPlaces").change(function() {
					$("select#javaType option").prop("selected", false);
					initJavaType(true);
				});
				
				initTooltips();
				
				km.js.ui.autoFormatFieldName({
					target: $("input[name='apiName']")
				});
			});
			
			function initTooltips()
			{
				km.js.ui.tooltip({
					afterTarget: $("input[name='apiName']"),
					text: "The name at which the field will be accessed from Java code"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='label']"),
					text: "Label of the field that will be displayed to users"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='javaType']"),
					text: "Java type that will be used to represent this numeric value"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input#linkingTypeId"),
					text: "Type that represents a link in between the two types used in this many-to-many relationship"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='inverseProperty']"),
					text: "Field of the referenced type that points to the current type"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("textarea[name='enumValues']"),
					text: "List of values for this enumeration. Please enter each value in a new line."
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='validateEnum']"),
					text: "Selecting this option will add a validation making sure that only values from the enumeration list are assigned to this field. Unselecting it will allow of other value to be assigned to it as well."
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='cascadeDelete']"),
					text: "Tells whether records of this type should be deleted when the record referenced by the object reference field is deleted"
				});
				
				km.js.ui.tooltip({
					afterTarget: $("input[name='labelKey']"),
					text: "Key of a user setting whose value will be used as a label for this field"
				});
			}
			
			function showAssociationType()
			{
				$(".dt-assoc-type").hide();
				var type = $("#associationType").val();
				
				$(".dt-details-10-" + type).show();
			}

			function showDataTypeDetails(dtId)
			{
				$(".dt-details").hide();
				$(".dt-details-" + dtId).css("display", "table-row");

				if (dtId == km.js.datatypes.inverse_collection.id || dtId == km.js.datatypes.association.id || dtId == km.js.datatypes.formula.id || dtId == km.js.datatypes.autonumber.id)
				{
					// make the required option invisible when data type is inverse collection
					$("#required-option-row").hide();
					$("#unique-option-row").hide();
				}
				else
				{
					$("#required-option-row").show();
					$("#unique-option-row").show();
				}
				
				if (dtId == km.js.datatypes.inverse_collection.id || dtId == km.js.datatypes.association.id)
				{
					$("tr.dt-collection").show();
				}
				else
				{
					$("tr.dt-collection").hide();
				}
				
				if (dtId == 8 || dtId == 9 || dtId == 10 || dtId == 11)
				{
					$("#text-default-value-row").hide();
				}
				else
				{
					$("#text-default-value-row").show();
				}

				if (dtId == 6)
				{
					$("#cascade-delete-option-row").show();
				}
				else
				{
					$("#cascade-delete-option-row").hide();
				}
				
				adjustDefaultField(dtId);
				
				// if it's a numeric type
				if (dtId == 0)
				{
					initJavaType(false);
				}
				
				// if it is an association
				if (dtId == 10)
				{
					showAssociationType();
				}
			}

			var onChangeLinkingType = function(data)
			{
				if (data.selectedTypeId)
				{
					<%-- get field candidates for the selected linking object and render select lists for their fields --%>
					$("#selfLinkingFieldCell").html(getSelectFromJSON("selfLinkingFieldId", "selfLinkingFieldId", linkingTypeOptions[data.selectedTypeId].selfLinkingFields, "id", "name", "${selfLinkingFieldId}"));
					$("#foreignLinkingFieldCell").html(getSelectFromJSON("foreignLinkingFieldId", "foreignLinkingFieldId", linkingTypeOptions[data.selectedTypeId].foreignLinkingFields, "id", "name", "${foreignLinkingFieldId}"));
				}
			}
			
			function adjustDefaultField(dtId)
			{
				// remove the date picker
				$("#defaultValue").datepicker("destroy");
				
				if (dtId == km.js.datatypes.date.id)
				{
					// show date picker for default value
					var defaultPicker = $("#defaultValue").datepicker({ dateFormat: "yy-mm-dd" });
					
					if ("${field.defaultValue}")
					{
						defaultPicker.datepicker("setDate", "${field.defaultValue}");
					}
				}
				else if (dtId == km.js.datatypes.datetime.id)
				{
					// show date picker for default value
					var defaultPicker = $("#defaultValue").datepicker({ dateFormat: "yy-mm-dd" });
					
					if ("${field.defaultValue}")
					{
						defaultPicker.datepicker("setDate", "${field.defaultValue}");
					}
				}
			}
			
			// adjusts java type input according to the decimal places of the number data type
			function initJavaType(isChangeSelection)
			{
				var dataTypeId = $("#dataType").val();
				
				if (!dataTypeId)
				{
					$("#java-type-row").hide();
				}
				
				var decimalPlaces = $("#decimalPlaces").val();
				if (!decimalPlaces)
				{
					$("#javaType").val(0);
					$("#java-type-row").hide();
				}
				
				if (parseInt(decimalPlaces) === 0)
				{
					if (isChangeSelection)
					{
						$("select#javaType").val("java.lang.Integer");
					}
					$("select#javaType > option.float-java-type").hide();
					$("select#javaType > option.int-java-type").show();
				}
				else
				{
					if (isChangeSelection)
					{
						$("select#javaType").val("java.lang.Double");
					}
					$("select#javaType > option.float-java-type").show();
					$("select#javaType > option.int-java-type").hide();
				}
			}

			var linkingTypeOptions = ${linkingTypeOptions};
			
			function createTypeLookup (target, inputName, selectedRecordId, types, afterSelect)
			{	
				// create an JSON datasource from the available types
				var offlineDS = km.js.datasource.create({
					type: "json",
					data: types
				});
				
				// jcr to query profiles
				var jcr = {
					properties: [
						{ name: "qualifiedName" },
						{ name: "id" },
						{ name: "label" }
					]
				};
				
				if ("" === selectedRecordId)
				{
					selectedRecordId = null;
				}
				
				// options of the available items list
				var availableItemsOptions = {
					options: {
						id: "type-lookup-search"
					},
					display: {
						properties: [
							{ name: "label", label: "Label", linkStyle: true },
							{ name: "qualifiedName", label: "API Name", linkStyle: true }
						],
						idProperty: { name: "id" },
						defaultProperty: { name: "label" }
					},
					title: "Objects"
				};
				
				// create the lookup
				var typeLookup = km.js.ref.create({
					datasource: offlineDS,
					selectedRecordDisplayField: { name: "label" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: inputName,
					selectedRecordId: selectedRecordId,
					afterSelect: afterSelect
				});
				
				typeLookup.render(target);
			}
			
			function initDictionaryLookup()
			{
				// jcr to query profiles
				var jcr = {
					baseTypeName: "kommet.basic.Dictionary",
					properties: [
						{ name: "id" },
						{ name: "name" }
					]
				};
				
				// options of the available items list
				var availableItemsOptions = {
					display: {
						properties: [
							{ name: "name", label: "Name", linkStyle: true }
						],
						idProperty: { name: "id" }
					},
					title: "Dictionaries",
					tableSearchOptions: {
						properties: [ { name: "name", operator: "ilike" } ]
					}
				};
				
				// create the lookup
				var lookup = km.js.ref.create({
					selectedRecordDisplayField: { name: "name" },
					jcr: jcr,
					availableItemsDialogOptions: {},
					availableItemsOptions: availableItemsOptions,
					inputName: "dictionaryId",
					selectedRecordId: "${dictionaryId}"
				});
				
				lookup.render($("#dictionary"));
			}
			
			window.inverseProperties = ${inverseProperties};
			
			$(document).ready(function() {
				
				var typeRefTypes = ${typesToReference};
				createTypeLookup($("#typeLookup"), "referencedObject", "${referencedTypeId}", typeRefTypes);
				initDictionaryLookup();
				
				var inverseTypes = ${typesForInverseCollection};
				var inverseTypeAfterSelect = function(typeId) {
					
					var fieldSelect = $("<select></select>").attr("name", "inverseProperty").attr("id", "inverseProperty");
					var inverseFields = window.inverseProperties[typeId];
					
					for (var i = 0; i < inverseFields.length; i++)
					{
						var option = $("<option></option>").attr("value", inverseFields[i]).text(inverseFields[i]);
						fieldSelect.append(option);
					}
					
					fieldSelect.val("${inverseProperty}").change();
					
					// replace the inverse property text input with a dropdown
					$("input#inverseProperty").replaceWith(fieldSelect);
					
					// show the row with the inverse property input
					$("tr.dt-details-inverse-prop").show();
				}
				
				// if inverse type is selected, show inverse property
				if ("${inverseTypeId}")
				{
					inverseTypeAfterSelect("${inverseTypeId}");
				}
				
				createTypeLookup($("#inverseTypeId"), "inverseTypeId", "${inverseTypeId}", inverseTypes, inverseTypeAfterSelect);
				
				var associatedTypes = ${associatedTypes};
				createTypeLookup($("#associatedTypeId"), "associatedTypeId", "${associatedTypeId}", associatedTypes, afterSelect);
				
				var linkingTypes = ${linkingTypeCandidates};
				
				if (linkingTypes.length > 0)
				{
					var afterSelect = function(recordId) { onChangeLinkingType({ "selectedTypeId": recordId }) };
					createTypeLookup($("#linkingTypeId"), "linkingTypeId", "${linkingTypeId}", linkingTypes, afterSelect);
				}
				else
				{
					$("#linkingTypeId").replaceWith($("<span></span>").text("No objects to serve as linking object"));
				}
			});
		
		</script>
		
		<km:breadcrumbs isAlwaysVisible="true" />
	
		<div class="ibox">
		
			<form method="post" action="${pageContext.request.contextPath}/km/field/save">
				
				<ko:pageHeader id="fieldTitle">
					<c:if test="${empty field.KID}">New field</c:if>
					<c:if test="${not empty field.KID}">Edit field ${field.label}</c:if>
				</ko:pageHeader>
				
				<kolmu:errors messages="${errorMsgs}" />
				
				<input type="hidden" name="typeId" value="${type.KID}" />
				<input type="hidden" name="fieldId" value="${field.KID}" />
				<input type="hidden" name="linkingTypeOptions" value="${linkingTypeOptions}" />
				
				<ko:propertyTable cssClass="new-prop">
					<ko:propertyRow>
						<ko:propertyLabel value="API name" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="apiName" value="${field.apiName}" id="fieldApiName" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Label" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="label" value="${field.label}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Label Key"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="labelKey" value="${field.uchLabel}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Data Type" required="true"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="dataType" id="dataType">
								<option value="">-- Select --</option>
								<c:forEach var="dt" items="${dataTypes}">
									<option value="${dt.id}"<c:if test="${dt.id == field.dataType.id}"> selected</c:if>>${dt.name}</option>
								</c:forEach>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="dt-details-1" cssClass="dt-details dt-details-1" cssStyle="display:none">
						<ko:propertyLabel value="Maximum text field length"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="textDataTypeLength" value="<c:if test="${field.dataType.name == 'Text'}">${field.dataType.length}</c:if>" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="dt-details-14" cssClass="dt-details dt-details-14" cssStyle="display:none">
						<ko:propertyLabel value="AutoNumber prefix"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="autonumberFormat" value="<c:if test="${field.dataType.name == 'AutoNumber'}">${field.dataType.format}</c:if>" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="dt-details-1" cssClass="dt-details dt-details-1" cssStyle="display:none">
						<ko:propertyLabel value="Display as"></ko:propertyLabel>
						<ko:propertyValue>
							<select id="textFieldDisplay" name="textFieldDisplay">
								<option value="singleLine" <c:if test="${field.dataType.name == 'Text' && field.dataType.isLong() == false}">selected</c:if>>single line input</option>
								<option value="multiLine" <c:if test="${field.dataType.name == 'Text' && field.dataType.isLong() == true}">selected</c:if>>multiline text area</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="dt-details-1" cssClass="dt-details dt-details-1" cssStyle="display:none">
						<ko:propertyLabel value="Is Formatted"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="isFormattedText" value="true"<c:if test="${field.dataType.name == 'Text' && field.dataType.isFormatted() == true}"> checked</c:if> />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-6" cssStyle="display:none">
						<ko:propertyLabel value="Referenced object"></ko:propertyLabel>
						<ko:propertyValue>
							<input id="typeLookup" type="text"></input>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-8" cssStyle="display:none">
						<ko:propertyLabel value="Referenced object"></ko:propertyLabel>
						<ko:propertyValue>
							<input id="inverseTypeId" type="text"></input>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-0" cssStyle="display:none">
						<ko:propertyLabel value="Decimal places"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="decimalPlaces" id="decimalPlaces">
								<option value="0"<c:if test="${decimalPlaces == 0}"> selected</c:if>>0</option>
								<option value="1"<c:if test="${decimalPlaces == 1}"> selected</c:if>>1</option>
								<option value="2"<c:if test="${decimalPlaces == 2}"> selected</c:if>>2</option>
								<option value="3"<c:if test="${decimalPlaces == 3}"> selected</c:if>>3</option>
								<option value="4"<c:if test="${decimalPlaces == 4}"> selected</c:if>>4</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="java-type-row" cssClass="dt-details dt-details-0" cssStyle="display:none">
						<ko:propertyLabel value="Java representation"></ko:propertyLabel>
						<ko:propertyValue>
							<select name="javaType" id="javaType">
								<option class="int-java-type" value="java.lang.Integer"<c:if test="${javaType == 'java.lang.Integer'}"> selected</c:if>>java.lang.Integer</option>
								<option class="int-java-type" value="java.lang.Long"<c:if test="${javaType == 'java.lang.Long'}"> selected</c:if>>java.lang.Long</option>
								<option class="float-java-type" value="java.lang.Double"<c:if test="${javaType == 'java.lang.Double'}"> selected</c:if>>java.lang.Double</option>
								<option class="float-java-type" value="java.math.BigDecimal"<c:if test="${javaType == 'java.math.BigDecimal'}"> selected</c:if>>java.math.BigDecimal</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-inverse-prop" cssStyle="display:none">
						<ko:propertyLabel value="Referenced property"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" id="inverseProperty" value="${inverseProperty}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-7" cssStyle="display:none">
						<ko:propertyLabel value="Enumeration values"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="enumValues" id="enumValues" rows="7"><c:if test="${field.dataType.name == 'Enumeration'}">${field.dataType.values}</c:if></textarea>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-7" cssStyle="display:none">
						<ko:propertyLabel value="Dictionary"></ko:propertyLabel>
						<ko:propertyValue>
							<input name="dictionary" id="dictionary"></input>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-10" cssStyle="display:none">
						<ko:propertyLabel value="Association object"></ko:propertyLabel>
						<ko:propertyValue>
							<select id="associationType" name="associationType">
								<option value="direct">direct</option>
								<option value="linking-type">through linking object</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-assoc-type dt-details-10-direct" cssStyle="display:none">
						<ko:propertyLabel value="Associated object"></ko:propertyLabel>
						<ko:propertyValue>
							<input id="associatedTypeId" name="associatedTypeId">
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-assoc-type dt-details-10-linking-type" cssStyle="display:none">
						<ko:propertyLabel value="Linking object"></ko:propertyLabel>
						<ko:propertyValue>
							<input id="linkingTypeId" name="linkingTypeId">
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-assoc-type dt-details-10-linking-type" cssStyle="display:none">
						<ko:propertyLabel value="Self linking field"></ko:propertyLabel>
						<ko:propertyValue id="selfLinkingFieldCell"></ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-assoc-type dt-details-10-linking-type" cssStyle="display:none">
						<ko:propertyLabel value="Foreign linking field"></ko:propertyLabel>
						<ko:propertyValue id="foreignLinkingFieldCell"></ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-collection" cssStyle="display:none">
						<ko:propertyLabel value="Display as"></ko:propertyLabel>
						<ko:propertyValue>
							<select id="collectionDisplay" name="collectionDisplay">
								<option value="inline" <c:if test="${collectionDisplay == 'inline'}">selected</c:if>>inline list</option>
								<option value="tab" <c:if test="${collectionDisplay == 'tab'}">selected</c:if>>tab</option>
							</select>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-11" cssStyle="display:none">
						<ko:propertyLabel value="Formula"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="formula" rows="7"><c:if test="${field.dataType.name == 'Formula'}">${field.dataType.userDefinition}</c:if></textarea>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="required-option-row">
						<ko:propertyLabel value="Required"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="required" value="true" <c:if test="${field.dataType.name == 'Formula'}"> disabled</c:if><c:if test="${isRequired == true}"> checked</c:if> />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="unique-option-row">
						<ko:propertyLabel value="Unique"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="unique" value="true"<c:if test="${isUnique == true}"> checked</c:if>/>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow cssClass="dt-details dt-details-7">
						<ko:propertyLabel value="Validate enumeration values"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="validateEnum" value="true" <c:if test="${field.dataType.id == 7 && field.dataType.validateValues == true}"> checked</c:if> />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Track history"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="trackHistory" value="true" <c:if test="${field.trackHistory == true}"> checked</c:if> />
						</ko:propertyValue>
					</ko:propertyRow>
					<c:if test="${empty field.KID}">
						<ko:propertyRow>
							<ko:propertyLabel value="Set as default field"></ko:propertyLabel>
							<ko:propertyValue>
								<input type="checkbox" name="isDefaultField" value="true" <c:if test="${isDefaultField == true}"> checked</c:if> />
							</ko:propertyValue>
						</ko:propertyRow>
					</c:if>
					<ko:propertyRow id="cascade-delete-option-row">
						<ko:propertyLabel value="Cascade delete"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="checkbox" name="cascadeDelete" value="true"<c:if test="${isCascadeDelete == true}"> checked</c:if>/>
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow id="text-default-value-row">
						<ko:propertyLabel value="Default value"></ko:propertyLabel>
						<ko:propertyValue>
							<input type="text" name="defaultValue" id="defaultValue" value="${field.defaultValue}" />
						</ko:propertyValue>
					</ko:propertyRow>
					<ko:propertyRow>
						<ko:propertyLabel value="Description"></ko:propertyLabel>
						<ko:propertyValue>
							<textarea name="description">${field.description}</textarea>
						</ko:propertyValue>
					</ko:propertyRow>
				</ko:propertyTable>
				
				<input type="submit" value="Save" id="saveFieldBtn"></input>
				<a href="${pageContext.request.contextPath}/km/type/${type.keyPrefix}#rm.tab.1" class="sbtn">Cancel</a>

			</form>
		
		</div>
		
	</jsp:body>
</ko:homeLayout>
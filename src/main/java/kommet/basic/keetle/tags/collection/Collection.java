/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.collection;

import java.util.Random;

import kommet.auth.AuthData;
import kommet.basic.keetle.tags.TagMode;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.env.EnvData;
import kommet.js.jsti.JSTI;
import kommet.utils.MiscUtils;

/**
 * Utility class for rendering code of the <tt>km:collection</tt> tag.
 * @author Radek Krawiec
 * @created 30/09/2014
 */
public class Collection
{
	// Field identifying the collection relationship. It has to be a field of
	// data type association or inverse collection.
	private Field relationField;
	
	// Base type on which the relation field is declared.
	private Type type;
	
	// The ID of the record on which the collection exists.
	private KID parentId;
	
	/**
	 * Generates and returns the tag code.
	 * @param mode
	 * @param env
	 * @return CollectionCode object, containing two parts of the code. Element code is a simple HTML placeholder
	 * to be put where the tag should be displayed. Initialization code is JQuery code responsible for rendering the
	 * tag with the use of the km.rel library. It should be called after the body of the page is loaded.
	 * @throws KommetException
	 */
	public CollectionCode getCode(TagMode mode, AuthData authData, EnvData env) throws KommetException
	{
		StringBuilder initializationCode = new StringBuilder();
		
		String componentIndex = String.valueOf((new Random()).nextInt(1000));
		
		// generate a random ID of this component
		String componentId = "coll-" + componentIndex;
		
		initializationCode.append("<script>$(document).ready(function() {");
		
		JSTI jsti = new JSTI();
		jsti.addType(this.type, env, true, false, authData);
		
		Type associatedType = null;
		Type linkingType = null;
		
		if (relationField.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			AssociationDataType dt = (AssociationDataType)relationField.getDataType();
			linkingType = dt.getLinkingType();
			jsti.addType(dt.getLinkingType(), env, true, false, authData);
			jsti.addType(dt.getAssociatedType(), env, true, false, authData);
			
			// fetch type from env to make sure it is up-to-date
			associatedType = env.getType(dt.getAssociatedType().getKID());
		}
		else if (relationField.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
		{
			// fetch type from env to make sure it is up-to-date
			associatedType = env.getType(((InverseCollectionDataType)relationField.getDataType()).getInverseTypeId());
			jsti.addType(associatedType, env, true, false, authData);
		}
		else
		{
			throw new KommetException("Field " + relationField.getApiName() + " is not a collection");
		}
		
		// create datasource
		initializationCode.append("var datasource = km.js.datasource.create({ type: \"database\" });\n");
		
		// add JSTI
		initializationCode.append("var jsti = ").append(JSTI.serialize(jsti)).append(";\n");
		
		// add table search configuration - let it be empty
		String tableSearchOptionsVar = "tablesearch_" + componentIndex;
		initializationCode.append("var ").append(tableSearchOptionsVar).append(" = {");
		initializationCode.append("};\n");
		
		// create options
		initializationCode.append("var options = {");
		initializationCode.append("id: \"" + componentId + "\",");
		initializationCode.append("typeId: \"" + this.type.getKID() + "\",");
		initializationCode.append("fieldId: \"" + this.relationField.getKID() + "\",");
		initializationCode.append("jsti: jsti,");
		
		if (!mode.equals(TagMode.EDIT))
		{
			// in read-only mode, do not display the add and remove buttons
			initializationCode.append("addBtn: false, ");
			initializationCode.append("removeBtn: false, ");
		}
		else
		{
			boolean canAddField = authData.canEditField(relationField, false, env);
			boolean canDeleteField = authData.canEditField(relationField, false, env);
			
			if (relationField.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				// to create an association, the user must also have rights on the linking type
				canAddField &= authData.canCreateType(linkingType.getKID(), false, env);
				canDeleteField &= authData.canDeleteType(linkingType.getKID(), false, env);
			}
			
			// whether buttons will be displayed depends on the user's permissions on the associated type
			initializationCode.append("addBtn: ").append(canAddField).append(", ");
			initializationCode.append("removeBtn: ").append(canDeleteField).append(", ");
		}
		
		initializationCode.append("recordId: \"" + this.parentId + "\",");
		initializationCode.append("selectedItemProperties: [ { id: \"" + associatedType.getDefaultFieldId() + "\"} ], ");
		
		// add optional availableItemOptions config - it could be skipped, in which case
		// only the default field of the selected item type would be included in the available item table
		initializationCode.append("availableItemsOptions: {\n");
		initializationCode.append("tableSearchOptions: ").append(tableSearchOptionsVar).append(",\n");
		initializationCode.append("title: \"" + associatedType.getInterpretedPluralLabel(authData) + "\",\n");
		initializationCode.append("properties: [");
		
		// searching by formula fields is not allowed, so if the default field is a formula field, we do not
		// add it to the search panel
		if (!associatedType.getDefaultField().getDataTypeId().equals(DataType.FORMULA))
		{
			initializationCode.append("{ id: \"").append(associatedType.getDefaultFieldId()).append("\", label: \"").append(associatedType.getDefaultFieldLabel(authData)).append("\", sortable: true, filterable: true }");
		}
		
		initializationCode.append("]");
		
		// add button panel to the available table options
		if (authData.canCreateType(associatedType.getKID(), true, env))
		{
			initializationCode.append(", ").append(getButtonPanelOptions(associatedType, componentId, env)).append(", ");
		}
		
		initializationCode.append("}");
		
		initializationCode.append("};\n");
		
		initializationCode.append("var coll = km.js.rel.create(options);\n");
		
		initializationCode.append("coll.render(function(elem) {	$(\"#" + componentId + "-wrapper\").html(elem); });");
		
		// end document ready call
		initializationCode.append("});");
		
		// close script tag
		initializationCode.append("</script>");
		
		StringBuilder elementCode = new StringBuilder();
		elementCode.append("<div id=\"").append(componentId).append("-wrapper\" class=\"km-coll-wrapper\"></div>");
		
		return new CollectionCode(initializationCode.toString(), elementCode.toString());
	}

	private String getButtonPanelOptions(Type associatedType, String relId, EnvData env) throws KommetException
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("buttonPanel: (function() {");
		sb.append("btnPanel = km.js.buttonpanel.create({ id: \"ri_" + MiscUtils.getHash(10) + "\" });");
		sb.append("btnPanel.addButton({ label: km.js.config.i18n['btn.new'], url: km.js.config.contextPath + \"/" + associatedType.getKeyPrefix().getPrefix() + "/n?km.lookup=" + relId + "&km.layout=" + env.getBlankLayoutId() + "\" });");
		sb.append("return btnPanel;})()");
				
		return sb.toString();
	}

	public void setRelationField(Field relationField)
	{
		this.relationField = relationField;
	}

	public Field getRelationField()
	{
		return relationField;
	}

	public void setParentId(KID parentId)
	{
		this.parentId = parentId;
	}

	public KID getParentId()
	{
		return parentId;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}
	
	public class CollectionCode
	{
		private String initializationCode;
		private String elementCode;
	
		public CollectionCode (String initializationCode, String elementCode)
		{
			this.initializationCode = initializationCode;
			this.elementCode = elementCode;
		}
		
		public String getInitializationCode()
		{
			return initializationCode;
		}
	
		public String getElementCode()
		{
			return elementCode;
		}
	}
}

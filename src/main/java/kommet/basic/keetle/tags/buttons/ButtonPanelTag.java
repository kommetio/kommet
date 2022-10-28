/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.buttons;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.LookupTag;
import kommet.basic.keetle.tags.ObjectDetailsTag;
import kommet.basic.keetle.tags.KommetTag;
import kommet.basic.keetle.tags.RelatedListTag;
import kommet.basic.keetle.tags.datatable.DataTableTag;
import kommet.basic.keetle.tags.objectlist.ObjectListTag;
import kommet.data.KommetException;

/**
 * Represents a button panel, either on the object list, or on a object details view.
 * @author Radek Krawiec
 *
 */
public class ButtonPanelTag extends KommetTag
{
	private static final long serialVersionUID = -7794605053999866243L;
	
	private ButtonPanel panel;

	public ButtonPanelTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		this.panel = new ButtonPanel(true);
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
    public int doEndTag() throws JspException
    {
		// get parent tag
		KommetTag parentTag = (KommetTag)findAncestorWithClass(this, KommetTag.class);
		
		if (parentTag == null)
		{
			return exitWithTagError("Misplaced buttons tag. Must be placed within either objectList or objectDetails.");
		}
		
		if (parentTag instanceof ObjectListTag)
		{
			// If buttons exist, pass them to the parent object list tag. If they don't, pass an empty
			// list to let the object list know that a button panel was placed within it, but it did not
			// contain any buttons. This will be a way to create an object list with no buttons.
			((ObjectListTag)parentTag).setButtonPanel(panel);
		}
		else if (parentTag instanceof ObjectDetailsTag)
		{
			((ObjectDetailsTag)parentTag).setButtonPanel(panel);
		}
		else if (parentTag instanceof LookupTag)
		{
			((LookupTag)parentTag).setButtonPanel(panel);
		}
		else if (parentTag instanceof DataTableTag)
		{
			((DataTableTag)parentTag).setButtonPanel(panel);
		}
		else if (parentTag instanceof RelatedListTag)
		{
			// If buttons exist, pass them to the parent related list tag. If they don't, pass an empty
			// list to let the object list know that a button panel was placed within it, but it did not
			// contain any buttons. This will be a way to create an object list with no buttons.
			((RelatedListTag)parentTag).setButtonPanel(panel);
		}
	
		cleanUp();
		return EVAL_PAGE;
    }
	
	@Override
	public void cleanUp()
	{
		this.panel = null;
	}
	
	public void addButton (ButtonPrototype button)
	{
		this.panel.addButton(button);
	}
}

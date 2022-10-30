/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.fieldhistory;

import javax.servlet.jsp.JspException;

import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KommetException;

public class FieldHistoryFieldTag extends KommetTag
{
	private static final long serialVersionUID = 8351065895439872855L;
	
	/**
	 * Field name. Must be a name of field of data type type reference, inverse collection or association.
	 */
	private String name;

	public FieldHistoryFieldTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		FieldHistoryTag parentFieldHistory = (FieldHistoryTag)findAncestorWithClass(this, FieldHistoryTag.class);
		if (parentFieldHistory == null)
		{
			return exitWithTagError("Field history field tag not placed within field history tag");
		}
		
		parentFieldHistory.addField(new FieldHistoryField(this.name));
		
		return EVAL_PAGE;
    }

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

}

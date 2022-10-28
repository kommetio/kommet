/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.ref;

import java.io.IOException;

import javax.servlet.jsp.JspException;

import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.FieldTag;
import kommet.basic.keetle.tags.MisplacedTagException;
import kommet.basic.keetle.tags.RecordContextProxy;
import kommet.basic.keetle.tags.TagErrorMessageException;
import kommet.basic.keetle.tags.ViewWrapperTag;
import kommet.data.DataService;
import kommet.data.KommetException;
import kommet.env.EnvService;
import kommet.i18n.InternationalizationService;
import kommet.utils.BeanUtils;

public class ReferenceTag extends FieldTag
{
	private static final long serialVersionUID = 6773846246814319821L;
	private AvailableItemsOptionsTag availableItemsOptions;
	private String inputName;
	
	private DataService dataService;
	private EnvService envService;
	private InternationalizationService i18n;

	public ReferenceTag() throws KommetException
	{
		super();
	}
	
	private void initRecordContext() throws KommetException
	{
		ViewWrapperTag viewWrapper = null;
		
		try
		{
			viewWrapper = this.getViewWrapper();
		}
		catch (MisplacedTagException e)
		{
			// this exception is thrown is view wrapper tag is not found
			// but in this case we can ignore it
		}
		
		try
		{
			if (viewWrapper == null)
			{
				this.dataService = (DataService)BeanUtils.getBean(DataService.class, this.pageContext.getServletContext());
				this.envService = (EnvService)BeanUtils.getBean(EnvService.class, this.pageContext.getServletContext());
				this.i18n = (InternationalizationService)BeanUtils.getBean(InternationalizationService.class, this.pageContext.getServletContext());
			}
			else
			{
				this.dataService = this.getViewWrapper().getDataService();
				this.envService = this.getViewWrapper().getEnvService();
				this.i18n = this.getViewWrapper().getI18n();
			}
		}
		catch (Exception e)
		{
			throw new KommetException("Error initializing tag: " + e.getMessage());
		}
		
		RecordContextProxy rcp = new RecordContextProxy();
		
		this.recordContext = rcp;
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		//initRecordContext();
		super.doStartTag();
		return EVAL_BODY_INCLUDE;
    }
	
	@Override
	public int doEndTag() throws JspException
	{
		StringBuilder initCode = new StringBuilder();
		
		// create datasource
		
		
		cleanUp();
		return EVAL_PAGE;
	}

	public AvailableItemsOptionsTag getAvailableItemsOptions()
	{
		return availableItemsOptions;
	}

	public void setAvailableItemsOptions(AvailableItemsOptionsTag availableItemsOptions)
	{
		this.availableItemsOptions = availableItemsOptions;
	}

	public String getInputName()
	{
		return inputName;
	}

	public void setInputName(String inputName)
	{
		this.inputName = inputName;
	}

}

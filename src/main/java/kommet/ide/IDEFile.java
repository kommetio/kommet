/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.ide;

import java.util.List;

import kommet.basic.Class;
import kommet.basic.Layout;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.utils.CodeUtils;

public class IDEFile
{
	private String name;
	private IDEFileType type;
	private Class cls;
	private View view;
	private Layout layout;
	private ViewResource viewResource;
	private List<String> collapsedSections;
	
	public IDEFile (Class cls)
	{
		this.name = cls.getName();
		this.type = IDEFileType.KOLL;
		this.cls = cls;
		
		this.collapsedSections = CodeUtils.getCollapsibleSections(cls.getKollCode());
	}
	
	public IDEFile (Layout layout)
	{
		this.name = layout.getName();
		this.type = IDEFileType.LAYOUT;
		this.layout = layout;
	}
	
	public IDEFile (View view) throws KommetException
	{
		this.name = view.getInterpretedName();
		this.type = IDEFileType.KEETLE;
		this.view = view;
	}
	
	public IDEFile (ViewResource resource) throws KommetException
	{
		this.name = resource.getName();
		this.type = IDEFileType.VIEW_RESOURCE;
		this.viewResource = resource;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setType(IDEFileType type)
	{
		this.type = type;
	}

	public IDEFileType getType()
	{
		return type;
	}

	public void setClass(Class cls)
	{
		this.cls = cls;
	}

	public Class getCls()
	{
		return cls;
	}

	public void setKeetleView(View keetleView)
	{
		this.view = keetleView;
	}

	public View getKeetleView()
	{
		return view;
	}
	
	public String getCode()
	{
		if (this.type == IDEFileType.KEETLE)
		{
			// Escape HTML entities like &sect and others. If we didn't, we would end up with all &sect
			// characters being replaced by actual section characters in the IDE code text area.
			return prepareKeetleForDisplay(this.view.getKeetleCode());
		}
		else if (this.type == IDEFileType.KOLL)
		{
			return this.cls.getKollCode();
		}
		else if (this.type == IDEFileType.LAYOUT)
		{
			return this.layout.getCode();
		}
		else if (this.type == IDEFileType.VIEW_RESOURCE)
		{
			return this.viewResource.getContent();
		}
		else
		{
			return null;
		}
	}
	
	private static String prepareKeetleForDisplay(String code)
	{
		return code!= null ? code.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;") : null;
	}
	
	public KID getId()
	{
		if (this.type == IDEFileType.KEETLE)
		{
			return this.view.getId();
		}
		else if (this.type == IDEFileType.KOLL)
		{
			return this.cls.getId();
		}
		else if (this.type == IDEFileType.LAYOUT)
		{
			return this.layout.getId();
		}
		else if (this.type == IDEFileType.VIEW_RESOURCE)
		{
			return this.viewResource.getId();
		}
		else
		{
			return null;
		}
	}

	public void setLayout(Layout layout)
	{
		this.layout = layout;
	}

	public Layout getLayout()
	{
		return layout;
	}

	public ViewResource getViewResource()
	{
		return viewResource;
	}

	public void setViewResource(ViewResource viewResource)
	{
		this.viewResource = viewResource;
	}

	public List<String> getCollapsedSections()
	{
		return collapsedSections;
	}

	public void setCollapsedSections(List<String> collapsedSections)
	{
		this.collapsedSections = collapsedSections;
	}
	
	public String getCollapsedSectionsJSON()
	{
		return CodeUtils.getCollapsibleSectionsJSON(this.collapsedSections);
	}
}
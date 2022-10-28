/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import kommet.basic.View;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class ViewFilter extends BasicFilter<View>
{
	private KID id;
	private boolean initCode = false;
	private Boolean systemView;
	private String name;
	private String qualifiedName;
	private String packageName;

	public void setKID (KID id)
	{
		this.id = id;
	}

	public KID getKID()
	{
		return id;
	}

	public void setInitCode(boolean initCode)
	{
		this.initCode = initCode;
	}

	public boolean isInitCode()
	{
		return initCode;
	}

	public void setSystemView(Boolean systemView)
	{
		this.systemView = systemView;
	}

	public Boolean getSystemView()
	{
		return systemView;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setPackage(String packageName)
	{
		this.packageName = packageName;
	}

	public String getPackage()
	{
		return packageName;
	}

	public String getQualifiedName()
	{
		return qualifiedName;
	}

	public void setQualifiedName(String qualifiedName)
	{
		this.qualifiedName = qualifiedName;
	}
}

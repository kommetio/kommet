/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import java.io.Serializable;

public abstract class BasicModel<T> implements Serializable
{
	private static final long serialVersionUID = 3790660484084578916L;
	protected T id;

	public void setId(T id)
	{
		this.id = id;
	}

	public abstract T getId();
}
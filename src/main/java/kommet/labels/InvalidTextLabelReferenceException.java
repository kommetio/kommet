/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.labels;

/**
 * Exception thrown when a non-existing text label is referenced.
 * @author Radek Krawiec
 * @created 4/06/2014
 */
public class InvalidTextLabelReferenceException extends TextLabelException
{
	private static final long serialVersionUID = 5477140747334018261L;
	private String labelKey;
	private TextLabelReference reference;
	
	public InvalidTextLabelReferenceException(String labelKey, TextLabelReference reference)
	{
		super("Trying to use a non-existing text label key " + labelKey);
		this.labelKey = labelKey;
		this.reference = reference;
	}

	public String getTextLabelKey()
	{
		return labelKey;
	}

	public TextLabelReference getReference()
	{
		return reference;
	}
}
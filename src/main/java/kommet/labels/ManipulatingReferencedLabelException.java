/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.labels;

/**
 * Exception thrown when an attempt is made to delete a text label used somewhere
 * in the system, e.g. as a validation rule message.
 * @author Radek Krawiec
 * @created 11/06/2014
 */
public class ManipulatingReferencedLabelException extends TextLabelException
{
	private static final long serialVersionUID = 7850324058934337909L;
	private TextLabelReference reference;

	public ManipulatingReferencedLabelException(String msg, TextLabelReference reference)
	{
		super(msg);
		this.reference = reference;
	}

	public TextLabelReference getReference()
	{
		return reference;
	}
}
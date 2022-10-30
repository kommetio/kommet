/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.ArrayList;

import kommet.basic.BusinessProcessInput;
import kommet.basic.BusinessProcessOutput;
import kommet.data.KID;

public interface ProcessBlock
{
	public String getName();
	public KID getId();
	public ArrayList<BusinessProcessInput> getInputs();
	public ArrayList<BusinessProcessOutput> getOutputs();
	public BusinessProcessInput getInput(String param);
	public BusinessProcessOutput getOutput(String param);
}
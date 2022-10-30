/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Label;
import kommet.basic.LabelAssignment;
import kommet.dao.LabelAssignmentDao;
import kommet.dao.LabelDao;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.LabelAssignmentFilter;
import kommet.filters.LabelFilter;

@Service
public class LabelService
{
	@Inject
	LabelDao labelDao;
	
	@Inject
	LabelAssignmentDao laDao;
	
	@Transactional(readOnly = true)
	public List<Label> get(LabelFilter filter, AuthData authData, EnvData env) throws KommetException
	{	
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			// first find label assignments
			LabelAssignmentFilter laFilter = new LabelAssignmentFilter();
			laFilter.setRecordIds(filter.getRecordIds());
			List<LabelAssignment> assignments = laDao.get(laFilter, authData, env);
			
			if (assignments.isEmpty())
			{
				return new ArrayList<Label>();
			}
			else
			{
				filter.setAssignments(assignments);
			}
		}
		
		return labelDao.get(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public Label get (String text, AuthData authData, EnvData env) throws KommetException
	{
		LabelFilter filter = new LabelFilter();
		filter.setText(text);
		List<Label> labels = labelDao.get(filter, authData, env);
		return labels.isEmpty() ? null : labels.get(0);
	}
	
	@Transactional
	public Label assign (String text, KID recordId, AuthData authData, EnvData env) throws KommetException
	{
		Label existingLabel = get(text, authData, env);
		if (existingLabel == null)
		{
			existingLabel = new Label();
			existingLabel.setText(text);
			existingLabel = labelDao.save(existingLabel, authData, env);
		}
		else
		{
			LabelAssignmentFilter filter = new LabelAssignmentFilter();
			filter.addLabelId(existingLabel.getId());
			filter.addRecordId(recordId);
			List<LabelAssignment> assignments = laDao.get(filter, authData, env);
			
			if (!assignments.isEmpty())
			{
				// label already assigned to this record
				return existingLabel;
			}
		}
		
		LabelAssignment assignment = new LabelAssignment();
		assignment.setLabel(existingLabel);
		assignment.setRecordId(recordId);
		laDao.save(assignment, authData, env);
		
		return existingLabel;
	}
}
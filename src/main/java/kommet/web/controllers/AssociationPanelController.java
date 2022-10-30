/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kommet.basic.keetle.tags.TagMode;
import kommet.basic.keetle.tags.associationpanel.AssociationPanel;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class AssociationPanelController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/associationpanel", method = RequestMethod.POST)
	@ResponseBody
	public void render (@RequestParam("associationFieldId") String associationFieldId,
								@RequestParam("recordId") String sRecordId,
								@RequestParam("addAction") String addAction,
								@RequestParam("viewAction") String viewAction,
								@RequestParam("refreshedPanelId") String refreshedPanelId,
								@RequestParam("displayFields") String displayFields, HttpSession session,
								HttpServletResponse resp) throws KommetException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);
		AssociationPanel panel = new AssociationPanel(env);
		panel.setDisplayFields(MiscUtils.splitAndTrim(displayFields, ","));
		panel.setAddActionURL(addAction);
		panel.setViewActionURL(viewAction);
		panel.setRefreshedPanelId(refreshedPanelId);
		
		KID recordId = KID.get(sRecordId);
		panel.setRecordId(recordId);
		
		Type type = env.getTypeByRecordId(recordId);
		panel.setAssociationField(type.getField(KID.get(associationFieldId)).getApiName());
		
		panel.setType(type.getQualifiedName());
		
		resp.getWriter().write(panel.getCode(null, TagMode.EDIT, session.getServletContext().getContextPath()));
	}
}
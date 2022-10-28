/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.ide;

import java.util.Collection;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import kommet.data.KID;

@Service
public class IDEService
{
	private static final String IDE_INFO_SESSION_KEY = "ideinfo";
	
	public static IDEInfo getInfo(HttpSession session)
	{
		if (session.getAttribute(IDE_INFO_SESSION_KEY) == null)
		{
			session.setAttribute(IDE_INFO_SESSION_KEY, new IDEInfo());
		}
		return (IDEInfo)session.getAttribute(IDE_INFO_SESSION_KEY);
	}

	public static Collection<IDEFile> getOpenFiles(HttpSession session)
	{
		return getInfo(session).getFiles().values();
	}

	public static void openFile(IDEFile file, HttpSession session)
	{
		IDEInfo info = getInfo(session);
		info.addFile(file);
		session.setAttribute(IDE_INFO_SESSION_KEY, info);
		setCurrentFile(file.getId(), session);
	}

	public static void refreshFile(IDEFile file, HttpSession session)
	{
		IDEInfo info = getInfo(session);
		info.reAddFile(file);
		session.setAttribute(IDE_INFO_SESSION_KEY, info);
	}
	
	public static IDEFile getCurrentFile(HttpSession session)
	{
		return getInfo(session).getCurrentFile();
	}

	public static void setCurrentFile(KID fileId, HttpSession session)
	{
		getInfo(session).setCurrentFileId(fileId);
	}

	public static void closeFile(KID fileId, HttpSession session)
	{
		IDEInfo info = getInfo(session);
		info.removeFile(fileId);
		session.setAttribute(IDE_INFO_SESSION_KEY, info);
		setCurrentFile(null, session);
	}

	public static boolean hasOpenFiles(HttpSession session)
	{
		return !getInfo(session).getFiles().isEmpty();
	}

	public static boolean isOpenFile(KID fileId, HttpSession session)
	{
		return getInfo(session).getFiles().containsKey(fileId);
	}
}
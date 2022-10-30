/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.notifications;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Notification;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;

@Service
public class NotificationService
{
	@Inject
	NotificationDao dao;
	
	@Inject
	SharingService sharingService;
	
	@Transactional(readOnly = true)
	public List<Notification> get (NotificationFilter filter, Collection<String> additionalQueriedProperties, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(filter, additionalQueriedProperties, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<Notification> get (NotificationFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(filter, null, authData, env);
	}
	
	@Transactional(readOnly = true)
	public Notification get (KID id, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(id, null, authData, env);
	}
	
	@Transactional
	public Notification save (Notification notification, AuthData authData, EnvData env) throws KommetException
	{
		Notification savedNotification = dao.save(notification, authData, env);
		
		// share this notification with the user it is assigned to
		sharingService.shareRecord(savedNotification.getId(), notification.getAssignee().getId(), authData, "Notification assignee", false, env);
		
		return savedNotification;
	}
	
	@Transactional
	public void delete (Notification notification, AuthData authData, EnvData env) throws KommetException
	{
		dao.delete(notification.getId(), authData, env);
	}
	
	@Transactional
	public void setViewedDate (KID notificationId, Date date, AuthData authData, EnvData env) throws KommetException
	{
		Notification notification = dao.get(notificationId, null, authData, env);
		if (notification == null)
		{
			throw new NotificationException("No notification with ID " + notificationId + " found");
		}
		notification.setViewedDate(date);
		dao.save(notification, authData, env);
	}
}
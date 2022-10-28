var notifications = {

	contextPath: null,
	title: null,
	noNotificationsMsg: null,
	
	init: function(path, title, noNotificationsMsg) {
		this.contextPath = path;
		this.title = title;
		this.noNotificationsMsg = noNotificationsMsg;
	},
		
	deleteNotification: function (id) {
		$("#notifications #notif-" + id).remove();
		
		$.post(km.js.config.sysContextPath + "/notifications/delete", { id: id }, function(data) {
		}, "json");
	},
	
	setNotificationViewed: function(id) {
		$("#notifications #notif-" + id).removeClass("unread-notification");
		
		$.post(km.js.config.sysContextPath + "/notifications/setviewed", { id: id }, function(data) {
			if (data.status == "success")
			{
				// do nothing
			}
			else
			{
				showMsg("notifications", "Could not set notification as viewed", "error", null);
			}
		}, "json");
	},
	
	notificationRenderFunction: function($container, contextPath, title, noNotifMsg) {
		
		return function(data) {
	
			if (data.status == "success")
			{
				html = "<div class=\"nt-list\"><h4 class=\"title\">" + title;
				html += "<span class=\"nt-count\">(" + data.data.length + ")</span>";
				html += "</h4>";
				
				if (data.data.length > 0)
				{
					html += "<div class=\"nt-list-items\">";
					for (i = 0; i < data.data.length; i++)
					{
						nt = data.data[i];
						html += "<div id=\"notif-" + nt.id + "\" class=\"notification ";
						if (nt.viewedDate == null)
						{
							html += " unread-notification";
						}
						html += "\">";
						
						html += "<div class=\"title-bar\">";
						html += "<span class=\"title\" onclick=\"notifications.setNotificationViewed('" + nt.id + "');\">" + nt.title + "</span>";
						html += "<span class=\"del\" onclick=\"notifications.deleteNotification('" + nt.id + "')\"><img src=\"" + contextPath + "/resources/images/ex.png\"></span>";
						html += "</div>";
						
						html += "<div class=\"text\" onclick=\"notifications.setNotificationViewed('" + nt.id + "');\">" + nt.text + "</div>";
						if (nt.createdDate)
						{
							html += "<div class=\"date\">" + nt.createdDate + "</div>";
						}
						html += "</div>"; 
					}
					html += "</div>";
				}
				else
				{
					html += "<div class=\"notif-msg\">" + noNotifMsg + "</div>";
				}
				
				html += "</div>";
		
				$container.html(html);
			}
			else
			{
				showMsg("notifications", "Could not retrieve notifications", "error", null);
			} 
			
		}
	},

	getNotifications: function ($container) {
		$.get(km.js.config.sysContextPath + "/notifications/me", { unreadfirst: 1 }, this.notificationRenderFunction($container, this.contextPath, this.title, this.noNotificationsMsg), "json");
	}
}
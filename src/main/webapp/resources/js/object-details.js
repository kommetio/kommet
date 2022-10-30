function deleteRecord(id, sysContextPath, contextPath)
{
	$.post(sysContextPath + "/delete", { "id": id }, function(data) {
		if (data.status == "success")
		{
			openUrl(contextPath + "/" + id.substring(0,3));
		}
		else
		{
			showMsg("warnPrompt", data.messages, "error", null);
		}
	}, "json");
}